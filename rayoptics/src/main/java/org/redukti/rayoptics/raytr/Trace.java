// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.*;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.PathSeg;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Coord;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.util.Lists;

import java.util.*;

public class Trace {

    /**
     * Trace a single ray via pupil, field and wavelength specs.
     *
     *     This function traces a single ray at a given wavelength, pupil and field specification.
     *
     *     Ray failures (miss surface, TIR) and aperture clipping are handled via RayError exceptions. If a failure occurs, a second item is returned (if  *rayerr_filter* is set to 'summary' or 'full') that contains information about the failure. Apertures are tested using the :meth:`~.seq.interface.Interface.point_inside` API when *check_apertures* is True.
     *
     *     The pupil coordinates by default are normalized to the vignetted pupil extent. Alternatively, the pupil coordinates can be taken as actual coordinates on the pupil plane (and similarly for ray direction) using the **pupil_type** keyword argument.
     *
     *     The amount of output that is returned can range from the entire ray (default) to the image segment only or even the return from a user-supplied filtering function.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         pupil: 2d vector of relative pupil coordinates
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *
     *         check_apertures: if True, do point_inside() test on inc_pt
     *         apply_vignetting: if True, apply the `fld` vignetting factors to **pupil**
     *
     *         pupil_type: ::
     *
     *             - 'rel pupil': relative pupil coordinates
     *             - 'aim pt': aim point on pupil plane
     *             - 'aim dir': aim direction in object space
     *
     *         use_named_tuples: if True, returns data as RayPkg and RaySeg.
     *
     *         output_filter: ::
     *
     *             - if None, append entire ray
     *             - if 'last', append the last ray segment only
     *             - else treat as callable and append the return value
     *
     *         rayerr_filter: ::
     *
     *             - if None, on ray error append nothing
     *             - if 'summary', append the exception without ray data
     *             - if 'full', append the exception with ray data up to error
     *             - else append nothing
     *
     *         eps: accuracy tolerance for surface intersection calculation
     *
     *     Returns:
     *         tuple: ray_pkg, trace_error | None
     */
    public static RayResult trace_ray(
            OpticalModel opt_model,
            Vector2 pupil,
            Field fld,
            double wvl,
            TraceOptions trace_options) {
        if (trace_options.rayerr_filter == null)
            trace_options.rayerr_filter = "full";
        return trace_safe(opt_model, pupil, fld, wvl, trace_options);
    }

    /**
     * Wrapper for trace_base that handles exceptions.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         pupil: 2d vector of relative pupil coordinates
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         output_filter: ::
     *
     *             - if None, append entire ray
     *             - if 'last', append the last ray segment only
     *             - else treat as callable and append the return value
     *
     *         rayerr_filter: ::
     *
     *             - if None, on ray error append nothing
     *             - if 'summary', append the exception without ray data
     *             - if 'full', append the exception with ray data up to error
     *             - else append nothing
     *
     *     Returns:
     *         ray_result: see discussion of filters, above.
     *
     */
    public static RayResult trace_safe(OpticalModel opt_model,
                                  Vector2 pupil,
                                  Field fld,
                                  double wvl,
                                  TraceOptions trace_options) {

        RayResult result = new RayResult();
        RayPkg ray_pkg;
        try {
            ray_pkg = Trace.trace_base(opt_model, pupil.as_array(), fld, wvl, trace_options);
            if (trace_options.output_filter == null)
                result.pkg = ray_pkg;
            else if ("last".equals(trace_options.output_filter)) {
                RaySeg seg = Lists.get(ray_pkg.ray, -1);
                ray_pkg = new RayPkg(Arrays.asList(seg), ray_pkg.op_delta, ray_pkg.wvl);
                result.pkg = ray_pkg;
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (TraceException rayerr) {
            if (Objects.equals(trace_options.rayerr_filter, "full")) {
                ray_pkg = rayerr.ray_pkg;
                result.pkg = ray_pkg;
                result.err = rayerr;
            }
            else if (Objects.equals(trace_options.rayerr_filter, "summary")) {
                rayerr.ray_pkg = null;
                result.err = rayerr;
                result.pkg = null;
            }
        }
        return result;
    }

    /**
     * returns (ray, ray_opl, wvl)
     * <p>
     * Args:
     * seq_model: the :class:`~.SequentialModel` to be traced
     * pt0: starting coordinate at object interface
     * dir0: starting direction cosines following object interface
     * wvl: ray trace wavelength in nm
     * **kwargs: keyword arguments
     * <p>
     * Returns:
     * (**ray**, **op_delta**, **wvl**)
     * <p>
     * - **ray** is a list for each interface in **path_pkg** of these
     * elements: [pt, after_dir, after_dst, normal]
     * <p>
     * - pt: the intersection point of the ray
     * - after_dir: the ray direction cosine following the interface
     * - after_dst: after_dst: the geometric distance to the next
     * interface
     * - normal: the surface normal at the intersection point
     * <p>
     * - **op_delta** - optical path wrt equally inclined chords to the
     * optical axis
     * - **wvl** - wavelength (in nm) that the ray was traced in
     */
    public static RayPkg trace(SequentialModel seq_model, Vector3 pt0, Vector3 dir0, double wvl, TraceOptions trace_options) {
        var options = new RayTraceOptions(trace_options);
        return RayTrace.trace(seq_model, pt0, dir0, wvl, options);
    }

    /**
     * Trace ray specified by relative aperture and field point.
     * <p>
     * `pupil_type` controls how `pupil` data is interpreted when calculating the starting ray coordinates.
     * <p>
     * Args:
     * opt_model: instance of :class:`~.OpticalModel` to trace
     * pupil: aperture coordinates of ray
     * fld: instance of :class:`~.Field`
     * wvl: ray trace wavelength in nm
     * apply_vignetting: if True, apply the `fld` vignetting factors to **pupil**
     * pupil_type: ::
     * <p>
     * - 'rel pupil': relative pupil coordinates
     * - 'aim pt': aim point on pupil plane
     * - 'aim dir': aim direction in object space
     * <p>
     * **kwargs: keyword arguments
     * <p>
     * Returns:
     * (**ray**, **op_delta**, **wvl**)
     * <p>
     * - **ray** is a list for each interface in **path_pkg** of these
     * elements: [pt, after_dir, after_dst, normal]
     * <p>
     * - pt: the intersection point of the ray
     * - after_dir: the ray direction cosine following the interface
     * - after_dst: after_dst: the geometric distance to the next
     * interface
     * - normal: the surface normal at the intersection point
     * <p>
     * - **op_delta** - optical path wrt equally inclined chords to the
     * optical axis
     * - **wvl** - wavelength (in nm) that the ray was traced in
     *
     * @param opt_model instance of :class:`~.OpticalModel` to trace
     * @param pupil     relative pupil coordinates of ray
     * @param fld       instance of :class:`~.Field`
     * @param wvl       ray trace wavelength in nm
     */
    public static RayPkg trace_base(OpticalModel opt_model, double[] pupil, Field fld, double wvl, TraceOptions trace_options) {
        double[] pupil_coords = pupil;
        if (trace_options.pupil_type == PupilType.REL_PUPIL) {
            if (trace_options.apply_vignetting)
                pupil_coords = fld.apply_vignetting(pupil);
        }
        Coord coord = opt_model.optical_spec.ray_start_from_osp(pupil_coords,fld,trace_options.pupil_type);
        var pt0 = coord.pt;
        var dir0 = coord.dir;

        // if wide_angle, don't try to intercept object and don't disallow
        // propagation against z_dir; this will be the case for rays exceeding
        // 90 degrees at the first surface.
        var options = new RayTraceOptions();
        options.check_apertures = trace_options.check_apertures;
        if (opt_model.optical_spec.fov.is_wide_angle)
            options.intersect_obj = false;
        else {
            // otherwise, if not wide angle, propagation against z_dir means
            // a virtual object. To handle virtual object distances, always
            // propagate from the object in a positive Z direction.
            if (dir0.z * opt_model.seq_model.z_dir.get(0).value < 0)
                dir0 = dir0.negate();
        }
        var pkg = RayTrace.trace(opt_model.seq_model, pt0, dir0, wvl, options);
        return pkg.with(fld,new Vector2(pupil[0], pupil[1]),new Vector2(pupil_coords[0], pupil_coords[1]));
    }

    static class BaseObjectiveFunction {
        final SequentialModel seq_model;
        final Integer ifcx;
        final Vector3 pt0;
        final double obj2enp_dist;
        final double wvl;
        final boolean not_wa;
        final RayResult rr;

        public BaseObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double obj2enp_dist, double wvl, boolean not_wa, RayResult rr) {
            this.seq_model = seq_model;
            this.ifcx = ifcx;
            this.pt0 = pt0;
            this.obj2enp_dist = obj2enp_dist;
            this.wvl = wvl;
            this.not_wa = not_wa;
            this.rr = rr;
        }

        public RaySeg eval(double x1, double y1) {
            Vector3 pt1 = new Vector3(x1, y1, obj2enp_dist);
            Vector3 dir0 = pt1.minus(pt0).normalize();
            // handle case where entrance pupil is behind the object
            if (not_wa && dir0.z * seq_model.z_dir.get(0).value < 0)
                dir0 = dir0.negate();

            RayPkg pkg;
            try {
                pkg = RayTrace.trace(seq_model, pt0, dir0, wvl);
                rr.pkg = pkg;
                rr.err = null;
            } catch (TraceException ray_error) {
                pkg = ray_error.ray_pkg;
                rr.pkg = ray_error.ray_pkg;
                rr.err = ray_error;
                if (ray_error.surf <= ifcx)
                    throw ray_error;
            }
            return pkg.ray.get(ifcx);
        }
    }

    /* 1D solver */
    static class SecantFunction extends BaseObjectiveFunction implements ScalarObjectiveFunction {

        final double y_target;

        public SecantFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target, boolean not_wa, RayResult rr) {
            super(seq_model, ifcx, pt0, dist, wvl, not_wa, rr);
            this.y_target = y_target;
        }

        @Override
        public Double eval(double y1) {
            RaySeg seg = eval(0., y1);
            double y_ray = seg.p.y;
            return y_ray - y_target;
        }
    }

    public static RayResultWithStartCoord get_1d_solution(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target, boolean not_wa) {
        RayResultWithStartCoord res = new RayResultWithStartCoord();
        SecantFunction fn = new SecantFunction(seq_model, ifcx, pt0, dist, wvl, y_target, not_wa, res.rr);
        double start_y = SecantSolver.find_root(fn, 0., 50, 1.48e-8).root;
        res.start_coords = new double[]{0, start_y};
        return res;
    }

    /* Solver for use in Minpack algos */
    static class HybrdObjectiveFunction extends BaseObjectiveFunction implements MinPack.Hybrd_Function {
        final double[] xy_target; // target x,y values

        public HybrdObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target, boolean not_wa, RayResult rr) {
            super(seq_model, ifcx, pt0, dist, wvl, not_wa, rr);
            this.xy_target = xy_target;
        }

        @Override
        public void apply(int n, double[] x, double[] fvec, int[] iflag) {
            RaySeg seg = eval(x[0], x[1]);
            fvec[0] = seg.p.x - xy_target[0];
            fvec[1] = seg.p.y - xy_target[1];
        }

    }

    public static RayResultWithStartCoord get_2d_solution(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target, boolean not_wa) {
        RayResultWithStartCoord res = new RayResultWithStartCoord();
        HybrdObjectiveFunction f = new HybrdObjectiveFunction(seq_model, ifcx, pt0, dist, wvl, Arrays.copyOf(xy_target, xy_target.length), not_wa, res.rr);
        double[] x = new double[2];
        double[] fvec = new double[2];
        int lwa = (2 * (3 * 2 + 13)) / 2;
        double[] wa = new double[lwa];
        int info[] = new int[1];
        // epsfcn is relative error; fdjac1 uses its square root as the step.
        double epsfcn = 1.0e-8;
        info[0] = MinPack.hybrd1(f, 2, x, fvec, 1.0e-10, wa, lwa, epsfcn);
        // Numerical Jacobian evaluation leaves rr referring to a perturbed ray.
        f.apply(2, x, fvec, new int[1]);

        double residual = Math.hypot(fvec[0], fvec[1]);
        double coordinateScale = Math.max(Math.max(Math.abs(x[0]), Math.abs(x[1])),
                Math.max(Math.abs(xy_target[0]), Math.abs(xy_target[1])));
        double residualTolerance = Math.max(1.0e-7,
                1.0e-8 * coordinateScale);
        boolean converged = info[0] >= 1 && info[0] <= 4
                && residual <= residualTolerance;
        if (!converged) {
            TraceException failure = new TraceException("2D ray aiming failed: MINPACK info=" + info[0]
                    + ", residual=" + residual + ", tolerance=" + residualTolerance
                    + ", start=" + Arrays.toString(x));
            failure.surf = ifcx;
            failure.ifc = seq_model.ifcs.get(ifcx);
            failure.ray_pkg = res.rr.pkg;
            throw failure;
        }
        res.start_coords = x;
        return res;
    }

    /**
     * iterates a ray to xy_target on interface ifcx, returns aim points on
     * the paraxial entrance pupil plane
     * <p>
     * If idcx is None, i.e. a floating stop surface, returns xy_target.
     * <p>
     * If the iteration fails, a TraceError will be raised
     *
     */
    public static RayResultWithStartCoord iterate_ray(final OpticalModel opt_model, Integer ifcx, double[] xy_target, Field fld, double wvl) {
        var seq_model = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        var fod = osp.parax_data.fod;
        double obj2enp_dist = fod.obj_dist + fod.enp_dist;
        boolean not_wa = !osp.fov.is_wide_angle;

        Coord coord = osp.obj_coords(fld);
        var pt0 = coord.pt;
        var d0 = coord.dir;

        if (ifcx != null) {
            if (pt0.x == 0.0 && xy_target[0] == 0.0) {
                // do 1D iteration if field and target points are zero in x
                var y_target = xy_target[1];
                return get_1d_solution(seq_model, ifcx, pt0, obj2enp_dist, wvl, y_target, not_wa);
            } else {
                return get_2d_solution(seq_model, ifcx, pt0, obj2enp_dist, wvl, xy_target, not_wa);
            }
        } else {
            // floating stop surface - use entrance pupil for aiming
            var result = new RayResultWithStartCoord();
            result.start_coords = xy_target;
            return result;
        }
    }

    /**
     * returns a list of RayPkgs for the boundary rays for field fld
     */
    public static List<RayPkg> trace_boundary_rays_at_field(OpticalModel opt_model, Field fld, double wvl, TraceOptions trace_options) {
        trace_options.rayerr_filter = "full";
        var ref_sphere_cr = setup_pupil_coords(opt_model,fld,wvl,0.0,null,null);
        fld.chief_ray = ref_sphere_cr.chief_ray_pkg;
        fld.ref_sphere = ref_sphere_cr.ref_sphere;
        List<RayPkg> rim_rays = new ArrayList<>();
        var osp = opt_model.optical_spec;
        for (double[] p : osp.pupil.pupil_rays) {
            var ray_result = trace_ray(opt_model, new Vector2(p[0], p[1]), fld, wvl, trace_options);
            rim_rays.add(ray_result.pkg);
        }
        return rim_rays;
    }

    public static Map<String, RayPkg> boundary_ray_dict(OpticalModel opt_model, List<RayPkg> rim_rays) {
        Map<String, RayPkg> pupil_rays = new HashMap<>();
        String[] ray_labels = opt_model.optical_spec.pupil.ray_labels;
        for (int i = 0; i < rim_rays.size(); i++) {
            if (i >= ray_labels.length)
                break;
            pupil_rays.put(ray_labels[i], rim_rays.get(i));
        }
        return pupil_rays;
    }

    public static List<List<RayPkg>> trace_boundary_rays(OpticalModel opt_model, TraceOptions trace_options) {
        List<List<RayPkg>> rayset = new ArrayList<>();
        double wvl = opt_model.seq_model.central_wavelength();
        FieldSpec fov = opt_model.optical_spec.fov;
        for (int fi = 0; fi < fov.fields.length; fi++) {
            Field fld = fov.fields[fi];
            var rim_rays = trace_boundary_rays_at_field(opt_model, fld, wvl,trace_options);
            fld.pupil_rays = boundary_ray_dict(opt_model, rim_rays);
            rayset.add(rim_rays);
        }
        return rayset;
    }


    /* returns a list of ray |DataFrame| for the ray_list at field fld */
    public static List<RayDataFrame> trace_ray_list_at_field(OpticalModel opt_model, double[][] ray_list, Field fld, double wvl, double foc, TraceOptions trace_options) {
        ArrayList<RayDataFrame> rayset = new ArrayList<>();
        for (double[] p : ray_list) {
            var ray_result = trace_ray(opt_model, new Vector2(p[0], p[1]), fld, wvl, trace_options);
            rayset.add(new RayDataFrame(ray_result.pkg.ray));
        }
        return rayset;
    }

    public static RayDataFrameByField trace_field(
            OpticalModel opt_model,
            Field fld,
            double wvl,
            double foc) {
        var osp = opt_model.optical_spec;
        var pupil_rays  = osp.pupil.pupil_rays;
        var rdf_list = trace_ray_list_at_field(opt_model,pupil_rays,fld,wvl,foc,new TraceOptions());
        return new RayDataFrameByField(fld,rdf_list);
    }

    public static List<RayDataFrameByField> trace_all_fields(OpticalModel opt_mode) {
        var osp = opt_mode.optical_spec;
        var t = osp.lookup_fld_wvl_focus(0);
        var fld = t.first;
        var wvl = t.second;
        var foc = t.third;
        List<RayDataFrameByField> fset  = new ArrayList<>();
        for (var f: osp.fov.fields) {
            var rset = trace_field(opt_mode,f,wvl,foc);
            fset.add(rset);
        }
        return fset;
    }

    /**
     * Trace a chief ray at fld and wvl.
     *
     *     Returns:
     *         tuple: **chief_ray**, **cr_exp_seg**
     *
     *             - **chief_ray**: RayPkg of chief ray
     *             - **cr_exp_seg**: exp_pt, exp_dir, exp_dst, ifc, b4_pt, b4_dir
     */
    public static ChiefRayPkg trace_chief_ray(OpticalModel opt_model, Field fld, double wvl, double foc) {
        var osp = opt_model.optical_spec;
        var fod = osp.parax_data.fod;
        var options = new TraceOptions();
        options.rayerr_filter = "full";
        var ray_result = trace_safe(opt_model, new Vector2(0., 0.), fld, wvl, options);
        var cr = ray_result.pkg;
        // op = rt.calc_optical_path(ray, opt_model.seq_model.path())

        // cr_exp_pt: E upper bar prime: pupil center for pencils from Q
        // cr_exp_pt, cr_b4_dir, cr_exp_dist
        var cr_exp_seg = WaveAbr.transfer_to_exit_pupil(
                Lists.get(opt_model.seq_model.ifcs, -2),
                new RayData(Lists.get(cr.ray, -2).p, Lists.get(cr.ray, -2).d),
                fod.exp_dist);

        return new ChiefRayPkg(cr, cr_exp_seg);
    }

    public static void apply_paraxial_vignetting(OpticalModel opt_model) {
        var fov = opt_model.optical_spec.field_of_view();
        var pm = opt_model.parax_model;
        var mf = fov.max_field();
        var max_field = mf.first;
        var jth = mf.second;
        for (int j = 0; j < fov.fields.length; j++) {
            var fld = fov.fields[j];
            var rel_fov = Math.sqrt(fld.x*fld.x + fld.y*fld.y);
            if (!fov.is_relative && max_field != 0)
                rel_fov = rel_fov/max_field;
            var vg = pm.paraxial_vignetting(rel_fov);
            var min_vly = vg.first;
            var min_vuy = vg.second;
            if (min_vly.second != null)
                fld.vly = 1.0 - min_vly.first;
            if (min_vuy.second != null)
                fld.vuy = 1.0 - min_vuy.first;
        }
    }

    /**
     * Get the chief ray package at **fld**, computing it if necessary.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *
     *     Returns:
     *         tuple: **chief_ray**, **cr_exp_seg**
     *
     *             - **chief_ray**: chief_ray, chief_ray_op, wvl
     *             - **cr_exp_seg**: chief ray exit pupil segment (pt, dir, dist)
     *
     *                 - pt: chief ray intersection with exit pupil plane
     *                 - dir: direction cosine of the chief ray in exit pupil space
     *                 - dist: distance from interface to the exit pupil point
     *
     */
    public static ChiefRayPkg get_chief_ray_pkg(OpticalModel opt_model, Field fld, double wvl, double foc) {
        ChiefRayPkg chief_ray_pkg;
        if (fld.chief_ray == null) {
            var res = aim_chief_ray(opt_model, fld, wvl);
            if (res.aim_pt != null) {
                fld.aim_info = res.aim_pt;
                fld.z_enp = null;
            }
            else {
                fld.z_enp = res.z_enp;
                fld.aim_info = null;
            }
            chief_ray_pkg = trace_chief_ray(opt_model, fld, wvl, foc);
        }
        else if (fld.chief_ray.chief_ray.wvl != wvl) {
            chief_ray_pkg = trace_chief_ray(opt_model, fld, wvl, foc);
        }
        else {
            chief_ray_pkg = fld.chief_ray;
        }
        return chief_ray_pkg;
    }

    /**
     * Trace chief ray and setup reference sphere for `fld`.
     *
     *     Returns:
     *         tuple: **ref_sphere**, **chief_ray_pkg**
     *
     *             - **ref_sphere**: image_pt, ref_dir, ref_sphere_radius, lcl_tfrm_last
     *             - **chief_ray_pkg**: chief_ray, cr_exp_seg
     */
    public static RefSphereCR setup_pupil_coords(
            OpticalModel opt_model,
            Field fld,
            double wvl,
            double foc,
            Vector2 image_pt,
            Vector2 image_delta) {
        var chief_ray_pkg = get_chief_ray_pkg(opt_model, fld, wvl, foc);
        var ref_sphere = WaveAbr.calculate_reference_sphere(opt_model,fld,wvl,foc,
                                chief_ray_pkg, image_pt, image_delta);
        return new RefSphereCR(ref_sphere, chief_ray_pkg);
    }

    public static AimInfo aim_chief_ray(OpticalModel opt_model, Field fld, Double wvl) {
        // aim chief ray at center of stop surface and save results on **fld**
        var seq_model = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        if (wvl == null)
            wvl = seq_model.central_wavelength();
        Integer stop = seq_model.stop_surface;
        AimInfo rvalue;
        if (osp.fov.is_wide_angle) {
            var res = Wideangle.find_real_enp(opt_model, stop, fld, wvl);
            rvalue = new AimInfo(null,res.z_enp);
        }
        else {
            var res = iterate_ray(opt_model, stop, new double[]{0., 0.}, fld, wvl);
            rvalue = new AimInfo(res.start_coords,null);
        }
        return rvalue;
    }

    public static List<GridItem> trace_fan(OpticalModel opt_model, TraceFanDef fan_rng, Field fld, double wvl, double foc, boolean append_if_none, ImageFilter img_filter, TraceOptions trace_options) {
        var start = fan_rng.start;
        var stop =  fan_rng.stop;
        var num = fan_rng.num_rays;
        var step = (stop.minus(start)).divide(num-1);
        List<GridItem> fan = new ArrayList<>();
        for (int r = 0; r < num; r++) {
            var pupil = start;
            var ray_result = trace_safe(opt_model, pupil, fld, wvl, trace_options);
            if (ray_result.pkg != null) {
                if (img_filter != null) {
                    fan.add(img_filter.apply(pupil,ray_result.pkg));
                }
                else {
                    fan.add(new GridItem(pupil,ray_result.pkg));
                }
            }
            else if (append_if_none) {
                //ray outside pupil or failed
                fan.add(new GridItem(pupil,null));
            }
            start = new Vector2(start.x+step.x, start.y+step.y);
        }
        return fan;
    }

    public static List<GridItem> trace_grid(OpticalModel opt_model, TraceGridDef grid_rng, Field fld, double wvl, double foc, ImageFilter img_filter, boolean append_if_none, TraceOptions trace_options) {
        trace_options = trace_options.copy();
        trace_options.check_apertures = true;
        var start = grid_rng.grid_start;
        var stop = grid_rng.grid_stop;
        var num = grid_rng.num_rays;
        var step = (stop.minus(start)).divide(num-1);
        var grid = new ArrayList<GridItem>();
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < num; j++) {
                var pupil = start;
                var ray_result = trace_safe(opt_model,pupil,fld,wvl,trace_options);
                if (ray_result.pkg != null) {
                    if (img_filter != null) {
                        grid.add(img_filter.apply(pupil,ray_result.pkg));
                    }
                    else {
                        grid.add(new GridItem(pupil,ray_result.pkg));
                    }
                }
                else {
                    //ray outside pupil or failed
                    if (img_filter != null) {
                        var item = img_filter.apply(pupil,null);
                        if (item != null || append_if_none)
                            grid.add(item);
                    }
                    else {
                        if (append_if_none)
                            grid.add(new GridItem(pupil,null));
                    }
                }
                start = new Vector2(start.x,start.y+step.y);
            }
            start = new Vector2(start.x+step.x, grid_rng.grid_start.y);
        }
        return grid;
    }

    public static List<GridItem> trace_rings(OpticalModel opt_model, TraceRingsDef grid_rng, Field fld, double wvl, double foc, ImageFilter img_filter, boolean append_if_none, TraceOptions trace_options) {
        trace_options = trace_options.copy();
        trace_options.check_apertures = true;
        trace_options.pupil_type = PupilType.REL_PUPIL;
        trace_options.apply_vignetting = true;

        var grid = new ArrayList<GridItem>();
        // Below creates concentric rings of points that will be relative to pupil of radius 1.0
        int num_rings = grid_rng.num_rings;
        double max_radius = grid_rng.max_radius; // max radius

        List<Vector2> points;
        if (grid_rng.hexapolar) {
            points = generate_hexapolar_points(grid_rng, max_radius, num_rings);
        }
        else {
            points = generate_points(grid_rng, num_rings, max_radius);
            //points = generate_gaussian(grid_rng, num_rings, max_radius);
        }

        for (int i = 0; i < points.size(); i++) {
            var pupil = points.get(i);
            var ray_result = trace_safe(opt_model,pupil,fld,wvl,trace_options);
            if (ray_result.pkg != null) {
                if (img_filter != null) {
                    grid.add(img_filter.apply(pupil,ray_result.pkg));
                }
                else {
                    grid.add(new GridItem(pupil,ray_result.pkg));
                }
            }
            else {
                //ray outside pupil or failed
                if (img_filter != null) {
                    var item = img_filter.apply(pupil,null);
                    if (item != null || append_if_none)
                        grid.add(item);
                }
                else {
                    if (append_if_none)
                        grid.add(new GridItem(pupil,null));
                }
            }
        }
        return grid;
    }

    public static List<GridItem> trace_gaussian_quadrature(
            OpticalModel opt_model, TraceRingsDef grid_rng, Integer num_spokes,
            Field fld, double wvl, double foc, ImageFilter img_filter,
            boolean append_if_none, TraceOptions trace_options) {
        trace_options = trace_options.copy();
        trace_options.check_apertures = true;
        trace_options.pupil_type = PupilType.REL_PUPIL;
        trace_options.apply_vignetting = true;

        var grid = new ArrayList<GridItem>();
        var points = generate_gaussian_quadrature(grid_rng, grid_rng.num_rings, num_spokes);
        for (var point : points) {
            var pupil = point.pupil();
            var ray_result = trace_safe(opt_model, pupil, fld, wvl, trace_options);
            if (ray_result.pkg != null) {
                GridItem item = img_filter != null
                        ? img_filter.apply(pupil, ray_result.pkg)
                        : new GridItem(pupil, ray_result.pkg);
                grid.add(item.withWeight(point.weight()));
            }
            else if (img_filter != null) {
                var item = img_filter.apply(pupil, null);
                if (item != null)
                    grid.add(item.withWeight(point.weight()));
                else if (append_if_none)
                    grid.add(new GridItem(pupil, null).withWeight(point.weight()));
            }
            else if (append_if_none) {
                grid.add(new GridItem(pupil, null).withWeight(point.weight()));
            }
        }
        return grid;
    }

    /**
     * Trace the reference and the two displaced rays required by contrast
     * optimization. Pupil displacements are expressed in relative pupil
     * coordinates of the physically vignetted pupil. Every quadrature sample
     * is returned so clients can keep a stable residual layout; a ray is null
     * when the trace fails.
     */
    public static List<ContrastRayTriplet> trace_contrast(
            OpticalModel opt_model, TraceRingsDef grid_rng, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift,
            Field fld, double wvl, TraceOptions trace_options) {
        trace_options = trace_options.copy();
        // The quadrature generator explicitly maps samples into the physical
        // vignetted pupil. Applying Field vignetting in trace_base as well
        // would independently scale the displaced rays and change their MTF
        // shear, especially when a pair straddles a pupil axis.
        // do not turn temporary clear-aperture clipping into a discontinuous
        // optimizer failure while a surface is being varied.
        trace_options.check_apertures = false;
        trace_options.pupil_type = PupilType.REL_PUPIL;
        trace_options.apply_vignetting = false;

        var samples = new ArrayList<ContrastRayTriplet>();
        var points = generate_contrast_quadrature(
                grid_rng, num_spokes, sagittal_shift, tangential_shift, fld);
        for (var point : points) {
            var pupil = point.pupil();
            var sagittalPupil = pupil.plus(sagittal_shift);
            var tangentialPupil = pupil.plus(tangential_shift);
            var reference = trace_safe(opt_model, pupil, fld, wvl, trace_options).pkg;
            var sagittal = trace_safe(opt_model, sagittalPupil, fld, wvl, trace_options).pkg;
            var tangential = trace_safe(opt_model, tangentialPupil, fld, wvl, trace_options).pkg;
            samples.add(new ContrastRayTriplet(
                    pupil, reference, sagittal, tangential, point.weight()));
        }
        return samples;
    }

    /**
     * Generate fixed-count quadrature samples in the physical vignetted pupil.
     * The complete pattern is contracted about the overlap centre until every
     * sample and both of its displaced partners are valid. A common contraction
     * preserves relative quadrature weights and avoids frequency-dependent ray
     * loss at the pupil boundary.
     */
    static List<GaussianQuadraturePoint> generate_contrast_quadrature(
            TraceRingsDef grid_rng, Integer num_spokes,
            Vector2 sagittal_shift, Vector2 tangential_shift, Field fld) {
        var nominal = generate_gaussian_quadrature(
                grid_rng, grid_rng.num_rings, num_spokes);
        var nominalCenter = new Vector2(grid_rng.cx, grid_rng.cy);
        var physicalCenter = apply_vignetting(nominalCenter, fld);
        var overlapCenter = physicalCenter.minus(
                sagittal_shift.plus(tangential_shift).times(0.5));

        if (!valid_contrast_point(overlapCenter, sagittal_shift, tangential_shift, fld)) {
            throw new IllegalArgumentException(
                    "The requested contrast shear has no common vignetted pupil overlap");
        }

        double contraction = 1.0;
        if (!valid_contrast_pattern(nominal, physicalCenter, overlapCenter,
                contraction, sagittal_shift, tangential_shift, fld)) {
            double low = 0.0;
            double high = 1.0;
            for (int iteration = 0; iteration < 60; iteration++) {
                double trial = 0.5 * (low + high);
                if (valid_contrast_pattern(nominal, physicalCenter, overlapCenter,
                        trial, sagittal_shift, tangential_shift, fld)) {
                    low = trial;
                } else {
                    high = trial;
                }
            }
            contraction = low;
        }

        var points = new ArrayList<GaussianQuadraturePoint>(nominal.size());
        double weightSum = 0.0;
        for (var point : nominal) {
            var physical = apply_vignetting(point.pupil(), fld);
            var pupil = overlapCenter.plus(
                    physical.minus(physicalCenter).times(contraction));
            double weight = point.weight() * vignetting_jacobian(point.pupil(), fld);
            points.add(new GaussianQuadraturePoint(pupil, weight));
            weightSum += weight;
        }
        if (!(weightSum > 0.0)) {
            throw new IllegalArgumentException("The vignetted pupil has zero area");
        }
        final double normalization = weightSum;
        return points.stream()
                .map(point -> new GaussianQuadraturePoint(
                        point.pupil(), point.weight() / normalization))
                .toList();
    }

    private static boolean valid_contrast_pattern(
            List<GaussianQuadraturePoint> nominal, Vector2 physicalCenter,
            Vector2 overlapCenter, double contraction, Vector2 sagittalShift,
            Vector2 tangentialShift, Field fld) {
        for (var point : nominal) {
            var physical = apply_vignetting(point.pupil(), fld);
            var pupil = overlapCenter.plus(
                    physical.minus(physicalCenter).times(contraction));
            if (!valid_contrast_point(pupil, sagittalShift, tangentialShift, fld)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valid_contrast_point(
            Vector2 pupil, Vector2 sagittalShift, Vector2 tangentialShift, Field fld) {
        return inside_vignetted_pupil(pupil, fld)
                && inside_vignetted_pupil(pupil.plus(sagittalShift), fld)
                && inside_vignetted_pupil(pupil.plus(tangentialShift), fld);
    }

    static boolean inside_vignetted_pupil(Vector2 pupil, Field fld) {
        double xScale = vignetting_scale(pupil.x, fld.vlx, fld.vux);
        double yScale = vignetting_scale(pupil.y, fld.vly, fld.vuy);
        if (!(xScale > 0.0) || !(yScale > 0.0)) {
            return false;
        }
        double x = pupil.x / xScale;
        double y = pupil.y / yScale;
        return x * x + y * y <= 1.0 + 1.0e-14;
    }

    private static Vector2 apply_vignetting(Vector2 pupil, Field fld) {
        return new Vector2(
                pupil.x * vignetting_scale(pupil.x, fld.vlx, fld.vux),
                pupil.y * vignetting_scale(pupil.y, fld.vly, fld.vuy));
    }

    private static double vignetting_jacobian(Vector2 pupil, Field fld) {
        return vignetting_scale(pupil.x, fld.vlx, fld.vux)
                * vignetting_scale(pupil.y, fld.vly, fld.vuy);
    }

    private static double vignetting_scale(double coordinate, double lower, double upper) {
        double factor = coordinate < 0.0 ? lower : upper;
        return factor == 0.0 ? 1.0 : 1.0 - factor;
    }

    private static List<Vector2> generate_hexapolar_points(TraceRingsDef grid_rng, double max_radius, int num_rings) {
        List<Vector2> points;
        points = new ArrayList<>();
        double cx = grid_rng.cx, cy = grid_rng.cy;  // center of ring
        points.add(new Vector2(cx,cy));
        double step = max_radius / num_rings;
        for (double r = max_radius; r > 1e-8; r -= step) {
            double astep = (step / r) * (Math.PI / 3);
            for (double a = 0; a < 2 * Math.PI - 1e-8; a += astep)
                points.add(new Vector2(cx + Math.sin(a) * r, cy + Math.cos(a) * r));
        }
        return points;
    }

    private static List<Vector2> generate_points(TraceRingsDef grid_rng, int num_rings, double max_radius) {
        List<Vector2> points;
        points = new ArrayList<>();
        double cx = grid_rng.cx, cy = grid_rng.cy;  // center of ring
        points.add(new Vector2(cx,cy));
        int num_points_in_ring_one = grid_rng.num_points_in_ring_one;
        double angle_deg_ring_one = 360.0 / num_points_in_ring_one;
        for (int ring = 1; ring <= num_rings; ring++) {
            // angular step
            double daz = angle_deg_ring_one / ring;
             // Odd rings offset by half-step
            double offset = (ring % 2 == 0) ? 0.0 : 0.5*daz;
            // Linear radius spacing
            double r = ring * max_radius / num_rings;
            // Number of points on this ring
            int numPoints = num_points_in_ring_one * ring;
            for (int jaz = 0; jaz < numPoints; jaz++) {
                double angle_deg  = offset + jaz*daz;
                double angle_rad = Math.toRadians(angle_deg);
                double x = cx + r*Math.cos(angle_rad);
                double y = cy + r*Math.sin(angle_rad);
                points.add(new Vector2(x, y));
            }
        }
        return points;
    }

    private static List<Vector2> generate_gaussian(TraceRingsDef grid_rng, int ncircles, double max_radius) {
        List<Vector2> points;
        points = new ArrayList<>();
        double cx = grid_rng.cx, cy = grid_rng.cy;  // center of ring
        points.add(new Vector2(cx,cy));
        double sigma = max_radius/Math.sqrt(2.0*Math.log(1+ncircles));
        for (int icirc=1; icirc<=ncircles; icirc++)
        {
            double daz = 60.0 / icirc;
            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz;
            double p = M.square(icirc)/(ncircles + M.square(ncircles));
            double r = sigma*Math.sqrt(2.0*Math.log(1/(1-p)));
            for (int jaz = 0; jaz<6*icirc; jaz++)
            {
                double angle_deg = offset + jaz*daz;
                double angle_rad = Math.toRadians(angle_deg);
                double x = cx + r*Math.cos(angle_rad);
                double y = cy + r*Math.sin(angle_rad);
                points.add(new Vector2(x, y));
            }
        }
        return points;
    }

    /**
     * Generates a Gaussian quadrature pattern over a circular pupil.
     *
     * <p>The radial coordinates are Gauss-Legendre nodes transformed from
     * {@code [-1, 1]} to squared pupil radius {@code [0, 1]}. Each radial node
     * is repeated at uniformly spaced angles. The returned weights are
     * normalized to sum to one, so they integrate a pupil average rather than
     * the area (pi) of the unit disk.</p>
     *
     * <p>Based on William H. Peirce, "Numerical Integration Over the Planar
     * Annulus", Journal of the Society for Industrial and Applied Mathematics,
     * Vol. 5, No. 2 (1957), pp. 66-73.</p>
     */
    static List<GaussianQuadraturePoint> generate_gaussian_quadrature(
            TraceRingsDef grid_rng, int num_rings, Integer num_spokes) {
        if (num_rings < 1 || (num_spokes != null && num_spokes < 1)) {
            throw new IllegalArgumentException("The number of rings and spokes must be at least 1");
        }

        int spokes = num_spokes == null ? 4 * (num_rings + 1) : num_spokes;
        double[][] nodesAndWeights = gauss_legendre_nodes_and_weights(num_rings);
        List<GaussianQuadraturePoint> points = new ArrayList<>(num_rings * spokes);

        for (int angle = 1; angle <= spokes; angle++) {
            double theta = 2.0 * Math.PI * angle / spokes;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            for (int ring = 0; ring < num_rings; ring++) {
                double radius = grid_rng.max_radius
                        * Math.sqrt(0.5 + 0.5 * nodesAndWeights[0][ring]);
                Vector2 pupil = new Vector2(
                        grid_rng.cx + radius * cosTheta,
                        grid_rng.cy + radius * sinTheta);
                double weight = 0.5 * nodesAndWeights[1][ring] / spokes;
                points.add(new GaussianQuadraturePoint(pupil, weight));
            }
        }
        return points;
    }

    /** Computes Gauss-Legendre nodes and weights on [-1, 1]. */
    private static double[][] gauss_legendre_nodes_and_weights(int order) {
        double[] nodes = new double[order];
        double[] weights = new double[order];
        int rootsToFind = (order + 1) / 2;

        for (int i = 0; i < rootsToFind; i++) {
            double x = Math.cos(Math.PI * (i + 0.75) / (order + 0.5));
            double derivative;
            double delta;
            do {
                double p0 = 1.0;
                double p1 = x;
                for (int degree = 2; degree <= order; degree++) {
                    double p2 = ((2.0 * degree - 1.0) * x * p1
                            - (degree - 1.0) * p0) / degree;
                    p0 = p1;
                    p1 = p2;
                }
                double polynomial = p1;
                double previousPolynomial = p0;
                derivative = order * (x * polynomial - previousPolynomial)
                        / (x * x - 1.0);
                delta = polynomial / derivative;
                x -= delta;
            } while (Math.abs(delta) > 1.0e-15);

            double weight = 2.0 / ((1.0 - x * x) * derivative * derivative);
            nodes[i] = -x;
            nodes[order - 1 - i] = x;
            weights[i] = weight;
            weights[order - 1 - i] = weight;
        }
        return new double[][]{nodes, weights};
    }

    record GaussianQuadraturePoint(Vector2 pupil, double weight) {}


    static class BaseObjectiveFunctionRaw {
        final List<PathSeg> pthlist;
        final Integer ifcx;
        final Vector3 pt0;
        final double dist;
        final double wvl;
        final boolean not_wa;
        final RayResult rr;

        public BaseObjectiveFunctionRaw(List<PathSeg> pthlist, Integer ifcx, Vector3 pt0, double dist, double wvl, boolean not_wa, RayResult rr) {
            this.pthlist = pthlist;
            this.ifcx = ifcx;
            this.pt0 = pt0;
            this.dist = dist;
            this.wvl = wvl;
            this.not_wa = not_wa;
            this.rr = rr;
        }

        public RaySeg eval(double x1, double y1) {
            Vector3 pt1 = new Vector3(x1, y1, dist);
            Vector3 dir0 = pt1.minus(pt0).normalize();
            // handle case where entrance pupil is behind the object
            if (not_wa && dir0.z * pthlist.get(0).Zdir.value < 0)
                dir0 = dir0.negate();

            RayPkg pkg = null;
            try {
                var options = new RayTraceOptions();
                options.check_apertures = false;
                options.intersect_obj = true;
                options.filter_out_phantoms = false;
                pkg = RayTrace.trace_raw(pthlist, pt0, dir0, wvl, options);
                rr.pkg = pkg;
                rr.err = null;
            } catch (TraceException ray_error) {
                pkg = ray_error.ray_pkg;
                rr.pkg = ray_error.ray_pkg;
                rr.err = ray_error;
                if (ray_error.surf <= ifcx)
                    throw ray_error;
            }
            return pkg.ray.get(ifcx);
        }
    }

    /* 1D solver */
    static class SecantFunctionRaw extends BaseObjectiveFunctionRaw implements ScalarObjectiveFunction {

        final double y_target;

        public SecantFunctionRaw(List<PathSeg> pthlist, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target, boolean not_wa, RayResult rr) {
            super(pthlist, ifcx, pt0, dist, wvl, not_wa, rr);
            this.y_target = y_target;
        }

        @Override
        public Double eval(double y1) {
            RaySeg seg = eval(0., y1);
            double y_ray = seg.p.y;
            return y_ray - y_target;
        }
    }

    public static RayResultWithStartCoord get_1d_solution_raw(List<PathSeg> pthlist, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target, boolean not_wa) {
        RayResultWithStartCoord res = new RayResultWithStartCoord();
        SecantFunctionRaw fn = new SecantFunctionRaw(pthlist, ifcx, pt0, dist, wvl, y_target, not_wa, res.rr);
        double start_y = SecantSolver.find_root(fn, 0., 50, 1.48e-8).root;
        res.start_coords = new double[]{0, start_y};
        return res;
    }

    /* Solver for use in Minpack algos */
    static class HybrdObjectiveFunctionRaw extends BaseObjectiveFunctionRaw implements MinPack.Hybrd_Function {
        final double[] xy_target; // target x,y values

        public HybrdObjectiveFunctionRaw(List<PathSeg> pthlist, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target, boolean not_wa, RayResult rr) {
            super(pthlist, ifcx, pt0, dist, wvl, not_wa, rr);
            this.xy_target = xy_target;
        }

        @Override
        public void apply(int n, double[] x, double[] fvec, int[] iflag) {
            RaySeg seg = eval(x[0], x[1]);
            fvec[0] = seg.p.x - xy_target[0];
            fvec[1] = seg.p.y - xy_target[1];
        }

    }

    public static RayResultWithStartCoord get_2d_solution_raw(List<PathSeg> pthlist, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target, boolean not_wa) {
        RayResultWithStartCoord res = new RayResultWithStartCoord();
        HybrdObjectiveFunctionRaw f = new HybrdObjectiveFunctionRaw(pthlist, ifcx, pt0, dist, wvl, Arrays.copyOf(xy_target, xy_target.length), not_wa, res.rr);
        double[] x = new double[2];
        double[] fvec = new double[2];
        int lwa = (2 * (3 * 2 + 13)) / 2;
        double[] wa = new double[lwa];
        int info[] = new int[1];
        // epsfcn is relative error; fdjac1 uses its square root as the step.
        double epsfcn = 1.0e-8;
        info[0] = MinPack.hybrd1(f, 2, x, fvec, 1.0e-10, wa, lwa, epsfcn);
        // Numerical Jacobian evaluation leaves rr referring to a perturbed ray.
        f.apply(2, x, fvec, new int[1]);

        double residual = Math.hypot(fvec[0], fvec[1]);
        double coordinateScale = Math.max(Math.max(Math.abs(x[0]), Math.abs(x[1])),
                Math.max(Math.abs(xy_target[0]), Math.abs(xy_target[1])));
        double residualTolerance = Math.max(1.0e-7,
                1.0e-8 * coordinateScale);
        boolean converged = info[0] >= 1 && info[0] <= 4
                && residual <= residualTolerance;
        if (!converged) {
            TraceException failure = new TraceException("2D ray aiming failed: MINPACK info=" + info[0]
                    + ", residual=" + residual + ", tolerance=" + residualTolerance
                    + ", start=" + Arrays.toString(x));
            failure.surf = ifcx;
            failure.ifc = pthlist.get(ifcx).ifc;
            failure.ray_pkg = res.rr.pkg;
            throw failure;
        }
        res.start_coords = x;
        return res;
    }


    /**
     * iterates a ray to xy_target on interface ifcx, returns aim points on
     *     the paraxial entrance pupil plane
     *
     *     If idcx is None, i.e. a floating stop surface, returns xy_target.
     *
     *     If the iteration fails, a TraceError will be raised
     */
    public static RayResultWithStartCoord iterate_ray_raw(List<PathSeg> pthlist, Integer ifcx, double[] xy_target, Vector3 pt0, Vector3 d0, double obj2pup_dist,
                                                          double eprad, double wvl, boolean not_wa) {
        if (ifcx != null) {
            if (pt0.x == 0.0 && xy_target[0] == 0.0) {
                // do 1D iteration if field and target points are zero in x
                var y_target = xy_target[1];
                try {
                    return get_1d_solution_raw(pthlist, ifcx, pt0, obj2pup_dist, wvl, y_target, not_wa);
                }
                catch (TraceException ray_err) {
                    var result = new RayResultWithStartCoord();
                    result.start_coords = new double[]{0,0};
                    result.rr = new RayResult(ray_err.ray_pkg,ray_err);
                    return result;
                }
            } else {
                try {
                    return get_2d_solution_raw(pthlist, ifcx, pt0, obj2pup_dist, wvl, xy_target, not_wa);
                }
                catch (TraceException ray_err) {
                    var result = new RayResultWithStartCoord();
                    result.start_coords = new double[]{0,0};
                    result.rr = new RayResult(ray_err.ray_pkg, ray_err);
                    return result;
                }
            }
        } else {
            // floating stop surface - use entrance pupil for aiming
            var result = new RayResultWithStartCoord();
            result.start_coords = xy_target;
            return result;
        }
    }

    public static StringBuilder list_ray(StringBuilder sb, RayPkg ray_pkg, Tfm3d tfrms, Integer start) {
        if (start == null) start = 0;
        var ray = ray_pkg.ray;
        sb.append("            X            Y            Z           L            M            N               Len\n");
        var colFormats = "%3d %12.5f %12.5f %12.5g %12.6f %12.6f %12.6f %12.5g\n";
        for (int i = start; i < ray.size(); i++) {
            var r = ray.get(i);
            if (tfrms == null) {
                sb.append(String.format(colFormats,i,r.p.x, r.p.y, r.p.z,
                        r.d.x, r.d.y, r.d.z, r.dst));
            }
            else {
                var rot = tfrms.rt;
                var trns = tfrms.t;
                var p = rot.multiply(r.p).plus(trns);
                var d = rot.multiply(r.d);
                sb.append(String.format(colFormats,i,p.x, p.y, p.z,
                        d.x, d.y, d.z, r.dst));
            }
        }
        return sb;
    }

}
