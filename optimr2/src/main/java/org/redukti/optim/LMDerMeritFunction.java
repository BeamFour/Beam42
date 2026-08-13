package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.M;
import org.redukti.mathlib.MinPack;

import java.util.Arrays;

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
            double value = okay ? functions[i].value() : BIGVAL;
            double r;
            if (!Double.isFinite(value) || value >= BIGVAL) {
                r = BIGVAL;
            } else {
                r = (value - functions[i]._target) * weights[i];
            }
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

    /**
     * Central differences where possible, degrading per residual rather than abandoning
     * the solve.
     *
     * <p>A trial step can kill a ray - most easily when thicknesses are varied, since
     * nothing stops an air space closing - and that used to fail the whole Jacobian and
     * terminate lmder with info &lt; 0. But a failure is usually confined to a handful of
     * residuals at one perturbed point, and the rest of the column is perfectly good. So
     * each entry falls back as far as it needs to:
     *
     * <ul>
     *   <li>both perturbed points usable - central difference, as before;</li>
     *   <li>one side usable - one-sided difference against the base point, losing an
     *       order of accuracy but keeping the derivative;</li>
     *   <li>neither usable - zero, which tells the solver only that it does not know,
     *       so that variable stays put this iteration instead of the run ending.</li>
     * </ul>
     *
     * <p>When nothing fails, every entry takes the first branch and the Jacobian is
     * numerically identical to the previous implementation.
     */
    public boolean buildJacobian(double[] x, double[] fjac, int ldfjac) {
        final int n = vars.length;
        final int m = functions.length;
        double[] base = new double[m];
        double[] forward = new double[m];
        double[] backward = new double[m];
        double[] delta = new double[n];

        // lmder computes fvec at x before asking for the Jacobian, but the last
        // evaluation may have been a rejected trial step, so re-establish x here.
        evaluate(x, delta, base);

        for (int j = 0; j < n; j++) {
            // Fixed absolute step per variable (scaled units). A step relative
            // to the current value is zero for zero-valued parameters (fresh
            // aspheric coefficients, conic k) and yields 0/0 = NaN columns.
            double dDelta = vars[j]._d_delta;
            if (!Double.isFinite(dDelta) || dDelta <= 0.0)
                return false;

            delta[j] = dDelta;
            evaluate(x, delta, forward);
            delta[j] = -dDelta;
            evaluate(x, delta, backward);
            delta[j] = 0.0;

            for (int i = 0; i < m; i++) {
                double derivative;
                if (!Double.isNaN(forward[i]) && !Double.isNaN(backward[i]))
                    derivative = (forward[i] - backward[i]) / (2.0 * dDelta);
                else if (!Double.isNaN(forward[i]) && !Double.isNaN(base[i]))
                    derivative = (forward[i] - base[i]) / dDelta;
                else if (!Double.isNaN(backward[i]) && !Double.isNaN(base[i]))
                    derivative = (base[i] - backward[i]) / dDelta;
                else
                    derivative = 0.0;
                fjac[i + j * ldfjac] = Double.isFinite(derivative)
                        ? derivative * weights[i] : 0.0;
            }
        }
        // Restore the prescription and analysis to the unperturbed point x
        evaluate(x, delta, base);
        return true;
    }

    /**
     * Applies {@code x + delta} and evaluates every goal, writing raw values into
     * {@code resid} and NaN for any that could not be evaluated.
     *
     * @return true if at least one goal produced a usable value
     */
    private boolean evaluate(double[] x, double[] delta, double[] resid) {
        try {
            for (int i = 0; i < delta.length; i++) {
                vars[i].set_scaled_value(x[i] + delta[i]);
                vars[i].write_to_prescription();
            }
            analysis.compute();
        } catch (Exception e) {
            Arrays.fill(resid, Double.NaN);
            return false;
        }
        boolean any = false;
        for (int i = 0; i < functions.length; i++) {
            double value = functions[i].value();
            boolean usable = Double.isFinite(value) && value < BIGVAL;
            resid[i] = usable ? value : Double.NaN;
            any |= usable;
        }
        return any;
    }

    private void validateInitialContrastSamples() {
        if (analysis._contrasts == null) return;
        int invalidCount = 0;
        String first = null;
        for (var contrast : analysis._contrasts) {
            for (var field : contrast.fields) {
                for (var wavelength : field.wavelengths()) {
                    for (var sample : wavelength.samples()) {
                        if (sample.valid()) continue;
                        invalidCount++;
                        if (first == null) {
                            var failure = sample.failure();
                            first = "first failure: " + failure.ray() + " ray encountered "
                                    + failure.exceptionType() + " at surface " + failure.surface()
                                    + ", field=" + field.field().y
                                    + ", wavelength=" + wavelength.wavelength() + " nm"
                                    + ", frequency=" + contrast.spatialFrequency + " cycles/mm"
                                    + ", pupil=" + sample.pupil();
                        }
                    }
                }
            }
        }
        if (invalidCount > 0)
            throw new IllegalStateException("Cannot start optimization: " + invalidCount
                    + " contrast samples contain failed rays; " + first);
    }

    private void validateInputs() {
        analysis.compute();
        validateInitialContrastSamples();
    }

    public Solver getSolver() {
        validateInputs();
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
