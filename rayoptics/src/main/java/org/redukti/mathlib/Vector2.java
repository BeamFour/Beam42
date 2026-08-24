package org.redukti.mathlib;

public class Vector2 {

    public static final Vector2 vector2_0 = new Vector2(0.0, 0.0);
    public static final Vector2 vector2_1 = new Vector2(1.0, 1.0);
    public static final Vector2 vector2_10 = new Vector2(1.0, 0.0);
    public static final Vector2 vector2_01 = new Vector2(0.0, 1.0);

    public final double x;
    public final double y;

    public Vector2(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new IllegalArgumentException("NaN");
        }
        this.x = x;
        this.y = y;
    }

    public Vector2(double v) {
        this(v, v);
    }

    public Vector2 plus(Vector2 v) {
        return new Vector2(x + v.x, y + v.y);
    }

    public Vector2 minus(Vector2 v) {
        return new Vector2(x - v.x, y - v.y);
    }

    public Vector2 divide(double scalar) {
        return new Vector2(x / scalar, y / scalar);
    }

    public Vector2 times(double scalar) {
        return new Vector2(x * scalar, y * scalar);
    }

    public double[] as_array() {
        return new double[]{x, y};
    }

    public final double x() {
        return this.x;
    }

    public final double y() {
        return this.y;
    }

    public final Vector2 x(double value) {
        return new Vector2(value, y());
    }

    public final Vector2 y(double value) {
        return new Vector2(x(), value);
    }

    public double v(int i) {
        switch (i) {
            case 0:
                return x;
            case 1:
                return y;
            default:
                throw new IllegalArgumentException("Invalid offset " + i);
        }
    }

    public Vector2 set(int i, double v) {
        double x1 = this.x;
        double y1 = this.y;
        switch (i) {
            case 0:
                x1 = v;
                break;
            case 1:
                y1 = v;
                break;
            default:
                throw new IllegalArgumentException("Invalid offset " + i);
        }
        return new Vector2(x1, y1);
    }

    /**
     * element by element divide
     */
    public Vector2 ebeDivide(Vector2 v) {
        return new Vector2(x / v.x, y / v.y);
    }

    /**
     * element by element multiply
     */
    public Vector2 ebeTimes(Vector2 v) {
        return new Vector2(x * v.x, y * v.y);
    }

    public Vector2 negate() {
        return new Vector2(-x, -y);
    }

    public double len() {
        double r = x * x + y * y;
        return Math.sqrt(r);
    }

    public static Vector2 from(Vector3 v3, int a, int b) {
        double x = v3.v(a);
        double y = v3.v(b);
        return new Vector2(x, y);
    }

    public Vector2 normalize() {
        double lengthsq = x * x + y * y;
        if (M.isZero(lengthsq)) {
            return this;
        } else {
            double denom = Math.sqrt(lengthsq);
            return new Vector2(x / denom, y / denom);
        }
    }

    @Override
    public String toString() {
        return "[" + x() + ',' + y() + ']';
    }

    public final boolean isEqual(Vector2 other, double tolerance) {
        return Math.abs(this.x() - other.x()) < tolerance &&
                Math.abs(this.y() - other.y()) < tolerance;
    }
}
