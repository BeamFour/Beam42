package org.redukti.jfotopix.math;

public class Vector3Pair {

    final Vector3 a;
    final Vector3 b;

    public Vector3Pair(Vector3 translation, Vector3 direction) {
        this.a = translation;
        this.b = direction;
    }
    public final Vector3 translation() { return a; }
    public final Vector3 direction() { return b; }
}
