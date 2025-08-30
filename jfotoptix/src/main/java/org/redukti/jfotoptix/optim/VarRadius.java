package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarRadius extends Var {
    public final int surfaceId;
    public VarRadius(Prescription prescription, int surfaceId) {
        super(prescription, prescription.surfaces[surfaceId]._radius, prescription.surfaces[surfaceId]._radius *0.001);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta) {
        prescription.surfaces[surfaceId]._radius = originalValue + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Radius: " + prescription.surfaces[surfaceId]._radius;
    }
}
