// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

public class WaveAbr {

    /**
     * Compute the reference sphere for a defocussed image point at **fld**.
     *
     *         The local transform from the final interface to the image interface is
     *         included to facilitate infinite refernce sphere calculations.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *         chief_ray_pkg: input tuple of chief_ray, cr_exp_seg
     *         image_pt_2d: x, y image point in (defocussed) image plane, if None, use
     *                      the chief ray coordinate.
     *         image_delta: x, y displacements from image_pt_2d in (defocussed)
     *                      image plane, if not None.
     *
     *     Returns:
     *         ref_sphere: tuple of image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last
     */
    public static ReferenceSphere calculate_reference_sphere(
            OpticalModel opt_model,
            Field fld,
            double wvl,
            double foc,
            ChiefRayPkg chief_ray_pkg,
            Vector2 image_pt_2d,
            Vector2 image_delta) {

        var cr = chief_ray_pkg.chief_ray;
        var cr_exp_seg = chief_ray_pkg.cr_exp_seg;

        Vector3 image_pt;
        if (image_pt_2d == null) {
            // get distance along cr corresponding to a z shift of the defocus
            var dist = foc / Lists.get(cr.ray,-1).d.z;
            image_pt = Lists.get(cr.ray,-1).p.plus(Lists.get(cr.ray,-1).d.times(dist));
        }
        else {
            image_pt = new Vector3(image_pt_2d.x, image_pt_2d.y, foc);
        }
        if (image_delta != null)
            image_pt = new Vector3(image_pt.x+image_delta.x, image_pt.y+image_delta.y, image_pt.z);

        // get the image point wrt the final surface
        var seq_model = opt_model.seq_model;
        var lcl_tfrm_last = Lists.get(seq_model.lcl_tfrms,-2);
        var image_thi = Lists.get(seq_model.gaps,-1).thi;
        var img_pt = new Vector3(image_pt.x, image_pt.y, image_pt.z+image_thi);

        // R' radius of reference sphere for O'
        var ref_sphere_vec = img_pt.minus(cr_exp_seg.exp_pt); //p=exp_pt
        var ref_sphere_radius = ref_sphere_vec.length();
        var ref_dir = ref_sphere_vec.normalize();

        return new ReferenceSphere(image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last);
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
     *         - **interface** - exiting :class:'~.Interface' for the path sequence
     *         - **b4_pt** - ray intersection pt wrt image gap coordinates
     *         - **b4_dir** - ray direction cosine wrt image gap coordinates
     */
    public static ChiefRayExitPupilSegment transfer_to_exit_pupil(
            Interface ifc,
            RayData ray_seg,
            double exp_dst_parax) {
        RayData b4_ray = Transform.transform_after_surface(ifc, ray_seg);
        Vector3 b4_pt = b4_ray.pt;
        Vector3 b4_dir = b4_ray.dir;

        // h = b4_pt[0]**2 + b4_pt[1]**2
        // u = b4_dir[0]**2 + b4_dir[1]**2
        // handle field points in the YZ plane

        double h = b4_pt.y;     // y=1
        double u = b4_dir.y;    // y=1
        double exp_dst;
        if (Math.abs(u) < 1e-14) {
            exp_dst = exp_dst_parax;
        } else {
            // exp_dst = -np.sign(b4_dir[2])*sqrt(h/u)
            exp_dst = -h / u;
        }
        Vector3 exp_pt = b4_pt.plus(b4_dir.times(exp_dst));
        Vector3 exp_dir = b4_dir;

        return new ChiefRayExitPupilSegment(exp_pt, exp_dir, exp_dst, ifc, b4_pt, b4_dir);
    }

    /**
     * calculate equally inclined chord distance between 2 rays
     * <p>
     * Args:
     * r: (p, d), where p is a point on the ray r and d is the direction
     * cosine of r
     * r0: (p0, d0), where p0 is a point on the ray r0 and d0 is the direction
     * cosine of r0
     * <p>
     * Returns:
     * float: distance along r from equally inclined chord point to p
     *
     * @param r
     * @param r0
     * @return
     */
    public static double eic_distance(RayData r, RayData r0) {
        // eq 3.9 Hopkins paper
        double e = (r.dir.plus(r0.dir).dot(r.pt.minus(r0.pt))) /
                (1. + r.dir.dot(r0.dir));
        return e;
    }

    /**
     * compute distance along ray to perpendicular to the origin.
     *
     *     Args:
     *         p, d: a ray, defined by point p and unit direction d
     *
     *     Returns:
     *         t: distance from p to perpendicular to the origin
     */
    public static double ray_dist_to_perp_from_origin(RayData r) {
        var p = r.pt;
        var d = r.dir;
        return d.dot(p.negate());
    }

    /**
     * compute distance to pts at the closest join between 2 rays.
     *
     *     Args:
     *         r1: ray 1, defined by point p1 and unit direction d1
     *         r2: ray 2, defined by point p2 and unit direction d2
     *
     *     Returns: (p1_min, t1), (p2_min, t2)
     *         p1_min: point on ray 1 at the closest join
     *         t1: distance from p1 to p1_min
     *         p2_min: point on ray 2 at the closest join
     *         t2: distance from p2 to p2_min
     */
    public static Pair<Pair<Vector3,Double>, Pair<Vector3,Double>> dist_to_shortest_join(RayData r1, RayData r2) {
        var p1 = r1.pt;
        var d1 = r1.dir;
        var p2 = r2.pt;
        var d2 = r2.dir;
        var del_p = p2.minus(p1);
        var n = d1.cross(d2);
        var nn = n.dot(n);
        if (nn == 0.0) {
            var t2 = p1.minus(p2).dot(d1) * d1.dot(d2);
            var p2_min = p2.plus(d2.times(t2));
            return new Pair<>(new Pair<>(p1,0.0),new Pair<>(p2_min,t2));
        }
        else {
            var t1 = d2.cross(n).dot(del_p) / nn;
            var t2 = d1.cross(n).dot(del_p) / nn;
            var p1_min = p1.plus(d1.times(t1));
            var p2_min = p2.plus(d2.times(t2));
            return new Pair<>(new Pair<>(p1_min,t1),new Pair<>(p2_min,t2));
        }
    }

    /**
     * Given a ray, a chief ray and an image pt, evaluate the OPD.
     *
     *     The main references for the calculations are in the H. H. Hopkins paper
     *     `Calculation of the Aberrations and Image Assessment for a General Optical
     *     System <https://doi.org/10.1080/713820605>`_
     *
     *     Args:
     *         fod: :class:`~.FirstOrderData` for object and image space refractive
     *              indices
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *         ray_pkg: input tuple of ray, ray_op, wvl
     *         chief_ray_pkg: input tuple of chief_ray, cr_exp_seg
     *         ref_sphere: input tuple of image_pt, ref_dir, ref_sphere_radius
     *
     *     Returns:
     *         opd: OPD of ray wrt chief ray at **fld**
     */
    public static double wave_abr_full_calc(FirstOrderData fod,Field fld,double wvl,double foc,RayPkg ray_pkg,ChiefRayPkg chief_ray_pkg,ReferenceSphere ref_sphere) {
        if (M.is_kinda_big(ref_sphere.ref_sphere_radius))
            return wave_abr_full_calc_inf_ref(fod, fld, wvl, foc, ray_pkg,
                                         chief_ray_pkg, ref_sphere);
        else
            return wave_abr_full_calc_finite_pup(fod, fld, wvl, foc, ray_pkg,
                                chief_ray_pkg, ref_sphere);
    }

    /**
     * Given a ray, a chief ray and an image pt, evaluate the OPD.
     *
     *     The main references for the calculations are in the H. H. Hopkins paper
     *     `Calculation of the Aberrations and Image Assessment for a General Optical
     *     System <https://doi.org/10.1080/713820605>`_
     *
     *     Args:
     *         fod: :class:`~.FirstOrderData` for object and image space refractive
     *              indices
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *         ray_pkg: input tuple of ray, ray_op, wvl
     *         chief_ray_pkg: input tuple of chief_ray, cr_exp_seg
     *         ref_sphere: input tuple of image_pt, ref_dir, ref_sphere_radius
     *
     *     Returns:
     *         opd: OPD of ray wrt chief ray at **fld**
     */
    private static double wave_abr_full_calc_finite_pup(FirstOrderData fod, Field fld, double wvl, double foc, RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg, ReferenceSphere ref_sphere) {
        var image_pt = ref_sphere.image_pt;
        var ref_dir = ref_sphere.ref_dir;
        var ref_sphere_radius = ref_sphere.ref_sphere_radius;
        var lcl_tfrm_last = ref_sphere.lcl_tfrm_last;
        var cr = chief_ray_pkg.chief_ray;
        var cr_exp_seg = chief_ray_pkg.cr_exp_seg;
        var cr_ray = cr.ray;
        var cr_op = cr.op_delta;
        wvl = cr.wvl;
        var cr_exp_pt =  cr_exp_seg.exp_pt;
        var cr_exp_dir = cr_exp_seg.exp_dir;
        var cr_exp_dist = cr_exp_seg.exp_dst;
        var ifc = cr_exp_seg.ifc;
        var cr_b4_pt = cr_exp_seg.b4_pt;
        var cr_b4_dir = cr_exp_seg.b4_dir;
        var ray = ray_pkg.ray;
        var ray_op = ray_pkg.op_delta;
        wvl = ray_pkg.wvl;

        int k = -2; // last interface in sequence

        // eq 3.12
        var e1 = eic_distance(new RayData(ray.get(1).p, ray.get(0).d),
                              new RayData(cr_ray.get(1).p, cr_ray.get(0).d));
        // eq 3.13
        var ekp = eic_distance(new RayData(Lists.get(ray,k).p, Lists.get(ray,k).d),
                              new RayData(Lists.get(cr_ray,k).p, Lists.get(cr_ray,k).d));

        var tafter =Transform.transform_after_surface(ifc, new RayData(Lists.get(ray,k).p,Lists.get(ray,k).d));
        var b4_pt = tafter.pt;
        var b4_dir = tafter.dir;
        var dst = ekp - cr_exp_dist;
        var eic_exp_pt = b4_pt.minus(b4_dir.times(dst));
        var p_coord = eic_exp_pt.minus(cr_exp_pt);

        var F = ref_dir.dot(b4_dir) - b4_dir.dot(p_coord)/ref_sphere_radius;
        var J = p_coord.dot(p_coord)/ref_sphere_radius - 2.0*ref_dir.dot(p_coord);

        var sign_soln = ref_dir.z*Lists.get(cr.ray,-1).d.z < 0 ? -1 : 1;
        var denom = F + sign_soln*Math.sqrt(F*F + J/ref_sphere_radius);
        var ep = denom == 0 ? 0.0  : J/denom;

        var n_obj = Math.abs(fod.n_obj);
        var n_img = Math.abs(fod.n_img);
        var opd = -n_obj*e1 - ray_op + n_img*ekp + cr_op - n_img*ep;

        return opd;
    }

    /**
     * Given a ray, a chief ray and an image pt, evaluate the OPD.
     *
     *     This set of functions calculates the wavefront aberration using an
     *     infinite reference sphere.
     *     The main references for the calculations are in the paper
     *     `Dependence of the wave-front aberration on the radius of the reference sphere <https://doi.org/10.1364/JOSAA.19.001187>`_ by Antonı́n Mikš.
     *
     *     Args:
     *         fod: :class:`~.FirstOrderData` for object and image space refractive
     *              indices
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *         ray_pkg: input tuple of ray, ray_op, wvl
     *         chief_ray_pkg: input tuple of chief_ray, cr_exp_seg
     *         ref_sphere: input tuple of image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last
     *
     *     Returns:
     *         opd: OPD of ray wrt chief ray at **fld**
     */
    private static double wave_abr_full_calc_inf_ref(FirstOrderData fod, Field fld, double wvl, double foc, RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg, ReferenceSphere ref_sphere) {
        var image_pt = ref_sphere.image_pt;
        var ref_dir = ref_sphere.ref_dir;
        var ref_sphere_radius = ref_sphere.ref_sphere_radius;
        var lcl_tfrm_last = ref_sphere.lcl_tfrm_last;
        var cr = chief_ray_pkg.chief_ray;
        var cr_exp_seg = chief_ray_pkg.cr_exp_seg;
        var cr_ray = cr.ray;
        var cr_op = cr.op_delta;
        wvl = cr.wvl;
        var ray = ray_pkg.ray;
        var ray_op = ray_pkg.op_delta;
        wvl = ray_pkg.wvl;

        int k = -2; // last interface in sequence
        var n_obj = Math.abs(fod.n_obj);
        var n_img = Math.abs(fod.n_img);

        // eq 3.12
        var e1 = eic_distance(new RayData(ray.get(1).p, ray.get(0).d),
                              new RayData(cr_ray.get(1).p, cr_ray.get(0).d));
        // eq 3.13
        var ekp = eic_distance(new RayData(Lists.get(ray,k).p, Lists.get(ray,k).d),
                              new RayData(Lists.get(cr_ray,k).p, Lists.get(cr_ray,k).d));

        Vector3 p_b4;
        Vector3 d_b4;
        Vector3 p_cr_b4;
        Vector3 d_cr_b4;

        if (lcl_tfrm_last != null) {
            var rt = lcl_tfrm_last.rt;
            var t = lcl_tfrm_last.t;

            p_b4 = rt.multiply(Lists.get(ray,k).p).minus(t);
            d_b4 = rt.multiply(Lists.get(ray,k).d);
            p_cr_b4 = rt.multiply(Lists.get(cr_ray,k).p).minus(t);
            d_cr_b4 = rt.multiply(Lists.get(cr_ray,k).d);
        }
        else {
            p_b4 = Lists.get(ray,k).p;
            d_b4 = Lists.get(ray,k).d;
            p_cr_b4 = Lists.get(cr_ray,k).p;
            d_cr_b4 = Lists.get(cr_ray,k).d;
        }
        var op_b4 = ray_dist_to_perp_from_origin(new RayData(p_b4, d_b4));
        var op_cr_b4 = ray_dist_to_perp_from_origin(new RayData(p_cr_b4, d_cr_b4));

        var P1_P2 = dist_to_shortest_join(new RayData(Lists.get(cr_ray,-1).p, Lists.get(cr_ray,-1).d),
                                          new RayData(Lists.get(ray,-1).p, Lists.get(ray,-1).d));
        var P1 = P1_P2.first;
        var P2 = P1_P2.second;

        var rF0 = (P1.first.plus(P2.first)).divide(2.0);
        var V_B = ray_op + op_b4;
        var V_BE = cr_op + op_cr_b4;

        var W0 = V_B - V_BE + n_img + d_b4.minus(d_cr_b4).dot(rF0);

        var ta = Lists.get(ray,-1).p.minus(image_pt);
        var numer = d_cr_b4.minus(d_b4.times(d_b4.dot(d_cr_b4))).dot(ta);
        var denom = 1.0 + d_b4.dot(d_cr_b4);
        var W_inf = W0 + n_img * numer / denom;

        var opd = -n_obj*e1 - W_inf;

        return opd;
    }


}
