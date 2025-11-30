package org.redukti.optim2;

import org.redukti.spec.Prescription;

public class VarThickness extends Var {
    public final int surfaceId;
    public VarThickness(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._thickness,0.001);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta, boolean apply_scale) {
        prescription._surfaces[surfaceId]._thickness = originalValue + delta;
    }

    @Override
    public double get_value() {
        return prescription._surfaces[surfaceId]._thickness;
    }

    @Override
    public void set_value(double value, double delta) {
        prescription._surfaces[surfaceId]._thickness = value+delta;
    }

    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Thickness: " + prescription._surfaces[surfaceId]._thickness;
    }
}
