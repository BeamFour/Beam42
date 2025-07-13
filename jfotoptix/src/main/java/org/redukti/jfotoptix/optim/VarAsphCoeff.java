package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class VarAsphCoeff extends Var {
    public final int surfaceId;
    public final int index;
    public final double scalingFactor;
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index) {
        super(prescription, prescription.surfaces[surfaceId].coeffs[index],0.0001);
        this.surfaceId = surfaceId;
        this.index = index;
        this.scalingFactor = Math.pow(10.0,4+index);
    }
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index,double scalingFactor) {
        super(prescription, prescription.surfaces[surfaceId].coeffs[index],0.0001);
        this.surfaceId = surfaceId;
        this.index = index;
        this.scalingFactor = scalingFactor;
    }
    @Override
    public void shift(double delta) {
        if (delta != 0.0) {
            // The aspheric coefficients are very small numbers
            // We want to shift the significant number ignoring the
            // exponential factor - the scaling factor essentially
            // helps us do that. It does mean however that when we
            // optimize the values tend to stay in the exponential range
            // chosen as scaling factor
            // It seems that roughly the quartic term needs a factor of
            // 1E6 and then it goes uo by power of 2.
            double scaled = originalValue * scalingFactor;
            double newValue = scaled + delta;
            double unscaled = newValue / scalingFactor;
            prescription.surfaces[surfaceId].coeffs[index] = unscaled;
        }
        else
            prescription.surfaces[surfaceId].coeffs[index] = originalValue;

    }
    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph Coeff [" + index + "]: " + prescription.surfaces[surfaceId].coeffs[index] + " scaling factor " + scalingFactor;
    }
}
