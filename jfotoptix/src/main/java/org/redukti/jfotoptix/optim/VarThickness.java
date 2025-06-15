package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarThickness extends Var {
    public final int surfaceId;
    public VarThickness(Prescription prescription, int surfaceId) {
        super(prescription, prescription.surfaces[surfaceId].thickness);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta) {
        prescription.surfaces[surfaceId].thickness = originalValue + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Thickness: " + prescription.surfaces[surfaceId].thickness;
    }
}
