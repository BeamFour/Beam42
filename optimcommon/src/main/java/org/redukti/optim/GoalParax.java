package org.redukti.optim;

/**
 * Paraxial goals are helpful in anchoring the system so that
 * the optimixer does not make massive changes to focal length etc.
 */
public class GoalParax extends Goal {
    public final int _parax_id;
    public GoalParax(Analysis analysis, int paraxId, double target, double weight) {
        super(analysis,target,weight);
        this._parax_id = paraxId;
    }
    @Override
    public double value() {
        return _analysis._pfo[_parax_id];
    }
    @Override
    public String toString() {
        return ParaxHelper.Names[_parax_id] + " = " + value();
    }
}
