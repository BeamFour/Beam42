package org.redukti.jfotoptix.math;

import static org.redukti.jfotoptix.util.MathUtils.square;

/**
 * Vector with 3 components named x,y,z.
 * Note that in the optical system the lens axis is z.
 */
public class Vector3 {

    private static final int N = 3;

    public static final Vector3 vector3_0 = new Vector3(0.0, 0.0, 0.0);
    public static final Vector3 vector3_1 = new Vector3(1.0, 1.0, 1.0);

    public static final Vector3 vector3_001 = new Vector3(0.0, 0.0, 1.0);
    public static final Vector3 vector3_010 = new Vector3(0.0, 1.0, 0.0);
    public static final Vector3 vector3_100 = new Vector3(1.0, 0.0, 0.0);

    final double[] values;

    public Vector3(double x, double y, double z) {
        this.values = new double[N];
        this.values[0] = x;
        this.values[1] = y;
        this.values[2] = z;
    }
    public Vector3(double v) {
        this(v, v, v);
    }

    private Vector3(double[] values) {
        this.values = values;
    }

    public final double x() {
        return this.values[0];
    }

    public final double y() {
        return this.values[1];
    }

    public final double z() {
        return this.values[2];
    }

//    public Vector3 mul (Vector3 v)
//    {
//        double vec[] = new double[N];
//        for (int i = 0; i < N; i++)
//            vec[i] = this.values[i] * v.values[i];
//
//        return new Vector3(vec);
//    }

    public double dotProduct(Vector3 v)
    {
        double r = 0;
        for (int i = 0; i < N; i++)
            r += values[i] * v.values[i];
        return r;
    }

    /**
     * The cross product a × b is defined as a vector c that is
     * perpendicular (orthogonal) to both a and b, with a direction given by the right-hand rule
     * and a magnitude equal to the area of the parallelogram that the vectors span.
     *
     * https://en.wikipedia.org/wiki/Cross_product
     */
    public Vector3 crossProduct(Vector3 b) {
        return new Vector3(y() * b.z() - z() * b.y(),
                z() * b.x() - x() * b.z(),
                x() * b.y() - y() * b.x());
    }

    public Vector3 plus(Vector3 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] + v.values[i];
        return new Vector3(r);
    }

    public Vector3 negate()
    {
        double[] r = new double[N];
        for ( int i = 0; i < N; i++)
            r[i] = -values[i];
        return new Vector3(r);
    }

    public Vector2 project_xy() {
        return new Vector2(x (), y());
    }

    double len ()
    {
        double r = 0;
        for (int i = 0; i < N; i++)
            r += square (values[i]);
        return Math.sqrt (r);
    }

    public Vector3 times(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] * scale;
        return new Vector3(r);
    }

    public Vector3 divide(double scale) {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] / scale;
        return new Vector3(r);
    }

    public Vector3 normalize() {
        return this.divide(len());
    }

    public double v(int i) {
        return this.values[i];
    }
    public Vector3 v(int i, double d) {
        double[] val = this.values.clone();
        val[i] = d;
        return new Vector3(val);
    }

    @Override
    public String toString() {
        return "[" + x() + ',' + y() + ',' + z() + ']';
    }

    public final boolean isEqual(Vector3 other, double tolerance) {
        return Math.abs(this.x() - other.x()) < tolerance &&
                Math.abs(this.y() - other.y()) < tolerance &&
                Math.abs(this.z() - other.z()) < tolerance;
    }
}
