package org.redukti.jfotoptix.math;

import java.util.Objects;

/*
Notes:
Plane can be represented as two vectors: point and normal
Ray can be represented as two vectors: origin and direction
*/

public class Vector3Pair {

    public static final Vector3Pair position_000_001 = new Vector3Pair(Vector3.vector3_0, Vector3.vector3_001);

    public final Vector3 v0;
    public final Vector3 v1;

    public Vector3Pair(Vector3 v0, Vector3 b) {
        Objects.requireNonNull(v0);
        Objects.requireNonNull(b);
        this.v0 = v0;
        this.v1 = b;
    }

    public final Vector3 point() {
        return v0;
    }

    public final Vector3 origin() {
        return v0;
    }

    public final Vector3 direction() {
        return v1;
    }

    public final Vector3 normal() {
        return v1;
    }

    public final double z0() { return  v0.z(); }
    public final double z1() { return  v1.z(); }

    public final boolean isEquals(Vector3Pair other, double tolerance) {
        return v0.isEqual(other.v0, tolerance) && v1.isEqual(other.v1, tolerance);
    }

    public double pl_ln_intersect_scale(Vector3Pair line) {
        // See https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection
        return (origin().dotProduct(normal()) - normal().dotProduct(line.origin())) /
                (line.normal().dotProduct(normal()));
    }

    /**
     * Swap the given element between the member vectors and return a new pair
     */
    public static Vector3Pair swapElement(Vector3Pair p, int j) {
        double[] n0 = new double[3];
        double[] n1 = new double[3];

        for (int i = 0; i < 3; i++) {
            if (i == j) {
                n0[i] = p.v1.v(i);
                n1[i] = p.v0.v(i);
            }
            else {
                n0[i] = p.v0.v(i);
                n1[i] = p.v1.v(i);
            }
        }
        return new Vector3Pair(new Vector3(n0[0],n0[1],n0[2]), new Vector3(n0[0],n0[1],n0[2]));
    }

    public String toString() {
        return "[" + v0.toString() + "," + v1.toString() + "]";
    }
}
