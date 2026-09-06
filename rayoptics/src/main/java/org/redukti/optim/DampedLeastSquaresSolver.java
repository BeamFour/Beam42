package org.redukti.optim;

import org.redukti.mathlib.DampedLeastSquares;
import org.redukti.mathlib.LMLSolver;
import java.util.Objects;
import java.util.Arrays;
import java.util.function.Function;

/** Beam42 adapter for constrained DLS. Constraint callbacks receive scaled
 * variables with the prescription and Analysis already evaluated at that point.
 * Existing Goal constraints remain soft penalties unless explicitly supplied here.
 */
public final class DampedLeastSquaresSolver implements Solver {
    private final Analysis analysis;
    private final Var[] variables;
    private final LMDerMeritFunction merit;
    private final int m;
    private final DampedLeastSquares.Options options;
    private final Function<double[], double[]> equalities, inequalities;
    private DampedLeastSquares.Result result;

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options) {
        this(analysis, variables, goals, options, x -> new double[0], x -> new double[0]);
    }

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options,
                                   Function<double[], double[]> equalities,
                                   Function<double[], double[]> inequalities) {
        this.analysis = Objects.requireNonNull(analysis);
        this.variables = variables.clone();
        this.merit = new LMDerMeritFunction(analysis, this.variables, goals.clone(), false);
        this.m = goals.length;
        this.options = Objects.requireNonNull(options);
        this.equalities = Objects.requireNonNull(equalities);
        this.inequalities = Objects.requireNonNull(inequalities);
    }

    public DampedLeastSquares.Result result() { return result; }

    /** Returns 1 for a successful result, 0 for an unsuccessful result.
     * Consult result().message() for the stopping reason (codes are not MINPACK codes).
     */
    @Override public int solve() {
        result = null;
        double[] initial = new double[variables.length];
        for (int i = 0; i < initial.length; i++) initial[i] = variables[i].read_from_prescription();
        DampedLeastSquares optimizer = null;
        try {
            optimizer = new DampedLeastSquares(new DampedLeastSquares.Problem() {
                private double[] prepared;
                @Override public double[] residuals(double[] x) {
                    prepared = null;
                    double[] r = new double[m];
                    merit.apply(m, variables.length, x, r, 1);
                    for (int i = 0; i < m; i++) if (r[i] >= LMLSolver.BIGVAL) r[i] = Double.NaN;
                    if (Arrays.stream(r).allMatch(Double::isFinite)) prepared = x.clone();
                    return r;
                }
                @Override public double[][] jacobian(double[] x) {
                    prepared = null;
                    double[] flat = new double[m * variables.length];
                    if (!merit.buildJacobian(x, flat, m))
                        throw new IllegalStateException("Cannot evaluate DLS Jacobian at accepted prescription");
                    double[][] j = new double[m][variables.length];
                    for (int i = 0; i < m; i++) for (int k = 0; k < variables.length; k++) j[i][k] = flat[i + k * m];
                    return j;
                }
                private void prepare(double[] x) {
                    if (!Arrays.equals(prepared, x)) { prepared = null; restore(x); prepared = x.clone(); }
                }
                @Override public double[] equalities(double[] x) { prepare(x); return equalities.apply(x); }
                @Override public double[] inequalities(double[] x) { prepare(x); return inequalities.apply(x); }
            }, initial, options);
            result = optimizer.run();
            return result.success() ? 1 : 0;
        } finally {
            // Finite-difference probes and rejected trials mutate the prescription.
            // Always restore the last accepted point, including on callback failure.
            restore(optimizer == null ? initial : optimizer.result().x());
        }
    }

    private void restore(double[] x) {
        for (int i = 0; i < x.length; i++) {
            variables[i].set_scaled_value(x[i]);
            variables[i].write_to_prescription();
        }
        analysis.compute();
    }
}
