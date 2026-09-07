package org.redukti.optim;

/**
 * A hard inequality on the prescription: feasible when {@link #value()} is non-negative.
 *
 * <p>The counterpart to {@link Constraint}, and deliberately a different thing. A
 * {@code Constraint} is a weighted residual anchored to the starting value: it resists
 * <em>change</em>, symmetrically, and how hard it resists depends on a weight that has to
 * be balanced against the size of the optical merit block. Grow the field count and the
 * optical block grows with it while the constraint block does not, so the same weight no
 * longer holds the same line. A {@code Bound} has no weight to detune. It states where the
 * design may not go, and the solver's active set keeps it there or reports that it cannot.
 *
 * <p>Only a solver with a constrained step can use these - presently
 * {@link DampedLeastSquaresSolver}. They are ignored by {@link LMDerSolver}, which has no
 * way to represent them.
 *
 * <h2>Why the value must be trace-free</h2>
 *
 * <p>Implementations must read the prescription only, never {@link Analysis}. A bound is
 * differentiated once per solver iteration, and the generic gradient perturbs every
 * variable in turn. If evaluating a bound needed a ray trace, differentiating it would
 * cost {@code 2n} full analyses per iteration - on a 44-variable lens that is 88 traces
 * to differentiate a quantity that is pure geometry. Reading the prescription directly
 * makes the whole constraint Jacobian free relative to the optical Jacobian.
 */
public abstract class Bound {

    /**
     * Margin against the bound, in system units: non-negative is feasible, negative is a
     * violation, and the magnitude is how far out.
     *
     * <p>Must be computed from the prescription alone, and must be finite everywhere the
     * solver probes. A non-finite value is a programming error rather than an
     * infeasibility, and the solver rejects it rather than stepping away from it.
     */
    public abstract double value();

    /** Short description used in reports and failure messages. */
    public abstract String describe();

    @Override
    public String toString() {
        return describe() + " margin=" + value();
    }
}
