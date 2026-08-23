package org.redukti.optim;

public class GoalSpotMaxRadius extends Goal {
    public final int _field;
    public GoalSpotMaxRadius(Analysis analysis, int field, double target, double weight) {
        super(analysis,target,weight);
        this._field = field;
    }

    @Override
    public double value() {
        return _analysis._spots[_field -1].get_max_radius();
    }

    @Override
    public String toString() {
        return "SpotMaxRadius field=" + _field + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
