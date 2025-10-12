// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.profiles;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.surface.IntersectionResult;
import org.redukti.rayoptics.util.ZDir;

import java.util.List;

/**
 * Base class for surface profiles.
 */
public abstract class SurfaceProfile {

    public double cv;

    public abstract SurfaceProfile update();

    public double r() {
        if (!M.isZero(cv))
            return 1.0 / cv;
        else
            return 0.0;
    }

    public SurfaceProfile r(double radius) {
        if (!M.isZero(radius))
            cv = 1.0 / radius;
        else
            cv = 0.0;
        return this;
    }

    /**
     * Returns the value of the profile surface function at point *p*.
     *
     * @param p Point
     */
    public abstract double f(Vector3 p);

    /**
     * Returns the gradient of the profile surface function at point *p*.
     */
    public abstract Vector3 df(Vector3 p);

    /**
     * Returns the unit normal of the profile at point *p*.
     */
    public Vector3 normal(Vector3 p) {
        // TODO check original as that returns p if norm() == 0
        return df(p).normalize();
    }

    /**
     * Returns the sagitta (z coordinate) of the surface at x, y.
     */
    public abstract double sag(double x, double y);

    /**
     * Return a 2d polyline approximating the surface profile.
     * <p>
     * @param sd semi-diameter of the profile (array of length 1 or 2)
     * @paran dir +1 for profile from neg to positive direction, -1 if otherwise
     * @param steps number of points to generate
     */
    public abstract List<Vector2> profile(double[] sd, int dir, int steps);

    /**
     * Apply *scale_factor* to the profile definition.
     */
    public abstract void apply_scale_factor(double scale_factor);

    /**
     * Intersect a profile, starting from an arbitrary point.
     * <p>
     * @param p0 start point of the ray in the profile's coordinate system
     * @param d direction cosine of the ray in the profile's coordinate system
     * @param eps numeric tolerance for convergence of any iterative procedure
     * @param z_dir +1 if propagation positive direction, -1 if otherwise
     * @return tuple: distance to intersection point *s1*, intersection point *p*
     */
    public IntersectionResult intersect(Vector3 p0, Vector3 d, double eps, ZDir z_dir) {
        return intersect_spencer(p0, d, eps, z_dir);
    }

    /**
     * Intersect a profile, starting from an arbitrary point.
     * <p>
     * From Spencer and Murty, `General Ray-Tracing Procedure
     * <https://doi.org/10.1364/JOSA.52.000672>`_
     *
     * @param p0 start point of the ray in the profile's coordinate system
     * @param d direction cosine of the ray in the profile's coordinate system
     * @param eps numeric tolerance for convergence of any iterative procedure
     * @param z_dir +1 if propagation positive direction, -1 if otherwise
     * @return tuple: distance to intersection point *s1*, intersection point *p*
     */
    private IntersectionResult intersect_spencer(Vector3 p0, Vector3 d, double eps, ZDir z_dir) {
        Vector3 p = p0;
        double s1 = -f(p) / d.dot(df(p));   // -f(p)/dot(d, df(p))
        double delta = Math.abs(s1);
        // print("intersect", s1)
        int iter = 0;
        while (delta > eps && iter < 1000) {
            p = p0.add(d.times(s1));  //  p0 + d*s1
            double s2 = s1 - f(p) / d.dot(df(p));  // s1 - f(p) / dot(d, df(p))
            delta = Math.abs(s2 - s1);
            // #print("intersect", s1, s2, delta)
            s1 = s2;
            iter++;
        }
        //# print('intersect iter =', iter)
        return new IntersectionResult(s1, p);
    }

    public StringBuilder toString(StringBuilder sb) {
        return sb;
    }

}
