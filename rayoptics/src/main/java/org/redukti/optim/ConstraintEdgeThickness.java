package org.redukti.optim;

import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.elem.profiles.RadialPolynomial;
import org.redukti.rayoptics.elem.profiles.Spherical;
import org.redukti.rayoptics.elem.profiles.SurfaceProfile;
import org.redukti.spec.SurfaceType;

/**
 * Holds the <em>edge</em> separation of a gap near its starting value.
 *
 * <p>{@link ConstraintThickness} holds axial centre thickness, which is not the same
 * thing: two surfaces can keep their axial gap and still pass through one another away
 * from the axis, because the separation at height {@code h} is
 *
 * <pre>{@code gap(h) = t + sag_next(h) - sag_this(h)}</pre>
 *
 * <p>and curvature is free to move. That is how a solve with thickness constraints in
 * place still produced overlapping first and second surfaces on the Leica 75/2. This
 * constraint watches the quantity that actually goes negative.
 *
 * <p>Measured by default at the smaller of the two bounding semi-diameters, which is the
 * outermost height at which both surfaces exist. Pass an explicit height to check
 * somewhere else - a mount land outside the clear aperture, say.
 *
 * <p>Like every {@link Constraint} this is a penalty, not a bound. It makes crossing
 * expensive rather than impossible, and it anchors to the starting separation rather than
 * to zero, so it resists <em>change</em> in either direction. A design that needs its
 * edges opened up rather than preserved wants a different goal.
 */
public class ConstraintEdgeThickness extends Constraint {

    /** Height at which the separation is measured, in system units. */
    public final double _height;

    /**
     * Constrain the gap following {@code surfaceId}, measured at the smaller of the two
     * bounding semi-diameters.
     */
    public ConstraintEdgeThickness(Analysis analysis, int surfaceId, double weight) {
        this(analysis, surfaceId, default_height(analysis, surfaceId), weight);
    }

    public ConstraintEdgeThickness(Analysis analysis, int surfaceId, double height, double weight) {
        super(analysis, surfaceId, edge_gap(analysis, surfaceId, checked_height(height)), weight);
        _height = height;
    }

    @Override
    protected double current_value() {
        return edge_gap(_analysis, _surface_id, _height);
    }

    /**
     * True when the gap after {@code surfaceId} can be constrained: there is a following
     * surface, and the starting separation is positive and finite.
     *
     * <p>A design that already starts with coincident or crossed surfaces has nothing
     * useful to anchor to, and a fractional constraint cannot be formed around zero.
     */
    public static boolean is_constrainable(Analysis analysis, int surfaceId) {
        if (surfaceId < 0 || surfaceId >= analysis._prescription._surfaces.length - 1)
            return false;
        double height = default_height(analysis, surfaceId);
        if (!(height > 0.0)) return false;
        double gap = edge_gap(analysis, surfaceId, height);
        return Double.isFinite(gap) && gap > 0.0;
    }

    /** The smaller of the two bounding semi-diameters. */
    private static double default_height(Analysis analysis, int surfaceId) {
        var surfaces = analysis._prescription._surfaces;
        if (surfaceId < 0 || surfaceId >= surfaces.length - 1)
            throw new IllegalArgumentException(
                    "an edge gap needs a following surface, but surface " + surfaceId
                            + " is the last of " + surfaces.length);
        return 0.5 * Math.min(
                surfaces[surfaceId].get_diameter_by_scenario(analysis._scenario),
                surfaces[surfaceId + 1].get_diameter_by_scenario(analysis._scenario));
    }

    private static double checked_height(double height) {
        if (!Double.isFinite(height) || height <= 0.0)
            throw new IllegalArgumentException(
                    "edge height must be finite and positive, got " + height);
        return height;
    }

    /**
     * Separation between this surface and the next at {@code height}. Returns NaN when
     * either surface is undefined there - a radius driven below the semi-diameter, say -
     * which {@link Constraint#value()} turns into a large finite miss rather than a
     * solve-ending BIGVAL.
     */
    private static double edge_gap(Analysis analysis, int surfaceId, double height) {
        var surfaces = analysis._prescription._surfaces;
        double thickness = surfaces[surfaceId].get_thickness_by_scenario(analysis._scenario);
        try {
            return thickness
                    + profile(surfaces[surfaceId + 1]).sag(0.0, height)
                    - profile(surfaces[surfaceId]).sag(0.0, height);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    /**
     * The same profile the model builder would construct for this surface, so the sag
     * here and the sag the ray tracer sees cannot drift apart.
     */
    private static SurfaceProfile profile(SurfaceType surface) {
        double radius = surface.get_radius_of_curvature();
        if (!surface.is_aspheric())
            return new Spherical(radius == 0.0 ? 0.0 : 1.0 / radius);
        if (surface.is_odd_asphere())
            return new RadialPolynomial().r(radius).cc(surface.get_cc())
                    .coefs(surface.get_aspheric_coeffs());
        return new EvenPolynomial().r(radius).cc(surface.get_cc())
                .coefs(surface.get_aspheric_coeffs());
    }

    @Override
    public String toString() {
        return super.toString() + ", height=" + _height;
    }
}
