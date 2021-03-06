package org.redukti.jfotoptix.math;

import java.util.Objects;

public class Vector3Pair {

    final Vector3 a;
    final Vector3 b;

    public Vector3Pair(Vector3 a, Vector3 b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        this.a = a;
        this.b = b;
    }
    public final Vector3 a() { return a; }
    public final Vector3 b() { return b; }

    public final boolean isEquals(Vector3Pair other, double tolerance) {
        return a.isEqual(other.a, tolerance) && b.isEqual(other.b, tolerance);
    }
}
