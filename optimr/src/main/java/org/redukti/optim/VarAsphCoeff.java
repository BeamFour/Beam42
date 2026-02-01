package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarAsphCoeff extends Var {
    public final int _surface_id;
    public final int _index;
    public final double _scaling_factor;
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index) {
        super(prescription, prescription._surfaces[surfaceId]._coeffs[index],0.0001);
        this._surface_id = surfaceId;
        this._index = index;
        this._scaling_factor = Math.pow(10.0,4+index);
    }
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index,double scalingFactor) {
        super(prescription, prescription._surfaces[surfaceId]._coeffs[index],0.0001);
        this._surface_id = surfaceId;
        this._index = index;
        this._scaling_factor = scalingFactor;
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
            double scaled = _original_value * _scaling_factor;
            double newValue = scaled + delta;
            double unscaled = newValue / _scaling_factor;
            _prescription._surfaces[_surface_id]._coeffs[_index] = unscaled;
        }
        else
            _prescription._surfaces[_surface_id]._coeffs[_index] = _original_value;

    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Asph Coeff [" + _index + "]: " + _prescription._surfaces[_surface_id]._coeffs[_index] + " scaling factor " + _scaling_factor;
    }
}
