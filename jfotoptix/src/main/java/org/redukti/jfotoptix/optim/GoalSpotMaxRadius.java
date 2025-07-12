package org.redukti.jfotoptix.optim;

public class GoalSpotMaxRadius extends Goal {
    public final int field;
    public GoalSpotMaxRadius(Analysis analysis, int field, double target, double weight) {
        super(analysis,target,weight);
        this.field = field;
    }

    @Override
    public double value() {
        return analysis.spots[field-1].get_max_radius();
    }

    @Override
    public String toString() {
        return "SpotMaxRadius field=" + field + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
