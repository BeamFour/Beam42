package org.redukti.optim_robeam;

import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;

/**
 * Paraxial goals are helpful in anchoring the system so that
 * the optimixer does not make massive changes to focal length etc.
 */
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
