package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.math.LMLFunction;
import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.spec.Prescription;

import java.util.Arrays;

import static org.redukti.jfotoptix.math.LMLSolver.BIGVAL;

public class MeritFunction implements LMLFunction {

        private double jac[][];
        private double resid[] ;
        private double point[]; // x,y at the first surface
        private Prescription prescription;
        private Var[] vars;
        private Goal[] outs;
        private double tol = 1E-6;

        public MeritFunction(Prescription prescription, Var[] vars, Goal[] outs) {
            this.prescription = prescription;
            this.vars = vars;
            this.outs = outs;
            this.resid = new double[outs.length];
            this.point = new double[vars.length];
            this.jac = new double[vars.length][vars.length];
        }

        @Override
        public double computeResiduals() {
            for (int i = 0; i < point.length; i++) {
                vars[i].shift(point[i]);
            }
            try {
                prescription.compute();
            }
            catch (Exception e) {
                return BIGVAL;
            }
            double sos = 0.0;
            for (int i = 0; i < outs.length; i++) {
                resid[i] = (outs[i].target - outs[i].value())*outs[i].weight;
                sos += MathUtils.square(resid[i]);
            }
            return Math.sqrt(sos / outs.length);
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
                final int nadj = vars.length;
                final int ngoals = outs.length;
                double delta[] = new double[nadj];
                double d=0;
                for (int j=0; j<nadj; j++)
                {
                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? vars[j].dDelta : 0.0;

                    d = nudge(delta); // resid at pplus
                    if (d== BIGVAL)
                    {
                        //badray = true;
                        return false;
                    }
                    for (int i=0; i<ngoals; i++)
                        jac[i][j] = getResidual(i);

                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? -2.0*vars[j].dDelta : 0.0;

                    d = nudge(delta); // resid at pminus
                    if (d== BIGVAL)
                    {
                        //badray = true;
                        return false;
                    }

                    for (int i=0; i<ngoals; i++)
                        jac[i][j] -= getResidual(i);

                    for (int i=0; i<ngoals; i++)
                        jac[i][j] /= (2.0*vars[j].dDelta);

                    for (int k=0; k<nadj; k++)
                        delta[k] = (k==j) ? vars[j].dDelta : 0.0;

                    d = nudge(delta);  // back to starting value.

                    if (d== BIGVAL)
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
            assert point.length == delta.length;
            for (int i = 0; i < delta.length; i++) {
                point[i] += delta[i];
            }
            return computeResiduals();
        }

        public LMLSolver getSolver() {
            return new LMLSolver(this, tol, vars.length, outs.length);
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("Vars:\n");
            for (int i = 0; i < vars.length; i++)
                sb.append(vars[i].toString()).append('\n');
            sb.append("Values:\n");
            for (int i = 0; i < outs.length; i++)
                sb.append(outs[i].toString()).append('\n');
            return sb.toString();
        }
}
