package org.redukti.optim;

import org.redukti.spec.Prescription;

/**
 * One side of a box around a surface's starting <em>curvature</em>:
 * {@code c >= lower} or {@code c <= upper}.
 *
 * <p>The hard-constraint counterpart of {@link ConstraintCurvature}, and the bound that
 * matters most for keeping a recognisable design. {@link BoundEdgeThickness} stops
 * surfaces passing through one another, but it says nothing about a surface that stays
 * clear of its neighbours while curling up into something no longer resembling the lens
 * it started as - and on a design where curvature is the only thing varied, that is the
 * whole of the freedom.
 *
 * <p>Curvature rather than radius, for the reason {@link ConstraintCurvature} gives:
 * radius runs away towards infinity on a near-flat surface for a negligible optical
 * change, so a fractional box on radius would barely restrain that surface while
 * over-restraining a strongly curved one. Curvature {@code c = 1/r} tracks optical effect.
 *
 * <p>The box is symmetric in the fractional sense, {@code c0 +/- fraction*|c0|}, and uses
 * the magnitude deliberately: a surface may flatten through zero and reverse sign, and
 * taking {@code c0*(1 +/- fraction)} would silently swap which limit is the upper one for
 * a negative starting curvature.
 *
 * <p>Two of these are needed per surface; see {@link #boxAroundCurrent}. Evaluated from
 * the prescription alone, so it costs no ray trace - see {@link Bound}.
 */
public class BoundCurvature extends Bound {

    /** How infeasible an undefined curvature reads: repellent, but finite. */
    private static final double UNDEFINED_VIOLATION = 1.0e3;

    public final Prescription _prescription;
    public final int _surface_id;
    /** The limit on curvature, in reciprocal system units. */
    public final double _limit;
    /** True when {@link #_limit} is a ceiling, false when it is a floor. */
    public final boolean _is_upper;

    public BoundCurvature(Prescription prescription, int surfaceId, double limit, boolean isUpper) {
        if (prescription == null)
            throw new IllegalArgumentException("bound needs a prescription");
        if (surfaceId < 0)
            throw new IllegalArgumentException("surface must be non-negative");
        if (!Double.isFinite(limit))
            throw new IllegalArgumentException("curvature limit must be finite, got " + limit);
        this._prescription = prescription;
        this._surface_id = surfaceId;
        this._limit = limit;
        this._is_upper = isUpper;
    }

    /**
     * The pair of bounds holding a surface within {@code fraction} of its present
     * curvature, returned lower limit first.
     *
     * @throws IllegalArgumentException if the surface is flat, which has no finite
     *         curvature to build a fractional box around. {@code buildVariables} never
     *         creates a {@link VarRadius} for a flat surface, so this cannot arise for a
     *         bound built by the builder.
     */
    public static BoundCurvature[] boxAroundCurrent(Prescription prescription, int surfaceId,
                                                    double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0)
            throw new IllegalArgumentException(
                    "curvature box fraction must be finite and non-negative, got " + fraction);
        double curvature = curvature(prescription, surfaceId);
        if (!Double.isFinite(curvature) || curvature == 0.0)
            throw new IllegalArgumentException(
                    "a fractional curvature box needs a finite non-zero starting curvature,"
                            + " but surface " + surfaceId + " has radius "
                            + prescription._surfaces[surfaceId]._radius);
        double margin = fraction * Math.abs(curvature);
        return new BoundCurvature[]{
                new BoundCurvature(prescription, surfaceId, curvature - margin, false),
                new BoundCurvature(prescription, surfaceId, curvature + margin, true)};
    }

    private static double curvature(Prescription prescription, int surfaceId) {
        double radius = prescription._surfaces[surfaceId]._radius;
        return radius == 0.0 ? Double.POSITIVE_INFINITY : 1.0 / radius;
    }

    @Override
    public double value() {
        // A radius driven through zero sends curvature to infinity. Report that as a
        // large finite violation rather than a non-finite one: the solver rejects
        // non-finite constraints outright, which ends the solve, where what is wanted
        // is for the step to be pushed back.
        double curvature = curvature(_prescription, _surface_id);
        if (!Double.isFinite(curvature))
            return -UNDEFINED_VIOLATION * Math.max(1.0, Math.abs(_limit));
        return _is_upper ? _limit - curvature : curvature - _limit;
    }

    @Override
    public String describe() {
        return "curvature(" + _surface_id + ") " + (_is_upper ? "<= " : ">= ") + _limit;
    }
}
