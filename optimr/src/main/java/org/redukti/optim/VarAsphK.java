package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarAsphK extends Var {
    public final int _surface_id;
    public VarAsphK(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._k,0.0001);
        this._surface_id = surfaceId;
    }
    @Override
    public void shift(double delta) {
        _prescription._surfaces[_surface_id]._k = _original_value + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Asph k: " + _prescription._surfaces[_surface_id]._k;
    }
}
