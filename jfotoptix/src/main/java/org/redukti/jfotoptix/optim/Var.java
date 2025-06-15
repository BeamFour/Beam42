package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public abstract class Var extends Param {
    public final double originalValue;
    public Var(Prescription prescription, double originalValue) {
        super(prescription);
        this.originalValue = originalValue;
    }
    public abstract void shift(double delta);
}
