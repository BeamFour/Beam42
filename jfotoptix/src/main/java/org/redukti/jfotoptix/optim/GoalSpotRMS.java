package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class GoalSpotRMS extends Goal {
    public final int field;
    public GoalSpotRMS(Analysis analysis, int field, double target, double weight) {
        super(analysis,target,weight);
        this.field = field;
    }

    @Override
    public double value() {
        if (field == 1)
            return analysis.sys1Spot.get_rms_radius();
        else if (field == 2)
            return analysis.sys2Spot.get_rms_radius();
        else if (field == 3)
            return analysis.sys3Spot.get_rms_radius();
        else
            throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public String toString() {
        return "SpotRMS field=" + field + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
