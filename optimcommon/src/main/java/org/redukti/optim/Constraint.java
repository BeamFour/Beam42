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
public abstract class Constraint extends Goal {

    /** How far out of range an undefined parameter reads: repellent, but finite. */
    private static final double UNDEFINED_DEVIATION = 1.0e6;

    public final int _surface_id;

    /**
     * @param base   the parameter's starting value, which becomes {@link #_target}
     * @param weight the caller's weight, before fractional normalization
     */
    protected Constraint(Analysis analysis, int surfaceId, double base, double weight) {
        super(analysis, base, normalized_weight(base, weight));
        if (surfaceId < 0)
            throw new IllegalArgumentException("surface must be non-negative");
        _surface_id = surfaceId;
    }

    /**
     * Fold the fractional normalization into the weight so the starting value can serve
     * as the target.
     *
     * <p>The solver forms {@code (value - target) * sqrt(weight)}. Holding a parameter to
     * a <em>fraction</em> of where it started means
     *
     * <pre>{@code (v/base - 1) * sqrt(w) = (v - base) * sqrt(w / base^2)}</pre>
     *
     * <p>so scaling the weight by {@code 1/base^2} is exactly equivalent, and lets
     * {@code value()} report the parameter itself against a target of its starting value -
     * the same shape as {@link GoalParax} - instead of carrying a separate base.
     *
     * <p>The consequence is that {@link #_weight} is not the number the caller passed. It
     * is larger for small parameters and smaller for large ones, which is the
     * normalization doing its job: a 0.1mm air gap and a 39mm back focus then resist a
     * given <em>proportional</em> change equally.
     */
    private static double normalized_weight(double base, double weight) {
        if (!Double.isFinite(base) || base == 0.0)
            throw new IllegalArgumentException(
                    "a fractional constraint needs a finite non-zero starting value, got " + base);
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("weight must be finite and non-negative");
        return weight / (base * base);
    }

    /** The parameter's present value, in the same units as {@link #_target}. */
    protected abstract double current_value();

    @Override
    public final double value() {
        double current = current_value();
        if (Double.isFinite(current)) return current;
        // An undefined parameter (a radius driven to zero, say) must still read as a
        // large finite miss rather than BIGVAL, which would abort the solve outright.
        return _target + Math.copySign(UNDEFINED_DEVIATION * Math.abs(_target), current);
    }

    /** Signed change from the starting value, as a fraction of it. */
    public final double fractional_deviation() {
        return value() / _target - 1.0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " surface=" + _surface_id
                + ", value=" + value() + ", start=" + _target
                + ", change=" + fractional_deviation() + ", weight=" + _weight;
    }
}
