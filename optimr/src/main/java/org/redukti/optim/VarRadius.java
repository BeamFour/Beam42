package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarRadius extends Var {
    public final int _surface_id;
    public VarRadius(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._radius, prescription._surfaces[surfaceId]._radius *0.001);
        this._surface_id = surfaceId;
    }
    @Override
    public void shift(double delta) {
        _prescription._surfaces[_surface_id]._radius = _original_value + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Radius: " + _prescription._surfaces[_surface_id]._radius;
    }
}
