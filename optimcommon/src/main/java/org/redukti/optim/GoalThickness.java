package org.redukti.optim;

/**
 * Holds an air space or element thickness near its starting value.
 *
 * <p>This is what stops the solver collapsing a gap or driving elements through one
 * another. Note it constrains the axial (centre) thickness only, so it does not by itself
 * guarantee positive <em>edge</em> separation, which also depends on the sag of the two
 * bounding surfaces. Keeping the layout recognisable is what makes it effective in
 * practice rather than any guarantee.
 */
public class GoalThickness extends GoalParameter {

    public GoalThickness(Analysis analysis, int surfaceId, double weight) {
        super(analysis, surfaceId, thickness(analysis, surfaceId), weight);
    }

    private static double thickness(Analysis analysis, int surfaceId) {
        var surface = analysis._prescription._surfaces[surfaceId];
        return surface._thickness_by_scenario != null
                ? surface._thickness_by_scenario[analysis._scenario]
                : surface._thickness;
    }

    @Override
    protected double fractional_deviation() {
        return thickness(_analysis, _surface_id) / _base - 1.0;
    }
}
