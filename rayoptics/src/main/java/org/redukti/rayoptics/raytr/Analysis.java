// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.firstorder.FirstOrderData;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Analysis {



    /**
     * Compute the reference sphere for a defocussed image point at **fld
     *
     * @param opt_model
     * @param fld           :class:`~.Field` point for wave aberration calculation
     * @param wvl           wavelength of ray (nm)
     * @param foc           defocus amount
     * @param chief_ray_pkg input tuple of chief_ray, cr_exp_seg
     * @param image_pt_2d   x, y image point in (defocussed) image plane, if None, use
     *                      the chief ray coordinate.
     * @return ref_sphere: tuple of image_pt, ref_dir, ref_sphere_radius
     */
    public static RefSphere setup_exit_pupil_coords(OpticalModel opt_model, Field fld, double wvl, double foc,
                                                    ChiefRayPkg chief_ray_pkg, Vector2 image_pt_2d) {
        RayPkg cr = chief_ray_pkg.chief_ray;
        ChiefRayExitPupilSegment cr_exp_seg = chief_ray_pkg.cr_exp_seg;
        // cr_exp_pt: E upper bar prime: pupil center for pencils from Q
        // cr_exp_pt, cr_b4_dir, cr_dst
        // cr_exp_pt = cr_exp_seg[mc.p]

        Vector3 image_pt;
        if (image_pt_2d == null) {
            // get distance along cr corresponding to a z shift of the defocus
            double dist = foc / Lists.get(cr.ray, -1).d.z;
            image_pt = Lists.get(cr.ray, -1).p.plus(Lists.get(cr.ray, -1).d.times(dist));
        } else {
            image_pt = new Vector3(image_pt_2d.x, image_pt_2d.y, foc);
        }

        // get the image point wrt the final surface
        double image_thi = Lists.get(opt_model.seq_model.gaps, -1).thi;
        Vector3 img_pt = new Vector3(image_pt.x, image_pt.y, image_pt.z + image_thi);

        // R' radius of reference sphere for O'
        Vector3 ref_sphere_vec = img_pt.minus(cr_exp_seg.exp_pt);
        double ref_sphere_radius = ref_sphere_vec.length();
        Vector3 ref_dir = ref_sphere_vec.normalize();

        return new RefSphere(image_pt, ref_dir, ref_sphere_radius);
    }

    static final class FanDef {
        double[] fan_start;
        double[] fan_stop;
        int num_rays;

        public FanDef(double[] fan_start, double[] fan_stop, int num_rays) {
            this.fan_start = fan_start;
            this.fan_stop = fan_stop;
            this.num_rays = num_rays;
        }
    }

//    /**
//     * Trace a fan of rays and precalculate data for rapid refocus later.
//     *
//     * @param opt_model
//     * @param fld
//     * @param wvl
//     * @param foc
//     * @param xy
//     * @param image_pt_2d
//     * @param num_rays
//     * @return
//     */
//    public static Pair<List<RayFanItem>, List<WaveAbrPreCalc>> trace_fan(OpticalModel opt_model, Field fld, double wvl,
//                                                                         double foc, int xy, Vector2 image_pt_2d, int num_rays,
//                                                                         String output_filter, String rayerr_filter) {
//        FirstOrderData fod = opt_model.optical_spec.parax_data.fod;
//        ChiefRayPkg cr_pkg = Trace.get_chief_ray_pkg(opt_model, fld, wvl, foc);
//        RefSphere ref_sphere = setup_exit_pupil_coords(opt_model, fld, wvl, foc, cr_pkg,
//                image_pt_2d);
//        fld.chief_ray = cr_pkg;
//        fld.ref_sphere = ref_sphere;
//
//        // xy determines whether x (=0) or y (=1) fan
//        double[] fan_start = new double[]{0., 0.};
//        double[] fan_stop = new double[]{0., 0.};
//        fan_stop[xy] = -1.0;
//        fan_stop[xy] = 1.0;
//        FanDef fan_def = new FanDef(fan_start, fan_stop, num_rays);
//        List<RayFanItem> fan = trace_ray_fan(opt_model, fan_def, fld, wvl, foc, output_filter, rayerr_filter);
//
//        List<WaveAbrPreCalc> upd_fan = new ArrayList<>();
//        for (int i = 0; i < fan.size(); i++) {
//            RayFanItem fi = fan.get(i);
//            if (fi.ray_pkg != null) { // && ! instanceof TraceError
//                WaveAbrPreCalc re_opd_pkg = wave_abr_pre_calc(fod, fld, wvl, foc, fi.ray_pkg, cr_pkg);
//                upd_fan.add(re_opd_pkg);
//            } else {
//                upd_fan.add(null);
//            }
//        }
//        return new Pair<>(fan, upd_fan);
//    }

    /**
     * Trace a fan of rays, according to fan_rng.
     *
     * @param opt_model
     * @param fan_rng
     * @param fld
     * @param wvl
     * @param foc
     * @return
     */
    public static List<RayFanItem> trace_ray_fan(OpticalModel opt_model, FanDef fan_rng, Field fld, double wvl, double foc,
                                                 String output_filter, String rayerr_filter) {
        Vector2 start = new Vector2(fan_rng.fan_start[0], fan_rng.fan_start[1]);
        Vector2 stop = new Vector2(fan_rng.fan_stop[0], fan_rng.fan_stop[1]);
        int num = fan_rng.num_rays;
        Vector2 step = start.minus(stop).divide(num - 1);
        // FIXME fan is not populated
        List<RayFanItem> fan = new ArrayList<>();
        for (int r = 0; r < num; r++) {
            Vector2 pupil = start;
            TraceOptions trace_options = new TraceOptions();
            trace_options.output_filter = output_filter;
            trace_options.rayerr_filter = rayerr_filter;
            Trace.trace_safe(opt_model, pupil, fld, wvl, trace_options);
            start = start.plus(step);
        }
        return fan;
    }

    /**
     * Pre-calculate the part of the OPD calc independent of focus.
     *
     * @param fod
     * @param fld
     * @param wvl
     * @param foc
     * @param ray_pkg
     * @param chief_ray_pkg
     * @return
     */
    public static WaveAbrPreCalc wave_abr_pre_calc(FirstOrderData fod, Field fld, double wvl, double foc,
                                                   RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg) {
        RayPkg cr = chief_ray_pkg.chief_ray;
        ChiefRayExitPupilSegment cr_exp_seg = chief_ray_pkg.cr_exp_seg;
        List<RaySeg> chief_ray = cr.ray;
        double chief_ray_op = cr.op_delta;
        wvl = cr.wvl;
        Vector3 cr_exp_pt = cr_exp_seg.exp_pt;
        Vector3 cr_exp_dir = cr_exp_seg.exp_dir;
        double cr_exp_dist = cr_exp_seg.exp_dst;
        Interface ifc = cr_exp_seg.ifc;
        Vector3 cr_b4_pt = cr_exp_seg.b4_pt;
        Vector3 cr_b4_dir = cr_exp_seg.b4_dir;

        List<RaySeg> ray = ray_pkg.ray;
        double ray_op = ray_pkg.op_delta;
        wvl = ray_pkg.wvl;

        int k = -2; // last interface in sequence

        // eq 3.12
        double e1 = Trace.eic_distance(
                new RayData(Lists.get(ray, 1).p, Lists.get(ray, 0).d),
                new RayData(Lists.get(chief_ray, 1).p, Lists.get(chief_ray, 0).d));
        // eq 3.13
        double ekp = Trace.eic_distance(
                new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d),
                new RayData(Lists.get(chief_ray, k).p, Lists.get(chief_ray, k).d));

        double pre_opd = -Math.abs(fod.n_obj) * e1 - ray_op + Math.abs(fod.n_img) * ekp + chief_ray_op;
        RayData b4 = Transform.transform_after_surface(ifc, new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d));
        Vector3 b4_pt = b4.pt;
        Vector3 b4_dir = b4.dir;
        double dst = ekp - cr_exp_dist;
        Vector3 eic_exp_pt = b4_pt.minus(b4_dir.times(dst));
        Vector3 p_coord = eic_exp_pt.minus(cr_exp_pt);

        return new WaveAbrPreCalc(pre_opd, p_coord, b4_pt, b4_dir);
    }

    /**
     * Given pre-calculated info and a ref. sphere, return the ray's OPD.
     *
     * @param fod
     * @param fld
     * @param wvl
     * @param foc
     * @param ray_pkg
     * @param chief_ray_pkg
     * @param pre_opd_pkg
     * @param ref_sphere
     * @return
     */
    public static double wave_abr_calc(FirstOrderData fod, Field fld, double wvl, double foc, RayPkg ray_pkg,
                                       ChiefRayPkg chief_ray_pkg,
                                       WaveAbrPreCalc pre_opd_pkg, RefSphere ref_sphere) {
        RayPkg cr = chief_ray_pkg.chief_ray;
        ChiefRayExitPupilSegment cr_exp_seg = chief_ray_pkg.cr_exp_seg;

        Vector3 image_pt = ref_sphere.image_pt;
        Vector3 ref_dir = ref_sphere.ref_dir;
        double ref_sphere_radius = ref_sphere.ref_sphere_radius;

        double pre_opd = pre_opd_pkg.pre_opd;
        Vector3 p_coord = pre_opd_pkg.p_coord;
        Vector3 b4_pt = pre_opd_pkg.b4_pt;
        Vector3 b4_dir = pre_opd_pkg.b4_dir;

        double F = ref_dir.dot(b4_dir) - b4_dir.dot(p_coord) / ref_sphere_radius;
        double J = p_coord.dot(p_coord) / ref_sphere_radius - 2.0 * ref_dir.dot(p_coord);

        double sign_soln = (ref_dir.z * Lists.get(cr.ray, -1).d.z < 0) ? -1.0 : 1.0;
        double denom = F + sign_soln * Math.sqrt(F * F + J / ref_sphere_radius);
        double ep = denom == 0.0 ? 0 : J / denom;

        double opd = pre_opd - Math.abs(fod.n_img) * ep;
        return opd;
    }

    /**
     * Refocus the fan of rays and return the tranverse abr. and OPD.
     *
     * @param opt_model
     * @param fan_pkg
     * @param fld
     * @param wvl
     * @param foc
     * @param image_pt_2d
     * @return
     */
    public static List<Pair<Vector2, Vector3>> focus_fan(OpticalModel opt_model, Pair<List<RayFanItem>,
            List<WaveAbrPreCalc>> fan_pkg, Field fld, double wvl, double foc,
                                                         Vector2 image_pt_2d) {
        FirstOrderData fod = opt_model.optical_spec.parax_data.fod;
        List<RayFanItem> fan = fan_pkg.first;
        List<WaveAbrPreCalc> upd_fan = fan_pkg.second;
        ChiefRayPkg cr_pkg = Trace.get_chief_ray_pkg(opt_model, fld, wvl, foc);
        RefSphere ref_sphere = setup_exit_pupil_coords(opt_model, fld, wvl, foc, cr_pkg,
                image_pt_2d);
        double central_wvl = opt_model.optical_spec.spectral_region.central_wvl();
        double convert_to_opd = 1 / opt_model.nm_to_sys_units(central_wvl);

        List<Pair<Vector2, Vector3>> fan_data = new ArrayList<>();
        for (int i = 0; i < fan.size(); i++) {
            RayFanItem fi = fan.get(i);
            WaveAbrPreCalc fiu = upd_fan.get(i);
            double pupil_x = fi.x;
            double pupil_y = fi.y;
            RayPkg ray_pkg = fi.ray_pkg;

            if (ray_pkg != null) { // && ! instanceof TraceError
                Vector3 image_pt = ref_sphere.image_pt;
                List<RaySeg> ray = ray_pkg.ray;
                double dist = foc / Lists.get(ray, -1).d.z;
                Vector3 defocused_pt = Lists.get(ray, -1).p.plus(Lists.get(ray, -1).d.times(dist));
                Vector3 t_abr = defocused_pt.minus(image_pt);
                double opdelta = wave_abr_calc(fod, fld, wvl, foc,
                        ray_pkg, cr_pkg, fiu, ref_sphere);
                double opd = convert_to_opd * opdelta;
                fan_data.add(new Pair<>(new Vector2(pupil_x, pupil_y), new Vector3(t_abr.x, t_abr.y, opd)));
            } else {
                fan_data.add(new Pair<>(new Vector2(pupil_x, pupil_y), new Vector3(Double.NaN, Double.NaN, Double.NaN)));
            }
        }
        return fan_data;
    }
}
