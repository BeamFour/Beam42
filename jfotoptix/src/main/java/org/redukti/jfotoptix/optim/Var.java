package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public abstract class Var {
    public final Prescription prescription;
    public final double originalValue;
    public final double dDelta;
    public Var(Prescription prescription, double originalValue, double dDelta) {
        this.prescription = prescription;
        this.originalValue = originalValue;
        this.dDelta = dDelta;
    }
    public abstract void shift(double delta);
}
