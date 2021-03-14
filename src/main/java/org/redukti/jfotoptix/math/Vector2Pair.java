package org.redukti.jfotoptix.math;

import java.util.Objects;

public class Vector2Pair {

    public final Vector2 v0;
    public final Vector2 v1;

    public final static Vector2Pair vector2_pair_00 = new Vector2Pair(Vector2.vector2_0, Vector2.vector2_0);

    public Vector2Pair(Vector2 v0, Vector2 b) {
        Objects.requireNonNull(v0);
        Objects.requireNonNull(b);
        this.v0 = v0;
        this.v1 = b;
    }

    public final boolean isEquals(Vector2Pair other, double tolerance) {
        return v0.isEqual(other.v0, tolerance) && v1.isEqual(other.v1, tolerance);
    }

    public double ln_intersect_ln_scale (Vector2Pair line)
    {
        // based on
        // http://geometryalgorithms.com/Archive/algorithm_0104/algorithm_0104B.htm

        Vector2 w = v0.minus(line.v0);

        double d = v1.x () * line.v1.y () - v1.y () * line.v1.x ();

        if (Math.abs (d) < 1e-10)
            throw new IllegalArgumentException ("ln_intersect_ln_scale: lines are parallel");

        double s = (line.v1.x () * w.y () - line.v1.y () * w.x ()) / d;

        return s;
    }

    public Vector2 ln_intersect_ln (Vector2Pair line)
    {
        return v0.plus(v1.times(ln_intersect_ln_scale (line)));
    }
}
