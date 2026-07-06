package org.redukti.optim;

import org.redukti.mathlib.M;
import org.redukti.spec.Prescription;

public abstract class Var {
    public final Prescription _prescription;
    public double _unscaled_value;
    public double _scaled_value;
    /**
     * Finite-difference step in scaled units, used when building the Jacobian.
     * Must be a fixed absolute step: a step proportional to the current value
     * degenerates to zero for parameters that start at (or cross) zero,
     * e.g. aspheric coefficients, producing NaN Jacobian columns.
     */
    public double _d_delta = 1.0e-4;
    public Var(Prescription prescription) {
        this._prescription = prescription;
    }
    public void set_unscaled_value(double d) {
        if (Double.isNaN(d))
            throw new IllegalArgumentException("NaN value supplied");
        _unscaled_value = d;
        _scaled_value = d * get_scaling_factor();
    }
    public void set_scaled_value(double d) {
        if (Double.isNaN(d))
            throw new IllegalArgumentException("NaN value supplied");
        _scaled_value = d;
        _unscaled_value = M.isZero(d) ? 0.0 : (d / get_scaling_factor());
    }
    public double get_unscaled_value() { return _unscaled_value; }
    public double get_scaled_value() { return _scaled_value; }
    public double get_scaling_factor() { return 1.0; }

    /**
     * Reads unscaled value from the prescription to this var
     * @return Scaled value of the var
     */
    public abstract double read_from_prescription();

    /**
     * Writes unscaled value back to the prescription.
     */
    public abstract void write_to_prescription();
}
