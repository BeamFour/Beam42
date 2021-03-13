package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;

import java.util.Random;
import java.util.function.Function;

public abstract class Round extends ShapeBase {

    static Random random = new Random();

    boolean hole;

    abstract double get_xy_ratio();

    abstract double get_external_xradius();

    abstract double get_internal_xradius();

    public void get_pattern(Function<Vector2, Void> f,
                            Distribution d,
                            boolean unobstructed) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / get_xy_ratio();
        final double tr = get_external_xradius() * d.get_scaling();
        boolean obstructed = hole && !unobstructed;
        final double hr = obstructed
                ? get_internal_xradius() * (2.0 - d.get_scaling())
                : 0.0;
        int rdens = (int) Math.floor((double) d.get_radial_density()
                - (d.get_radial_density() * (hr / tr)));
        rdens = Math.max(1, rdens);
        final double step = (tr - hr) / rdens;

        Pattern p = d.get_pattern();

        switch (p) {
            case MeridionalDist: {

                if (!obstructed)
                    f.apply(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.apply(new Vector2(0, r * xyr));
                    f.apply(new Vector2(0, -r * xyr));
                }
            }
            break;

            case SagittalDist: {

                if (!obstructed)
                    f.apply(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.apply(new Vector2(r, 0));
                    f.apply(new Vector2(-r, 0));
                }
            }
            break;

            case CrossDist: {

                if (!obstructed)
                    f.apply(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.apply(new Vector2(0, r * xyr));
                    f.apply(new Vector2(r, 0));
                    f.apply(new Vector2(0, -r * xyr));
                    f.apply(new Vector2(-r, 0));
                }
            }
            break;

            case RandomDist: {
                if (!obstructed)
                    f.apply(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                double tr1 = tr / 20;
                for (double r = tr1; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);
                    // angle
                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep) {
                        Vector2 v = new Vector2(Math.sin(a) * r + (random.nextDouble() - .5) * step,
                                Math.cos(a) * r * xyr + (random.nextDouble() - .5) * step);
                        double h = Math.hypot(v.x(), v.y() / xyr);
                        if (h < tr && (h > hr || unobstructed))
                            f.apply(v);
                    }
                }
            }
            break;

            case DefaultDist:
            case HexaPolarDist: {

                if (!obstructed)
                    f.apply(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);

                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep)
                        f.apply(new Vector2(Math.sin(a) * r, Math.cos(a) * r * xyr));
                }
            }
            break;

            default: {
                System.exit(20);
//                DPP_DELEGATE3_OBJ (f2, void, (const math::Vector2 &v),
//                           const math::Vector2::put_delegate_t &, f, // _0
//                double, xyr,                              // _1
//                double, tr,                               // _2
//                    {
//                // unobstructed pattern must be inside external
//                // radius
//                if (math::square (v.x ())
//                    + math::square (v.y () / _1)
//                    < math::square (_2))
//                _0 (v);
//                           });
//
//                Base::get_pattern (f2, d, unobstructed);
                break;
            }
        }
    }

    @Override
    public int get_contour_count() {
        return hole ? 2 : 1;
    }

    double get_radial_step(double resolution) {
        final double xyr = 1.0 / get_xy_ratio();
        double width = xyr <= 1.
                ? get_external_xradius() - get_internal_xradius()
                : get_external_xradius() * xyr
                - get_internal_xradius() * xyr;

        if (resolution < width / 30.)
            resolution = width / 30.;

        if (resolution > width)
            resolution = width;

        if (hole && resolution > get_internal_xradius())
            resolution = get_internal_xradius();

        return (get_external_xradius() - get_internal_xradius())
                / Math.ceil(width / resolution);
    }

    @Override
    public void get_contour(int contour,
                            Function<Vector2, Void> f,
                            double resolution) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / get_xy_ratio();
        double r;

        assert (contour < get_contour_count());

        if (hole && contour == 1)
            r = get_internal_xradius();
        else
            r = get_external_xradius();

        double astep1 = (Math.PI / 3.0) / Math.round(r / get_radial_step(resolution));

        for (double a1 = 0; a1 < 2 * Math.PI - epsilon; a1 += astep1)
            f.apply(new Vector2(Math.cos(a1) * r, Math.sin(a1) * r * xyr));
    }

//    template <class X, bool hole>
//    void
//    Round<X, hole>::get_triangles (const math::Triangle<2>::put_delegate_t &f,
//                                   double resolution) const
//    {
//        static const double epsilon = 1e-8;
//  const double xyr = 1.0 / X::get_xy_ratio ();
//  const double rstep = get_radial_step (resolution);
//
//        double astep1;
//        double r;
//
//        if (!hole)
//        {
//            r = rstep;
//            astep1 = M_PI / 3;
//
//            // central hexagon
//
//            for (double a1 = 0; a1 < M_PI - epsilon; a1 += astep1)
//            {
//                math::Vector2 a (cos (a1) * rstep, sin (a1) * rstep * xyr);
//                math::Vector2 b (cos (a1 + astep1) * rstep,
//                    sin (a1 + astep1) * rstep * xyr);
//                math::Vector2 z (0, 0);
//
//                f (math::Triangle<2> (b, a, z));
//                f (math::Triangle<2> (-b, -a, z));
//            }
//        }
//        else
//        {
//            r = X::get_internal_xradius ();
//            astep1 = (M_PI / 3.0) / round (r / rstep);
//        }
//
//        // hexapolar distributed triangles
//
//        for (; r < X::get_external_xradius () - epsilon; r += rstep)
//        {
//            double astep2 = (M_PI / 3.0) / round ((r + rstep) / rstep);
//            double a1 = 0, a2 = 0;
//
//            while ((a1 < M_PI - epsilon) || (a2 < M_PI - epsilon))
//            {
//                math::Vector2 a (cos (a1) * r, sin (a1) * r * xyr);
//                math::Vector2 b (cos (a2) * (r + rstep),
//                    sin (a2) * (r + rstep) * xyr);
//                math::Vector2 c;
//
//                if (a1 + epsilon > a2)
//                {
//                    a2 += astep2;
//                    c = math::Vector2 (cos (a2) * (r + rstep),
//                        sin (a2) * (r + rstep) * xyr);
//                }
//                else
//                {
//                    a1 += astep1;
//                    c = math::Vector2 (cos (a1) * r, sin (a1) * r * xyr);
//                }
//
//                f (math::Triangle<2> (a, c, b));
//                f (math::Triangle<2> (-a, -c, -b));
//            }
//
//            astep1 = astep2;
//        }
//    }

}
