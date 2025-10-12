// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.surface.IntersectionResult;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.exceptions.TraceRayBlockedException;
import org.redukti.rayoptics.exceptions.TraceTIRException;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.seq.InteractMode;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.PathSeg;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class RayTrace {


    /**
     * refract incoming direction, d_in, about normal
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
     */
    public static RayPkg trace(SequentialModel seq_model, Vector3 pt0, Vector3 dir0, double wvl, RayTraceOptions options) {
        List<PathSeg> path = seq_model.path(wvl, null, null, 1);
        if (options.first_surf == null) options.first_surf = 1;
        if (options.last_surf == null) options.last_surf = seq_model.get_num_surfaces()-2;
        return trace_raw(path, pt0, dir0, wvl, options);
    }

    public static RayPkg trace(SequentialModel seq_model, Vector3 pt0, Vector3 dir0, double wvl) {
        RayTraceOptions options = new RayTraceOptions();
        options.first_surf = 1;
        options.last_surf = seq_model.get_num_surfaces()-2;
        return trace(seq_model, pt0, dir0, wvl, options);
    }

    private static boolean in_gap_range(int first_surf, Integer last_surf, int gap_indx, boolean include_last_surf) {
        if (last_surf != null && first_surf == last_surf)
            return false;
        if (gap_indx < first_surf)
            return false;
        if (last_surf == null)
            return true;
        else
            return include_last_surf ? gap_indx <= last_surf : gap_indx < last_surf;
    }

    private static boolean in_surface_range(int first_surf, Integer last_surf, int s) {
        if (s < first_surf)
            return false;
        if (last_surf == null)
            return true;
        else if (s > last_surf)
            return false;
        else
            return true;
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
     * Fundamental raytrace function
     *
     * @param path    a list containing interfaces and gaps to be traced.
     *                each component contains: Intfc, Gap, Trfm, Index, Z_Dir
     * @param pt0     starting point in coords of first interface
     * @param dir0    starting direction cosines in coords of first interface
     * @param wvl     wavelength in nm
     * @param options Options
     * @return RayPkg containing
     * - **ray** is a list for each interface in **path** of these
     * elements: [pt, after_dir, after_dst, normal]
     * <p>
     * - pt: the intersection point of the ray
     * - after_dir: the ray direction cosine following the interface
     * - after_dst: the geometric distance to the next interface
     * - normal: the surface normal at the intersection point
     * <p>
     * - **op_delta** - optical path wrt equally inclined chords to the optical axis
     * - **wvl** - wavelength (in nm) that the ray was traced in
     */
    public static RayPkg trace_raw(List<PathSeg> path, Vector3 pt0, Vector3 dir0, double wvl, RayTraceOptions options) {
        int first_surf = options.first_surf != null ? options.first_surf : 0;
        Integer last_surf = options.last_surf;

        List<RaySeg> ray = new ArrayList<>();

        Iterator<PathSeg> iter = path.iterator();

        // trace object surface
        PathSeg obj = iter.next();
        PathSeg before = obj;
        InteractMode b4_interact_mode = InteractMode.DUMMY;
        Vector3 before_pt;
        Vector3 before_normal;
        if (options.intersect_obj) {
            Interface srf_obj = obj.ifc;
            b4_interact_mode = srf_obj.interact_mode;
            IntersectionResult intersection = srf_obj.intersect(pt0, dir0, options.eps, obj.Zdir);
            before_pt = intersection.intersection_point;
            before_normal = srf_obj.normal(before_pt);
        }
        else {
            before_pt = pt0;
            before_normal = Vector3.vector3_001;
        }

        Vector3 before_dir = dir0;
        Tfm3d tfrm_from_before = before.Tfrm;
        ZDir z_dir_before = before.Zdir;

        double op_delta = 0.0;
        double opl = 0.0;
        int surf = 0;

        Vector3 inc_pt = null;
        Vector3 after_dir = null;
        Vector3 normal = null;

        // loop of remaining surfaces in path
        while (true) {

            double pp_dst = 0.0;
            Interface ifc = null;
            try {
                PathSeg after = iter.next();
                surf += 1;

                Matrix3 rt = tfrm_from_before.rt;
                Vector3 t = tfrm_from_before.t;
                Vector3 b4_pt = rt.multiply(before_pt.minus(t));
                Vector3 b4_dir = rt.multiply(before_dir);

                pp_dst = -b4_pt.dot(b4_dir);
                Vector3 pp_pt_before = b4_pt.plus(b4_dir.times(pp_dst));

                ifc = after.ifc;
                InteractMode interact_mode = ifc.interact_mode;
                ZDir z_dir_after = after.Zdir;

                // intersect ray with profile
                IntersectionResult intersection = ifc.intersect(pp_pt_before, b4_dir, options.eps, z_dir_before);
                double pp_dst_intrsct = intersection.distance;
                inc_pt = intersection.intersection_point;
                double dst_b4 = pp_dst + pp_dst_intrsct;

                if (b4_interact_mode == InteractMode.PHANTOM && options.filter_out_phantoms) {
                    // if a phantom interface, don't add intersection point
                    // but do add the path length.
                    ray.get(ray.size()-1).dst += dst_b4;
                }
                else {
                    // add *previous* intersection point, direction, etc., to ray
                    ray.add(new RaySeg(before_pt, before_dir, dst_b4, before_normal));
                }

                if (in_gap_range(first_surf,last_surf,surf-1,false))
                    opl += before.Indx * dst_b4;

                normal = ifc.normal(inc_pt);

                if (options.check_apertures &&
                    in_surface_range(first_surf,last_surf,surf) &&
                        interact_mode != InteractMode.PHANTOM) {
                    if (options.pt_inside_fuzz != null && !ifc.point_inside(inc_pt.x, inc_pt.y, options.pt_inside_fuzz))
                        throw new TraceRayBlockedException(ifc, inc_pt);
                }

                /*
                # if present, use the phase element to calculate after_dir
                if hasattr(ifc, 'phase_element'):
                    ifc_cntxt = (z_dir_before, wvl,
                                 before[mc.Indx], after[mc.Indx],
                                 interact_mode)
                    after_dir, phs = phase(ifc, inc_pt, b4_dir, normal, ifc_cntxt)
                    op_delta += phs
                else:
                 */

                // refract or reflect ray at interface
                if (interact_mode == InteractMode.REFLECT)
                    after_dir = reflect(b4_dir, normal);
                else if (interact_mode == InteractMode.TRANSMIT)
                    after_dir = bend(b4_dir, normal, before.Indx, after.Indx);
                else if (interact_mode == InteractMode.DUMMY)
                    after_dir = b4_dir;
                else if (interact_mode == InteractMode.PHANTOM)
                    after_dir = b4_dir;
                else // no action, input becomes output
                    after_dir = b4_dir;

                before_pt = inc_pt;
                before_normal = normal;
                before_dir = after_dir;
                z_dir_before = z_dir_after;
                b4_interact_mode = interact_mode;
                before = after;
                tfrm_from_before = before.Tfrm;

            } catch (TraceMissedSurfaceException ray_miss) {
                ray.add(new RaySeg(before_pt, before_dir, pp_dst, before_normal));
                ray_miss.surf = surf;
                ray_miss.ifc = ifc;
                ray_miss.prev_tfrm = before.Tfrm;
                ray_miss.ray_pkg = new RayPkg(ray, opl, wvl);
                throw ray_miss;

            } catch (TraceTIRException ray_tir) {
                ray.add(new RaySeg(inc_pt, before_dir, 0.0, normal));
                ray_tir.surf = surf;
                ray_tir.ifc = ifc;
                ray_tir.int_pt = inc_pt;
                ray_tir.ray_pkg = new RayPkg(ray, opl, wvl);
                throw ray_tir;

            } catch (TraceRayBlockedException ray_blocked) {
                ray.add(new RaySeg(inc_pt, before_dir, 0.0, normal));
                ray_blocked.surf = surf;
                ray_blocked.ray_pkg = new RayPkg(ray, opl, wvl);
                throw ray_blocked;

            } catch (NoSuchElementException e) {
                ray.add(new RaySeg(inc_pt, after_dir, 0.0, normal));
                op_delta += opl;
                break;
            }
        }
        return new RayPkg(ray, op_delta, wvl);
    }

}
