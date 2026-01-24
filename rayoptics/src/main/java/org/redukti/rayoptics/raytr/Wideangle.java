// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.*;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

import java.util.Objects;

public class Wideangle {

    /**
     * Trace a ray thru the center of the entrance pupil at z_enp.
     *
     *     Args:
     *
     *         z_enp:      The z distance from the 1st interface of the
     *                     entrance pupil for the field angle dir0
     *         seq_model:  The sequential model
     *         stop_idx:   index of the aperture stop interface
     *         dir0:       direction cosine vector in object space
     *         obj_dist:   object distance to first interface
     *         wvl:        wavelength of raytrace (nm)
     */
    public static Pair<Vector3,RayResult> enp_z_coordinate(double z_enp, SequentialModel seq_model, int stop_idx, Vector3 dir0, double obj_dist, double wvl) {
        var obj2enp_dist = -(obj_dist + z_enp);
        var pt1 = new Vector3(0., 0., obj2enp_dist);
        var rot_mat = Matrix3.rot_v1_into_v2(Vector3.vector3_001, dir0);
        var pt0 = rot_mat.multiply(pt1).minus(pt1);
        RayPkg ray_pkg = null;
        RayResult rr = null;
        Vector3 final_coord = null;
        try {
            var options = new RayTraceOptions();
            options.intersect_obj = false;
            ray_pkg = RayTrace.trace(seq_model,pt0,dir0,wvl,options);
            rr = new RayResult(ray_pkg,null);
            final_coord = Lists.get(ray_pkg.ray,stop_idx).p;
        }
        catch (TraceException ray_error) {
            //print(f'  ray_error: "{type(ray_error).__name__}", '
            //              f'{ray_error.surf=}')
            //logger.debug(f'   ray_error: "{type(ray_error).__name__}", '
            //f'{ray_error.surf=}')
            ray_pkg = ray_error.ray_pkg;
            rr = new RayResult(ray_pkg,ray_error);
            final_coord = Vector3.ZERO;
        }
        return new Pair<>(final_coord,rr);
    }

    /**
     * Locate the z center of the real pupil for `fld`
     */
    public static Pair<Double,RayResult> find_real_enp(OpticalModel opm, Integer stop_idx, Field fld, double wvl, String selector) {
        if (Objects.equals(selector,"rev1"))
            return find_real_enp_rev1(opm, stop_idx, fld, wvl, null);
        else
            return find_real_enp_orig(opm, stop_idx, fld, wvl);
    }
    public static Pair<Double,RayResult> find_real_enp(OpticalModel opm, Integer stop_idx, Field fld, double wvl) {
        return find_real_enp(opm,stop_idx,fld,wvl,"rev1");
    }

    /**
     * returns the function value or None, if fct failed to evalute.
     */

    public static class Enp_z_coordinate_wrapper implements ScalarObjectiveFunction {
        SequentialModel seq_model;
        int stop_idx;
        Vector3 dir0;
        double obj_dist;
        double wvl;

        public Enp_z_coordinate_wrapper(SequentialModel seq_model, int stop_idx, Vector3 dir0, double obj_dist, double wvl) {
            this.seq_model = seq_model;
            this.stop_idx = stop_idx;
            this.dir0 = dir0;
            this.obj_dist = obj_dist;
            this.wvl = wvl;
        }
        public Double eval(double z_enp) {
            var coord_rr = enp_z_coordinate(z_enp,seq_model,stop_idx,dir0,obj_dist,wvl);
            var final_coord = coord_rr.first;
            var rr = coord_rr.second;
            if (rr.err == null) {
                var ht_at_stop = final_coord.y;
                return ht_at_stop;
            }
            else
                return null;
        }
    }

    /**
     * Locate the z center of the real pupil for `fld`, wrt 1st ifc
     *
     *     This function implements a 2 step process to finding the chief ray
     *     for `fld` and `wvl` for wide angle systems. `fld` should be of type
     *     ('object', 'angle'), even for finite object distances.
     *
     *     The first phase searches for the window of pupil locations by sampling the
     *     z coordinate starting from the paraxial pupil location. The real pupil can move either inward or outward from the paraxial pupil location. As soon as 2 successful rays are traced, the search direction is updated if needed. The search continues until z_enp values are found giving rays that straddle the stop center. If no interval is found that contains the central ray, a finer sampled search is done to find the edges more accurately. If only a single successful trace is in hand, a second, more finely subdivided search is conducted around the successful point.
     *
     *     The outcome is a range, start_z -> end_z, an estimate of where the crossing point is (z_estimate), and a ray iteration (using :func:`~.raytr.wideangle.find_z_enp_on_interval`) to find the center of the stop surface.
     */
    public static Pair<Double,RayResult> find_real_enp_rev1(OpticalModel opm, Integer stop_idx, Field fld, double wvl, Boolean check_direction) {
        if (check_direction == null) check_direction = true;
        var sm = opm.seq_model;
        var osp = opm.optical_spec;
        var fov = osp.fov;
        var fod = osp.parax_data.fod;
        if (stop_idx == null) stop_idx = 1;

        var coord = osp.obj_coords(fld);
        var pt0 = coord.pt;
        var dir0 = coord.dir;

        // If there is aim_info, try it and return if good.
        if (fld.z_enp != null) {
            var z_enp = fld.z_enp;
            var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
            var final_coord = coord_rr.first;
            var rr = coord_rr.second;
            var tol = 1.48e-08;
            if (Math.abs(final_coord.y)<tol)
                return new Pair<>(z_enp,rr);
        }

        // filter on-axis chief ray. z_enp is the paraxial result.
        var z_enp_0 = fod.enp_dist;
        if (dir0.z == 1.) {
            // axial chief ray
            var coord_rr = enp_z_coordinate(z_enp_0,sm,stop_idx,dir0,fod.obj_dist,wvl);
            var final_coord = coord_rr.first;
            var rr = coord_rr.second;
            //logger.info(f"  axial chief {z_enp_0=:8.4f}  {rr.err is None}")
            return new Pair<>(z_enp_0,rr);
        }
        Pair<Double,Double> start_z = null;
        Pair<Double,Double> prev_z = null;
        Pair<Double,Double> end_z = null;
        var del_z = -z_enp_0/16.0;
        var z_enp = z_enp_0;
        boolean keep_going = true;
        var direction = "first";
        int first_surf_misses = 0;
        // protect against infinite loops
        int trial = 0;
        // if the trace succeeds 5 times in a row, go on to the next phase
        int successes = 0;
        while (keep_going && trial < 64 && first_surf_misses < 2) {
            var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
            var rr = coord_rr.second;
            var final_coord = coord_rr.first;
            if (rr.err == null) {
                var ht_at_stop = final_coord.y;
                //            logger.debug(f"  ray passed at z_enp={z_enp:10.5f},  "
                //                         f"{ht_at_stop=:7.3f}")
                successes++;
                if (start_z == null)
                    start_z = new Pair<>(z_enp, ht_at_stop);
                prev_z = end_z;
                end_z = new Pair<>(z_enp, ht_at_stop);
                if (successes > 1) {
                    // check for a zero crossing, if so, we're done
                    if (prev_z.second * end_z.second < 0)
                        keep_going = false;
                }
                if (successes == 2 && check_direction) {
                    // check that we're searching in the right direction
                    if (Math.abs(start_z.second) < Math.abs(end_z.second)) {
                        if (Objects.equals(direction,"first")) {
                            // first time through, reverse direction and start on
                            // the other side of z_enp_0.
                            //logger.debug("  --> reverse search direction")
                            del_z = -del_z;
                            z_enp = z_enp_0;
                            direction = "reverse";
                            var tmp = end_z;
                            end_z = start_z;
                            start_z = tmp;
                        }
                    }
                }
            }
            else {
                // logger.debug(f"  ray failed at z_enp={z_enp:10.5f}, "
                //                         f"{type(rr.err).__name__} at surf {rr.err.surf}")
                if (rr.err instanceof TraceMissedSurfaceException) {
                    // if the first surface was missed, then exit
                    if (rr.err.surf == 1) {
                        //logger.debug(f"Num 1st surf misses {first_surf_misses}: "
                        //                                 +msg1)
                        del_z = -del_z;
                        z_enp = z_enp_0;
                        first_surf_misses++;
                    }
                }
                // if the first surface was missed, then exit
                if (start_z != null) {
                    if (Objects.equals(direction,"first")) {
                        // logger.debug("  --> reverse search direction")
                        del_z = -del_z;
                        z_enp = z_enp_0;
                        direction = "reverse";
                        var tmp = end_z;
                        end_z = start_z;
                        start_z = tmp;
                    }
                    else keep_going = false;
                }
            }
            z_enp += del_z;
            trial += 1;
        }

        var z_enp_a = start_z.first;
        var ht_at_stop_a = start_z.second;
        var z_enp_b = end_z.first;
        var ht_at_stop_b = end_z.second;

        Double a = null, b = null;
        // If start and end are equal, then only one ray was successful.
        // Sample z_enp evenly 1 del_z to either side.
        if (Objects.equals(z_enp_a, z_enp_b)) {
            var start_new = z_enp_a - del_z;
            var end_new = z_enp_b + del_z;
            start_z = null;
            end_z = null;
            for (var x: linspace(start_new,end_new,8)) {
                z_enp = x;
                var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
                var final_coord = coord_rr.first;
                var rr = coord_rr.second;
                if (rr.err == null) {
                    var ht_at_stop = final_coord.y;
                    if (start_z == null)
                        start_z = new Pair<>(z_enp,ht_at_stop);
                    end_z = new Pair<>(z_enp,ht_at_stop);
                }
            }
            a = start_z.first;
            b = end_z.first;
        }
        // test for crossing between the end points
        else if (ht_at_stop_a * ht_at_stop_b < 0) {
            // yes, there was a crossing somewhere in the interval
            //        # set the bracket using start_z
            a = z_enp_a;
            b = z_enp_b;
            if (prev_z != null) {
                // if there's a previous sample, see if the crossing can be
                //            # more tightly bracketed.
                var z_enp_c = prev_z.first;
                var ht_at_stop_c = prev_z.second;
                if (ht_at_stop_c * ht_at_stop_b < 0) {
                    // set the smallest bracket using prev_z
                    var pt_a = prev_z;
                    start_z = prev_z;
                    a = z_enp_c;
                    b = z_enp_b;
                }
            }
        }
        else {
            // we haven't found a zero crossing yet
            //        # refine the interval by finding the effective "edge" of the beam
            var z_enp_coordinate_wrapper = new Enp_z_coordinate_wrapper(sm,stop_idx,dir0,fod.obj_dist,wvl);
            var edge_b = find_edge(z_enp_coordinate_wrapper,
                    z_enp_b,
                    z_enp_b+del_z,
                    6);
            var z_enp_edge_b = edge_b.first;
            var ht_at_stop_edg_b = edge_b.second;
            //logger.debug(f"  edge_b found at at z_enp={z_enp_edge_b:10.5f},  "
            //                     f"{ht_at_stop_edg_b=:7.3f}")
            if (ht_at_stop_edg_b * ht_at_stop_b < 0) {
                start_z = new Pair<>(z_enp_b, ht_at_stop_b);
                end_z = new Pair<>(z_enp_edge_b, ht_at_stop_edg_b);
                a = z_enp_b;
                b = z_enp_edge_b;
            }
            else {
                // find the other effective "edge" of the beam
                z_enp_coordinate_wrapper = new Enp_z_coordinate_wrapper(sm,stop_idx,dir0,fod.obj_dist,wvl);
                var edge_a = find_edge(z_enp_coordinate_wrapper,
                        z_enp_a,
                        z_enp_a-del_z,
                        6);
                var z_enp_edge_a = edge_a.first;
                var ht_at_stop_edg_a = edge_a.second;
                // logger.debug(f"  edge_a found at at z_enp={z_enp_edge_a:10.5f},  "
                //                         f"{ht_at_stop_edg_a=:7.3f}")
                if (ht_at_stop_edg_a * ht_at_stop_a < 0) {
                    // found an interval containing a crossover point
                    start_z = new Pair<>(z_enp_a, ht_at_stop_a);
                    end_z = new Pair<>(z_enp_edge_a, ht_at_stop_edg_a);
                    a = z_enp_a;
                    b = z_enp_edge_a;
                }
                else {
                    //  there is no ray that passes thru the center of the stop
                    //                # surface.
                    System.err.println(String.format("chief ray trace failed at field %3.1f",fld.yv()));
                    var z_enp_cntr = z_enp_edge_a + (z_enp_edge_b - z_enp_edge_a)/2;
                    var coord_rr = enp_z_coordinate(z_enp_cntr,sm,stop_idx,dir0,fod.obj_dist,wvl);
                    var final_coord = coord_rr.first;
                    var rr = coord_rr.second;
                    var ht_at_stop = final_coord.y;
                    // logger.debug(f"  fld: {fld.yv:3.1f}:   {z_enp_edge_a=:8.4f}  "
                    //                    f"{z_enp_edge_b=:8.4f}  {z_enp_cntr=:8.4f}  "
                    //                    f"{ht_at_stop=:10.2e}")
                    return new Pair<>(z_enp_b, rr);
                }
            }
        }
        // compute the straightline crossing pt given the interval
        double z_estimate;
        if (M.isZero(end_z.second - start_z.second)) {
            z_estimate = start_z.first;
        }
        else {
            z_estimate = start_z.first - ((end_z.first - start_z.first) /
                    (end_z.second - start_z.second)) * start_z.second;
        }

        //    logger.debug(f"  trials: {trial},   {successes=}")
        //    logger.debug(f"  z_enp: start_z={a:10.5f} z_estimate={z_estimate:10.5f}  "
        //                 f"end_z={b:10.5f}")
        //    logger.debug(f"  ht_at_stop: start_z={start_z[1]:10.5f} "
        //                 f"end_z={end_z[1]:10.5f}")

        var result = find_z_enp_on_interval(opm,stop_idx,a,b,z_estimate,fld,wvl);
        var start_coord = result.first;
        var rr = result.second;
        z_enp = start_coord.z;
        var final_coord = Lists.get(rr.pkg.ray,stop_idx).p;
        var ht_at_stop = final_coord.y;
        //logger.info(f"fld: {fld.yv:3.1f}:   {z_enp=:8.4f}  {ht_at_stop=:10.2e}")
        return new Pair<>(z_enp,rr);
    }

    static Pair<Double,Double> find_edge(ScalarObjectiveFunction f, double a, double b, Integer max_iter) {
        // use binary search to find the edge of the fct's range.
        if (max_iter == null) max_iter = 3;
        var fa = f.eval(a);
        var fb = f.eval(b);
        for (int i = 0; i < max_iter; i++) {
            var c = a + (b - a)/2;
            var fc = f.eval(c);
            if (fc == null) {
                b = c;
                fb = fc;
            }
            else {
                a = c;
                fa = fc;
            }
        }
        if (fb == null)
            return new Pair<>(a,fa);
        else
            return new Pair<>(b,fb);
    }

    static final class Eval_Z_Enp_Function implements ScalarObjectiveFunction {
        final SequentialModel seq_model;
        final int stop_idx;
        final Vector3 dir0;
        final double obj_dist;
        final double wvl;
        final double y_target;
        RayResult rr;

        public Eval_Z_Enp_Function(SequentialModel seq_model, int stop_idx, Vector3 dir0, double obj_dist, double wvl, double y_target) {
            this.seq_model = seq_model;
            this.stop_idx = stop_idx;
            this.dir0 = dir0;
            this.obj_dist = obj_dist;
            this.wvl = wvl;
            this.y_target = y_target;
        }

        @Override
        public Double eval(double z_enp) {
            var coord_rr = enp_z_coordinate(z_enp,seq_model,stop_idx,dir0,obj_dist,wvl);
            var final_coord = coord_rr.first;
            rr = coord_rr.second;
            return final_coord.y - y_target;
        }
    }

    /**
     * iterates a ray to [0, 0] on interface stop_ifc, returning aim info
     *
     *     This function finds the entrance pupil location, z_enp, inside a range of pupil locations. The rays in the interval must be trace without throwing TraceError exceptions (ignoring aperture clipping).
     *
     *     Args:
     *
     *         opt_model:  input OpticalModel
     *         stop_idx:   index of the aperture stop interface
     *         start_z:    lower bound of the z_enp interval to be searched
     *         end_z:      upper bound of the z_enp interval to be searched
     *         z_estimate: estimate of pupil location. this estimate must support
     *                     a raytrace up to stop_ifc
     *         fld:        field point
     *         wvl:        wavelength of raytrace (nm)
     *
     *     Returns z distance from 1st interface to the entrance pupil.
     *
     *     If stop_ifc is None, i.e. a floating stop surface, returns paraxial
     *     entrance pupil.
     *
     *     If the iteration fails, a TraceError will be raised
     */
    public static Pair<Vector3,RayResult> find_z_enp_on_interval(OpticalModel opt_model, Integer stop_idx, double start_z, double end_z, double z_estimate, Field fld, double wvl) {
        RayResult rr = null;
        var sm = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        var fod = osp.parax_data.fod;
        var z_enp = z_estimate;
        var obj_dist = fod.obj_dist;

        var coord = osp.obj_coords(fld);
        var pt0 = coord.pt;
        var dir0 = coord.dir;
        double y_target = 0.;
        Vector3 start_coords = null;
        boolean converged = false;
        if (stop_idx != null) {
            // do 1D iteration if field and target points are zero in x
            var fn = new Eval_Z_Enp_Function(sm,stop_idx,dir0,obj_dist,wvl,y_target);
            try {
                var result = SecantSolver.find_root(fn,z_enp,50,1.48e-8);
                z_enp = result.root;
                converged = result.converged;
                rr = fn.rr;
                var ht_at_stop = Lists.get(rr.pkg.ray,stop_idx).p.y;
                if (Math.abs(ht_at_stop - y_target) < 1e-6)
                    converged = true;
                start_coords = new Vector3(0.,0.,z_enp);
            }
            catch (TraceException ray_err) {
                converged = false;
            }
            if (!converged) {
                //                logger.debug(f'  {results.method} converged: '
                //                             f'{results.converged},  # fct evals='
                //                             f'{results.function_calls}  msg: "{results.flag}" '
                //                             f'{z_enp=:9.4f}')
                try {
                    var result = BrentSolver.find_root(start_z, end_z, fn);
                    z_enp = result.root;
                    converged = result.converged;
                    start_coords = new Vector3(0.,0.,z_enp);
                }
                catch (Exception e) {
                    throw new IllegalStateException();
                }
            }
        }
        else
            start_coords = new Vector3(0., 0., fod.enp_dist);
        //    logger.debug(f'  {results.method} converged: {results.converged},  '
        //                 f'# fct evals={results.function_calls}  msg: "{results.flag}"')
        return new Pair<>(start_coords,rr);
    }

    public static double[] linspace(double start, double end, int num) {
        double[] result = new double[num];
        if (num == 1) {
            result[0] = start;
            return result;
        }
        double step = (end - start) / (num - 1);
        for (int i = 0; i < num; i++) {
            result[i] = start + step * i;
        }
        return result;
    }

    /**
     * Locate the z center of the real pupil for `fld`, wrt 1st ifc
     *
     *     This function implements a 2 step process to finding the chief ray
     *     for `fld` and `wvl` for wide angle systems. `fld` should be of type
     *     ('object', 'angle'), even for finite object distances.
     *
     *     The first phase searches for the window of pupil locations by sampling the
     *     z coordinate from the paraxial pupil location towards the first interface
     *     vertex. Failed rays are discarded until a range of z coordinates is found
     *     where rays trace successfully. If the search forward is unsuccessful (i.e.
     *     winds up missing the 1st surface), the search is restarted moving away from
     *     the first interface. If only a single successful trace is in hand, a
     *     second, more finely subdivided search is conducted about the successful
     *     point.
     *
     *     The outcome is a range, start_z -> end_z, that is divided in 3 and a ray
     *     iteration (using :func:`~.raytr.wideangle.find_z_enp`) to find the center
     *     of the stop surface is done. Sometimes the start point doesn't produce a
     *     solution; use of the mid-point as a start is a reliable second try.
     */
    public static Pair<Double,RayResult> find_real_enp_orig(OpticalModel opm, Integer stop_idx, Field fld, double wvl) {
        var sm = opm.seq_model;
        var osp = opm.optical_spec;
        var fod = osp.parax_data.fod;

        if (stop_idx == null) stop_idx = 1;

        RayResult rr = null;

        var coord = osp.obj_coords(fld);
        var pt0 = coord.pt;
        var dir0 = coord.dir;

        if (fld.z_enp != null) {
            var z_enp = fld.z_enp;
            var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
            double tol = 1.48e-8;
            if (Math.abs(coord_rr.first.y)<tol)
                return new Pair<>(z_enp,coord_rr.second);
        }

        var z_enp_0 = fod.enp_dist;
        if (dir0.z == 1.0) { // axial chief ray
            var coord_rr = enp_z_coordinate(z_enp_0,sm,stop_idx,dir0,fod.obj_dist,wvl);
            return new Pair<>(z_enp_0,coord_rr.second);
        }

        Double start_z = null;
        Double end_z = null;
        var del_z = -z_enp_0/16.0;
        var z_enp = z_enp_0;
        boolean keep_going = true;
        int first_surf_misses = 0;
        // protect against infinite loops
        int trial = 0;
        // if the trace succeeds 5 times in a row, go on to the next phase
        int successes = 0;
        while (keep_going && successes < 4 && trial < 64 && first_surf_misses < 2) {
            var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
            rr = coord_rr.second;
            if (rr.err == null) {
                successes++;
                if (start_z == null)
                    start_z = z_enp;
                end_z = z_enp;
            }
            else {
                if (rr.err instanceof TraceMissedSurfaceException) {
                    if (rr.err.surf == 1) {
                        del_z = -del_z;
                        z_enp = z_enp_0;
                        first_surf_misses++;
                    }
                    // if the first surface was missed, then exit
                    if (start_z != null)
                        keep_going = false;
                }
            }
            z_enp += del_z;
            trial += 1;
        }

        // If start and end are equal, then only one ray was successful.
        // Sample z_enp evenly 1 del_z to either side.
        if (start_z != null && start_z.equals(end_z)) {
            var start_new = start_z - del_z;
            var end_new = end_z + del_z;
            start_z = null;
            end_z = null;
            for (var x: linspace(start_new,end_new,8)) {
                z_enp = x;
                var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
                rr = coord_rr.second;
                if (rr.err == null) {
                    if (start_z == null)
                        start_z = z_enp;
                    end_z = z_enp;
                }
            }
        }
        // Now that candidate z_enps have been identified that trace without
        // ray failures, iterate to find the ray thru the stop center
        double[] starting_pts = {start_z, (start_z + end_z)/2.0, end_z};
        Vector3 start_coord = null;
        for (var init_z: starting_pts) {
            var result = find_z_enp(opm,stop_idx,init_z,fld,wvl);
            rr = result.second;
            start_coord = result.first;
            if (rr.err == null)
                break;
        }
        z_enp = start_coord.z;

        var final_coord = Lists.get(rr.pkg.ray,stop_idx).p;
        var y_ht = final_coord.y;
        return new Pair<>(z_enp,rr);
    }

    /**
     * iterates a ray to [0, 0] on interface stop_ifc, returning aim info
     *
     *     Args:
     *
     *         opt_model:  input OpticalModel
     *         stop_idx:   index of the aperture stop interface
     *         z_enp_0:    estimate of pupil location. this estimate must support
     *                     a raytrace up to stop_ifc
     *         fld:        field point
     *         wvl:        wavelength of raytrace (nm)
     *
     *     Returns z distance from 1st interface to the entrance pupil.
     *
     *     If stop_ifc is None, i.e. a floating stop surface, returns paraxial
     *     entrance pupil.
     *
     *     If the iteration fails, a TraceError will be raised
     */
    public static Pair<Vector3,RayResult> find_z_enp(OpticalModel opt_model, Integer stop_idx, double z_enp_0, Field fld, double wvl) {
        RayResult rr = null;
        var seq_model = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        var fod = osp.parax_data.fod;
        var z_enp = z_enp_0;
        var obj_dist = fod.obj_dist;

        var coord = osp.obj_coords(fld);
        var pt0 = coord.pt;
        var dir0 = coord.dir;
        double y_target = 0.;
        Vector3 start_coords = null;

        if (stop_idx != null) {
            // do 1D iteration if field and target points are zero in x
            try {
                var func = new Eval_Z_Enp_Function(seq_model,stop_idx,dir0,obj_dist,wvl,y_target);
                var result = SecantSolver.find_root(func,z_enp,50,1.48e-8);
                z_enp = result.root;
                rr = func.rr;
            }
            catch (TraceException ray_err) {
                z_enp = 0.0;
            }
            start_coords = new Vector3(0.,0.,z_enp);
        }
        else
            start_coords = new Vector3(0., 0., fod.enp_dist);
        return new Pair<>(start_coords,rr);
    }

    /**
     * Trace reverse ray from image point to get object space inputs.
     *
     *     This function traces the chief ray for `fld` and `wvl` through the center of the stop surface, starting from the specified real image height.
     *
     *     This is the implementation of :meth:`~.raytr.opticalspec.FieldSpec.obj_coords` for ('image', 'real height')
     */
    public static RayDataWithZ_Enp eval_real_image_ht(OpticalModel opt_model, Field fld, double wvl) {
        var sm = opt_model.seq_model;
        var osp = opt_model.optical_spec;
        var fov = osp.fov;
        var fod = osp.parax_data.fod;

        var not_wa = !fov.is_wide_angle;
        var stop_idx = sm.stop_surface == null ? 1 : sm.stop_surface;
        var ifcx = sm.ifcs.size() - stop_idx - 1;
        var rpath = sm.reverse_path(wvl, sm.ifcs.size(), null, -1);
        var eprad = fod.exp_radius;
        var obj2pup_dist = fod.exp_dist - fod.img_dist;
        var p_exp = new Vector3(0., 0., obj2pup_dist);
        var xy_target = Vector2.vector2_0;
        var p_i = new Vector3(fld.x, fld.y, 0);
        if (fov.is_relative)
            p_i = p_i.times(fov.value);
        var d_i = p_exp.minus(p_i).normalize();
        var result = Trace.iterate_ray_raw(rpath,ifcx,xy_target.as_array(),p_i,d_i,obj2pup_dist,eprad,wvl,not_wa);
        var rrev_cr = result.rr;

        var p_k = Lists.get(rrev_cr.pkg.ray,-2).p;
        var p_k01 = Math.sqrt(p_k.x*p_k.x + p_k.y*p_k.y);
        var d_k = Lists.get(rrev_cr.pkg.ray,-2).d;
        var d_o = d_k.negate();
        var d_k01 = Math.sqrt(d_k.x*d_k.x + d_k.y*d_k.y);
        double z_enp;
        if (d_k01 == 0.)
            z_enp = fod.enp_dist;
        else
            z_enp = p_k.z + p_k01 * d_o.z / d_k01;
        var obj2enp_dist = fod.obj_dist + z_enp;
        var enp_pt = new Vector3(0,0,obj2enp_dist);
        var p_o = enp_pt.plus(d_k.times(obj2enp_dist));
        return new RayDataWithZ_Enp(new RayData(p_o,d_o),z_enp);
    }
}
