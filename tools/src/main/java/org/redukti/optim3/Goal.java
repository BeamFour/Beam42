package org.redukti.optim3;

/**
 * Goal represents a target we would like to achieve
 */
public abstract class Goal {
    public final Analysis analysis;
    public final double weight;
    public final double target;
    public Goal(Analysis analysis, double target, double weight) {
        this.analysis = analysis;
        this.target = target;
        this.weight = weight;
    }
    public abstract double value();
}
