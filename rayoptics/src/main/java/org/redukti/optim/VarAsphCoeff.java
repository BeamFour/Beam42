package org.redukti.optim;

import org.redukti.spec.Prescription;

public class VarAsphCoeff extends Var {
    public final int _surface_id;
    public final int _index;
    public final double _scaling_factor;
    public VarAsphCoeff(Prescription prescription, int surfaceId, int index, double scalingFactor) {
        super(prescription);
        this._surface_id = surfaceId;
        this._index = index;
        this._scaling_factor = scalingFactor != 0.0 ? scalingFactor : 1.0;
    }
    @Override
    public double get_scaling_factor() {
        return _scaling_factor;
    }

    @Override
    public double read_from_prescription() {
        set_unscaled_value(_prescription._surfaces[_surface_id]._coeffs[_index]);
        return get_scaled_value();
    }

    @Override
    public void write_to_prescription() {
        _prescription._surfaces[_surface_id]._coeffs[_index] = get_unscaled_value();
    }
    @Override
    public String toString() {
        return "Surface ID: " + _surface_id + " Asph Coeff [" + _index + "]: " + get_unscaled_value() + " scaling factor " + _scaling_factor;
    }
}
