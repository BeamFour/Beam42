package org.redukti.jfotoptix.math;

public class Vector3Position extends Vector3Pair {

    public static final Vector3Position position_000_001 = new Vector3Position(Vector3.vector3_0, Vector3.vector3_001);

    public Vector3Position(Vector3 translation, Vector3 direction) {
        super(translation, direction);
    }

    public final Vector3 translation() {
        return super.v0;
    }

    public final Vector3 direction() {
        return super.v1;
    }

    @Override
    public String toString() {
        return "{" +
                "translation=" + v0 +
                ", direction=" + v1 +
                '}';
    }
}
