package org.redukti.optim2;

import org.redukti.jm.minpack.MinPack;

public class LMDerMeritFunction implements MinPack.Lmder_Function {

    public static final double BIGVAL = 9.876543e+99;

    private double weights[];
    private Analysis analysis;
    private Var[] vars;
    private Goal[] functions;

    public LMDerMeritFunction(Analysis analysis, Var[] vars, Goal[] functions) {
        this.analysis = analysis;
        this.vars = vars;
        this.functions = functions;
        this.weights = new double[functions.length];
        // Weights are transformed to sqrt() because they are supplied to
        // lmder as diag vector and lmder will apply the weights
        // when computing least square
        for (int i = 0; i < functions.length; i++) {
            weights[i] = Math.sqrt(functions[i].weight);
        }
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

        assert m == functions.length;
        assert n == vars.length;

        // if the nprint parameter to lmder is positive, the function is
        // called every nprint iterations with iflag=0, so that the
        // function may perform special operations, such as printing
        // residuals.
        if (iflag == 0) return 0;
        if (iflag != 2) {
            //compute residuals
            for (int i = 0; i < x.length; i++) {
                vars[i].set_value(x[i]);
            }
            boolean okay = true;
            try {
                analysis.compute();
            } catch (Exception e) {
                okay = false;
            }
            for (int i = 0; i < functions.length; i++) {
                fvec[i] = okay ? (functions[i].value() * weights[i]) : BIGVAL;
            }
        } else {
            // compute jacobian
            if (!buildJacobian(x, fjac, ldfjac))
                return -99;
        }
        return 0;
    }

    public boolean buildJacobian(double[] x, double[] fjac, int ldfjac) {
        final int n = vars.length;
        final int m = functions.length;
        double[] resid = new double[m];
        double delta[] = new double[n];
        for (int j = 0; j < n; j++) {
            for (int k = 0; k < n; k++)
                delta[k] = (k == j) ? vars[j].dDelta : 0.0;
            if (!nudge(x, delta, resid)) {
                return false;
            }
            for (int i = 0; i < m; i++)
                fjac[i + j * ldfjac] = resid[i];

            for (int k = 0; k < n; k++)
                delta[k] = (k == j) ? -1.0 * vars[j].dDelta : 0.0;
            if (!nudge(x, delta, resid)) {
                return false;
            }
            for (int i = 0; i < m; i++)
                fjac[i + j * ldfjac] -= resid[i];

            for (int i = 0; i < m; i++)
                fjac[i + j * ldfjac] /= (2.0 * vars[j].dDelta);
        }
        // Scale by weights
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                fjac[i + j * ldfjac] = fjac[i + j * ldfjac] * weights[i];
            }
        }
        return true;
    }

    public boolean nudge(double[] x, double[] delta, double[] resid) {
        for (int i = 0; i < delta.length; i++) {
            vars[i].set_value(x[i],delta[i]);
        }
        boolean okay = true;
        try {
            analysis.compute();
        } catch (Exception e) {
            okay = false;
        }
        for (int i = 0; i < functions.length; i++) {
            resid[i] = okay ? functions[i].value() : BIGVAL;
        }
        return true;
    }

    public Solver getSolver() {
        return new LMDerSolver(analysis, vars, functions);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Vars:\n");
        for (int i = 0; i < vars.length; i++)
            sb.append(vars[i].toString()).append('\n');
        sb.append("Values:\n");
        for (int i = 0; i < functions.length; i++)
            sb.append(functions[i].toString()).append('\n');
        return sb.toString();
    }
}
