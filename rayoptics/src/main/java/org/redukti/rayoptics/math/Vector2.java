package org.redukti.rayoptics.math;

public class Vector2 {
    public final double x;
    public final double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 add(Vector2 v) {
        return new Vector2(x+v.x, y+v.y);
    }

    public Vector2 subtract(Vector2 v) {
        return new Vector2(x-v.x, y-v.y);
    }

}