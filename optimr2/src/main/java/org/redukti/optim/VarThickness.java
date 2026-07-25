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
        super(prescription);
        this._surface_id = surfaceId;
        this._scenario = scenario;
        this._d_delta = 1.0e-3; // matches optimr's VarThickness
    }
    public VarThickness(Prescription prescription, int surfaceId) {
        this(prescription,surfaceId,0);
    }
    @Override
    public double read_from_prescription() {
        if (_prescription._surfaces[_surface_id]._thickness_by_scenario != null)
            set_unscaled_value(_prescription._surfaces[_surface_id]._thickness_by_scenario[_scenario]);
        else
            set_unscaled_value(_prescription._surfaces[_surface_id]._thickness);
        return get_scaled_value();
    }

    @Override
    public void write_to_prescription() {
        if (_prescription._surfaces[_surface_id]._thickness_by_scenario != null)
            _prescription._surfaces[_surface_id]._thickness_by_scenario[_scenario] = get_unscaled_value();
        else
            _prescription._surfaces[_surface_id]._thickness = get_unscaled_value();
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Thickness: " + get_unscaled_value();
    }
}
