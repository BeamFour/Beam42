package org.redukti.optim;

/**
 * Holds a surface near its starting <em>curvature</em>.
 *
 * <p>Curvature rather than radius, deliberately. The optimizer varies radius through
 * {@link VarRadius}, but radius is a poor measure of how much a surface has really
 * changed: on a near-flat surface it runs away towards infinity for a negligible optical
 * change, so constraining it fractionally would barely restrain that surface while
 * over-restraining a strongly curved one. Curvature {@code c = 1/r} tracks optical effect.
 *
 * <p>Target and value are therefore both curvatures, and the fractional change the
 * constraint resists is
 *
 * <pre>{@code c/c0 - 1 = (1/r)/(1/r0) - 1 = r0/r - 1}</pre>
 *
 * <p>It behaves sensibly at both extremes. As the surface flattens, {@code r -> infinity}
 * and the change tends to -1, so fully flattening a surface reads as 100%. As it curves
 * up, {@code r -> 0} and the change grows without bound, which is exactly where strong
 * resistance is wanted. A surface that starts flat has no curvature variable in the first
 * place, so a zero starting radius cannot arise here.
 */
public class ConstraintCurvature extends Constraint {

    public ConstraintCurvature(Analysis analysis, int surfaceId, double weight) {
        super(analysis, surfaceId, curvature(analysis, surfaceId), weight);
    }

    /** Curvature, so that target, value and residual are all in the same quantity. */
    private static double curvature(Analysis analysis, int surfaceId) {
        return 1.0 / analysis._prescription._surfaces[surfaceId]._radius;
    }

    @Override
    protected double current_value() {
        // A radius driven to zero gives a non-finite curvature, which the base class
        // turns into a large finite miss rather than a solve-killing BIGVAL.
        return curvature(_analysis, _surface_id);
    }
}
