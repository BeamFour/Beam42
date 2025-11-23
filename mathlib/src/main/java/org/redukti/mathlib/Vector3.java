package org.redukti.mathlib;

import java.util.Objects;

public class Vector3 {

    public static Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 vector3_0 = new Vector3(0.0, 0.0, 0.0);
    public static final Vector3 vector3_1 = new Vector3(1.0, 1.0, 1.0);

    public static final Vector3 vector3_001 = new Vector3(0.0, 0.0, 1.0);
    public static final Vector3 vector3_010 = new Vector3(0.0, 1.0, 0.0);
    public static final Vector3 vector3_100 = new Vector3(1.0, 0.0, 0.0);

    public final double x;
    public final double y;
    public final double z;

    public Vector3(double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            throw new IllegalArgumentException("NaN");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector3(double v) {this(v,v,v);}

    public Vector3 deg2rad() {
        return new Vector3(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
    }

    public Vector3 tan() {
        return new Vector3(Math.tan(x), Math.tan(y), Math.tan(z));
    }

    public Vector3 times(double scale) {
        return new Vector3(x * scale, y * scale, z * scale);
    }
    public Vector3 divide(double scale) {
        return new Vector3(x / scale, y / scale, z / scale);
    }

    public Vector3 negate() {
        return new Vector3(-x, -y, -z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 normalize() {
        double lengthsq = x * x + y * y + z * z;
        if (M.isZero(lengthsq)) {
            return ZERO;
        } else {
            //double factor = 1.0 / Math.sqrt(lengthsq);
            //return new Vector3(x * factor, y * factor, z * factor);
            double denom = Math.sqrt(lengthsq);
            return new Vector3(x / denom, y / denom, z / denom);
        }
    }

    public double dot(Vector3 vector) {
        return x * vector.x + y * vector.y + z * vector.z;
    }

    /**
     * The cross product a × b is defined as a vector c that is
     * perpendicular (orthogonal) to both a and b, with a direction given by the right-hand rule
     * and a magnitude equal to the area of the parallelogram that the vectors span.
     * <p>
     * https://en.wikipedia.org/wiki/Cross_product
     */
    public Vector3 cross(Vector3 b) {
        return new Vector3(y() * b.z() - z() * b.y(),
                z() * b.x() - x() * b.z(),
                x() * b.y() - y() * b.x());
    }

    public Vector3 add(Vector3 v) {
        return new Vector3(x + v.x, y + v.y, z + v.z);
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

    public final double x() {
        return this.x;
    }

    public final double y() {
        return this.y;
    }

    public final double z() {
        return this.z;
    }

    public final Vector3 x(double v) {
        return new Vector3(v, y(), z());
    }

    public final Vector3 y(double v) {
        return new Vector3(x(), v, z());
    }

    public final Vector3 z(double v) {
        return new Vector3(x(), y(), v);
    }

    public final Vector3 sin() { return new Vector3(Math.sin(x), Math.sin(y), Math.sin(z)); }

    public Vector2 project_xy() {
        return new Vector2(x(), y());
    }

    public Vector2 project_zy() {
        return new Vector2(z(), y());
    }

    public double v(int i) {
        switch (i) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
            default:
                throw new IllegalArgumentException("Invalid offset " + i);
        }
    }

    public Vector3 v(int i, double v) {
        double x1 = this.x;
        double y1 = this.y;
        double z1 = this.z;
        switch (i) {
            case 0:
                x1 = v;
                break;
            case 1:
                y1 = v;
                break;
            case 2:
                z1 = v;
                break;
            default:
                throw new IllegalArgumentException("Invalid offset " + i);
        }
        return new Vector3(x1, y1, z1);
    }

    public final boolean isEqual(Vector3 other, double tolerance) {
        return Math.abs(this.x() - other.x()) < tolerance &&
                Math.abs(this.y() - other.y()) < tolerance &&
                Math.abs(this.z() - other.z()) < tolerance;
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
        return isEqual(o, 1e-13);
    }
}
