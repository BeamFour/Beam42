package org.redukti.optim;

import org.redukti.spec.Prescription;

public abstract class Var {
    public final Prescription _prescription;
    public final double _original_value;
    public final double _d_delta;
    public Var(Prescription prescription, double originalValue, double dDelta) {
        this._prescription = prescription;
        this._original_value = originalValue;
        this._d_delta = dDelta;
    }
    public abstract void shift(double delta);
}
