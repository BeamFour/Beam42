package org.redukti.jfotoptix.math;

/**
 * Vector with 2 components named x,y.
 */
public class Vector2 {

    private static final int N = 2;

    final double[] values;

    public Vector2(double x, double y) {
        this.values = new double[N];
        this.values[0] = x;
        this.values[1] = y;
    }

    private Vector2(double[] values) {
        this.values = values;
    }

    public final double x() {
        return this.values[0];
    }

    public final double y() {
        return this.values[1];
    }

    public double get(int i) {return this.values[i];}

    @Override
    public String toString() {
        return "[" + x() + ',' + y() + ']';
    }

    public final boolean isEqual(Vector2 other, double tolerance) {
        return Math.abs(this.x() - other.x()) < tolerance &&
                Math.abs(this.y() - other.y()) < tolerance;
    }
}
