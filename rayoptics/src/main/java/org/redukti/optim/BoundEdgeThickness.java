package org.redukti.optim;

import org.redukti.spec.Prescription;

/**
 * Floor on the <em>edge</em> separation of a gap: {@code t + sag_next(h) - sag_this(h) >= minimum}.
 *
 * <p>The hard-constraint counterpart of {@link ConstraintEdgeThickness}, and the reason
 * bounds were added at all. Two surfaces can hold their axial gap and still pass through
 * one another away from the axis once curvature moves, which is how the Leica 75/2 solve
 * produced overlapping first and second surfaces with thickness constraints in place. The
 * penalty version resists change in the separation symmetrically - it charges as much for
 * opening the edge up as for closing it, because a fractional residual is symmetric - and
 * it can be outvoted by a large enough optical merit block. This one only forbids crossing.
 *
 * <p>The height is fixed at construction, at the smaller of the two bounding
 * semi-diameters by default. It is not re-derived as the design moves, so a surface whose
 * aperture grows during the solve is still checked where it was originally checked.
 *
 * <p>Evaluated from the prescription alone: no ray trace, see {@link Bound}.
 */
public class BoundEdgeThickness extends Bound {

    public final Prescription _prescription;
    public final int _surface_id;
    /** Height at which the separation is measured, in system units. */
    public final double _height;
    /** The floor, in system units. */
    public final double _minimum;
    private final int _scenario;

    public BoundEdgeThickness(Prescription prescription, int surfaceId, int scenario,
                              double height, double minimum) {
        if (prescription == null)
            throw new IllegalArgumentException("bound needs a prescription");
        if (surfaceId < 0)
            throw new IllegalArgumentException("surface must be non-negative");
        if (!Double.isFinite(height) || height <= 0.0)
            throw new IllegalArgumentException("edge height must be finite and positive, got " + height);
        if (!Double.isFinite(minimum))
            throw new IllegalArgumentException("edge floor must be finite, got " + minimum);
        this._prescription = prescription;
        this._surface_id = surfaceId;
        this._height = height;
        this._minimum = minimum;
        this._scenario = scenario;
    }

    /** A floor at {@code fraction} of the present edge separation, measured at the
     * smaller of the two bounding semi-diameters. See
     * {@link BoundThickness#fractionOfCurrent} for why the floor is fractional. */
    public static BoundEdgeThickness fractionOfCurrent(Prescription prescription, int surfaceId,
                                                       int scenario, double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0 || fraction >= 1.0)
            throw new IllegalArgumentException(
                    "edge floor fraction must be in [0, 1), got " + fraction);
        double height = ConstraintEdgeThickness.default_height(prescription, scenario, surfaceId);
        double gap = ConstraintEdgeThickness.edge_gap(prescription, scenario, surfaceId, height);
        return new BoundEdgeThickness(prescription, surfaceId, scenario, height, fraction * gap);
    }

    /**
     * True when the gap after {@code surfaceId} can be bounded: there is a following
     * surface and the present separation is positive and finite. A design that already
     * starts crossed has nothing to hold.
     */
    public static boolean is_boundable(Prescription prescription, int scenario, int surfaceId) {
        if (surfaceId < 0 || surfaceId >= prescription._surfaces.length - 1)
            return false;
        double height = ConstraintEdgeThickness.default_height(prescription, scenario, surfaceId);
        if (!(height > 0.0)) return false;
        double gap = ConstraintEdgeThickness.edge_gap(prescription, scenario, surfaceId, height);
        return Double.isFinite(gap) && gap > 0.0;
    }

    @Override
    public double value() {
        // A radius driven below the semi-diameter leaves the surface undefined at this
        // height and edge_gap returns NaN. Report it as a large violation rather than a
        // non-finite value: the solver rejects non-finite constraints outright, which
        // would end the solve, where what is wanted is for the step to be pushed back.
        double gap = ConstraintEdgeThickness.edge_gap(_prescription, _scenario, _surface_id, _height);
        if (!Double.isFinite(gap)) return -UNDEFINED_VIOLATION * Math.max(1.0, Math.abs(_minimum));
        return gap - _minimum;
    }

    /** How infeasible an undefined surface reads: repellent, but finite and differentiable-ish. */
    private static final double UNDEFINED_VIOLATION = 1.0e3;

    @Override
    public String describe() {
        return "edge(" + _surface_id + " at h=" + _height + ") >= " + _minimum;
    }
}
