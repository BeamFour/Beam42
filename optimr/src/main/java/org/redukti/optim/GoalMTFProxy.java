package org.redukti.optim;

import org.redukti.rayoptics.util.Lists;

/**
 * MTF proxy for a field / xy / pos / wvl.
 * Based on Kidger paper.
 */
public class GoalMTFProxy extends Goal {
    public final int field;
    public final int xy;
    public final int pos;
    public final double wvl;
    public final double freq;
    public GoalMTFProxy(Analysis analysis, int field, int xy, int pos, double wvl, double freq, double target, double weight) {
        super(analysis,target,weight);
        this.field = field-1;
        this.xy = xy;
        this.pos = pos;
        this.wvl = wvl;
        this.freq = freq;
    }

    @Override
    public double value() {
        var fans = analysis.ray_aberrations.get_fans(field,xy,wvl);
        if (fans != null) {
            double aberration = 0;
            try {
                aberration = Lists.get(fans.fan_y, pos);
            }
            catch (Exception e) {
                return 1.0;
            }
            return Math.sin(Math.PI * freq * aberration);
        }
        throw new IllegalArgumentException("Invalid field, xy or position");
    }

    @Override
    public String toString() {
        return "RayAberration field=" + field + ", xy=" + xy + ", pos=" + pos + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
