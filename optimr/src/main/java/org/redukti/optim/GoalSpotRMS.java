package org.redukti.optim;

public class GoalSpotRMS extends Goal {
    public final int field;
    public GoalSpotRMS(Analysis analysis, int field, double target, double weight) {
        super(analysis,target,weight);
        this.field = field;
    }

    @Override
    public double value() {
        return analysis.spots[field-1].get_mean_radius();
    }

    @Override
    public String toString() {
        return "SpotRMS field=" + field + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
