package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public abstract class Out extends Var {
    public final double weight;
    public final double target;
    public Out(Prescription prescription, double target, double weight) {
        super(prescription);
        this.target = target;
        this.weight = weight;
    }
    public abstract double value();
}
