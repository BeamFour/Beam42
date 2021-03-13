package org.redukti.jfotoptix.math;

import static org.redukti.jfotoptix.util.MathUtils.square;

/**
 * Vector with 2 components named x,y.
 */
public class Vector2 {

    public static final Vector2 vector2_0 = new Vector2 (0.0, 0.0);
    public static final Vector2 vector2_1 = new Vector2 (1.0, 1.0);
    public static final  Vector2 vector2_10 = new Vector2 (1.0, 0.0);

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

    public Vector2 plus(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] + v.values[i];
        return new Vector2(r);
    }

    public Vector2 minus(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] - v.values[i];
        return new Vector2(r);
    }

    public Vector2 divide(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] / scale;
        return new Vector2(r);
    }

    public Vector2 times(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] * scale;
        return new Vector2(r);
    }

    /**
     * element by element divide
     */
    public Vector2 ebeDivide(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] / v.values[i];
        return new Vector2(r);
    }

    /** element by element multiply */
    public Vector2 ebeTimes(Vector2 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] * v.values[i];
        return new Vector2(r);
    }

    public Vector2 negate()
    {
        double[] r = new double[N];
        for ( int i = 0; i < N; i++)
            r[i] = -values[i];
        return new Vector2(r);
    }

    public double len ()
    {
        double r = 0;
        for (int i = 0; i < N; i++)
            r += square (values[i]);
        return Math.sqrt (r);
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
