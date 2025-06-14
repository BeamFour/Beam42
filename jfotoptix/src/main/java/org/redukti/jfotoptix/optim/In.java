package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public abstract class In extends Var {
    public final double originalValue;
    public In(Prescription prescription, double originalValue) {
        super(prescription);
        this.originalValue = originalValue;
    }
    public abstract void shift(double delta);
}
