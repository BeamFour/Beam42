package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarRadius extends Var {
    public final int surfaceId;
    public VarRadius(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._radius, prescription._surfaces[surfaceId]._radius *0.001);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta, boolean apply_scale) {
        prescription._surfaces[surfaceId]._radius = originalValue + delta;
    }

    @Override
    public double get_value() {
        return prescription._surfaces[surfaceId]._radius;
    }

    @Override
    public void set_value(double value) {
        prescription._surfaces[surfaceId]._radius = value;
    }

    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Radius: " + prescription._surfaces[surfaceId]._radius;
    }
}
