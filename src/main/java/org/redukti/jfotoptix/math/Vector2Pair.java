package org.redukti.jfotoptix.math;

import java.util.Objects;

public class Vector2Pair {

    final Vector2 a;
    final Vector2 b;

    public Vector2Pair(Vector2 a, Vector2 b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        this.a = a;
        this.b = b;
    }
    public final Vector2 a() { return a; }
    public final Vector2 b() { return b; }

    public final boolean isEquals(Vector2Pair other, double tolerance) {
        return a.isEqual(other.a, tolerance) && b.isEqual(other.b, tolerance);
    }
}
