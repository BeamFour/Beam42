package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarThickness extends Var {
    public final int _surface_id;
    public VarThickness(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._thickness,0.001);
        this._surface_id = surfaceId;
    }
    @Override
    public void shift(double delta) {
        _prescription._surfaces[_surface_id]._thickness = _original_value + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Thickness: " + _prescription._surfaces[_surface_id]._thickness;
    }
}
