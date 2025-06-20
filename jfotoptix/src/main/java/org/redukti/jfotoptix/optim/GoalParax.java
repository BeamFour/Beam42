package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;

public class GoalParax extends Goal {
    public final int paraxId;
    public GoalParax(Prescription prescription, int paraxId, double target, double weight) {
        super(prescription,target,weight);
        this.paraxId = paraxId;
    }
    @Override
    public double value() {
        return prescription.pfo[paraxId];
    }
    @Override
    public String toString() {
        return ParaxialFirstOrderInfo.Names[paraxId] + " = " + value();
    }
}
