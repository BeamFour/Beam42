package org.redukti.mathlib;

import java.util.Objects;

public class Vector3 {

    public static Vector3 ZERO = new Vector3(0, 0, 0);

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
        return new Vector3(x * scale, y * scale, z * scale);
    }

    public Vector3 negate() {
        return new Vector3(-x, -y, -z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3 normalize() {
        double lengthsq = x * x + y * y + z * z;
        if (M.isZero(lengthsq)) {
            return ZERO;
        } else {
            double factor = 1.0/Math.sqrt(lengthsq);
            return new Vector3(x * factor, y * factor, z * factor);
        }
    }

    public double dot(Vector3 vector) {
        return x * vector.x + y * vector.y + z * vector.z;
    }

    public Vector3 add(Vector3 v) {
        return new Vector3(x+v.x, y+v.y, z+v.z);
    }

    public boolean isZero() {
        return M.isZero(x * x + y * y + z * z);
    }

    public boolean any() {
        return !isZero();
    }

    public Vector3 plus(Vector3 v) {
        return new Vector3(x+v.x, y+v.y, z+v.z);
    }

    public Vector3 minus(Vector3 v) {
        return new Vector3(x - v.x, y - v.y, z - v.z);
    }

    public String toString() {
        return "[" + x + "," + y + "," + z + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3 vector3 = (Vector3) o;
        return Double.compare(vector3.x, x) == 0 && Double.compare(vector3.y, y) == 0 && Double.compare(vector3.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public boolean effectivelyEqual(Vector3 o) {
        return Math.abs(x - o.x) < 1e-13
                && Math.abs(y - o.y) < 1e-13
                && Math.abs(z - o.z) < 1e-13;
    }
}
