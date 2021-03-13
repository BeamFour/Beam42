package org.redukti.jfotoptix.math;

import java.util.Objects;

public class Vector3Pair {

    final Vector3 v0;
    final Vector3 v1;

    public Vector3Pair(Vector3 v0, Vector3 b) {
        Objects.requireNonNull(v0);
        Objects.requireNonNull(b);
        this.v0 = v0;
        this.v1 = b;
    }

    public final boolean isEquals(Vector3Pair other, double tolerance) {
        return v0.isEqual(other.v0, tolerance) && v1.isEqual(other.v1, tolerance);
    }
}
