package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.elem.Transform;
import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Analysis {

    /**
     * Get the chief ray package at **fld**, computing it if necessary.
     *
     * @param opt_model
     * @param fld :class:`~.Field` point for wave aberration calculation
     * @param wvl wavelength of ray (nm)
     * @param foc defocus amount
     * @return tuple of chief_ray, cr_exp_seg
     */
    public static ChiefRayPkg get_chief_ray_pkg(OpticalModel opt_model, Field fld, double wvl, double foc) {
        if (fld.chief_ray == null) {
            Trace.aim_chief_ray(opt_model, fld, wvl);
            ChiefRayPkg chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg;
        } else if (fld.chief_ray.chief_ray.wvl != wvl) {
            ChiefRayPkg  chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg;
        }
        return fld.chief_ray;
    }

    /**
     * Compute the reference sphere for a defocussed image point at **fld
     *
     * @param opt_model
     * @param fld :class:`~.Field` point for wave aberration calculation
     * @param wvl wavelength of ray (nm)
     * @param foc defocus amount
     * @param chief_ray_pkg input tuple of chief_ray, cr_exp_seg
     * @param image_pt_2d x, y image point in (defocussed) image plane, if None, use
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
            image_pt = Lists.get(cr.ray,-1).p.plus(Lists.get(cr.ray, -1).d.times(dist));
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

    /**
     * Trace a fan of rays and precalculate data for rapid refocus later.
     *
     * @param opt_model
     * @param fld
     * @param wvl
     * @param foc
     * @param xy
     * @param image_pt_2d
     * @param num_rays
     * @return
     */
    public static Pair<List<RayFanItem>, List<WaveAbrPreCalc>> trace_fan(OpticalModel opt_model, Field fld, double wvl,
                                                                         double foc, int xy, Vector2 image_pt_2d, int num_rays,
                                                                         String output_filter, String rayerr_filter) {
        FirstOrderData fod = opt_model.optical_spec.parax_data.fod;
        ChiefRayPkg cr_pkg = get_chief_ray_pkg(opt_model, fld, wvl, foc);
        RefSphere ref_sphere = setup_exit_pupil_coords(opt_model, fld, wvl, foc, cr_pkg,
                image_pt_2d);
        fld.chief_ray = cr_pkg;
        fld.ref_sphere = ref_sphere;

        // xy determines whether x (=0) or y (=1) fan
        double[] fan_start = new double[]{0., 0.};
        double[] fan_stop = new double[]{0., 0.};
        fan_stop[xy] = -1.0;
        fan_stop[xy] = 1.0;
        FanDef fan_def = new FanDef(fan_start, fan_stop, num_rays);
        List<RayFanItem> fan = trace_ray_fan(opt_model, fan_def, fld, wvl, foc, output_filter, rayerr_filter);

        List<WaveAbrPreCalc> upd_fan = new ArrayList<>();
        for (int i = 0; i < fan.size(); i++) {
            RayFanItem fi = fan.get(i);
            if (fi.ray_pkg != null) { // && ! instanceof TraceError
                WaveAbrPreCalc re_opd_pkg = wave_abr_pre_calc(fod, fld, wvl, foc, fi.ray_pkg, cr_pkg);
                upd_fan.add(re_opd_pkg);
            } else {
                upd_fan.add(null);
            }
        }
        return new Pair<>(fan, upd_fan);
    }

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
        Vector2 step = start.subtract(stop).div(num - 1);
        List<RayFanItem> fan = new ArrayList<>();
        for (int r = 0; r < num; r++) {
            Vector2 pupil = start;
            trace_safe(opt_model, pupil, fld, wvl, fan,
                    output_filter, rayerr_filter);
            start = start.add(step);
        }
        return fan;
    }

    /**
     * Wrapper for trace_base that handles exceptions.
     * <p>
     * Args:
     * opt_model: :class:`~.OpticalModel` instance
     * pupil: 2d vector of relatice pupil coordinates
     * fld: :class:`~.Field` point for wave aberration calculation
     * wvl: wavelength of ray (nm)
     * ray_list: list to append the ray data
     * output_filter:
     * <p>
     * - if None, append entire ray
     * - if 'last', append the last ray segment only
     * - else treat as callable and append the return value
     * <p>
     * rayerr_filter:
     * <p>
     * - if None, on ray error append nothing
     * - if 'summary', append the exception without ray data
     * - if 'full', append the exception with ray data up to error
     * - else append nothing
     *
     * @param opt_model
     * @param pupil
     * @param fld
     * @param wvl
     * @param ray_list
     * @param output_filter
     * @param rayerr_filter
     */
    public static void trace_safe(OpticalModel opt_model, Vector2 pupil, Field fld, double wvl,
                                  List<RayFanItem> ray_list, String output_filter, String rayerr_filter) {

        RayPkg ray_pkg;
        try {
            ray_pkg = Trace.trace_base(opt_model, pupil.as_array(), fld, wvl);
        } catch (Exception e) {
            // TODO
            return;
        }
        if (output_filter == null)
            ray_list.add(new RayFanItem(pupil.x, pupil.y, ray_pkg));
        else if ("last".equals(output_filter)) {
            RaySeg seg = Lists.get(ray_pkg.ray, -1);
            ray_pkg = new RayPkg(Arrays.asList(seg), ray_pkg.op_delta, ray_pkg.wvl);
            ray_list.add(new RayFanItem(pupil.x, pupil.y, ray_pkg));
        } else {
            throw new UnsupportedOperationException();
        }
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
    public static WaveAbrPreCalc wave_abr_pre_calc(FirstOrderData fod, Field fld, double wvl, double foc, RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg) {
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
        double e1 = RayTrace.eic_distance(
                new RayData(Lists.get(ray, 1).p, Lists.get(ray, 0).d),
                new RayData(Lists.get(chief_ray, 1).p, Lists.get(chief_ray, 0).d));
        // eq 3.13
        double ekp = RayTrace.eic_distance(
                new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d),
                new RayData(Lists.get(chief_ray, k).p, Lists.get(chief_ray, k).d));

        double pre_opd = -Math.abs(fod.n_obj) * e1 - ray_op + Math.abs(fod.n_img) * ekp + chief_ray_op;
        RayData b4 = Transform.transform_after_surface(ifc, new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d));
        Vector3 b4_pt = b4.p;
        Vector3 b4_dir = b4.d;
        double dst = ekp - cr_exp_dist;
        Vector3 eic_exp_pt = b4_pt.minus(b4_dir.times(dst));
        Vector3 p_coord = eic_exp_pt.minus(cr_exp_pt);

        return new WaveAbrPreCalc(pre_opd, p_coord, b4_pt, b4_dir);
    }

}
