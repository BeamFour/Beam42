// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

import org.redukti.rayoptics.util.Orientation;
import org.redukti.rayoptics.util.Lists;

/**
 * Ray aberration for a field / orientation / pos / wvl.
 */
public class GoalRayAberration extends Goal {
    public final int _field;
    public final int _orientation;
    public final int _pos;
    public final double _wvl;
    public GoalRayAberration(Analysis analysis, int field, int orientation, int pos, double wvl, double target, double weight) {
        super(analysis,target,weight);
        this._field = field-1;
        this._orientation = Orientation.checked(orientation);
        this._pos = pos;
        this._wvl = wvl;
        if (pos < 0)
            pos += Analysis.NUM_TRANSVERSE_RAYS;
        if (pos < 0 || pos >= Analysis.NUM_TRANSVERSE_RAYS)
            throw new IllegalArgumentException("position out of range, max number of rays is " + Analysis.NUM_TRANSVERSE_RAYS);
    }

    @Override
    public double value() {
        var fans = _analysis._ray_aberrations.get_fans(_field, _orientation, _wvl);
        if (fans != null && _pos < fans.fan_x.size()) {
            var result = Lists.get(fans.fan_y, _pos);
            return result != null && Double.isFinite(result)
                    ? result
                    : LMDerMeritFunction.BIGVAL;
        }
        return LMDerMeritFunction.BIGVAL;
    }

    @Override
    public String toString() {
        return "RayAberration field=" + _field + ", orientation=" + Orientation.name(_orientation) + ", pos=" + _pos + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
