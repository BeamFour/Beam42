package org.redukti.optim;

/**
 * Goal represents a target we would like to achieve
 */
public abstract class Goal {
    public final Analysis _analysis;
    public final double _weight;
    public final double _target;
    public Goal(Analysis analysis, double target, double weight) {
        this._analysis = analysis;
        this._target = target;
        this._weight = weight;
    }
    public abstract double value();
}
