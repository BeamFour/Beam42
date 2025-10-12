// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.profiles;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;

import java.util.List;

/**
 * Even Polynomial asphere, even terms up to 20th order, on base conic.
 *
 *     Conics produced for conic constant values:
 *
 *         + cc > 0.0: oblate spheroid
 *         + cc = 0.0: sphere
 *         + cc < 0.0 and > -1.0: ellipsoid
 *         + cc = -1.0: paraboloid
 *         + cc < -1.0: hyperboloid
 *
 *     Conics produced for conic asphere values:
 *
 *         + ec > 1.0: oblate spheroid
 *         + ec = 1.0: sphere
 *         + ec > 0.0 and < 1.0: ellipsoid
 *         + ec = 0.0: paraboloid
 *         + ec < 0.0: hyperboloid
 *
 *     The conic constant is related to the conic asphere as:
 *
 *         + cc = ec - 1
 *
 *     The sag :math:`z` is given by:
 *
 *     :math:`z(r)=\\dfrac{cr^2}{1+\sqrt[](1-\\textbf{ec } c^2 r^2)}+\sum_{i=1}^{20} a_ir^{2i}`
 *
 *     where :math:`r^2=x^2+y^2`
 */
public class EvenPolynomial extends SurfaceProfile {

    double cc;
    double[] coefs;
    int max_nonzero_coef;

    /**
     * initialize a EvenPolynomial profile.
     *
     * @param c     curvature
     * @param cc    conic constant
     * @param r     radius of curvature. If zero, taken as planar. If r is specified, it overrides any input for c (curvature).
     * @param ec    conic asphere (= cc + 1). If ec is specified, it overrides any input for the conic constant (cc).
     * @param coefs a list of even power coefficents, starting with the quadratic term, and not exceeding the 20th order term.
     */
    public EvenPolynomial(double c, double cc, Double r, Double ec, double[] coefs) {
        if (r != null) {
            r(r);
        } else {
            cv = c;
        }
        if (ec != null)
            ec(ec);
        else
            this.cc = cc;
        if (coefs != null) {
            this.coefs = coefs;
        } else {
            this.coefs = new double[10];
        }
        max_nonzero_coef = 0;
    }

    public EvenPolynomial() {
        this.coefs = new double[10];
    }

    public EvenPolynomial c(double _c) {
        cv = _c;
        return this;
    }

    @Override
    public EvenPolynomial r(double radius) {
        super.r(radius);
        return this;
    }

    public EvenPolynomial coefs(double[] _coefs) {
        this.coefs = _coefs;
        return this;
    }

    public double ec() {
        return cc + 1.0;
    }

    public void ec(double ec) {
        cc = ec - 1.0;
    }

    public EvenPolynomial cc(double _cc) {
        this.cc = _cc;
        return this;
    }

    @Override
    public SurfaceProfile update() {
        gen_coef_list();
        return this;
    }

    @Override
    public double f(Vector3 p) {
        return p.z - sag(p.x, p.y);
    }

    @Override
    public Vector3 df(Vector3 p) {
        // sphere + conic contribution
        double r2 = p.x * p.x + p.y * p.y;
        double t = 1. - ec() * cv * cv * r2;
        if (t < 0)
            throw new TraceMissedSurfaceException();
        double e = cv / Math.sqrt(t);

        // polynomial asphere contribution
        double r_pow = 1.0;
        double e_asp = 0.0;
        double c_coef = 2.0;
        for (int i = 0; i < max_nonzero_coef; i++) {
            e_asp += c_coef * coefs[i] * r_pow;
            c_coef += 2.0;
            r_pow *= r2;
        }
        double e_tot = e + e_asp;
        return new Vector3(-e_tot * p.x, -e_tot * p.y, 1.0);
    }

    @Override
    public double sag(double x, double y) {
        double r2 = x * x + y * y;
        // sphere + conic contribution
        double t = 1. - (cc + 1.0) * cv * cv * r2;
        if (t < 0)
            throw new TraceMissedSurfaceException();
        double z = cv * r2 / (1. + Math.sqrt(t));
        // polynomial asphere contribution
        double z_asp = 0.0;
        double r_pow = r2;
        for (int i = 0; i < max_nonzero_coef; i++) {
            z_asp += coefs[i] * r_pow;
            r_pow *= r2;
        }
        double z_tot = z + z_asp;
        return z_tot;
    }

    @Override
    public List<Vector2> profile(double[] sd, int dir, int steps) {
        return null;
    }

    @Override
    public void apply_scale_factor(double scale_factor) {
        cv /= scale_factor;
        var sf = 1.0 / scale_factor;
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = Math.pow(sf,2*i+1) * coefs[i];
        }
    }

    void gen_coef_list() {
        for (int i = 0; i < coefs.length; i++) {
            if (coefs[i] != 0.0)
                max_nonzero_coef = i;
        }
        max_nonzero_coef++;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(getClass().getSimpleName()).append("(");
        sb.append("c=").append(cv).append(", ");
        sb.append("cc=").append(cc).append(", ");
        sb.append("coefs=[");
        for (int i = 0; i < coefs.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(coefs[i]);
        }
        sb.append("]");
        sb.append(")");
        return sb;
    }
}
