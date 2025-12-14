package org.redukti.optim;

import org.redukti.rayoptics.util.Lists;

/**
 * Ray aberration for a field / xy / pos / wvl.
 */
public class GoalRayAberration extends Goal {
    public final int field;
    public final int xy;
    public final int pos;
    public final double wvl;
    public GoalRayAberration(Analysis analysis, int field, int xy, int pos, double wvl, double target, double weight) {
        super(analysis,target,weight);
        this.field = field-1;
        this.xy = xy;
        this.pos = pos;
        this.wvl = wvl;
    }

    @Override
    public double value() {
        var fans = analysis.ray_aberrations.get_fans(field,xy,wvl);
        if (fans != null)
            return Lists.get(fans.fan_y,pos);
        throw new IllegalArgumentException("Invalid field, xy or position");
    }

    @Override
    public String toString() {
        return "RayAberration field=" + field + ", xy=" + xy + ", pos=" + pos + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
