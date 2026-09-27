// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

public class GoalSpotRMS extends Goal {
    public final int _field;
    public GoalSpotRMS(Analysis analysis, int field, double target, double weight) {
        super(analysis,target,weight);
        this._field = field;
    }

    @Override
    public double value() {
        return _analysis._spots[_field -1].get_mean_radius();
    }

    @Override
    public String toString() {
        return "SpotRMS field=" + _field + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
