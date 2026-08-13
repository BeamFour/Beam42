package org.redukti.optim;

/**
 * Anchors a prescription parameter to the value it started at.
 *
 * <p>Contrast optimization sees the design clearly enough to rearrange it wholesale, and
 * left to itself it will: on the Leica 75/2 with every air space free it drove three gaps
 * negative, passing elements through each other and through the stop. Nothing in an
 * optical merit function has an opinion about mechanical layout, so something has to.
 *
 * <p>These goals supply that opinion the same way {@link GoalParax} holds focal length and
 * f-number: a target, a deviation, and a weight. There is no dead band and no bound. The
 * parameter is free to move, it simply costs merit to do so, and the weight decides how
 * much. Raise it to hold the design close, lower it to let the optimizer explore.
 *
 * <p>The residual is a <em>fraction</em> of the starting value rather than an absolute
 * deviation, which is what makes one weight sensible across a whole prescription: a lens
 * has 0.1mm air gaps beside 39mm ones, and surfaces at r=14 beside r=2009. An absolute
 * residual would effectively freeze the small ones and ignore the large.
 *
 * <p>Deliberately never reports {@link org.redukti.mathlib.LMLSolver#BIGVAL}. A goal like
 * this exists to steer the solver, not to end the run, and a single BIGVAL raised during
 * a Jacobian probe step aborts the whole solve.
 */
public abstract class GoalParameter extends Goal {

    /** Stands in for an undefined ratio: large enough to repel, small enough to be finite. */
    private static final double UNDEFINED_DEVIATION = 1.0e6;

    public final int _surface_id;
    /** The value this goal holds the parameter to, read when the goal was built. */
    public final double _base;

    protected GoalParameter(Analysis analysis, int surfaceId, double base, double weight) {
        super(analysis, 0.0, weight);
        if (surfaceId < 0)
            throw new IllegalArgumentException("surface must be non-negative");
        if (!Double.isFinite(base) || base == 0.0)
            throw new IllegalArgumentException(
                    "a fractional goal needs a finite non-zero base value, got " + base);
        _surface_id = surfaceId;
        _base = base;
    }

    /** Signed deviation from {@link #_base}, as a fraction of it. */
    protected abstract double fractional_deviation();

    @Override
    public final double value() {
        double deviation = fractional_deviation();
        if (Double.isNaN(deviation)) return UNDEFINED_DEVIATION;
        if (Double.isInfinite(deviation))
            return deviation > 0 ? UNDEFINED_DEVIATION : -UNDEFINED_DEVIATION;
        return deviation;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " surface=" + _surface_id
                + ", base=" + _base + ", deviation=" + value() + ", weight=" + _weight;
    }
}
