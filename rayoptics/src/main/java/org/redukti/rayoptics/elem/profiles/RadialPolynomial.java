package org.redukti.rayoptics.elem.profiles;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;

import java.util.List;

/**
 * Radial Polynomial asphere, both even and odd terms, on base conic.
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
 *     :math:`z(r)=\\dfrac{cr^2}{1+\sqrt[](1-\\textbf{ec } c^2 r^2)}+\sum_{i=1}^{10} a_ir^i`
 *
 *     where :math:`r^2 = x^2+y^2`
 */
public class RadialPolynomial extends SurfaceProfile {

    public double ec;
    double[] coefs;
    int max_nonzero_coef;

    public RadialPolynomial(Double c, Double cc, Double r, Double ec, double[] coefs) {
        if (ec == null)
            ec = 1.0;
        if (c == null)
            c = 0.0;
        if (r != null) {
            r(r);
        } else {
            cv = c;
        }
        if (cc != null)
            cc(cc);
        else
            this.ec = ec;
        if (coefs != null) {
            this.coefs = coefs;
        } else {
            this.coefs = new double[10];
        }
        max_nonzero_coef = 0;
        update();
    }

    public RadialPolynomial() {
        this(null,null,null,null,null);
    }

    public RadialPolynomial coefs(double[] _coefs) {
        this.coefs = _coefs;
        update();
        return this;
    }

    @Override
    public RadialPolynomial r(double radius) {
        super.r(radius);
        return this;
    }

    public RadialPolynomial cc(double cc) {
        this.ec = cc + 1.0;
        return this;
    }
    public double cc() {
        return ec - 1.0;
    }

    void calc_max_nonzero_coef() {
        max_nonzero_coef = -1;
        for (int i = 0; i < coefs.length; i++) {
            if (coefs[i] != 0.0)
                max_nonzero_coef = i;
        }
        max_nonzero_coef++;
    }

    @Override
    public void apply_scale_factor(double scale_factor) {
        cv /= scale_factor;
        var sf = 1.0 / scale_factor;
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = Math.pow(sf,i) * coefs[i];
        }
    }

    @Override
    public SurfaceProfile update() {
        calc_max_nonzero_coef();
        return this;
    }

    @Override
    public double sag(double x, double y) {
        double r2 = x*x + y*y;
        double r = Math.sqrt(r2);
        // sphere + conic contribution
        double t = 1. - ec * cv * cv * r2;
        if (t < 0)
            throw new TraceMissedSurfaceException();
        double z = cv * r2 / (1. + Math.sqrt(t));
        // polynomial asphere contribution
        double z_asp = 0.0;
        double r_pow = r;
        for (int i = 0; i < max_nonzero_coef; i++) {
            z_asp += coefs[i] * r_pow;
            r_pow *= r;
        }
        double z_tot = z + z_asp;
        return z_tot;
    }

    @Override
    public double f(Vector3 p) {
        return p.z - sag(p.x, p.y);
    }

    @Override
    public Vector3 df(Vector3 p) {
        // sphere + conic contribution
        double r2 = p.x * p.x + p.y * p.y;
        double r = Math.sqrt(r2);
        double t = 1. - ec * cv * cv * r2;
        if (t < 0)
            throw new TraceMissedSurfaceException();
        double e = cv / Math.sqrt(t);

        // polynomial asphere contribution
        double e_asp = 0.0;
        double r_pow;
        if (r == 0.0)
            r_pow = 1.0;
        else
            // Initialize to 1/r because we multiply by r's components p[0] and
            // p[1] at the final normalization step.
            r_pow = 1/r;
        double c_coef = 1.0;
        for (int i = 0; i < max_nonzero_coef; i++) {
            e_asp += c_coef * coefs[i] * r_pow;
            c_coef += 1.0;
            r_pow *= r;
        }
        double e_tot = e + e_asp;
        return new Vector3(-e_tot * p.x, -e_tot * p.y, 1.0);
    }

    @Override
    public List<Vector2> profile(double[] sd, int dir, int steps) {
        return null;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(getClass().getSimpleName()).append("(");
        sb.append("c=").append(cv).append(", ");
        sb.append("ec=").append(ec).append(", ");
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
