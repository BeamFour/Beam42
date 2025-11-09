// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.SecantSolver;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;

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
            ray_pkg = ray_error.ray_pkg;
            rr = new RayResult(ray_pkg,ray_error);
            final_coord = Vector3.ZERO;
        }
        return new Pair<>(final_coord,rr);
    }

    static final class Eval_Z_Enp_Function implements SecantSolver.ObjectiveFunction {
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
        public double eval(double z_enp) {
            var coord_rr = enp_z_coordinate(z_enp,seq_model,stop_idx,dir0,obj_dist,wvl);
            var final_coord = coord_rr.first;
            rr = coord_rr.second;
            return final_coord.y - y_target;
        }
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
                z_enp = SecantSolver.find_root(func,z_enp,50,1.48e-8);
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
     *     for `fld` and `wvl` for wide angle systems. `fld` should be of type ('object', 'angle'), even for finite object distances.
     *
     *     The first phase searches for the window of pupil locations by sampling the
     *     z coordinate from the paraxial pupil location towards the first interface
     *     vertex. Failed rays are discarded until a range of z coordinates is found
     *     where rays trace successfully. If only a single successful trace is in
     *     hand, a second, more finely subdivided search is conducted about the
     *     successful point.
     *
     *     The outcome is a range, start_z -> end_z, that is divided in 3 and a ray
     *     iteration (using :func:`~.raytr.wideangle.find_z_enp`) to find the center of the stop surface is done. Sometimes the
     *     start point doesn't produce a solution; use of the mid-point as a start is
     *     a reliable second try.
     */
    public static Pair<Double,RayResult> find_real_enp(OpticalModel opm, Integer stop_idx, Field fld, double wvl) {
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
        // protect against infinite loops
        int trial = 0;
        // if the trace succeeds 5 times in a row, go on to the next phase
        int successes = 0;
        while (keep_going && successes < 4 && trial < 64) {
            var coord_rr = enp_z_coordinate(z_enp,sm,stop_idx,dir0,fod.obj_dist,wvl);
            rr = coord_rr.second;
            if (rr.err == null) {
                successes++;
                if (start_z == null)
                    start_z = z_enp;
                end_z = z_enp;
            }
            else if (rr.err instanceof TraceMissedSurfaceException) {
                // if the first surface was missed, then exit
                if (start_z != null)
                    keep_going = false;
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
