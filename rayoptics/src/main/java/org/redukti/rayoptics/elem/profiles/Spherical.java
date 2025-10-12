// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.profiles;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.surface.IntersectionResult;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.util.ZDir;

import java.util.List;

/**
 * Spherical surface profile parameterized by curvature.
 */
public class Spherical extends SurfaceProfile {

    public Spherical(double c) {
        this.cv = c;
    }

    public Spherical() {
        this(0.0);
    }

    @Override
    public SurfaceProfile update() {
        return null;
    }

    @Override
    public double f(Vector3 p) {
        return p.z - 0.5 * cv * p.lengthSquared();
    }

    @Override
    public Vector3 df(Vector3 p) {
        return new Vector3(-cv * p.x, -cv * p.y, 1.0 - cv * p.z);
    }

    @Override
    public double sag(double x, double y) {
        if (!M.isZero(cv)) {
            double r = 1.0 / cv; // radius = 1/curvature
            double adj = r * r - x * x - y * y;
            if (adj < 0.0)
                throw new TraceMissedSurfaceException(); //  (self, (x, y))
            adj = Math.sqrt(adj);
            return r * (1.0 - Math.abs(adj / r));
        }
        else {
            return 0.0;
        }
    }

    @Override
    public List<Vector2> profile(double[] sd, int dir, int steps) {
        return null;
    }

    @Override
    public void apply_scale_factor(double scale_factor) {
        if (M.isZero(scale_factor))
            return;
        cv /= scale_factor;
    }

    /**
     * Intersection with a sphere, starting from an arbitrary point.
     *
     * @param p     start point of the ray in the profile's coordinate system
     * @param d     direction cosine of the ray in the profile's coordinate system
     * @param eps   numeric tolerance for convergence of any iterative procedure
     * @param z_dir +1 if propagation positive direction, -1 if otherwise
     * @return
     */
    @Override
    public IntersectionResult intersect(Vector3 p, Vector3 d, double eps, ZDir z_dir) {
//        Substitute expressions equivalent to Welford's 4.8 and 4.9
//        For quadratic equation ax**2 + bx + c = 0:
//         ax2 = 2a
//         cx2 = 2c

        double ax2 = cv;
        double cx2 = cv * p.lengthSquared() - 2.0 * p.z;
        double b = cv * d.dot(p) - d.z;
        double s = 0.0;
        try {
            s = cx2 / (z_dir.value * Math.sqrt(b * b - ax2 * cx2) - b);
        } catch (Exception e) {
            throw new TraceMissedSurfaceException(e);
        }
        Vector3 p1 = p.add(d.times(s));
        return new IntersectionResult(s, p1);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(getClass().getSimpleName()).append("(")
                .append("c=")
                .append(cv)
                .append(")");
        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
