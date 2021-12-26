package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.elem.IntersectionResult;
import org.redukti.rayoptics.elem.Transform;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.exceptions.TraceTIRException;
import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SeqPathComponent;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class RayTrace {

    /**
     * fundamental raytrace function
     *
     *     Args:
     *         seq_model: the sequential model to be traced
     *         pt0: starting point in coords of first interface
     *         dir0: starting direction cosines in coords of first interface
     *         wvl: wavelength in nm
     *         eps: accuracy tolerance for surface intersection calculation
     *
     *     Returns:
     *         (**ray**, **op_delta**, **wvl**)
     *
     *         - **ray** is a list for each interface in **path_pkg** of these
     *           elements: [pt, after_dir, after_dst, normal]
     *
     *             - pt: the intersection point of the ray
     *             - after_dir: the ray direction cosine following the interface
     *             - after_dst: after_dst: the geometric distance to the next
     *               interface
     *             - normal: the surface normal at the intersection point
     *
     *         - **op_delta** - optical path wrt equally inclined chords to the
     *           optical axis
     *         - **wvl** - wavelength (in nm) that the ray was traced in
     * @param seq_model
     * @param pt0
     * @param dir0
     * @param wvl
     */
    public static RayPkg trace(SequentialModel seq_model, Vector3 pt0, Vector3 dir0, double wvl) {
        List<SeqPathComponent> path = seq_model.path(wvl, null, null, 1);
        RayTraceOptions options = new RayTraceOptions();
        options.first_surf = 1;
        options.last_surf = seq_model.get_num_surfaces()-2;
        return trace_raw(path, pt0, dir0, wvl, options);
    }

    private static boolean in_surface_range(int first_surf, Integer last_surf, int s, boolean include_last_surf) {
        if (last_surf != null && first_surf == last_surf)
            return false;
        if (s < first_surf)
            return false;
        if (last_surf == null)
            return true;
        else
            return include_last_surf ? s <= last_surf : s < last_surf;
    }

    /**
     * calculate equally inclined chord distance between a ray and the axis
     *
     *     Args:
     *         r: (p, d), where p is a point on the ray r and d is the direction
     *            cosine of r
     *         z_dir: direction of propagation of ray segment, +1 or -1
     *
     *     Returns:
     *         float: distance along r from equally inclined chord point to p
     * @param p
     * @param d
     * @param z_dir
     * @return
     */
    private static double eic_distance_from_axis(Vector3 p, Vector3 d, ZDir z_dir) {
        // eq 3.20/3.21
        double e = ((p.dot(d) + z_dir.value * p.z) /
                (1.0 + z_dir.value * d.z));
        return e;
    }

    /**
     * fundamental raytrace function
     *
     *     Args:
     *         path: an iterator containing interfaces and gaps to be traced.
     *               for each iteration, the sequence or generator should return a
     *               list containing: **Intfc, Gap, Trfm, Index, Z_Dir**
     *         pt0: starting point in coords of first interface
     *         dir0: starting direction cosines in coords of first interface
     *         wvl: wavelength in nm
     *         eps: accuracy tolerance for surface intersection calculation
     *
     *     Returns:
     *         (**ray**, **op_delta**, **wvl**)
     *
     *         - **ray** is a list for each interface in **path** of these
     *           elements: [pt, after_dir, after_dst, normal]
     *
     *             - pt: the intersection point of the ray
     *             - after_dir: the ray direction cosine following the interface
     *             - after_dst: the geometric distance to the next interface
     *             - normal: the surface normal at the intersection point
     *
     *         - **op_delta** - optical path wrt equally inclined chords to the
     *           optical axis
     *         - **wvl** - wavelength (in nm) that the ray was traced in
     *
     * @param path
     * @param pt0
     * @param dir0
     * @param wvl
     * @param options
     * @return
     */
    private static RayPkg trace_raw(List<SeqPathComponent> path, Vector3 pt0, Vector3 dir0, double wvl, RayTraceOptions options) {
        int first_surf = options.first_surf != null ? options.first_surf : 0;
        Integer last_surf = options.last_surf;

        List<RaySeg> ray = new ArrayList<>();
        List<double[]> eic = new ArrayList<>();

        // trace object surface
        Iterator<SeqPathComponent> iter = path.iterator();
        SeqPathComponent obj = iter.next();
        Interface srf_obj = obj.ifc;
        IntersectionResult intersection = srf_obj.intersect(pt0, dir0, 1.0e-12, obj.z_dir);
        double dst_b4 = intersection.distance;
        Vector3 pt_obj = intersection.intersection_point;

        SeqPathComponent before = obj;
        Vector3 before_pt = pt_obj;
        Vector3 before_dir = dir0;
        Vector3 before_normal = srf_obj.normal(before_pt);
        Transform3 tfrm_from_before = before.transform3;
        ZDir z_dir_before = before.z_dir;

        double op_delta = 0.0;
        double opl = 0.0;
        double opl_eic = 0.0;
        int surf = 0;

        Vector3 inc_pt = null;
        Vector3 after_dir = null;
        Vector3 normal = null;

        // loop of remaining surfaces in path
        while (true) {

            try {
                SeqPathComponent after = iter.next();
                Matrix3 rt = tfrm_from_before.rot_mat;
                Vector3 t = tfrm_from_before.vec;
                Vector3 b4_pt = rt.multiply(before_pt.minus(t));
                Vector3 b4_dir = rt.multiply(before_dir);
                double pp_dst = -b4_pt.dot(b4_dir);
                Vector3 pp_pt_before = b4_pt.plus(b4_dir.times(pp_dst));

                Interface ifc = after.ifc;
                ZDir z_dir_after = after.z_dir;

                // intersect ray with profile
                intersection = ifc.intersect(pp_pt_before, b4_dir, options.eps, z_dir_before);
                double pp_dst_intrsct = intersection.distance;
                inc_pt = intersection.intersection_point;
                dst_b4 = pp_dst + pp_dst_intrsct;
                ray.add(new RaySeg(before_pt, before_dir, dst_b4, before_normal));

                if (in_surface_range(first_surf, last_surf, surf, false))
                    opl += before.rndx * dst_b4;

                normal = ifc.normal(inc_pt);
                double eic_dst_before = eic_distance_from_axis(inc_pt, b4_dir, z_dir_before);

                /*
                # if the interface has a phase element, process that first
                if hasattr(ifc, 'phase_element'):
                    doe_dir, phs = phase(ifc, inc_pt, b4_dir, normal, z_dir_before,
                                     wvl, before[Indx], after[Indx])
                    # the output of the phase element becomes the input for the
                    #  refraction/reflection calculation
                    b4_dir = doe_dir
                    op_delta += phs
                 */

                // refract or reflect ray at interface
                if (ifc.interact_mode.equals("reflect"))
                    after_dir = reflect(b4_dir, normal);
                else if (ifc.interact_mode.equals("transmit"))
                    after_dir = bend(b4_dir, normal, before.rndx, after.rndx);
                else if (ifc.interact_mode.equals("dummy"))
                    after_dir = b4_dir;
                else // no action, input becomes output
                    after_dir = b4_dir;

                surf += 1;

                // Per `Hopkins, 1981 <https://dx.doi.org/10.1080/713820605>`_, the
                //  propagation direction is given by the direction cosines of the
                //  ray and therefore doesn't require the use of a negated
                //  refractive index following a reflection. Thus we use the
                //  (positive) refractive indices from the seq_model.rndx array.

                if (!ifc.interact_mode.equals("dummy")) {
                    double eic_dst_after = eic_distance_from_axis(inc_pt, after_dir, z_dir_after);
                    double dW = after.rndx*eic_dst_after - before.rndx*eic_dst_before;
                    eic.add(new double[]{before.rndx, eic_dst_before,
                            after.rndx, eic_dst_after, dW});
                    if (in_surface_range(first_surf, last_surf, surf, true))
                        opl_eic += dW;
                    /*
                                    if print_details:
                    print("after:", surf, inc_pt, after_dir)
                    print("e{}= {:12.5g} e{}'= {:12.5g} dW={:10.8g} n={:8.5g}"
                          " n'={:8.5g} zdb4={:2.0f} zdaft={:2.0f}"
                          .format(surf, eic_dst_before, surf, eic_dst_after,
                                  dW, before[Indx], after[Indx],
                                  z_dir_before, z_dir_after))
                     */
                }

                before_pt = inc_pt;
                before_normal = normal;
                before_dir = after_dir;
                z_dir_before = z_dir_after;
                before = after;
                tfrm_from_before = before.transform3;
            }
            catch (TraceMissedSurfaceException ray_miss) {
                //ray.add([before_pt, before_dir, pp_dst, before_normal])
//                ray_miss.surf = surf+1
//                ray_miss.ifc = ifc
//                ray_miss.prev_tfrm = before[Tfrm]
//                ray_miss.ray_pkg = ray, opl, wvl
                throw ray_miss;
            }
            catch (TraceTIRException ray_tir) {
//                ray.append([inc_pt, before_dir, 0.0, normal])
//                ray_tir.surf = surf+1
//                ray_tir.ifc = ifc
//                ray_tir.int_pt = inc_pt
//                ray_tir.ray_pkg = ray, opl, wvl
                throw ray_tir;
            }
            catch (NoSuchElementException e) {
                ray.add(new RaySeg(inc_pt, after_dir, 0.0, normal));
                op_delta += opl;
                break;
            }
        }
        return new RayPkg(ray, op_delta, wvl);
    }

    /**
     * refract incoming direction, d_in, about normal
     * @param d_in
     * @param normal
     * @param n_in
     * @param n_out
     * @return
     */
    private static Vector3 bend(Vector3 d_in, Vector3 normal, Double n_in, Double n_out) {
        double normal_len = normal.length();
        double cosI = d_in.dot(normal) / normal_len;
        double sinI_sqr = 1.0 - cosI * cosI;
        double sqrrt_in = n_out*n_out - n_in*n_in*sinI_sqr;
        if (sqrrt_in <= 0)
            throw new TraceTIRException(d_in, normal, n_in, n_out);
        double sqrrt = Math.sqrt(sqrrt_in);
        double n_cosIp = cosI > 0 ? sqrrt : -sqrrt;
        double alpha = n_cosIp - n_in*cosI;
        Vector3 d_out = (d_in.times(n_in).plus(normal.times(alpha))).times(1.0 / n_out);
        return d_out;
    }

    /**
     * reflect incoming direction, d_in, about normal
     * @param d_in
     * @param normal
     * @return
     */
    private static Vector3 reflect(Vector3 d_in, Vector3 normal) {
        double normal_len = normal.length();
        double cosI = d_in.dot(normal) / normal_len;
        Vector3 d_out = d_in.minus(normal.times(2.0 * cosI));
        return d_out;
    }

    public static final class TransferResults {
        public Vector3 exp_pt;
        public Vector3 exp_dir;
        public double exp_dst;
        public Interface ifc;
        public Vector3 b4_pt;
        public Vector3 b4_dir;

        public TransferResults(Vector3 exp_pt, Vector3 exp_dir, double exp_dst, Interface ifc, Vector3 b4_pt, Vector3 b4_dir) {
            this.exp_pt = exp_pt;
            this.exp_dir = exp_dir;
            this.exp_dst = exp_dst;
            this.ifc = ifc;
            this.b4_pt = b4_pt;
            this.b4_dir = b4_dir;
        }
    }

    /**
     * Given the exiting interface and chief ray data, return exit pupil ray coords.
     *
     *     Args:
     *         interface: the exiting :class:'~.Interface' for the path sequence
     *         ray_seg: ray segment exiting from **interface**
     *         exp_dst_parax: z distance to the paraxial exit pupil
     *
     *     Returns:
     *         (**exp_pt**, **exp_dir**, **exp_dst**)
     *
     *         - **exp_pt** - ray intersection with exit pupil plane
     *         - **exp_dir** - direction cosine of the ray in exit pupil space
     *         - **exp_dst** - distance from interface to exit pupil pt
     * @param ifc
     * @param ray_seg
     * @param exp_dst_parax
     */
    public static TransferResults transfer_to_exit_pupil(Interface ifc, Ray ray_seg, double exp_dst_parax) {
        Ray b4_ray = Transform.transform_after_surface(ifc, ray_seg);
        Vector3 b4_pt = b4_ray.p;
        Vector3 b4_dir = b4_ray.d;

        // h = b4_pt[0]**2 + b4_pt[1]**2
        // u = b4_dir[0]**2 + b4_dir[1]**2
        // handle field points in the YZ plane

        double h = b4_pt.y;
        double u = b4_dir.y;
        double exp_dst;
        if (Math.abs(u) < 1e-14) {
            exp_dst = exp_dst_parax;
        }
        else {
            // exp_dst = -np.sign(b4_dir[2])*sqrt(h/u)
            exp_dst = -h/u;
        }
        Vector3 exp_pt = b4_pt.plus(b4_dir.times(exp_dst));
        Vector3 exp_dir = b4_dir;

        return new TransferResults(exp_pt, exp_dir, exp_dst, ifc, b4_pt, b4_dir);
    }
}
