package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;

public class GoalParax extends Goal {
    public final int paraxId;
    public GoalParax(Analysis analysis, int paraxId, double target, double weight) {
        super(analysis,target,weight);
        this.paraxId = paraxId;
    }
    @Override
    public double value() {
        return analysis.pfo[paraxId];
    }
    @Override
    public String toString() {
        return ParaxialFirstOrderInfo.Names[paraxId] + " = " + value();
    }
}
