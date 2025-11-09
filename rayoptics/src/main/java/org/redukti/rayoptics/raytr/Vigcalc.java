// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.SecantSolver;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.specs.ValueKey;
import org.redukti.rayoptics.util.Lists;

import java.util.Objects;

// Vignetting and clear aperture setting operations
public class VigCalc {

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
        var rayset = Trace.trace_boundary_rays(opm,new TraceOptions());
        for (int i = 0; i < opm.seq_model.ifcs.size(); i++) {
            var ifc = opm.seq_model.ifcs.get(i);
            var max_ap = -1.0e+10;
            boolean update = true;
            for (var f: rayset) {
                for (var p: f) {
                    var ray = p.ray;
                    if (ray.size() > i) {
                        var ap = Math.sqrt(ray.get(i).p.x*ray.get(i).p.x + ray.get(i).p.y*ray.get(i).p.y);
                        if (ap > max_ap)
                            max_ap = ap;
                    }
                    else
                        update = false;
                }
            }
            if (update)
                ifc.set_max_aperture(max_ap);
        }
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
            calc_vignetting_for_field(opm,fld,wvl,use_bisection);
        }
    }

    /**
     * From existing stop size, calculate pupil spec and vignetting.
     *
     *     Use the upper Y marginal ray on-axis (field #0) and iterate until it
     *     goes through the edge of the stop surface. Use the object or image
     *     segments of this ray to update the pupil specification value
     *     e.g. EPD, NA or f/#.
     */
    public static void set_pupil(OpticalModel opm) {
        var sm = opm.seq_model;
        if (sm.stop_surface == null) {
            System.out.println("floating stop surface");
            return;
        }
        var osp = opm.optical_spec;

        // iterate the on-axis marginal ray thru the edge of the stop.
        var fld_foc = osp.lookup_fld_wvl_focus(0);
        var fld = fld_foc.first;
        var wvl = fld_foc.second;
        var foc = fld_foc.third;
        var stop_radius = Lists.get(sm.ifcs,sm.stop_surface).surface_od();
        var start_coords = iterate_pupil_ray(opm,sm.stop_surface,1,1.0,stop_radius,fld,wvl);

        // trace the real axial marginal ray
        var options = new TraceOptions();
        options.output_filter = null;
        options.rayerr_filter = null;
        options.apply_vignetting = false;
        options.check_apertures = true;
        var ray_result = Trace.trace_safe(opm,start_coords,fld,wvl,options);
        var ray_pkg = ray_result.pkg;
        var ray_err = ray_result.err;

        var obj_img_key = osp.pupil.key.imageKey;
        var pupil_spec = osp.pupil.key.valueKey;
        var pupil_value_orig = osp.pupil.value;

        if (obj_img_key == ImageKey.Object) {
            if (pupil_spec == ValueKey.EPD) {
                var rs1 = ray_pkg.ray.get(1);
                var ht = rs1.p.y;
                osp.pupil.value = 2.0 * ht;
            }
            else {
                var rs0 = ray_pkg.ray.get(0);
                var slp0 = rs0.d.y / rs0.d.z;
                if (pupil_spec == ValueKey.NA) {
                    var n0 = sm.central_rndx(0);
                    osp.pupil.value = n0*rs0.d.y;
                }
                else if (pupil_spec == ValueKey.Fnum) {
                    osp.pupil.value = 1.0/(2.0*slp0);
                }
            }
        }
        else if (obj_img_key == ImageKey.Image) {
            var rsm2 = Lists.get(ray_pkg.ray,-2);
            if (pupil_spec == ValueKey.EPD) {
                var ht = rsm2.p.y;
                osp.pupil.value = 2.0 * ht;
            }
            else {
                var slpk = rsm2.d.y/rsm2.d.z;
                if (pupil_spec == ValueKey.NA) {
                    var nk = sm.central_rndx(0);
                    osp.pupil.value = -nk * rsm2.d.y;
                }
                else if (pupil_spec == ValueKey.Fnum) {
                    osp.pupil.value = -1.0/(2.0 * slpk);
                }
            }
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
            Boolean use_bisection) {
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
                result = calc_vignetted_ray_by_bisection(opm,xy,startv,fld,wvl,null);
            }
            else {
                result = calc_vignetted_ray(opm,xy,startv,fld,wvl,null);
            }
            vig_factors[i] = result.vig;
        }

        // update the field's vignetting factors
        fld.vux = vig_factors[0];
        fld.vlx = vig_factors[1];
        fld.vuy = vig_factors[2];
        fld.vly = vig_factors[3];
    }

    public static VigResult calc_vignetted_ray(
            OpticalModel opm,
            int xy,
            Vector2 start_dir,
            Field fld,
            double wvl,
            Integer max_iter_count) {
        if (max_iter_count == null) max_iter_count = 10;

        var rel_p1 = start_dir;
        var sm = opm.seq_model;
        var still_iterating = true;
        Integer last_index = null;
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
                if (last_index != null)
                    // fall through and exit
                    still_iterating = false;
                else {
                    // this is the first time through
                    // iterate to find the ray that goes through the edge
                    // of the stop surface
                    Integer indx;
                    indx = stop_indx = sm.stop_surface;
                    if (stop_indx != null) {
                        var r_target = Lists.get(sm.ifcs,stop_indx).edge_pt_target(start_dir);
                        rel_p1 = iterate_pupil_ray(opm,indx,xy,rel_p1.v(xy),r_target.v(xy),fld,wvl);
                        still_iterating = true;
                        last_index = indx;
                    }
                    else still_iterating = false;
                }
            }
            catch (TraceException ray_error) {
                ray_pkg = ray_error.ray_pkg;
                Integer indx = ray_error.surf;
                if (Objects.equals(indx,last_index)) {
                    still_iterating = false;
                }
                else {
                    var r_target = Lists.get(sm.ifcs,indx).edge_pt_target(start_dir);
                    rel_p1 = iterate_pupil_ray(opm,indx,xy,rel_p1.v(xy),r_target.v(xy),fld,wvl);
                    still_iterating = true;
                    last_index = indx;
                }
            }
        }
        var vig = 1.0 - (rel_p1.v(xy)/start_dir.v(xy));
        return new VigResult(vig,last_index,ray_pkg);
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
     *         (**vig**, **last_indx**, **ray_pkg**)
     *
     *         - **vig** - vignetting factor
     *         - **last_indx** - the index of the limiting interface
     *         - **ray_pkg** - the vignetting-limited ray
     */
    public static VigResult calc_vignetted_ray_by_bisection(
            OpticalModel opm,
            int xy,
            Vector2 start_dir,
            Field fld,
            double wvl,
            Integer max_iter_count) {
        if (max_iter_count == null) max_iter_count = 10;

        var rel_p1 = start_dir;
        var still_iterating = true;
        Integer last_index = null;
        var iter_count = 0;
        var step_size = 1.0;
        RayPkg ray_pkg = null;

        while (still_iterating && iter_count < max_iter_count) {
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
                last_index = ray_error.surf;
                rel_p1 = start_dir.times(-step_size).plus(rel_p1);
            }
        }
        var vig = 1.0 - (rel_p1.v(xy)/start_dir.v(xy));
        return new VigResult(vig,last_index,ray_pkg);
    }

    static class R_Pupil_Coordinate implements SecantSolver.ObjectiveFunction {
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
        public double eval(double xy_coord) {
            var rel_p1 = Vector2.vector2_0;
            rel_p1.set(xy, xy_coord);
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
                    if (ray_err.surf <= indx)
                        throw ray_err;
                }
                else if (ray_err.surf < indx)
                    throw ray_err;
            }
            var ray = ray_pkg.ray;
            var r_ray = Lists.get(ray,indx).p.v(xy);
            return r_ray - r_target;
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
        if (indx == null) {
            var objective_fn = new R_Pupil_Coordinate(opt_model,indx,xy,fld,wvl,r_target);
            try {
                start_r = SecantSolver.find_root(objective_fn, start_r0, 50, 1e-6);
            }
            catch (TraceException ray_err) {
                start_r = 0;
            }
            start_coord.set(xy,start_r);
        }
        else
            start_coord.set(xy,r_target);
        return start_coord;
    }
}
