package org.redukti.jfotoptix.math;

import java.util.Objects;

public class Vector2Pair {

    public final Vector2 v0;
    public final Vector2 v1;

    public Vector2Pair(Vector2 v0, Vector2 b) {
        Objects.requireNonNull(v0);
        Objects.requireNonNull(b);
        this.v0 = v0;
        this.v1 = b;
    }

    public final boolean isEquals(Vector2Pair other, double tolerance) {
        return v0.isEqual(other.v0, tolerance) && v1.isEqual(other.v1, tolerance);
    }
}
