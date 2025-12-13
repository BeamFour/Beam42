package org.redukti.optim;

/**
 * Average aberration across wavelengths for a field / xy / pos.
 */
public class GoalRayAberration extends Goal {
    public final int field;
    public final int xy;
    public final int pos;
    public GoalRayAberration(Analysis analysis, int field, int xy, int pos, double target, double weight) {
        super(analysis,target,weight);
        this.field = field;
        this.xy = xy;
        this.pos = pos;
    }

    @Override
    public double value() {
        for (var result: analysis.ray_aberrations.results) {
            if (result.fi == field &&
                result.xy == xy) {
                int count = result.fans.size();
                double v = 0.0;
                for (var fan: result.fans) {
                    v += fan.fan_y.get(pos);
                }
                return v/count;
            }
        }
        throw new IllegalArgumentException("Invalid field, xy or position");
    }

    @Override
    public String toString() {
        return "RayAberration field=" + field + ", xy=" + xy + ", pos=" + pos + ", target=" + target + ", weight=" + weight + " = " + value();
    }
}
