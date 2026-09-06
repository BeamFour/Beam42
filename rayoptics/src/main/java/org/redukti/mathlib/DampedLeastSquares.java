/*
 * Port of prysm/x/optym/least_squares.py, DampedLeastSquares.
 * Copyright (c) 2017 Brandon Dube. MIT license; see META-INF/LICENSE-prysm.txt.
 */
package org.redukti.mathlib;

import org.redukti.mathlib.jama.Matrix;
import org.redukti.mathlib.jama.LUDecomposition;
import org.redukti.mathlib.jama.SingularValueDecomposition;
import java.util.*;
import java.util.function.Function;

/** Sequential constrained damped least squares. Equalities are zero and
 * inequalities are nonnegative. Callbacks receive scaled parameter vectors;
 * returned Jacobians are row-major (residuals by parameters).
 */
public final class DampedLeastSquares {
    @FunctionalInterface
    public interface Problem {
        double[] residuals(double[] x);
        default double[][] jacobian(double[] x) { return null; }
        default double[] equalities(double[] x) { return new double[0]; }
        default double[] inequalities(double[] x) { return new double[0]; }
    }

    public enum DampingMode { IDENTITY, SENSITIVITY }

    /** Arrays accept either one value or one value per parameter. */
    public static final class Options {
        public double[] damping = {1e-6}, dampingMin = {0}, dampingMax = {Double.POSITIVE_INFINITY};
        public double[] trustRadii;
        public DampingMode dampingMode = DampingMode.IDENTITY;
        public double dampingFloor = 1, dampingIncrease = 10, dampingDecrease = .2;
        public boolean adaptiveDamping;
        public int maxIterations = 25, maxDampingAttempts = 6, maxActiveIterations = 20, maxLineSearch = 12;
        public double xtol = 1e-10, ftol = 1e-12, constraintTolerance = 1e-10,
                activeTolerance = 1e-10, finiteDifferenceStep = 1e-6;

        private Options copy() {
            Options c = new Options();
            c.damping = damping.clone(); c.dampingMin = dampingMin.clone(); c.dampingMax = dampingMax.clone();
            c.trustRadii = trustRadii == null ? null : trustRadii.clone();
            c.dampingMode = dampingMode; c.dampingFloor = dampingFloor;
            c.dampingIncrease = dampingIncrease; c.dampingDecrease = dampingDecrease;
            c.adaptiveDamping = adaptiveDamping; c.maxIterations = maxIterations;
            c.maxDampingAttempts = maxDampingAttempts; c.maxActiveIterations = maxActiveIterations;
            c.maxLineSearch = maxLineSearch; c.xtol = xtol; c.ftol = ftol;
            c.constraintTolerance = constraintTolerance; c.activeTolerance = activeTolerance;
            c.finiteDifferenceStep = finiteDifferenceStep;
            return c;
        }
    }

    /** Detached snapshot. Multipliers follow H dx + grad + A^T lambda = 0;
     * active inequality multipliers are consequently nonpositive. */
    public record Result(double[] x, double[] residuals, double cost, double constraintViolation,
                         boolean success, String message, int iterations, int nfev, int njev, int ncev,
                         double[] equalityMultipliers, double[] inequalityMultipliers,
                         int[] activeInequalities, List<Iteration> history) {}
    public record Iteration(double[] x, double cost, double constraintViolation, double stepNorm,
                            double alpha, double trustScale, double[] dampingDiagonal,
                            int dampingAttempts, int[] activeInequalities) {}
    private record State(double[] x, double[] r, double[] eq, double[] ineq, double cost, double violation) {}
    private record Direction(double[] dx, double[] eqMultipliers, double[] ineqMultipliers, int[] active) {}

    private final Problem problem;
    private final Options o;
    private final int n;
    private final double[] damping, dampingMin, dampingMax, radii;
    private State state;
    private int iterations, nfev, njev, ncev;
    private boolean done, success;
    private String message = "";
    private double[] lambdaEq, lambdaIneq;
    private int[] active = new int[0];
    private final List<Iteration> history = new ArrayList<>();

    public DampedLeastSquares(Problem problem, double[] x0) { this(problem, x0, new Options()); }

    public DampedLeastSquares(Problem problem, double[] x0, Options options) {
        this.problem = Objects.requireNonNull(problem);
        o = Objects.requireNonNull(options).copy();
        n = x0.length;
        if (n == 0 || !finite(x0)) throw new IllegalArgumentException("x0 must be nonempty and finite");
        damping = vector(o.damping, "damping");
        dampingMin = vector(o.dampingMin, "dampingMin");
        dampingMax = vector(o.dampingMax, "dampingMax");
        radii = o.trustRadii == null ? null : vector(o.trustRadii, "trustRadii");
        for (int j = 0; j < n; j++) {
            nonnegative(damping[j]); nonnegative(dampingMin[j]);
            if (Double.isNaN(dampingMax[j]) || dampingMax[j] < dampingMin[j])
                throw new IllegalArgumentException("invalid damping bounds");
            if (radii != null && !(radii[j] > 0)) throw new IllegalArgumentException("trust radii must be positive");
        }
        Objects.requireNonNull(o.dampingMode);
        nonnegative(o.dampingFloor); nonnegative(o.xtol); nonnegative(o.ftol);
        nonnegative(o.constraintTolerance); nonnegative(o.activeTolerance);
        if (!(o.finiteDifferenceStep > 0) || !Double.isFinite(o.finiteDifferenceStep)
                || !(o.dampingIncrease > 1) || !Double.isFinite(o.dampingIncrease)
                || !(o.dampingDecrease > 0 && o.dampingDecrease < 1)
                || o.maxIterations < 0 || o.maxDampingAttempts < 0
                || o.maxActiveIterations < 1 || o.maxLineSearch < 0)
            throw new IllegalArgumentException("invalid DLS options");
        state = evaluate(x0.clone());
        if (!valid(state)) throw new IllegalArgumentException("initial residuals and constraints must be finite");
        lambdaEq = new double[state.eq.length]; lambdaIneq = new double[state.ineq.length];
    }

    public boolean isDone() { return done; }

    public Result result() {
        List<Iteration> snapshot = new ArrayList<>();
        for (Iteration h : history) snapshot.add(new Iteration(h.x.clone(), h.cost, h.constraintViolation,
                h.stepNorm, h.alpha, h.trustScale, h.dampingDiagonal.clone(), h.dampingAttempts,
                h.activeInequalities.clone()));
        return new Result(state.x.clone(), state.r.clone(), state.cost, state.violation, success,
                message, iterations, nfev, njev, ncev, lambdaEq.clone(), lambdaIneq.clone(),
                active.clone(), List.copyOf(snapshot));
    }

    public Result run() { while (!done) step(); return result(); }

    /** Performs one accepted iteration, including any damping retries. */
    public Result step() {
        if (done) throw new IllegalStateException("optimizer has finished");
        if (iterations >= o.maxIterations) { finish(feasible(), "maximum iterations reached"); return result(); }
        State previous = state;
        for (int attempt = 0; ; attempt++) {
            double[][] j = problem.jacobian(state.x.clone());
            njev++;
            if (j == null) j = differentiate(this::residuals, state.r);
            checkJacobian(j, state.r.length);
            double[][] ae = differentiate(x -> constraints(x, true), state.eq);
            double[][] ai = differentiate(x -> constraints(x, false), state.ineq);
            double[] diagonal = damping.clone();
            if (o.dampingMode == DampingMode.SENSITIVITY) {
                for (int k = 0; k < n; k++) {
                    double sensitivity = 0;
                    for (double[][] matrix : new double[][][]{j, ae, ai})
                        for (double[] row : matrix) sensitivity += row[k] * row[k];
                    diagonal[k] *= Math.max(sensitivity, o.dampingFloor);
                }
            }
            Direction direction = direction(j, ae, ai, diagonal);
            if (direction == null) { finish(false, "active set did not converge"); return result(); }
            lambdaEq = direction.eqMultipliers; lambdaIneq = direction.ineqMultipliers; active = direction.active;
            double[] dx = direction.dx;
            double trustScale = 1;
            if (radii != null) for (int k = 0; k < n; k++)
                if (Math.abs(dx[k]) > radii[k]) trustScale = Math.min(trustScale, radii[k] / Math.abs(dx[k]));
            for (int k = 0; k < n; k++) dx[k] *= trustScale;
            double stepNorm = norm(dx);
            if (!Double.isFinite(stepNorm)) { finish(false, "nonfinite step"); return result(); }
            if (stepNorm <= o.xtol * (o.xtol + norm(state.x)) && feasible()) {
                finish(true, "step tolerance reached"); return result();
            }
            State trial = null;
            double alpha = 1;
            for (int search = 0; search <= o.maxLineSearch; search++, alpha *= .5) {
                double[] x = state.x.clone();
                for (int k = 0; k < n; k++) x[k] += alpha * dx[k];
                State candidate = evaluate(x);
                if (valid(candidate) && (feasible()
                        ? candidate.violation <= o.constraintTolerance
                            && candidate.cost <= state.cost + o.ftol * Math.max(1, state.cost)
                        : candidate.violation < state.violation)) { trial = candidate; break; }
            }
            if (trial == null) {
                if (!o.adaptiveDamping || attempt >= o.maxDampingAttempts) {
                    finish(false, "line search failed"); return result();
                }
                rescale(o.dampingIncrease);
                continue;
            }
            state = trial; iterations++;
            history.add(new Iteration(state.x.clone(), state.cost, state.violation, stepNorm,
                    alpha, trustScale, diagonal.clone(), attempt, active.clone()));
            if (o.adaptiveDamping) rescale(alpha == 1 ? o.dampingDecrease : o.dampingIncrease);
            if (feasible() && Math.abs(previous.cost - state.cost) <= o.ftol * Math.max(1, Math.abs(previous.cost)))
                finish(true, "cost tolerance reached");
            else if (feasible() && alpha * stepNorm <= o.xtol * (o.xtol + norm(previous.x)))
                finish(true, "step tolerance reached");
            else if (iterations >= o.maxIterations) finish(feasible(), "maximum iterations reached");
            return result();
        }
    }

    private Direction direction(double[][] j, double[][] ae, double[][] ai, double[] diagonal) {
        double[][] h = new double[n][n];
        double[] gradient = new double[n];
        for (int k = 0; k < n; k++) {
            h[k][k] = diagonal[k];
            for (int row = 0; row < j.length; row++) {
                gradient[k] += j[row][k] * state.r[row];
                for (int l = 0; l < n; l++) h[k][l] += j[row][k] * j[row][l];
            }
        }
        TreeSet<Integer> working = new TreeSet<>();
        for (int i = 0; i < ai.length; i++) if (state.ineq[i] <= o.activeTolerance) working.add(i);
        for (int iteration = 0; iteration < o.maxActiveIterations; iteration++) {
            int size = n + ae.length + working.size();
            double[][] kkt = new double[size][size];
            double[] rhs = new double[size];
            for (int k = 0; k < n; k++) { System.arraycopy(h[k], 0, kkt[k], 0, n); rhs[k] = -gradient[k]; }
            int row = n;
            for (int i = 0; i < ae.length; i++, row++) addConstraint(kkt, rhs, row, ae[i], -state.eq[i]);
            for (int i : working) { addConstraint(kkt, rhs, row, ai[i], -state.ineq[i]); row++; }
            double[] solution = solveLinearSystem(kkt, rhs);
            double[] dx = Arrays.copyOf(solution, n);
            boolean added = false;
            for (int i = 0; i < ai.length; i++)
                if (state.ineq[i] + dot(ai[i], dx) < -o.constraintTolerance && working.add(i)) added = true;
            if (added) continue;
            row = n + ae.length;
            List<Integer> drop = new ArrayList<>();
            for (int i : working) {
                if (solution[row++] > o.constraintTolerance && state.ineq[i] >= -o.constraintTolerance) drop.add(i);
            }
            if (!drop.isEmpty()) { working.removeAll(drop); continue; }
            double[] le = Arrays.copyOfRange(solution, n, n + ae.length), li = new double[ai.length];
            row = n + ae.length;
            for (int i : working) li[i] = solution[row++];
            return new Direction(dx, le, li, working.stream().mapToInt(Integer::intValue).toArray());
        }
        return null;
    }

    private static double[] solveLinearSystem(double[][] coefficients, double[] rhs) {
        Matrix matrix = new Matrix(coefficients);
        LUDecomposition lu = new LUDecomposition(matrix);
        boolean nonsingular = lu.isNonsingular();
        // Jama tests for exactly zero pivots. Retain the previous solver's
        // threshold so nearly dependent constraints also use the SVD fallback.
        double[][] u = lu.getU().getArray();
        for (int i = 0; i < rhs.length; i++)
            if (Math.abs(u[i][i]) < 1e-11) nonsingular = false;
        if (nonsingular)
            return lu.solve(new Matrix(rhs, rhs.length)).getColumnPackedCopy();

        // Jama exposes SVD factors, but no least-squares solve. Apply the
        // truncated pseudoinverse V S^+ U^T to obtain a minimum-norm solution.
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        double[] singularValues = svd.getSingularValues();
        double[][] left = svd.getU().getArray(), right = svd.getV().getArray();
        double tolerance = Math.max(rhs.length * singularValues[0] * Math.ulp(1.0),
                Math.sqrt(Double.MIN_NORMAL));
        double[] solution = new double[rhs.length];
        for (int k = 0; k < singularValues.length; k++) {
            if (singularValues[k] <= tolerance) continue;
            double projection = 0;
            for (int i = 0; i < rhs.length; i++) projection += left[i][k] * rhs[i];
            projection /= singularValues[k];
            for (int i = 0; i < rhs.length; i++) solution[i] += right[i][k] * projection;
        }
        return solution;
    }

    private void addConstraint(double[][] matrix, double[] rhs, int row, double[] a, double b) {
        for (int k = 0; k < n; k++) matrix[row][k] = matrix[k][row] = a[k];
        rhs[row] = b;
    }

    private double[][] differentiate(Function<double[], double[]> function, double[] base) {
        double[][] jac = new double[base.length][n];
        if (base.length == 0) return jac;
        for (int k = 0; k < n; k++) {
            double h = o.finiteDifferenceStep * Math.max(1, Math.abs(state.x[k]));
            double[] xp = state.x.clone(), xm = state.x.clone(); xp[k] += h; xm[k] -= h;
            double[] fp = function.apply(xp), fm = function.apply(xm);
            if (fp.length != base.length || fm.length != base.length)
                throw new IllegalArgumentException("callback dimensions changed");
            for (int i = 0; i < base.length; i++) jac[i][k] = (fp[i] - fm[i]) / (2 * h);
        }
        checkJacobian(jac, base.length);
        return jac;
    }

    private void checkJacobian(double[][] j, int rows) {
        if (j.length != rows) throw new IllegalArgumentException("wrong Jacobian row count");
        for (double[] row : j) if (row == null || row.length != n || !finite(row))
            throw new IllegalArgumentException("Jacobian must have finite entries and one column per parameter");
    }

    private double[] residuals(double[] x) { nfev++; return problem.residuals(x.clone()).clone(); }
    private double[] constraints(double[] x, boolean equality) {
        ncev++; return (equality ? problem.equalities(x.clone()) : problem.inequalities(x.clone())).clone();
    }
    private State evaluate(double[] x) {
        double[] r = residuals(x);
        // Invalid optical trials may have no usable Analysis for constraint callbacks.
        if (state != null && r.length == state.r.length && (!finite(r) || !finite(x)))
            return new State(x, r, state.eq, state.ineq, Double.NaN, Double.NaN);
        double[] eq = constraints(x, true), ineq = constraints(x, false);
        if (state != null && (r.length != state.r.length || eq.length != state.eq.length || ineq.length != state.ineq.length))
            throw new IllegalArgumentException("callback dimensions changed");
        double violation = norm(eq);
        for (double v : ineq) violation = Math.hypot(violation, Math.min(v, 0));
        return new State(x, r, eq, ineq, .5 * dot(r, r), violation);
    }
    private static boolean valid(State s) {
        return finite(s.x) && finite(s.r) && finite(s.eq) && finite(s.ineq)
                && Double.isFinite(s.cost) && Double.isFinite(s.violation);
    }
    private boolean feasible() { return state.violation <= o.constraintTolerance; }
    private void finish(boolean ok, String reason) { done = true; success = ok; message = reason; }
    private void rescale(double factor) {
        for (int k = 0; k < n; k++) damping[k] = Math.max(dampingMin[k], Math.min(dampingMax[k], damping[k] * factor));
    }
    private double[] vector(double[] value, String name) {
        if (value == null || (value.length != 1 && value.length != n))
            throw new IllegalArgumentException(name + " must have length 1 or " + n);
        double[] result = new double[n];
        for (int k = 0; k < n; k++) result[k] = value[value.length == 1 ? 0 : k];
        return result;
    }
    private static void nonnegative(double x) {
        if (!Double.isFinite(x) || x < 0) throw new IllegalArgumentException("expected finite nonnegative value");
    }
    private static boolean finite(double[] x) { for (double v : x) if (!Double.isFinite(v)) return false; return true; }
    private static double dot(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }
    private static double norm(double[] a) { double s = 0; for (double v : a) s = Math.hypot(s, v); return s; }
}
