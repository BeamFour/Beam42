package org.redukti.optim;

import org.redukti.rayoptics.util.Lists;

/**
 * MTF proxy for a field / orientation / pos / wvl.
 * Based on Kidger paper.
 */
public class GoalMTFProxy extends Goal {
    public final int _field;
    public final int _orientation;
    public final int _pos;
    public final double _wvl;
    public final double _freq;
    public GoalMTFProxy(Analysis analysis, int field, int orientation, int pos, double wvl, double freq, double target, double weight) {
        super(analysis,target,weight);
        this._field = field-1;
        this._orientation = Orientation.checked(orientation);
        this._pos = pos;
        this._wvl = wvl;
        this._freq = freq;
    }

    @Override
    public double value() {
        var fans = _analysis._ray_aberrations.get_fans(_field, _orientation, _wvl);
        if (fans != null) {
            double aberration = 0;
            try {
                aberration = Lists.get(fans.fan_y, _pos);
            }
            catch (Exception e) {
                return 1.0;
            }
            return Math.sin(Math.PI * _freq * aberration);
        }
        throw new IllegalArgumentException("Invalid field, orientation or position");
    }

    @Override
    public String toString() {
        return "MTFProxy field=" + _field + ", orientation=" + Orientation.name(_orientation) + ", pos=" + _pos + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
