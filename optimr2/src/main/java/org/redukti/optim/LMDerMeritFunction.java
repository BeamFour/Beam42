package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.M;
import org.redukti.mathlib.MinPack;

public class LMDerMeritFunction implements MinPack.Lmder_Function {

    private static final double BIGVAL = LMLSolver.BIGVAL;

    private double weights[];
    private Analysis analysis;
    private Var[] vars;
    private Goal[] functions;
    private boolean use_native;

    public LMDerMeritFunction(Analysis analysis, Var[] vars, Goal[] functions, boolean use_native) {
        this.analysis = analysis;
        this.vars = vars;
        this.functions = functions;
        this.weights = new double[functions.length];
        this.use_native = use_native;
        for (int i = 0; i < functions.length; i++) {
            if (!Double.isFinite(functions[i]._weight) || functions[i]._weight < 0.0)
                throw new IllegalArgumentException("Goal weight must be finite and non-negative");
            weights[i] = Math.sqrt(functions[i]._weight);
        }
        for (int i = 0; i < vars.length; i++)
            vars[i].read_from_prescription();
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
            computeResiduals(x, fvec);
        } else {
            // compute jacobian
            if (!buildJacobian(x, fjac, ldfjac))
                return -99;
        }
        return 0;
    }

    /**
     * Evaluates the weighted residuals (value - target)*weight at x.
     * On any failure (killed ray, NaN in x, exception in the analysis)
     * all residuals are set to BIGVAL so that lmder rejects the trial step.
     * lmder minimizes ||fvec||^2, so fvec must be the deviation from target:
     * using the raw goal value would drive EFL/Fno/MTF towards zero.
     */
    private void computeResiduals(double[] x, double[] fvec) {
        boolean okay = true;
        try {
            for (int i = 0; i < x.length; i++) {
                vars[i].set_scaled_value(x[i]);
                vars[i].write_to_prescription();
            }
            analysis.compute();
        } catch (Exception e) {
            okay = false;
        }
        for (int i = 0; i < functions.length; i++) {
            double r = okay ? (functions[i].value() - functions[i]._target) * weights[i] : BIGVAL;
            fvec[i] = Double.isFinite(r) ? r : BIGVAL;
        }
    }

    @Override
    public int apply(int m, int n, double[] x, double[] fvec, int iflag) {
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
        computeResiduals(x, fvec);
        return 0;
    }

    public boolean buildJacobian(double[] x, double[] fjac, int ldfjac) {
        final int n = vars.length;
        final int m = functions.length;
        double[] resid = new double[m];
        double delta[] = new double[n];
        for (int j = 0; j < n; j++) {
            // Fixed absolute step per variable (scaled units). A step relative
            // to the current value is zero for zero-valued parameters (fresh
            // aspheric coefficients, conic k) and yields 0/0 = NaN columns.
            double dDelta = vars[j]._d_delta;
            if (!Double.isFinite(dDelta) || dDelta <= 0.0)
                return false;
            for (int k = 0; k < n; k++)
                delta[k] = (k == j) ? dDelta : 0.0;
            if (!nudge(x, delta, resid)) {
                return false;
            }
            for (int i = 0; i < m; i++)
                fjac[i + j * ldfjac] = resid[i];

            for (int k = 0; k < n; k++)
                delta[k] = (k == j) ? -1.0 * dDelta : 0.0;
            if (!nudge(x, delta, resid)) {
                return false;
            }
            for (int i = 0; i < m; i++)
                fjac[i + j * ldfjac] -= resid[i];

            for (int i = 0; i < m; i++) {
                fjac[i + j * ldfjac] /= (2.0 * dDelta);
                if (!Double.isFinite(fjac[i + j * ldfjac]))
                    return false;
            }
        }
        // Scale by weights
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                fjac[i + j * ldfjac] = fjac[i + j * ldfjac] * weights[i];
            }
        }
        // Restore the prescription and analysis to the unperturbed point x
        for (int k = 0; k < n; k++)
            delta[k] = 0.0;
        return nudge(x, delta, resid);
    }

    public boolean nudge(double[] x, double[] delta, double[] resid) {
        boolean okay = true;
        try {
            for (int i = 0; i < delta.length; i++) {
                vars[i].set_scaled_value(x[i] + delta[i]);
                vars[i].write_to_prescription();
            }
            analysis.compute();
        } catch (Exception e) {
            okay = false;
        }
        if (!okay)
            // A killed ray during Jacobian evaluation: differencing BIGVAL
            // residuals would poison the Jacobian, so report failure instead
            // (buildJacobian returns false, lmder terminates with info < 0).
            return false;
        for (int i = 0; i < functions.length; i++) {
            resid[i] = functions[i].value();
        }
        return okay;
    }

    public Solver getSolver() {
        return new LMDerSolver(analysis, vars, functions, use_native);
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
        sb.append("RMS: ").append(getRMS()).append('\n');
        return sb.toString();
    }

    public Goal[] goals() {
        return functions;
    }
    public Var[] variables() {
        return vars;
    }
    public double getRMS() {
        double sos = 0.0;
        double[] resid = new double[functions.length];
        for (int i = 0; i < functions.length; i++) {
            resid[i] = (functions[i]._target - functions[i]. value())*weights[i];
            sos += M.square(resid[i]);
        }
        if (M.isZero(sos))
            return sos;
        return Math.sqrt(sos / functions.length);
    }
}
