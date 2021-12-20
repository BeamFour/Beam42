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
            RayTraceResults rayTraceResults = RayTrace.trace(seq_model, pt0, dir0, wvl);
            List<RayTraceElement> ray = rayTraceResults.ray;
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
            LMLSolver lm = new LMLSolver(fn, 1e-5, 2, 2);
            int istatus = 0;
            while (istatus != LMLSolver.BADITER &&
                    istatus != LMLSolver.LEVELITER &&
                    istatus != LMLSolver.MAXITER) {
                istatus = lm.iLMiter();
            }
            if (istatus == LMLSolver.LEVELITER)
                return fn.point;
        } else {
            throw new UnsupportedOperationException();
        }
        return new double[]{0.0, 0.0};
    }
}
