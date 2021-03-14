package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.*;

import static org.redukti.jfotoptix.math.MathUtils.square;

public abstract class Rotational extends CurveBase {

    /**
     * Get curve sagitta (z) at specified distance from origin.
     * A derived class need only implement this method, in which case
     * derivatives will be computed numerically. Derived classes may of course
     * provide analytic implementations of derivatives.
     *
     * @param s distance from curve origin (0, 0), s = sqrt(x^2 + y^2).
     */
    public abstract double sagitta(double s);

    public double sagitta(Vector2 xy) {
        return sagitta(xy.len());
    }

    public double derivative(double r) {
        return rotational_derivative(r);
    }

    protected double rotational_derivative(double r) {
        DerivFunction df = (x) -> this.sagitta(x);
        DerivResult result = Derivatives.central_derivative(df, r, 1e-4);
        return result.result;
    }

    public Vector2 derivative(Vector2 xy) {
        return rotational_derivative(xy);
    }

    protected Vector2 rotational_derivative(Vector2 xy) {
        final double r = xy.len();
        if (r == 0)
            return Vector2.vector2_0;
        final double p = derivative(r);
        return xy.times(p / r);
    }


    public Vector3 normal(Vector3 point) {
        return rotational_normal(point);
    }

    protected Vector3 rotational_normal(Vector3 point) {
        final double r = Math.sqrt(square(point.x()) + square(point.y()));
        if (r == 0)
            return new Vector3(0, 0, -1);
        else {
            final double p = derivative(r);
            return new Vector3(point.x() * p / r, point.y() * p / r, -1.0).normalize();
        }
    }
}
