package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.SecantSolver;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

// Vignetting and clear aperture setting operations
public class Vigcalc {

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
