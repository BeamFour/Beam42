package org.redukti.optim;

import org.redukti.jm.minpack.MinPack;

public class LMDerMeritFunction implements MinPack.Lmder_Function {

    private double jac[][];
    private double resid[];
    private double point[]; // x,y at the first surface
    private double weights[];
    private Analysis analysis;
    private Var[] vars;
    private Goal[] outs;
    private double tol = 1E-6;

    public LMDerMeritFunction(Analysis analysis, Var[] vars, Goal[] outs) {
        this.analysis = analysis;
        this.vars = vars;
        this.outs = outs;
        this.resid = new double[outs.length];
        this.point = new double[vars.length];
        this.jac = new double[vars.length][vars.length];
    }

    @Override
    public boolean hasJacobian() {
        return true;
    }

    @Override
    public int apply(int m, int n, double[] x, double[] fvec, double[] fjac, int ldfjac, int iflag) {
        // m should be size of outs
        // n should be size of vars
        // x is current guess for vars
        // fvec is the result of outs
        // fjac is jacobian

        assert m == outs.length;
        assert n == vars.length;

        if (iflag == 0)
            return 0;
        /*      insert print statements here when nprint is positive. */
    /* if the nprint parameter to lmder is positive, the function is
       called every nprint iterations with iflag=0, so that the
       function may perform special operations, such as printing
       residuals. */

        if (iflag != 2) {
            //compute residuals
            for (int i = 0; i < x.length; i++) {
                vars[i].set_value(x[i]);
            }
            try {
                analysis.compute();
            } catch (Exception e) {
                return -1;
            }
            for (int i = 0; i < outs.length; i++) {
                fvec[i] = outs[i].value() * outs[i].weight;
            }
        } else {
            // compute jacobian
            buildJacobian(x);
            for (int i = 0; i < jac.length; i++) {
                for (int j = 0; j < jac[0].length; j++) {

                }
            }
        }
        return 0;
    }

    public boolean computeResiduals() {
        for (int i = 0; i < point.length; i++) {
            vars[i].set_value(point[i]);
        }
        try {
            analysis.compute();
        } catch (Exception e) {
            return false;
        }
        for (int i = 0; i < outs.length; i++) {
            resid[i] = outs[i].value() * outs[i].weight;
        }
        return true;
    }


    public boolean buildJacobian(double[] x)
    // Uses current vector parms[].
    // If current parms[] is bad, returns false.
    // False should trigger an explanation.
    // Called by LMray.iLMiter().
    {
        final int nadj = vars.length;
        final int ngoals = outs.length;
        double delta[] = new double[nadj];
        double d = 0;
        for (int j = 0; j < nadj; j++) {
            for (int k = 0; k < nadj; k++)
                delta[k] = (k == j) ? vars[j].dDelta : 0.0;
            if (!nudge(x, delta)) {
                return false;
            }
            for (int i = 0; i < ngoals; i++)
                jac[i][j] = getResidual(i);

            for (int k = 0; k < nadj; k++)
                delta[k] = (k == j) ? -2.0 * vars[j].dDelta : 0.0;

            // resid at pminus
            if (!nudge(x, delta)) {
                return false;
            }
            for (int i = 0; i < ngoals; i++)
                jac[i][j] -= getResidual(i);

            for (int i = 0; i < ngoals; i++)
                jac[i][j] /= (2.0 * vars[j].dDelta);

        }
        return true;
    }

    public double getResidual(int i)
    // Returns one element of the array resid[].
    {
        return resid[i];
    }

    public boolean nudge(double[] x, double[] delta) {
        for (int i = 0; i < delta.length; i++) {
            point[i] = x[i] + delta[i];
        }
        return computeResiduals();
    }

    public Solver getSolver() {
        return null;
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
