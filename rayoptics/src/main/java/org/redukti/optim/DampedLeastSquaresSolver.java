package org.redukti.optim;

import org.redukti.mathlib.DampedLeastSquares;
import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.MinPack;
import java.util.Objects;
import java.util.Arrays;
import java.util.function.Function;

/** Beam42 adapter for constrained DLS.
 *
 * <p>Two kinds of hard constraint can be supplied. {@link Bound}s read the prescription
 * only and are differentiated here without touching {@link Analysis}, so they cost
 * nothing measured against the optical Jacobian. The raw callbacks receive scaled
 * variables with the prescription and Analysis already evaluated at that point, and are
 * differentiated by the solver with central differences - {@code 2n} full analyses per
 * iteration, which on a ray-traced problem doubles the cost of an iteration. Prefer a
 * {@code Bound} wherever the quantity is geometric.
 *
 * <p>Existing Goal constraints remain soft penalties unless explicitly supplied here.
 */
public final class DampedLeastSquaresSolver implements Solver {

    /** Converged on a tolerance at a feasible point. */
    public static final int CONVERGED = 1;
    /** Ran out of iterations at a feasible point. Progress, not convergence. */
    public static final int ITERATION_LIMIT = 2;
    /** Line search, active set or step failed; the accepted point has been restored. */
    public static final int FAILED = 0;

    private static final Bound[] NO_BOUNDS = new Bound[0];

    private final Analysis analysis;
    private final Var[] variables;
    private final LMDerMeritFunction merit;
    private final int m;
    private final DampedLeastSquares.Options options;
    private final Function<double[], double[]> equalities, inequalities;
    private final Bound[] bounds;
    private DampedLeastSquares.Result result;

    /**
     * Beam42's defaults, which are not Prysm's.
     *
     * <p>Prysm defaults to identity damping with a scalar {@code 1e-6} and 25 iterations.
     * Neither travels to lens design. The parameter vector mixes radii of 14mm and 2000mm
     * with 0.1mm air spaces and scaled aspheric coefficients, so a single damping value
     * applied to an identity matrix has no consistent meaning across it; sensitivity
     * damping scales by {@code diag(J^T J)}, which is Marquardt's scaling and is roughly
     * what MINPACK's {@code lmder} does internally with {@code mode=1}. 25 iterations is
     * simply too few - the measured Otus solve was still moving at 21.
     *
     * <p>{@code ftol} is raised from Prysm's {@code 1e-12} to {@code sqrt(eps)}, the value
     * {@code lmder} uses. Against a cost of order {@code 1e-2}, {@code 1e-12} is an
     * absolute threshold that essentially cannot fire, so the cost-tolerance test was
     * dead and every solve terminated some other way.
     */
    public static DampedLeastSquares.Options defaultOptions() {
        var options = new DampedLeastSquares.Options();
        options.dampingMode = DampedLeastSquares.DampingMode.SENSITIVITY;
        options.adaptiveDamping = true;
        options.maxIterations = 100;
        options.ftol = Math.sqrt(MinPack.dpmpar(1));
        // Prysm's 20 active-set passes assume a handful of constraints. A lens bounded
        // gap by gap has dozens, and the set is refined one constraint at a time, so 20
        // passes cannot even visit them all. Raising it is free: a pass is a dense solve
        // of order n, not a ray trace.
        options.maxActiveIterations = 200;
        // Feasibility tolerance, and unlike the others this one has units: a Bound is a
        // length in mm, so Prysm's 1e-10 asks the design to sit on the boundary to within
        // a ten-thousandth of an Angstrom. An edge gap is not linear in curvature, so a
        // step that runs along the linearised boundary leaves it by a second-order amount,
        // and the line search must halve until that amount falls under the tolerance.
        // Measured on the Leica 75/2: at 1e-10 the solve stalled at two active bounds with
        // steps of 1e-3 and never converged; at 1e-6 it converged on cost tolerance to a
        // better merit than the penalty form reached. The behaviour is flat from 1e-6 to
        // 1e-4, so this is the tight end of the range that works, and a nanometre of slack
        // on an air space is far below anything that can be manufactured.
        options.constraintTolerance = 1.0e-6;
        return options;
    }

    /**
     * {@link #defaultOptions()} with a much larger starting damping, for a problem that
     * carries {@link Bound}s.
     *
     * <p>Prysm's {@code 1e-6} leaves the first step essentially undamped, which on an
     * unconstrained problem is what you want and on a bounded one is not: the Leica 75/2
     * took a first step of norm 70.8, landed on three bounds at once, and then zig-zagged
     * along the active set for eighty iterations to buy 0.6% of cost. Starting at
     * {@code 1e-2} it never lunges, keeps making real progress at iteration 200, and
     * reaches a merit of 0.02235 where the undamped start reached 0.02653.
     *
     * <p>This is one lens. The number is a starting point that behaves, not a tuned
     * optimum, and adaptive damping moves it from there in any case.
     */
    public static DampedLeastSquares.Options boundedDefaultOptions() {
        var options = defaultOptions();
        options.damping = new double[]{1.0e-2};
        return options;
    }

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options) {
        this(analysis, variables, goals, options, NO_BOUNDS);
    }

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options, Bound[] bounds) {
        this(analysis, variables, goals, options, bounds, x -> new double[0], x -> new double[0]);
    }

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options,
                                   Function<double[], double[]> equalities,
                                   Function<double[], double[]> inequalities) {
        this(analysis, variables, goals, options, NO_BOUNDS, equalities, inequalities);
    }

    public DampedLeastSquaresSolver(Analysis analysis, Var[] variables, Goal[] goals,
                                   DampedLeastSquares.Options options, Bound[] bounds,
                                   Function<double[], double[]> equalities,
                                   Function<double[], double[]> inequalities) {
        this.analysis = Objects.requireNonNull(analysis);
        this.variables = variables.clone();
        this.merit = new LMDerMeritFunction(analysis, this.variables, goals.clone(), false);
        this.m = goals.length;
        this.options = Objects.requireNonNull(options);
        this.bounds = Objects.requireNonNull(bounds).clone();
        for (Bound bound : this.bounds) Objects.requireNonNull(bound, "null bound");
        this.equalities = Objects.requireNonNull(equalities);
        this.inequalities = Objects.requireNonNull(inequalities);
    }

    public DampedLeastSquares.Result result() { return result; }

    /** The hard bounds in the order they occupy the inequality vector, after any
     * inequalities the caller's own callback returns. */
    public Bound[] bounds() { return bounds.clone(); }

    /**
     * Returns {@link #CONVERGED}, {@link #ITERATION_LIMIT} or {@link #FAILED}. These are
     * not MINPACK codes; consult {@code result().message()} for the stopping reason.
     *
     * <p>Note that {@code ITERATION_LIMIT} is reported separately from success even
     * though the point is feasible. For an unconstrained problem every point is feasible,
     * so Prysm's convention of calling that success is unconditional, and it blessed a
     * measured Otus run that stopped at 25 iterations with on-axis MTF of 0.077.
     */
    @Override public int solve() {
        result = null;
        // The same start-of-solve check LMDerMeritFunction.getSolver() runs, which this
        // path previously skipped: it turns an unrepresentable start into a diagnostic
        // naming the first failed contrast sample rather than an opaque solver failure.
        merit.validateInputs();
        double[] initial = new double[variables.length];
        for (int i = 0; i < initial.length; i++) initial[i] = variables[i].read_from_prescription();
        // validateInputs left Analysis computed at the starting prescription, so the
        // caller's callbacks can be sized here. A callback that returns nothing is never
        // invoked again, which matters: invoking one costs a full Analysis recompute.
        callerEqualityCount = equalities.apply(initial).length;
        callerInequalityCount = inequalities.apply(initial).length;
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
                    prepared = x.clone();
                    return j;
                }
                private void prepare(double[] x) {
                    if (!Arrays.equals(prepared, x)) { prepared = null; restore(x); prepared = x.clone(); }
                }
                @Override public double[] equalities(double[] x) {
                    if (callerEqualityCount == 0) return EMPTY;
                    prepare(x);
                    return equalities.apply(x);
                }
                @Override public double[] inequalities(double[] x) {
                    double[] supplied = EMPTY;
                    if (callerInequalityCount != 0) { prepare(x); supplied = inequalities.apply(x); }
                    if (bounds.length == 0) return supplied;
                    // Bounds read the prescription only, so x has to be written but no
                    // Analysis is needed. Whatever computed last left Analysis at x too.
                    writeVariables(x);
                    double[] all = Arrays.copyOf(supplied, supplied.length + bounds.length);
                    for (int i = 0; i < bounds.length; i++) all[supplied.length + i] = bounds[i].value();
                    return all;
                }
                @Override public double[][] inequalityJacobian(double[] x) {
                    // Only the bound half can be differentiated cheaply. When the caller
                    // also supplies inequalities, fall back to the solver's central
                    // differences for the whole vector rather than mixing the two.
                    if (bounds.length == 0 || callerInequalityCount != 0) return null;
                    return boundJacobian(x);
                }
            }, initial, options);
            result = optimizer.run();
            return switch (result.status()) {
                case CONVERGED -> CONVERGED;
                case ITERATION_LIMIT -> result.feasible() ? ITERATION_LIMIT : FAILED;
                default -> FAILED;
            };
        } finally {
            // Finite-difference probes and rejected trials mutate the prescription.
            // Always restore the last accepted point, including on callback failure.
            restore(optimizer == null ? initial : optimizer.result().x());
        }
    }

    /** How many constraints the caller's own callbacks return, measured once at the
     * starting point. Zero means the callback is never invoked again - it would cost a
     * full Analysis recompute - and, for inequalities, that the bound Jacobian can be
     * supplied instead of central-differenced. */
    private int callerEqualityCount, callerInequalityCount;

    private static final double[] EMPTY = new double[0];

    /**
     * Gradient of every bound with respect to every scaled variable, by central
     * differences over the prescription. No {@link Analysis#compute()} is involved, so
     * the whole matrix costs less than a single residual evaluation - against the
     * {@code 2n} full analyses the solver's own differencing would spend.
     *
     * <p>The step is {@code 1e-6 max(1, |x|)} rather than the variable's
     * {@link Var#_d_delta}. Those steps are sized to clear ray-trace noise; sag and
     * thickness are exact functions of the prescription, so a far smaller step is both
     * admissible and more accurate.
     */
    private double[][] boundJacobian(double[] x) {
        int n = variables.length;
        double[][] jacobian = new double[bounds.length][n];
        for (int k = 0; k < n; k++) {
            double h = 1.0e-6 * Math.max(1.0, Math.abs(x[k]));
            double[] probe = x.clone();
            probe[k] = x[k] + h; writeVariables(probe);
            double[] forward = boundValues();
            probe[k] = x[k] - h; writeVariables(probe);
            double[] backward = boundValues();
            for (int i = 0; i < bounds.length; i++)
                jacobian[i][k] = (forward[i] - backward[i]) / (2.0 * h);
        }
        // Leave the prescription where the caller left it. Analysis is untouched
        // throughout and so remains consistent with x.
        writeVariables(x);
        for (double[] row : jacobian)
            for (double value : row)
                if (!Double.isFinite(value))
                    throw new IllegalStateException("Bound gradient is not finite; a bound must be "
                            + "a finite function of the prescription everywhere the solver probes");
        return jacobian;
    }

    private double[] boundValues() {
        double[] values = new double[bounds.length];
        for (int i = 0; i < bounds.length; i++) values[i] = bounds[i].value();
        return values;
    }

    /** Writes scaled variables to the prescription without recomputing Analysis. */
    private void writeVariables(double[] x) {
        for (int i = 0; i < x.length; i++) {
            variables[i].set_scaled_value(x[i]);
            variables[i].write_to_prescription();
        }
    }

    private void restore(double[] x) {
        writeVariables(x);
        analysis.compute();
    }
}
