package org.redukti.optim3;

import org.redukti.spec.Prescription;

public abstract class Var {
    public final Prescription prescription;
    public final double originalValue;
    public final double dDelta;
    public Var(Prescription prescription, double originalValue, double dDelta) {
        this.prescription = prescription;
        this.originalValue = originalValue;
        this.dDelta = dDelta;
    }
    public abstract void shift(double delta, boolean apply_scale);
    public double scaling_factor() {
        return 1.;
    }
    public abstract double get_value();
    public abstract void set_value(double value,double delta);
    public final void set_value(double value) {
        set_value(value,0.0);
    }
}
