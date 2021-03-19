package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

public class Conic extends ConicBase {

    public Conic(double roc, double sc) {
        super(roc, sc);
    }

    public double derivative(double r) {
        // conical section differentiate (computed with Maxima)

        final double s2 = _sh * MathUtils.square(r);
        final double s3 = Math.sqrt(1 - s2 / MathUtils.square(_roc));
        final double s4
                = 2.0 / (_roc * (s3 + 1))
                + s2 / (MathUtils.square(_roc) * _roc * s3 * MathUtils.square(s3 + 1));
        return r * s4;
    }

    public double sagitta(double r) {
        return MathUtils.square(r)
                / (_roc
                * (Math.sqrt(1 - (_sh * MathUtils.square(r)) / MathUtils.square(_roc)) + 1));
    }

    @Override
    public Vector3 intersect(Vector3Pair ray) {
        final double ax = ray.origin().x();
        final double ay = ray.origin().y();
        final double az = ray.origin().z();
        final double bx = ray.direction().x();
        final double by = ray.direction().y();
        final double bz = ray.direction().z();

  /*
    find intersection point between conical section and ray,
    telescope optics, page 266
  */
        double a = (_sh * MathUtils.square(bz) + MathUtils.square(by) + MathUtils.square(bx));
        double b = ((_sh * bz * az + by * ay + bx * ax) / _roc - bz) * 2.0;
        double c = (_sh * MathUtils.square(az) + MathUtils.square(ay) + MathUtils.square(ax))
                / _roc
                - 2.0 * az;

        double t;

        if (a == 0) {
            t = -c / b;
        } else {
            double d = MathUtils.square(b) - 4.0 * a * c / _roc;

            if (d < 0)
                return null; // no intersection

            double s = Math.sqrt(d);

            if (a * bz < 0)
                s = -s;

            if (_sh < 0)
                s = -s;

            t = (2 * c) / (s - b);
        }

        if (t <= 0) // ignore intersection if before ray origin
            return null;

        return ray.origin().plus(ray.direction().times(t));
    }

}
