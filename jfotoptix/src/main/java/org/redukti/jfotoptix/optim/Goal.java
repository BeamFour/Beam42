package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public abstract class Goal extends Param {
    public final double weight;
    public final double target;
    public Goal(Prescription prescription, double target, double weight) {
        super(prescription);
        this.target = target;
        this.weight = weight;
    }
    public abstract double value();
}
