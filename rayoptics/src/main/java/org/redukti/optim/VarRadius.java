// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarRadius extends Var {
    public final int _surface_id;
    public VarRadius(Prescription prescription, int surfaceId) {
        super(prescription);
        this._surface_id = surfaceId;
        // same step as optimr's VarRadius, with a floor for flat/near-flat surfaces
        this._d_delta = Math.max(Math.abs(prescription._surfaces[surfaceId]._radius) * 0.001, 1.0e-3);
    }

    @Override
    public double read_from_prescription() {
        set_unscaled_value(_prescription._surfaces[_surface_id]._radius);
        return get_scaled_value();
    }

    @Override
    public void write_to_prescription() {
        _prescription._surfaces[_surface_id]._radius = get_unscaled_value();
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Radius: " + get_unscaled_value();
    }
}
