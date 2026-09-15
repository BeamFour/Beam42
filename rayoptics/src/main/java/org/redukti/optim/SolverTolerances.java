package org.redukti.optim;

import org.redukti.mathlib.MinPack;

/**
 * The lmder stopping tolerances, and the defaults a trial starts from. A trial's
 * 'solver' settings replace them one at a time; everything else keeps the value
 * here, so these four fields are the only place a default is stated.
 *
 * @param ftol           relative reduction in the sum of squares that stops the solve
 * @param xtol           relative change in the varied values themselves - the
 *                       curvatures, thicknesses and aspheric terms - between two
 *                       iterations, below which the solve stops; 0 disables the test
 * @param gtol           gradient flatness that stops the solve
 * @param maxEvaluations the lmder maxfev: trial steps, NOT the Jacobian probes around
 *                       them. {@link #FROM_VARIABLE_COUNT} derives it from the number
 *                       of variables, which is what a trial that says nothing gets.
 */
public record SolverTolerances(double ftol, double xtol, double gtol, int maxEvaluations) {

    /** Relative reduction in the sum of squares: the square root of the machine epsilon. */
    public static final double DEFAULT_FTOL = Math.sqrt(MinPack.dpmpar(1));
    /** Don't stop on how far the variables moved; ray-trace noise makes late steps tiny. */
    public static final double DEFAULT_XTOL = 0.0;
    /** Stop when the gradient is genuinely flat. */
    public static final double DEFAULT_GTOL = Math.sqrt(MinPack.dpmpar(1));
    /** maxEvaluations placeholder for 100 * (variables + 1), which needs the variable count. */
    public static final int FROM_VARIABLE_COUNT = 0;

    public static final SolverTolerances DEFAULTS =
            new SolverTolerances(DEFAULT_FTOL, DEFAULT_XTOL, DEFAULT_GTOL, FROM_VARIABLE_COUNT);

    public SolverTolerances {
        check(ftol, "ftol");
        check(xtol, "xtol");
        check(gtol, "gtol");
        if (maxEvaluations < 0)
            throw new IllegalArgumentException("max-evaluations must be positive");
    }

    /** The trial step limit for a solve over {@code variables} variables. */
    public int maxEvaluations(int variables) {
        return maxEvaluations == FROM_VARIABLE_COUNT ? (variables + 1) * 100 : maxEvaluations;
    }

    private static void check(double value, String what) {
        if (!Double.isFinite(value) || value < 0.0)
            throw new IllegalArgumentException(what + " must be finite and non-negative");
    }
}
