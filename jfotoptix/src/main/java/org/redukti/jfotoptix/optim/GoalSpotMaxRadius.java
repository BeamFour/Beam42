package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.spec.Prescription;

public class GoalSpotMaxRadius extends Goal {
    public final int field;
    public GoalSpotMaxRadius(Prescription prescription, int field, double target, double weight) {
        super(prescription,target,weight);
        this.field = field;
    }

    @Override
    public double value() {
        if (field == 1)
            return prescription.sys1Spot.get_max_radius();
        else if (field == 2)
            return prescription.sys2Spot.get_max_radius();
        else if (field == 3)
            return prescription.sys3Spot.get_max_radius();
        else
            throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public String toString() {
        return "SpotMaxRadius field=" + field + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
