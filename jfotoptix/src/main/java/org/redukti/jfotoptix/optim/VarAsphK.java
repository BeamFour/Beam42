package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAsphK extends Var {
    public final int surfaceId;
    public VarAsphK(Prescription prescription, int surfaceId) {
        super(prescription, prescription.surfaces[surfaceId].k);
        this.surfaceId = surfaceId;
    }
    @Override
    public void shift(double delta) {
        prescription.surfaces[surfaceId].k = originalValue + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph k: " + prescription.surfaces[surfaceId].k;
    }
}
