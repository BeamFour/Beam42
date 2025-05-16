package org.redukti.jfotoptix.analysis;

import org.redukti.jfotoptix.math.LMLFunction;
import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.Stop;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.SequentialRayTracer;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.List;

public class ChiefRayFinder {

    static final class ObjectiveFunction implements LMLFunction {
        private double jac[][]  = new double[2][2];
        private double resid[]  = {0, 0};
        private double dDelta[] = {1E-6, 1E-6};
        private double point[] = {0, 0}; // x,y at the first surface

        private OpticalSystem system;
        private RayTraceParameters parameters;
        private Distribution d = new Distribution(Pattern.UserDefined, 10, 0.999);
        private Stop.ApertureStop stop;
        private RayTraceResults results;

        public ObjectiveFunction(OpticalSystem system) {
            this.system = system;
            this.parameters = new RayTraceParameters(system);
            this.parameters.set_default_distribution(d);
            this.stop = this.system.get_sequence()
                    .stream()
                    .filter(e -> e instanceof Stop.ApertureStop)
                    .map(e -> (Stop.ApertureStop)e)
                    .findFirst()
                    .orElse(null);
            if (this.stop == null) {
                throw new IllegalArgumentException("An aperture stop is required");
            }
        }

        @Override
        public double computeResiduals() {
            Vector2 pt = new Vector2(point[0], point[1]);
            d.set_user_defined_points(List.of(pt));
            SequentialRayTracer rayTracer = new SequentialRayTracer();
            results = rayTracer.trace(system, parameters);
            List<TracedRay> rays = results.get_intercepted(stop);
            if (rays == null || rays.isEmpty())
                return LMLSolver.BIGVAL;
            TracedRay ray = rays.get(0);
            double[] p = { ray.get_intercept_point().x(), ray.get_intercept_point().y() };
            double sos = 0.0;
            for (int i = 0; i < p.length; i++) {
                resid[i] = 0.0 - p[i];
                sos += MathUtils.square(resid[i]);
            }
            return Math.sqrt(sos / p.length);
        }

        /**
         *  @author: M.Lampton (c) 2005 Stellar Software
         *  Original License: GPL v2
         */
        @Override
        public boolean buildJacobian()
            // Uses current vector parms[].
            // If current parms[] is bad, returns false.
            // False should trigger an explanation.
            // Called by LMray.iLMiter().
            {
                final int nadj = 2;
                final int ngoals = 2;
                double delta[] = new double[nadj];
                double d=0;
                for (int j=0; j<nadj; j++)
                {
                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? dDelta[j] : 0.0;

                    d = nudge(delta); // resid at pplus
                    if (d== LMLSolver.BIGVAL)
                    {
                        //badray = true;
                        return false;
                    }
                    for (int i=0; i<ngoals; i++)
                        jac[i][j] = getResidual(i);

                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? -2.0*dDelta[j] : 0.0;

                    d = nudge(delta); // resid at pminus
                    if (d== LMLSolver.BIGVAL)
                    {
                        //badray = true;
                        return false;
                    }

                    for (int i=0; i<ngoals; i++)
                        jac[i][j] -= getResidual(i);

                    for (int i=0; i<ngoals; i++)
                        jac[i][j] /= (2.0*dDelta[j]);

                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? dDelta[j] : 0.0;

                    d = nudge(delta);  // back to starting value.

                    if (d== LMLSolver.BIGVAL)
                    {
                        //badray = true;
                        return false;
                    }
                }
                return true;
            }

        @Override
        public double getResidual(int i)
        // Returns one element of the array resid[].
        {
            return resid[i];
        }

        @Override
        public double getJacobian(int i, int j)
        // Returns one element of the Jacobian matrix.
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

    public static RayTraceResults findChiefRay(OpticalSystem system) {
        ObjectiveFunction f = new ObjectiveFunction(system);
        LMLSolver lm = new LMLSolver(f, 1e-5, 2, 2);
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = lm.iLMiter();
        }
        if (istatus == LMLSolver.LEVELITER)
            return f.results;
        return null;
    }

}
