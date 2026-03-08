package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.rayoptics.util.Lists;

/**
 * Ray aberration for a field / xy / pos / wvl.
 */
public class GoalRayAberration extends Goal {
    public final int _field;
    public final int _xy;
    public final int _pos;
    public final double _wvl;
    public GoalRayAberration(Analysis analysis, int field, int xy, int pos, double wvl, double target, double weight) {
        super(analysis,target,weight);
        this._field = field-1;
        this._xy = xy;
        this._pos = pos;
        this._wvl = wvl;
    }

    @Override
    public double value() {
        var fans = _analysis._ray_aberrations.get_fans(_field, _xy, _wvl);
        if (fans != null && _pos < fans.fan_x.size())
            return Lists.get(fans.fan_y, _pos);
        return LMLSolver.BIGVAL;
    }

    @Override
    public String toString() {
        return "RayAberration field=" + _field + ", xy=" + _xy + ", pos=" + _pos + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
