package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.exceptions.TraceMissedSurfaceException;
import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.math.Vector3;

import java.util.List;

/**
 * Even Polynomial asphere up to 20th order, on base conic.
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
            this.coefs = new double[0];
        }
        max_nonzero_coef = 0;
    }

    public double ec() {
        return cc + 1.0;
    }

    public void ec(double ec) {
        cc = ec - 1.0;
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

    }

    void gen_coef_list() {
        for (int i = 0; i < coefs.length; i++) {
            if (coefs[i] != 0.0)
                max_nonzero_coef = i;
        }
        max_nonzero_coef++;
    }
}
