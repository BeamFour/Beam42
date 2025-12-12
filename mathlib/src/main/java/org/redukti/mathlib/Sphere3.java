package org.redukti.mathlib;

public class Sphere3 {
    public final Vector3 center;
    public final double radius;

    public Sphere3(Vector3 center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    // Based on Geometric Tools for Computer Graphics
    // Returns up to two solutions
    public Double[] intersect(Line3 line) {
        var diff = line.origin.minus(center);
        var a0 = diff.dot(diff) - radius*radius;
        var a1 = line.direction.dot(diff);
        var discr = a1 * a1 - a0;
        if (M.isZero(discr)) {
            return new Double[]{-a1, null};
        }
        else if (discr > 0.0) {
            var root = Math.sqrt(discr);
            return new Double[]{-a1 - root, -a1 + root};
        }
        return new Double[]{null, null};
    }
}
