package org.redukti.optim2;

import org.redukti.spec.Prescription;

import static org.redukti.jm.minpack.MinPack.dpmpar;

public class VarAsphCoeff extends Var {
    public final int surfaceId;
    public final int index;
    public final double scalingFactor;

    private static double eps = Math.sqrt(dpmpar(1));
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index,double scalingFactor) {
        super(prescription, prescription._surfaces[surfaceId]._coeffs[index],0.0001);
        this.surfaceId = surfaceId;
        this.index = index;
        this.scalingFactor = scalingFactor;
    }
    @Override
    public void shift(double delta, boolean apply_scale) {
        if (delta != 0.0) {
            // The aspheric coefficients are very small numbers
            // We want to shift the significant number ignoring the
            // exponential factor - the scaling factor essentially
            // helps us do that. It does mean however that when we
            // optimize the values tend to stay in the exponential range
            // chosen as scaling factor
            // It seems that roughly the quartic term needs a factor of
            // 1E6 and then it goes uo by power of 2
            if (apply_scale) {
                double scaled = originalValue * scalingFactor;
                double newValue = scaled + delta;
                double unscaled = newValue / scalingFactor;
                prescription._surfaces[surfaceId]._coeffs[index] = unscaled;
            }
            else
                prescription._surfaces[surfaceId]._coeffs[index] = originalValue + delta;
        }
        else
            prescription._surfaces[surfaceId]._coeffs[index] = originalValue;
    }

    @Override
    public double scaling_factor() {
        return scalingFactor;
    }

    @Override
    public double get_value() {
        return prescription._surfaces[surfaceId]._coeffs[index];
    }

    @Override
    public void set_value(double value, double delta) {
        double unscaled = value;
        if (value == 0.0) {
            double scaled = value * scalingFactor;
            double newValue = scaled + delta;
            unscaled = newValue / scalingFactor;
        }
        else if (delta != 0.0) {
            delta = Math.abs(value)*eps * Math.signum(value);
            unscaled = value + delta;
        }
//        if (delta != 0.0) {
//            double scaled = value * scalingFactor;
//            double newValue = scaled + delta;
//            unscaled = newValue / scalingFactor;
//        }
        prescription._surfaces[surfaceId]._coeffs[index] = unscaled;
    }

    @Override
    public String toString() {
        return "Surface ID: " + surfaceId + " Asph Coeff [" + index + "]: " + prescription._surfaces[surfaceId]._coeffs[index] + " scaling factor " + scalingFactor;
    }
}
