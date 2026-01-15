// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.ScalarObjectiveFunction;
import org.redukti.mathlib.SecantSolver;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.exceptions.TraceRayBlockedException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.specs.ValueKey;
import org.redukti.rayoptics.util.Lists;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

// Vignetting and clear aperture setting operations
public class VigCalc {

    public static Double max_aperture_at_surf(List<List<RayPkg>> rayset, int i) {
        double max_ap = -1.0e+10;
        for (var f: rayset) {
            for (var p: f) {
                var ray = p.ray;
                if (ray.size() > i) {
                    var ap = Math.sqrt(ray.get(i).p.x*ray.get(i).p.x + ray.get(i).p.y*ray.get(i).p.y);
                    if (ap > max_ap)
                        max_ap = ap;
                }
                else
                    return null;
            }
        }
        return max_ap;
    }

    /**
     * From existing fields and vignetting, calculate clear apertures.
     *
     *     Args:
     *         avoid_list: list of surfaces to skip when setting apertures.
     *         include_list: list of surfaces to include when setting apertures.
     *
     *     If specified, only one of either `avoid_list` or `include_list` should be specified. If neither is specified, all surfaces are set. If both are specified, the `avoid_list` is used.
     *
     *     If a surface is specified as the aperture stop, that surface's aperture is determined from the boundary rays of the first field.
     *
     *     The avoid_list idea and implementation was contributed by Quentin Bécar
     */
    public static void set_clear_apertures(OpticalModel opt_model,List<Integer> avoid_list, List<Integer> include_list) {
        var sm = opt_model.seq_model;
        var num_surfs = sm.get_num_surfaces();
        if (avoid_list == null) {
            if (include_list == null) {
                include_list = IntStream.range(0, num_surfs).boxed().toList();
            }
        }
        else {
            include_list = IntStream.range(0,num_surfs).filter(i->!avoid_list.contains(i)).boxed().toList();
        }
        var rayset = Trace.trace_boundary_rays(opt_model,new TraceOptions());
        var stop_surf = sm.stop_surface;
        if (stop_surf != null && include_list.contains(stop_surf)) {
            Double max_ap = max_aperture_at_surf(List.of(rayset.get(0)),stop_surf);
            if (max_ap != null) {
                System.out.println("Setting stop aperture to " + max_ap);
                sm.ifcs.get(stop_surf).set_max_aperture(max_ap);
            }
        }
        for (var i: include_list) {
            if (!Objects.equals(i,stop_surf)) {
                Double max_ap = max_aperture_at_surf(rayset,i);
                if (max_ap != null)
                    sm.ifcs.get(i).set_max_aperture(max_ap);
            }
        }
    }

    /**
     * From existing fields and vignetting, calculate clear apertures.
     *
     *     This function modifies the max_aperture maintained by the list of
     *     :class:`~.interface.Interface` in the
     *     :class:`~.sequential.SequentialModel`. For each interface, the smallest
     *     aperture that will pass all of the (vignetted) boundary rays, for each
     *     field, is chosen.
     *
     *     The change of the apertures is propagated to the
     *     :class:`~.elements.ElementModel` via
     *     :meth:`~.elements.ElementModel.sync_to_seq`.
     */
    public static void set_ape(OpticalModel opm) {
        set_clear_apertures(opm,null,null);
    }

    /**
     * From existing fields and clear apertures, calculate vignetting.
     */
    public static void set_vig(OpticalModel opm, Boolean use_bisection) {
        var osp = opm.optical_spec;
        for (int fi = 0; fi < osp.fov.fields.length; fi++) {
            var fld_wvl_foc = osp.lookup_fld_wvl_focus(fi);
            var fld = fld_wvl_foc.first;
            var wvl = fld_wvl_foc.second;
            calc_vignetting_for_field(opm,fld,wvl,use_bisection,null);
        }
    }

    public static void set_stop_aperture(OpticalModel opm) {
        var sm = opm.seq_model;
        opm.optical_spec.fov.with_index_label("axis").clear_vignetting();
        set_clear_apertures(opm,null,List.of(sm.stop_surface));
        set_vig(opm,false);
    }

    /**
     * From existing stop size, calculate pupil spec and vignetting.
     *
     *     Use the upper Y marginal ray on-axis (field #0) and iterate until it
     *     goes through the edge of the stop surface. Use the object or image
     *     segments of this ray to update the pupil specification value
     *     e.g. EPD, NA or f/#.
     */
    public static void set_pupil(OpticalModel opm, boolean use_parax) {
        var sm = opm.seq_model;
        if (sm.stop_surface == null) {
            System.out.println("floating stop surface");
            return;
        }
        var idx_stop = sm.stop_surface;
        var osp = opm.optical_spec;

        // iterate the on-axis marginal ray thru the edge of the stop.
        var fld_foc = osp.lookup_fld_wvl_focus(0);
        var fld_0 = fld_foc.first;
        var cwl = fld_foc.second;
        var foc = fld_foc.third;
        var stop_radius = Lists.get(sm.ifcs,idx_stop).surface_od();
        var start_coords = iterate_pupil_ray(opm,sm.stop_surface,1,1.0,stop_radius,fld_0,cwl);

        // trace the real axial marginal ray
        var options = new TraceOptions();
        options.output_filter = null;
        options.rayerr_filter = "full";
        options.apply_vignetting = false;
        options.check_apertures = true;
        var ray_result = Trace.trace_safe(opm,start_coords,fld_0,cwl,options);
        var ray_pkg = ray_result.pkg;
        var ray_err = ray_result.err;

        var obj_img_key = osp.pupil.key.imageKey;
        var pupil_spec = osp.pupil.key.valueKey;
        var pupil_value_orig = osp.pupil.value;

        var parax_data = opm.optical_spec.parax_data;
        var ax_ray = parax_data.ax_ray;
        var fod = parax_data.fod;
        if (use_parax) {
            var scale_ratio = stop_radius/ax_ray.get(idx_stop).ht;
            //logger.debug(f"{scale_ratio=:8.5f} (parax)")
            if (obj_img_key == ImageKey.Object) {
                if (pupil_spec == ValueKey.EPD) {
                    osp.pupil.value = scale_ratio * (2 * fod.enp_radius);
                }
                else {
                    var slp0 = scale_ratio*ax_ray.get(0).slp;
                    if (pupil_spec == ValueKey.NA) {
                        var n0 = sm.central_rndx(0);
                        var rs0 = ray_pkg.ray.get(0);
                        osp.pupil.value = n0*rs0.d.y;
                    }
                    else if (pupil_spec == ValueKey.Fnum) {
                        osp.pupil.value = 1.0/(2.0*slp0);
                    }
                }
            }
            else if (obj_img_key == ImageKey.Image) {
                if (pupil_spec == ValueKey.EPD) {
                    osp.pupil.value = scale_ratio*(2*fod.exp_radius);
                }
                else {
                    var slpk = scale_ratio*Lists.get(ax_ray,-1).slp;
                    if (pupil_spec == ValueKey.NA) {
                        var nk = sm.central_rndx(-1);
                        var rsm2 = Lists.get(ray_pkg.ray,-2);
                        osp.pupil.value = -nk*rsm2.d.y;
                    }
                    else if (pupil_spec == ValueKey.Fnum) {
                        osp.pupil.value = -1.0/(2.0*slpk);
                    }
                }
            }
        }
        else {
            var scale_ratio = ray_pkg.ray.get(1).p.y/ax_ray.get(1).ht;
            if (obj_img_key == ImageKey.Object) {
                if (pupil_spec == ValueKey.EPD) {
                    var rs1 = ray_pkg.ray.get(1);
                    var ht = rs1.p.y;
                    osp.pupil.value *= scale_ratio;
                } else {
                    var rs0 = ray_pkg.ray.get(0);
                    var slp0 = rs0.d.y / rs0.d.z;
                    if (pupil_spec == ValueKey.NA) {
                        var n0 = sm.central_rndx(0);
                        osp.pupil.value = n0 * rs0.d.y;
                    } else if (pupil_spec == ValueKey.Fnum) {
                        osp.pupil.value = 1.0 / (2.0 * slp0);
                    }
                }
            } else if (obj_img_key == ImageKey.Image) {
                var rsm2 = Lists.get(ray_pkg.ray, -2);
                if (pupil_spec == ValueKey.EPD) {
                    var ht = rsm2.p.y;
                    osp.pupil.value = 2.0 * ht;
                } else {
                    var slpk = scale_ratio * Lists.get(ax_ray,-1).slp;
                    if (pupil_spec == ValueKey.NA) {
                        var nk = sm.central_rndx(0);
                        osp.pupil.value = -nk * rsm2.d.y;
                    } else if (pupil_spec == ValueKey.Fnum) {
                        osp.pupil.value = -1.0 / (2.0 * slpk);
                    }
                }
            }
        }
        // trace the real axial marginal ray with aperture clipping
        var clipoptions = new TraceOptions();
        clipoptions.output_filter = null;
        clipoptions.rayerr_filter = "full";
        clipoptions.apply_vignetting = false;
        clipoptions.check_apertures = true;
        var clipped_rr = Trace.trace_safe(opm, start_coords, fld_0, cwl,clipoptions);
        var clipped_ray_err = clipped_rr.err;
        if (clipped_ray_err != null) {
            if (clipped_ray_err instanceof TraceRayBlockedException)
                System.err.println("Axial bundle limited by surface " + clipped_ray_err.surf + " not stop surface.");
        }
        if (!Objects.equals(osp.pupil.value,pupil_value_orig)) {
            opm.update_model();
            set_vig(opm,null);
        }
    }

    public static void calc_vignetting_for_field(
            OpticalModel opm,
            Field fld,
            double wvl,
            Boolean use_bisection,
            Integer max_iter_count) {
        if (use_bisection == null)
            use_bisection = opm.optical_spec.fov.is_wide_angle;
        var pupil_starts = opm.optical_spec.pupil.pupil_rays;
        var vig_factors = new double[4];
        for (int i = 0; i < vig_factors.length; i++) {
            int xy = i/2;
            var start =  pupil_starts[i+1];
            var startv = new Vector2(start[0],start[1]);
            VigResult result;
            if (use_bisection) {
                result = calc_vignetted_ray_by_bisection(opm,xy,startv,fld,wvl,max_iter_count);
            }
            else {
                result = calc_vignetted_ray(opm,xy,startv,fld,wvl,max_iter_count);
            }
            vig_factors[i] = result.vig;
        }
        // update the field's vignetting factors
        fld.vux = vig_factors[0];
        fld.vlx = vig_factors[1];
        fld.vuy = vig_factors[2];
        fld.vly = vig_factors[3];
    }

    /**
     * Find the limiting aperture and return the vignetting factor.
     *
     *     Args:
     *         opm: :class:`~.OpticalModel` instance
     *         xy: 0 or 1 depending on x or y axis as the pupil direction
     *         start_dir: the unit length starting pupil coordinates, e.g [1., 0.].
     *                    This establishes the radial direction of the ray iteration.
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         max_iter_count: fail-safe limit on aperture search
     *
     *     Returns:
     *         (**vig**, **clip_indx**, **ray_pkg**)
     *
     *         - **vig** - vignetting factor
     *         - **clip_indx** - the index of the limiting interface
     *         - **ray_pkg** - the vignetting-limited ray
     */
    public static VigResult calc_vignetted_ray(
            OpticalModel opm,
            int xy,
            Vector2 start_dir,
            Field fld,
            double wvl,
            Integer max_iter_count) {
        if (max_iter_count == null) max_iter_count = 50;

        var rel_p1 = start_dir;
        var sm = opm.seq_model;
        var still_iterating = true;
        Integer clip_indx = null;
        Integer stop_indx = null;
        var iter_count = 0;
        RayPkg ray_pkg = null;

        while (still_iterating && iter_count < max_iter_count) {
            iter_count++;
            try {
                var options = new TraceOptions();
                options.apply_vignetting = false;
                options.check_apertures = true;
                options.pt_inside_fuzz=1e-4;
                ray_pkg = Trace.trace_base(opm,rel_p1.as_array(),fld,wvl,options);
                //  ray successfully traced.
                if (clip_indx != null) {
                    // fall through and exit
                    var r_target = Lists.get(sm.ifcs, clip_indx).edge_pt_target(start_dir);
                    var p = Lists.get(ray_pkg.ray, clip_indx).p;
                    var r_ray = Math.copySign(Math.sqrt(p.x * p.x + p.y * p.y), r_target.v(xy));
                    var r_error = r_ray - r_target.v(xy);
//                    logger.debug(f" C {xy_str[xy]} = {rel_p1[xy]:10.6f}:   "
//                            f"blocked at {clip_indx}, del={r_error:8.1e}, "
//                            "exiting")
                    still_iterating = false;
                }
                else {
                    // this is the first time through
                    // iterate to find the ray that goes through the edge
                    // of the stop surface
                    Integer indx;
                    indx = stop_indx = sm.stop_surface;
                    if (stop_indx != null) {
                        var r_target = Lists.get(sm.ifcs,stop_indx).edge_pt_target(start_dir);
//                        logger.debug(f" D {xy_str[xy]} = {rel_p1[xy]:10.6f}:   "
//                                f"passed first time, iterate to edge of stop, "
//                                f"ifcs[{stop_indx}]")
                        rel_p1 = iterate_pupil_ray(opm,indx,xy,rel_p1.v(xy),r_target.v(xy),fld,wvl);
                        still_iterating = true;
                        clip_indx = indx;
                    }
                    else still_iterating = false;
                }
            }
            catch (TraceException ray_error) {
                ray_pkg = ray_error.ray_pkg;
                Integer indx = ray_error.surf;
                if (Objects.equals(indx,clip_indx)) {
                    var r_target = Lists.get(sm.ifcs,clip_indx).edge_pt_target(start_dir);
                    var p = Lists.get(ray_pkg.ray,clip_indx).p;
                    var r_ray = Math.copySign(Math.sqrt(p.x*p.x + p.y*p.y), r_target.v(xy));
                    var r_error = r_ray - r_target.v(xy);
//                    logger.debug(f" A {xy_str[xy]} = {rel_p1[xy]:10.6f}:   "
//                            f"blocked at {clip_indx}, del={r_error:8.1e}, "
//                            "exiting")
                    still_iterating = false;
                }
                else {
                    var r_target = Lists.get(sm.ifcs,indx).edge_pt_target(start_dir);
                    rel_p1 = iterate_pupil_ray(opm,indx,xy,rel_p1.v(xy),r_target.v(xy),fld,wvl);
                    still_iterating = true;
                    clip_indx = indx;
                }
            }
        }
        var vig = 1.0 - (rel_p1.v(xy)/start_dir.v(xy));
//        logger.info(f" ray: ({start_dir[0]:2.0f}, {start_dir[1]:2.0f}), "
//                f"vig={vig:8.4f}, limited at ifcs[{clip_indx}]")
        return new VigResult(vig,clip_indx,ray_pkg);
    }

    /**
     * Find the limiting aperture and return the vignetting factor.
     *
     *     Args:
     *         opm: :class:`~.OpticalModel` instance
     *         xy: 0 or 1 depending on x or y axis as the pupil direction
     *         start_dir: the unit length starting pupil coordinates, e.g [1., 0.].
     *                    This establishes the radial direction of the ray iteration.
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         max_iter_count: fail-safe limit on aperture search
     *
     *     Returns:
     *         (**vig**, **clip_indx**, **ray_pkg**)
     *
     *         - **vig** - vignetting factor
     *         - **clip_indx** - the index of the limiting interface
     *         - **ray_pkg** - the vignetting-limited ray
     */
    public static VigResult calc_vignetted_ray_by_bisection(
            OpticalModel opm,
            int xy,
            Vector2 start_dir,
            Field fld,
            double wvl,
            Integer max_iter_count) {
//        logger.debug(f"fld={fld.yf:5.2f}, [{start_dir[0]:5.2f}, "
//                f"{start_dir[1]:5.2f}]")
        if (max_iter_count == null) max_iter_count = 10;

        var rel_p1 = start_dir;
        Integer clip_indx = null;
        var iter_count = 0;
        var step_size = 1.0;
        RayPkg ray_pkg = null;

        while (iter_count < max_iter_count) {
            iter_count++;
            try {
                step_size /= 2.0;
                var options = new TraceOptions();
                options.apply_vignetting = false;
                options.check_apertures = true;
                options.pt_inside_fuzz=1e-4;
                ray_pkg = Trace.trace_base(opm,rel_p1.as_array(),fld,wvl,options);
                rel_p1 = start_dir.times(step_size).plus(rel_p1);
            }
            catch (TraceException ray_error) {
                ray_pkg = ray_error.ray_pkg;
                clip_indx = ray_error.surf;
                rel_p1 = start_dir.times(-step_size).plus(rel_p1);
//                logger.debug(f"{xy_str[xy]} = {rel_p1[xy]:10.6f}: "
//                        f"blocked at {clip_indx}")
            }
        }
        var vig = 1.0 - (rel_p1.v(xy)/start_dir.v(xy));
//        logger.debug(f"   {vig=:7.4f}, {clip_indx=}")
        return new VigResult(vig,clip_indx,ray_pkg);
    }

    static class R_Pupil_Coordinate implements ScalarObjectiveFunction {
        OpticalModel opt_model;
        int indx;
        int xy;
        Field fld;
        double wvl;
        double r_target;

        public R_Pupil_Coordinate(OpticalModel opt_model, int indx, int xy, Field fld, double wvl, double r_target) {
            this.opt_model = opt_model;
            this.indx = indx;
            this.xy = xy;
            this.fld = fld;
            this.wvl = wvl;
            this.r_target = r_target;
        }

        @Override
        public Double eval(double xy_coord) {
            var rel_p1 = Vector2.vector2_0.set(xy, xy_coord);
            RayPkg ray_pkg;
            try {
                var options = new TraceOptions();
                options.apply_vignetting = false;
                options.check_apertures = false;
                ray_pkg = Trace.trace_base(opt_model,rel_p1.as_array(),fld,wvl,options);
            }
            catch (TraceException ray_err) {
                ray_pkg = ray_err.ray_pkg;
                if (ray_err instanceof TraceMissedSurfaceException) {
                    if (ray_err.surf <= indx) {
                        ray_err.rel_p1 = rel_p1;
                        throw ray_err;
                    }
                }
                else if (ray_err.surf < indx) {
                    ray_err.rel_p1 = rel_p1;
                    throw ray_err;
                }
            }
            // compute the radial distance to the intersection point
            var p = Lists.get(ray_pkg.ray,indx).p;
            var r_ray = Math.copySign(Math.sqrt(p.x*p.x + p.y*p.y), r_target);
            var delta = r_ray - r_target;
//            logger.debug(f"  {xy_coord=:8.5f}   {r_ray=:8.5f}    "
//                    f"delta={delta:9.2g}")
            return delta;
        }
    }

    /**
     * iterates a ray to r_target on interface indx, returns aim points on
     *     the paraxial entrance pupil plane
     *
     *     If indx is None, i.e. a floating stop surface, returns r_target.
     *
     *     If the iteration fails, a :class:`~.traceerror.TraceError` will be raised
     *
     *     Args:
     *         opm: :class:`~.OpticalModel` instance
     *         indx: index of interface whose edge is the iteration target
     *         xy: 0 or 1 depending on x or y axis as the pupil direction
     *         start_r0: iteration starting point
     *         r_target: clear aperture radius that is the iteration target.
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *
     *     Returns:
     *         start_coords: pupil coordinates for ray thru r_target on ifc indx.
     */
    public static Vector2 iterate_pupil_ray(OpticalModel opt_model, Integer indx, int xy, double start_r0, double r_target, Field fld, double wvl) {
        Vector2 start_coord = Vector2.vector2_0;
        double start_r = 0;
        if (indx != null) {
            var objective_fn = new R_Pupil_Coordinate(opt_model,indx,xy,fld,wvl,r_target);
            try {
                start_r = SecantSolver.find_root(objective_fn, start_r0, 50, 1e-6).root;
            }
            catch (TraceException rt_err) {
//                logger.debug(f"  {type(rt_err).__name__}: surf={rt_err.surf}    "
//                        f"rel_p1={rt_err.rel_p1[xy]=:8.5f}   ")
                start_r = 0.9*rt_err.rel_p1.v(xy);
            }
            return start_coord.set(xy,start_r);
        }
        else
            return start_coord.set(xy,r_target);
    }
}
