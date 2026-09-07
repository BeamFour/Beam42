package org.redukti.optim;

import org.redukti.spec.Prescription;

/**
 * Floor on an axial (centre) thickness: {@code t >= minimum}.
 *
 * <p>The hard-constraint counterpart of {@link ConstraintThickness}. That one anchors the
 * thickness to where it started and charges merit for moving in either direction; this one
 * says nothing at all until the thickness reaches the floor, and then does not let it
 * through. A design free to redistribute its spaces, but not to collapse one, wants this.
 */
public class BoundThickness extends Bound {

    public final Prescription _prescription;
    public final int _surface_id;
    /** The floor, in system units. */
    public final double _minimum;
    private final int _scenario;

    public BoundThickness(Prescription prescription, int surfaceId, int scenario, double minimum) {
        if (prescription == null)
            throw new IllegalArgumentException("bound needs a prescription");
        if (surfaceId < 0)
            throw new IllegalArgumentException("surface must be non-negative");
        if (!Double.isFinite(minimum))
            throw new IllegalArgumentException("thickness floor must be finite, got " + minimum);
        this._prescription = prescription;
        this._surface_id = surfaceId;
        this._minimum = minimum;
        this._scenario = scenario;
    }

    /**
     * A floor at {@code fraction} of the present thickness.
     *
     * <p>Fractional rather than absolute for the same reason {@link Constraint} normalizes
     * fractionally: a prescription holds 0.1mm air gaps beside 39mm ones, and one absolute
     * floor cannot be right for both.
     */
    public static BoundThickness fractionOfCurrent(Prescription prescription, int surfaceId,
                                                   int scenario, double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0.0 || fraction >= 1.0)
            throw new IllegalArgumentException(
                    "thickness floor fraction must be in [0, 1), got " + fraction);
        return new BoundThickness(prescription, surfaceId, scenario,
                fraction * thickness(prescription, surfaceId, scenario));
    }

    @Override
    public double value() {
        return thickness(_prescription, _surface_id, _scenario) - _minimum;
    }

    private static double thickness(Prescription prescription, int surfaceId, int scenario) {
        var surface = prescription._surfaces[surfaceId];
        return surface._thickness_by_scenario != null
                ? surface._thickness_by_scenario[scenario]
                : surface._thickness;
    }

    @Override
    public String describe() {
        return "thickness(" + _surface_id + ") >= " + _minimum;
    }
}
