package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAsphCoeff extends Var {
    public final int surfaceId;
    public final int index;
    public final double scale;
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index) {
        super(prescription, prescription.surfaces[surfaceId].coeffs[index],0.0001);
        this.surfaceId = surfaceId;
        this.index = index;
        this.scale = Math.pow(10.0,4+index);
    }
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index,double scale) {
        super(prescription, prescription.surfaces[surfaceId].coeffs[index],0.0001);
        this.surfaceId = surfaceId;
        this.index = index;
        this.scale = scale;
    }
    @Override
    public void shift(double delta) {
        if (delta != 0.0) {
            double scaled = originalValue * scale;
            double newValue = scaled + delta;
            double unscaled = newValue / scale;
            prescription.surfaces[surfaceId].coeffs[index] = unscaled;
        }
        else
            prescription.surfaces[surfaceId].coeffs[index] = originalValue;

    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph Coeff [" + index + "]: " + prescription.surfaces[surfaceId].coeffs[index];
    }
}
