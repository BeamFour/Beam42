package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAsphCoeff extends Var {
    public final int surfaceId;
    public final int index;
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index) {
        super(prescription, prescription.surfaces[surfaceId].coeffs[index]);
        this.surfaceId = surfaceId;
        this.index = index;
    }
    @Override
    public void shift(double delta) {
        prescription.surfaces[surfaceId].coeffs[index] = originalValue + delta;
    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph Coeff [" + index + "]: " + prescription.surfaces[surfaceId].coeffs[index];
    }
}
