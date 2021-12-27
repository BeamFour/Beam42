package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.math.LMLFunction;
import org.redukti.rayoptics.math.LMLSolver;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.List;

public class Trace {

    static class ObjectiveFunction implements LMLFunction {

        private final double[][] jac = new double[2][2];
        private final double[] resid = {0, 0};
        private final double[] dDelta = {1E-6, 1E-6};
        private final double[] point = {0, 0}; // Actual x,y values

        final SequentialModel seq_model;
        final Integer ifcx;
        final Vector3 pt0;
        final double dist;
        final double wvl;
        final double[] xy_target; // target x,y values

        public ObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target) {
            this.seq_model = seq_model;
            this.ifcx = ifcx;
            this.pt0 = pt0;
            this.dist = dist;
            this.wvl = wvl;
            this.xy_target = xy_target;
        }

        @Override
        public double computeResiduals() {
            Vector3 pt1 = new Vector3(point[0], point[1], dist);
            Vector3 dir0 = pt1.minus(pt0);
            dir0 = dir0.normalize();
            RayPkg rayPkg = RayTrace.trace(seq_model, pt0, dir0, wvl);
            List<RaySeg> ray = rayPkg.ray;
            double[] p = {Lists.get(ray, ifcx).p.x, Lists.get(ray, ifcx).p.y};
            double sos = 0.0;
            for (int i = 0; i < p.length; i++) {
                resid[i] = xy_target[i] - p[i];
                sos = resid[i] * resid[i];
            }
            return sos;
        }

        @Override
        public boolean buildJacobian()             // Uses current vector parms[].
        // If current parms[] is bad, returns false.
        // False should trigger an explanation.
        // Called by LMray.iLMiter().
        {
            final int nadj = 2;
            final int ngoals = 2;
            double delta[] = new double[nadj];
            double d = 0;
            for (int j = 0; j < nadj; j++) {
                for (int k = 0; k < nadj; k++)
                    delta[k] = (k == j) ? dDelta[j] : 0.0;

                d = nudge(delta); // resid at pplus
                if (d == LMLSolver.BIGVAL) {
                    //badray = true;
                    return false;
                }
                for (int i = 0; i < ngoals; i++)
                    jac[i][j] = getResidual(i);

                for (int k = 0; k < nadj; k++)
                    delta[k] = (k == j) ? -2.0 * dDelta[j] : 0.0;

                d = nudge(delta); // resid at pminus
                if (d == LMLSolver.BIGVAL) {
                    //badray = true;
                    return false;
                }

                for (int i = 0; i < ngoals; i++)
                    jac[i][j] -= getResidual(i);

                for (int i = 0; i < ngoals; i++)
                    jac[i][j] /= (2.0 * dDelta[j]);

                for (int k = 0; k < nadj; k++)
                    delta[k] = (k == j) ? dDelta[j] : 0.0;

                d = nudge(delta);  // back to starting value.

                if (d == LMLSolver.BIGVAL) {
                    //badray = true;
                    return false;
                }
            }
            return true;
        }

        @Override
        public double getResidual(int i)         // Returns one element of the array resid[].
        {
            return resid[i];
        }

        @Override
        public double getJacobian(int i, int j)         // Returns one element of the Jacobian matrix.
        // i=datapoint, j=whichparm.
        {
            return jac[i][j];
        }

        @Override
        public double nudge(double[] delta) {
            point[0] += delta[0];
            point[1] += delta[1];
            return computeResiduals();
        }
    }


    public static double[] aim_chief_ray(OpticalModel opt_model, Field fld, Double wvl) {
        // aim chief ray at center of stop surface and save results on **fld**
        SequentialModel seq_model = opt_model.seq_model;
        if (wvl == null)
            wvl = seq_model.central_wavelength();
        Integer stop = seq_model.stop_surface;
        double[] aim_pt = iterate_ray(opt_model, stop, new double[]{0., 0.}, fld, wvl);
        return aim_pt;
    }

    /**
     * iterates a ray to xy_target on interface ifcx, returns aim points on
     * the paraxial entrance pupil plane
     * <p>
     * If idcx is None, i.e. a floating stop surface, returns xy_target.
     * <p>
     * If the iteration fails, a TraceError will be raised
     *
     * @param opt_model
     * @param ifcx
     * @param xy_target
     * @param fld
     * @param wvl
     * @return
     */
    public static double[] iterate_ray(final OpticalModel opt_model, Integer ifcx, double[] xy_target, Field fld, double wvl) {
        final SequentialModel seq_model = opt_model.seq_model;
        final OpticalSpecs osp = opt_model.optical_spec;
        final FirstOrderData fod = osp.parax_data.fod;
        double dist = fod.obj_dist + fod.enp_dist;
        Vector3 pt0 = osp.obj_coords(fld);

        if (ifcx != null) {
            ObjectiveFunction fn = new ObjectiveFunction(seq_model, ifcx, pt0, dist, wvl, new double[]{0.0, 0.0});
            LMLSolver lm = new LMLSolver(fn, 1e-12, 2, 2);
            int istatus = 0;
            while (istatus != LMLSolver.BADITER &&
                    istatus != LMLSolver.LEVELITER &&
                    istatus != LMLSolver.MAXITER) {
                istatus = lm.iLMiter();
            }
            if (istatus == LMLSolver.LEVELITER)
                return fn.point;
        } else {
            // floating stop surface - use entrance pupil for aiming
            return xy_target;
        }
        return new double[]{0.0, 0.0};
    }

    /**
     * Trace ray specified by relative aperture and field point.
     *
     *     Args:
     *         opt_model: instance of :class:`~.OpticalModel` to trace
     *         pupil: relative pupil coordinates of ray
     *         fld: instance of :class:`~.Field`
     *         wvl: ray trace wavelength in nm
     *         **kwargs: keyword arguments
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
     * @param opt_model instance of :class:`~.OpticalModel` to trace
     * @param pupil relative pupil coordinates of ray
     * @param fld instance of :class:`~.Field`
     * @param wvl ray trace wavelength in nm
     */
    public static RayPkg trace_base(OpticalModel opt_model, double[] pupil, Field fld, double wvl) {
        double[] vig_pupil = fld.apply_vignetting(pupil);
        OpticalSpecs osp = opt_model.optical_spec;
        FirstOrderData fod = osp.parax_data.fod;
        double eprad = fod.enp_radius;
        double[] aim_pt = new double[]{0., 0.};
        if (fld.aim_pt != null) {
            aim_pt = fld.aim_pt;
        }
        Vector3 pt1 = new Vector3(eprad * vig_pupil[0] * aim_pt[0],
                eprad * vig_pupil[1] * aim_pt[1],
                fod.obj_dist + fod.enp_dist);
        Vector3 pt0 = osp.obj_coords(fld);
        Vector3 dir0 = pt1.minus(pt0);
        dir0 = dir0.normalize();
        return RayTrace.trace(opt_model.seq_model, pt0, dir0, wvl);
    }

    /**
     * returns (ray, ray_opl, wvl)
     *
     *     Args:
     *         seq_model: the :class:`~.SequentialModel` to be traced
     *         pt0: starting coordinate at object interface
     *         dir0: starting direction cosines following object interface
     *         wvl: ray trace wavelength in nm
     *         **kwargs: keyword arguments
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
     * @param seq_model
     * @param pt0
     * @param dir0
     * @param wvl
     * @return
     */
    public static RayPkg trace(SequentialModel seq_model, Vector3 pt0, Vector3 dir0, double wvl) {
        return RayTrace.trace(seq_model, pt0, dir0, wvl);
    }

    /*
    public static Pair<RefSpherePkg, RayPkg> setup_canonical_coords(OpticalModel opt_model, Field fld, double wvl, Vector3 image_pt) {
        OpticalSpecs osp = opt_model.optical_spec;
        SequentialModel seq_model = opt_model.seq_model;
        FirstOrderData fod = osp.parax_data.fod;

        if (fld.chief_ray == null) {
            fld.chief_ray = trace_base(opt_model, new double[] {0., 0.}, fld, wvl);
        }
        RayPkg cr = fld.chief_ray;
        if (image_pt == null)
            image_pt = Lists.get(cr.ray, -1).p;

        // cr_exp_pt: E upper bar prime: pupil center for pencils from Q
        // cr_exp_pt, cr_b4_dir, cr_dst
        ChiefRayExitPupilSegment cr_exp_seg = RayTrace.transfer_to_exit_pupil(Lists.get(seq_model.ifcs, -2),
                new Ray(Lists.get(cr.ray, -2).p,
                        Lists.get(cr.ray, -2).d),
                fod.exp_dist);
        Vector3 cr_exp_pt = cr_exp_seg.exp_pt;
        double cr_exp_dist = cr_exp_seg.exp_dst;

        double img_dist = Lists.get(seq_model.gaps, -1).thi;
        Vector3 img_pt = new Vector3(image_pt.x, image_pt.y, image_pt.z + img_dist);

        // R' radius of reference sphere for O'
        Vector3 ref_sphere_vec = img_pt.minus(cr_exp_pt);
        double ref_sphere_radius = ref_sphere_vec.length();
        Vector3 ref_dir = ref_sphere_vec.normalize();

        RefSphere ref_sphere = new RefSphere(image_pt, cr_exp_pt, cr_exp_dist,
                ref_dir, ref_sphere_radius);

        ZDir z_dir = Lists.get(seq_model.z_dir, -1);
        int wl = seq_model.index_for_wavelength(wvl);
        double n_obj = Lists.get(seq_model.rndx, 0)[wl];
        double n_img = Lists.get(seq_model.rndx, -1)[wl];
        RefSpherePkg ref_sphere_pkg = new RefSpherePkg(ref_sphere, osp.parax_data, n_obj, n_img, z_dir);
        fld.ref_sphere = ref_sphere_pkg;
        return new Pair<>(ref_sphere_pkg, cr);
    }
    */

    /**
     * Trace a chief ray for fld and wvl, returning the ray_pkg and exit pupil segment.
     * @param opt_model
     * @param fld
     * @param wvl
     * @param foc
     * @return
     */
    public static ChiefRayPkg trace_chief_ray(OpticalModel opt_model, Field fld, double wvl, double foc) {
        OpticalSpecs osp = opt_model.optical_spec;
        FirstOrderData fod = osp.parax_data.fod;

        RayPkg cr = trace_base(opt_model, new double[]{0., 0.}, fld, wvl);
        // op = rt.calc_optical_path(ray, opt_model.seq_model.path())

        // cr_exp_pt: E upper bar prime: pupil center for pencils from Q
        // cr_exp_pt, cr_b4_dir, cr_exp_dist
        ChiefRayExitPupilSegment cr_exp_seg = RayTrace.transfer_to_exit_pupil(
                Lists.get(opt_model.seq_model.ifcs, -2),
                new Ray(Lists.get(cr.ray, -2).p,
                        Lists.get(cr.ray, -2).d), fod.exp_dist);

        return new ChiefRayPkg(cr, cr_exp_seg);
    }
}
