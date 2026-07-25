package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarThickness extends Var {
    public final int _surface_id;
    public final int _scenario;

    /**
     * If the thickness varies by scenario then this constructor should be used
     *
     * @param prescription  Lens prescription
     * @param surfaceId The surface index (0-based)
     * @param scenario  The scenario number, default is 0
     */
    public VarThickness(Prescription prescription, int surfaceId, int scenario) {
        super(prescription,
                prescription._surfaces[surfaceId]._thickness_by_scenario != null
                    ? prescription._surfaces[surfaceId]._thickness_by_scenario[scenario]
                    : prescription._surfaces[surfaceId]._thickness,
                0.001);
        this._surface_id = surfaceId;
        this._scenario = scenario;
    }
    public VarThickness(Prescription prescription, int surfaceId) {
        this(prescription,surfaceId,0);
    }
    @Override
    public void shift(double delta) {
        if (_prescription._surfaces[_surface_id]._thickness_by_scenario != null)
            _prescription._surfaces[_surface_id]._thickness_by_scenario[_scenario] = _original_value + delta;
        else
            _prescription._surfaces[_surface_id]._thickness = _original_value + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Thickness: " +
                (_prescription._surfaces[_surface_id]._thickness_by_scenario != null
                    ? _prescription._surfaces[_surface_id]._thickness_by_scenario[_scenario]
                    : _prescription._surfaces[_surface_id]._thickness);
    }
}
