package org.redukti.jfotoptix.math;

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

    public double get(int i) {return this.values[i];}

//    public Vector3 mul (Vector3 v)
//    {
//        double vec[] = new double[N];
//        for (int i = 0; i < N; i++)
//            vec[i] = this.values[i] * v.values[i];
//
//        return new Vector3(vec);
//    }

    public double dot (Vector3 v)
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

    public Vector3 add (Vector3 v)
    {
        double[] r = new double[N];
        for (int i = 0; i < N; i++)
            r[i] = values[i] + v.values[i];
        return new Vector3(r[0], r[1], r[2]);
    }

    public Vector3 negate()
    {
        double[] r = new double[N];
        for ( int i = 0; i < N; i++)
            r[i] = -values[i];
        return new Vector3(r[0], r[1], r[2]);
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
