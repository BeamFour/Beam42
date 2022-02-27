package org.redukti.mathlib;

public class Vector2 {

    public static final Vector2 ZERO = new Vector2(0.0, 0.0);

    public final double x;
    public final double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 v) {
        return new Vector2(x + v.x, y + v.y);
    }

    public Vector2 subtract(Vector2 v) {
        return new Vector2(x - v.x, y - v.y);
    }

    public Vector2 div(double scalar) {
        return new Vector2(x / scalar, y / scalar);
    }

    public double[] as_array() {
        return new double[]{x, y};
    }
}
