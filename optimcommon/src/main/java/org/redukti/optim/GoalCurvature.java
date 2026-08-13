package org.redukti.optim;

/**
 * Holds a surface near its starting <em>curvature</em>.
 *
 * <p>Curvature rather than radius, deliberately. {@link VarRadius} optimizes radius, but
 * radius is a poor measure of how much a surface has really changed: on a near-flat
 * surface it runs away towards infinity for a negligible optical change, so a fractional
 * radius goal would barely restrain it there while over-restraining a strongly curved one.
 * Curvature {@code c = 1/r} is the quantity that tracks optical effect.
 *
 * <p>The fractional curvature deviation falls out of the radii directly, with no division
 * by a curvature that might be zero:
 *
 * <pre>{@code c/c0 - 1 = (1/r)/(1/r0) - 1 = r0/r - 1}</pre>
 *
 * <p>It behaves sensibly at both extremes. As the surface flattens, {@code r -> infinity}
 * and the deviation tends to -1, so fully flattening a surface reads as a 100% change. As
 * it curves up, {@code r -> 0} and the deviation grows without bound, which is exactly
 * where strong resistance is wanted. A surface that starts flat has no curvature variable
 * in the first place, so a zero base radius cannot arise here.
 */
public class GoalCurvature extends GoalParameter {

    public GoalCurvature(Analysis analysis, int surfaceId, double weight) {
        super(analysis, surfaceId, analysis._prescription._surfaces[surfaceId]._radius, weight);
    }

    @Override
    protected double fractional_deviation() {
        double radius = _analysis._prescription._surfaces[_surface_id]._radius;
        // A radius driven to zero gives a non-finite ratio, which the base class turns
        // into a large finite deviation rather than a solve-killing BIGVAL.
        return _base / radius - 1.0;
    }
}
