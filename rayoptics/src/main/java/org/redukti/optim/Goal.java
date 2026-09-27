// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
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
