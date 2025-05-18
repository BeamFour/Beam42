package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.LMLFunction;
import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.SecantSolver;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.exceptions.TraceTIRException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.firstorder.FirstOrderData;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.util.Lists;

import java.util.*;

public class Trace {

    static class BaseObjectiveFunction {
        final SequentialModel seq_model;
        final Integer ifcx;
        final Vector3 pt0;
        final double dist;
        final double wvl;

        public BaseObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl) {
            this.seq_model = seq_model;
            this.ifcx = ifcx;
            this.pt0 = pt0;
            this.dist = dist;
            this.wvl = wvl;
        }

        public RaySeg eval(double x1, double y1) {
            Vector3 pt1 = new Vector3(x1, y1, dist);
            Vector3 dir0 = pt1.minus(pt0);
            dir0 = dir0.normalize();
            RayPkg pkg = null;
            try {
                pkg = RayTrace.trace(seq_model, pt0, dir0, wvl);
            } catch (TraceMissedSurfaceException ray_miss) {
                pkg = ray_miss.ray_pkg;
                if (ray_miss.surf <= ifcx)
                    throw ray_miss;
            } catch (TraceTIRException ray_tir) {
                pkg = ray_tir.ray_pkg;
                if (ray_tir.surf < ifcx)
                    throw ray_tir;
            }
            return pkg.ray.get(ifcx);
        }
    }

    /* 1D solver */
    static class SecantFunction extends BaseObjectiveFunction implements SecantSolver.ObjectiveFunction {

        final double y_target;

        public SecantFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target) {
            super(seq_model, ifcx, pt0, dist, wvl);
            this.y_target = y_target;
        }

        @Override
        public double eval(double y1) {
            RaySeg seg = eval(0., y1);
            double y_ray = seg.p.y;
            return y_ray - y_target;
        }
    }

//    /* Solver for use in Minpack algos */
//    static class LmObjectiveFunction extends BaseObjectiveFunction implements MinPack.Hybrd_Function, MinPack.Lmder_function {
//        final double[] xy_target; // target x,y values
//
//        public LmObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target) {
//            super(seq_model, ifcx, pt0, dist, wvl);
//            this.xy_target = xy_target;
//        }
//
//        @Override
//        public void apply(int n, double[] x, double[] fvec, int[] iflag) {
//            RaySeg seg = eval(x[0], x[1]);
//            // TODO following is only applicable when solving for y alone
//            // we need a way to not do this when solving x and y.
//            double residual = seg.p.x - xy_target[0];
//            if (Math.abs(residual) > 2.2204460492503131e-16)
//                residual = 9.876543e+99;
//            fvec[0] = residual;
//            fvec[1] = seg.p.y - xy_target[1];
//        }
//
//        @Override
//        public int apply(int m, int n, double[] x, double[] fvec, int iflag) {
//            int[] iflags = new int[1];
//            apply(n, x, fvec, iflags);
//            return iflags[0];
//        }
//
//        @Override
//        public boolean hasJacobian() {
//            return false;
//        }
//    }

    /* Solver for use in LMLSolver */
    static class ObjectiveFunction extends BaseObjectiveFunction implements LMLFunction {

        private final double[][] jac = new double[2][2];
        private final double[] resid = {0, 0};
        private final double[] dDelta = {1E-6, 1E-6};
        private final double[] point = {0, 0}; // Actual x,y values

        final double[] xy_target; // target x,y values

        public ObjectiveFunction(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target) {
            super(seq_model, ifcx, pt0, dist, wvl);
            this.xy_target = xy_target;
        }

        @Override
        public double computeResiduals() {
            RaySeg seg = eval(point[0], point[1]);
            double[] p = {seg.p.x, seg.p.y};
            double sos = 0.0;
            for (int i = 0; i < p.length; i++) {
                resid[i] = xy_target[i] - p[i];
                sos += (resid[i] * resid[i]);
            }
            return Math.sqrt(sos / p.length);
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

    public static double[] get_1d_solution(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double y_target) {
        SecantFunction fn = new SecantFunction(seq_model, ifcx, pt0, dist, wvl, y_target);
        double start_y = SecantSolver.find_root(fn, 0., 50, 1.48e-8);
        return new double[]{0, start_y};
    }

    public static double[] get_2d_mike_lampton_lavenberg_marquardt_solution(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target) {
        ObjectiveFunction fn = new ObjectiveFunction(seq_model, ifcx, pt0, dist, wvl, Arrays.copyOf(xy_target, xy_target.length));
        LMLSolver lm = new LMLSolver(fn, 1e-12, 2, 2);
        int istatus = 0;
        while (istatus != LMLSolver.BADITER &&
                istatus != LMLSolver.LEVELITER &&
                istatus != LMLSolver.MAXITER) {
            istatus = lm.iLMiter();
        }
        if (istatus == LMLSolver.LEVELITER)
            return fn.point;
        return new double[]{0.0, 0.0};
    }

//    public static double[] get_2d_minpack_lavenberg_marquardt_solution(SequentialModel seq_model, Integer ifcx, Vector3 pt0, double dist, double wvl, double[] xy_target, FirstOrderData fod) {
//
//        LmObjectiveFunction f = new LmObjectiveFunction(seq_model, ifcx, pt0, dist, wvl, xy_target);
//        double[] x = new double[2];
//        double[] fvec = new double[2];
//        double[] fjac = new double[4];
//        int lwa = (2 * (3 * 2 + 13)) / 2;
//        double[] wa = new double[lwa];
//        int info[] = new int[1];
//        int[] ipvt = new int[2];
//        double epsfcn = 0.0001 * fod.enp_radius;
//        //info[0] = MinPack.hybrd1(f, 2, x, fvec, 1e-15, wa, lwa, epsfcn);
//        info[0] = MinPack.lmder1(f, 2, 2, x, fvec, fjac, 2, 1e-15, ipvt, wa, lwa, epsfcn);
//        if (info[0] == 2)
//            return x;
//        return new double[]{0.0, 0.0};
//    }

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

        // 0.3171082641317441 (secant)
        // 0.3171081317490797 (lm)
        // 0.3171081737822994 (expected)
        if (ifcx != null) {
            if (pt0.x == 0.0 && xy_target[0] == 0.0) {
                return get_1d_solution(seq_model, ifcx, pt0, dist, wvl, xy_target[1]);
            } else {
                return get_2d_mike_lampton_lavenberg_marquardt_solution(seq_model, ifcx, pt0, dist, wvl, xy_target);
                //return get_2d_minpack_lavenberg_marquardt_solution(seq_model, ifcx, pt0, dist, wvl, xy_target, fod);
            }
        } else {
            // floating stop surface - use entrance pupil for aiming
            return xy_target;
        }
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
        Vector3 pt1 = new Vector3(eprad * vig_pupil[0] + aim_pt[0],
                eprad * vig_pupil[1] + aim_pt[1],
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
                new RayData(Lists.get(cr.ray, -2).p,
                        Lists.get(cr.ray, -2).d), fod.exp_dist);

        return new ChiefRayPkg(cr, cr_exp_seg);
    }

    /**
     * returns a list of RayPkgs for the boundary rays for field fld
     *
     * @param opt_model
     * @param fld
     * @param wvl
     * @return
     */
    public static List<RayPkg> trace_boundary_rays_at_field(OpticalModel opt_model, Field fld, double wvl) {
        List<RayPkg> rim_rays = new ArrayList<>();
        OpticalSpecs osp = opt_model.optical_spec;
        for (double[] p : osp.pupil.pupil_rays) {
            RayPkg ray_pkg;
            try {
                ray_pkg = trace_base(opt_model, p, fld, wvl);
            } catch (TraceException ray_error) {
                ray_pkg = ray_error.ray_pkg;
            }
            rim_rays.add(ray_pkg);
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

    public static List<RayPkg> trace_boundary_rays(OpticalModel opt_model) {
        List<RayPkg> rayset = new ArrayList<>();
        double wvl = opt_model.seq_model.central_wavelength();
        FieldSpec fov = opt_model.optical_spec.field_of_view;
        for (int fi = 0; fi < fov.fields.length; fi++) {
            Field fld = fov.fields[fi];
            List<RayPkg> rim_rays = trace_boundary_rays_at_field(opt_model, fld, wvl);
            fld.pupil_rays = boundary_ray_dict(opt_model, rim_rays);
            rayset.addAll(rim_rays);
        }
        return rayset;
    }

    /* returns a list of ray |DataFrame| for the ray_list at field fld */
    public static List<RayDataFrame> trace_ray_list_at_field(OpticalModel opt_model, double[][] ray_list, Field fld, double wvl, double foc) {
        ArrayList<RayDataFrame> rayset = new ArrayList<>();
        for (double[] p : ray_list) {
            RayPkg ray = trace_base(opt_model, p, fld, wvl);
            rayset.add(new RayDataFrame(ray.ray));
        }
        return rayset;
    }

}
