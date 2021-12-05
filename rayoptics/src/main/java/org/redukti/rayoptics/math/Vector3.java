package org.redukti.rayoptics.math;

public class Vector3 {

    public final double x;
    public final double y;
    public final double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 deg2rad() {
        return new Vector3(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
    }

    public Vector3 tan() {
        return new Vector3(Math.tan(x), Math.tan(y), Math.tan(z));
    }

    public Vector3 times(double scale) {
        return new Vector3(x*scale, y*scale, z*scale);
    }

    public Vector3 negate() {
        return new Vector3(-x, -y, -z);
    }
}
