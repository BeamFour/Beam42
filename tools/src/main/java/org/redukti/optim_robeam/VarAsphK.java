package org.redukti.optim_robeam;

import org.redukti.spec.Prescription;

public class VarAsphK extends Var {
    public final int surfaceId;
    public VarAsphK(Prescription prescription, int surfaceId) {
        super(prescription, prescription._surfaces[surfaceId]._k,0.0001);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta, boolean apply_scale) {
        prescription._surfaces[surfaceId]._k = originalValue + delta;
    }

    @Override
    public double get_value() {
        return prescription._surfaces[surfaceId]._k;
    }

    @Override
    public void set_value(double value,double delta) {
        prescription._surfaces[surfaceId]._k = value + delta;
    }

    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph k: " + prescription._surfaces[surfaceId]._k;
    }
}
