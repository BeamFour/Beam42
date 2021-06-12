package org.redukti.jfotoptix.patterns;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.jfotoptix.shape.Round;
import org.redukti.jfotoptix.shape.Shape;
import org.redukti.jfotoptix.shape.ShapeBase;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static org.redukti.jfotoptix.math.MathUtils.square;

public class PatternGenerator {

    static Random random = new Random();

    private static void add_pattern_point(Vector2 v, boolean unobstructed, Shape shape, Consumer<Vector2> f) {
        if (unobstructed || shape.inside(v))
            f.accept(v);
    }

    /** Get points distributed on shape area with given pattern */
    public static void get_pattern (Shape shape, Consumer<Vector2> f,
                      Distribution d,
                      boolean unobstructed) {
        if (shape instanceof Rectangle) {
            get_pattern_rect((Rectangle) shape, f, d, unobstructed);
        }
        else if (shape instanceof Round) {
            get_pattern_round((Round) shape, f, d, unobstructed);
        }
        else {
            get_pattern_base((ShapeBase) shape, f, d, unobstructed);
        }
    }

    public static void get_pattern_base(ShapeBase shape, Consumer<Vector2> f,
                                   Distribution d,
                                   boolean unobstructed) {
        final double epsilon = 1e-8;
        // FIXME use bounding box instead of max radius
        final double tr = shape.max_radius() * d.get_scaling();
        final double step = tr / d.get_radial_density();

        Pattern p = d.get_pattern();

        switch (p) {
            case MeridionalDist: {

                double r = tr;

                add_pattern_point(Vector2.vector2_0, unobstructed, shape, f);

                for (int i = 0; i < d.get_radial_density(); i++) {
                    add_pattern_point(new Vector2(0, r), unobstructed, shape, f);
                    add_pattern_point(new Vector2(0, -r), unobstructed, shape, f);
                    r -= step;
                }
                break;
            }

            case SagittalDist: {

                double r = tr;

                add_pattern_point(new Vector2(0, 0), unobstructed, shape, f);

                for (int i = 0; i < d.get_radial_density(); i++) {
                    add_pattern_point(new Vector2(r, 0), unobstructed, shape, f);
                    add_pattern_point(new Vector2(-r, 0), unobstructed, shape, f);
                    r -= step;
                }
                break;
            }

            case CrossDist: {

                double r = step;

                add_pattern_point(Vector2.vector2_0, unobstructed, shape, f);

                for (int i = 0; i < d.get_radial_density(); i++) {
                    add_pattern_point(new Vector2(0, r), unobstructed, shape, f);
                    add_pattern_point(new Vector2(r, 0), unobstructed, shape, f);
                    add_pattern_point(new Vector2(0, -r), unobstructed, shape, f);
                    add_pattern_point(new Vector2(-r, 0), unobstructed, shape, f);
                    r += step;
                }
                break;
            }

            case RandomDist: {

                double x, y;

                for (x = -tr; x < tr; x += step) {
                    double ybound = Math.sqrt(square(tr) - square(x));

                    for (y = -ybound; y < ybound; y += step) {
                        add_pattern_point(
                                new Vector2(x + (random.nextDouble() - .5) * step,
                                        y + (random.nextDouble() - .5) * step), unobstructed, shape, f);
                    }
                }
                break;
            }

            case HexaPolarDist: {

                add_pattern_point(Vector2.vector2_0, unobstructed, shape, f);

                for (double r = tr; r > epsilon; r -= step) {
                    double astep = (step / r) * (Math.PI / 3);

                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep)
                        add_pattern_point(new Vector2(Math.sin(a) * r, Math.cos(a) * r), unobstructed, shape, f);
                }

                break;
            }

            case SquareDist: {

                add_pattern_point(Vector2.vector2_0, unobstructed, shape, f);

                double x, y;

                for (x = tr; x > epsilon; x -= step) {
                    double ybound = Math.sqrt(square(tr) - square(x));

                    for (y = step; y < ybound; y += step) {
                        add_pattern_point(new Vector2(x, y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(x, -y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(-x, y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(-x, -y), unobstructed, shape, f);
                    }

                    add_pattern_point(new Vector2(x, 0), unobstructed, shape, f);
                    add_pattern_point(new Vector2(-x, 0), unobstructed, shape, f);
                }

                for (y = step; y < tr + epsilon; y += step) {
                    add_pattern_point(new Vector2(0, y), unobstructed, shape, f);
                    add_pattern_point(new Vector2(0, -y), unobstructed, shape, f);
                }
                break;
            }

            case DefaultDist:
            case TriangularDist: {
                final double sqrt_3_2 = 0.86602540378443864676;
                double x, y;
                int i = 1;

                for (x = step * sqrt_3_2; x < tr + epsilon; x += step * sqrt_3_2) {
                    for (y = step / (double) i; y < tr + epsilon; y += step) {
                        double h = Math.hypot(x, y);

                        if (h > tr)
                            break;

                        add_pattern_point(new Vector2(x, y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(-x, y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(x, -y), unobstructed, shape, f);
                        add_pattern_point(new Vector2(-x, -y), unobstructed, shape, f);
                    }

                    i ^= 3;
                }

                for (y = step / 2.0; y < tr + epsilon; y += step) {
                    add_pattern_point(new Vector2(0, y), unobstructed, shape, f);
                    add_pattern_point(new Vector2(0, -y), unobstructed, shape, f);
                }

                for (x = step * sqrt_3_2; x < tr + epsilon; x += step * sqrt_3_2 * 2.0) {
                    add_pattern_point(new Vector2(x, 0), unobstructed, shape, f);
                    add_pattern_point(new Vector2(-x, 0), unobstructed, shape, f);
                }
                break;
            }

            case UserDefined: {
                List<Vector2> points = d.get_user_defined_points();
                if (points != null && !points.isEmpty()) {
                    for (Vector2 v: points) {
                        f.accept(v);
                    }
                }
                break;
            }

            default:
                throw new IllegalArgumentException("distribution pattern not supported for this shape");
        }
    }

    private static void get_pattern_round(Round shape, Consumer<Vector2> f,
                                   Distribution d,
                                   boolean unobstructed) {
        final double epsilon = 1e-8;
        final double xyr = 1.0 / shape.get_xy_ratio();
        final double tr = shape.get_external_xradius() * d.get_scaling();
        boolean obstructed = shape.get_hole() && !unobstructed;
        final double hr = obstructed
                ? shape.get_internal_xradius() * (2.0 - d.get_scaling())
                : 0.0;
        int rdens = (int) Math.floor((double) d.get_radial_density()
                - (d.get_radial_density() * (hr / tr)));
        rdens = Math.max(1, rdens);
        final double step = (tr - hr) / rdens;

        Pattern p = d.get_pattern();

        switch (p) {
            case MeridionalDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(0, r * xyr));
                    f.accept(new Vector2(0, -r * xyr));
                }
            }
            break;

            case SagittalDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(r, 0));
                    f.accept(new Vector2(-r, 0));
                }
            }
            break;

            case CrossDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    f.accept(new Vector2(0, r * xyr));
                    f.accept(new Vector2(r, 0));
                    f.accept(new Vector2(0, -r * xyr));
                    f.accept(new Vector2(-r, 0));
                }
            }
            break;

            case RandomDist: {
                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                double tr1 = tr / 20.0;
                for (double r = tr1; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);
                    // angle
                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep) {
                        Vector2 v = new Vector2(Math.sin(a) * r + (random.nextDouble() - .5) * step,
                                Math.cos(a) * r * xyr + (random.nextDouble() - .5) * step);
                        double h = Math.hypot(v.x(), v.y() / xyr);
                        if (h < tr && (h > hr || unobstructed))
                            f.accept(v);
                    }
                }
            }
            break;

            case DefaultDist:
            case HexaPolarDist: {

                if (!obstructed)
                    f.accept(Vector2.vector2_0);

                final double bound = obstructed ? hr - epsilon : epsilon;

                for (double r = tr; r > bound; r -= step) {
                    double astep = (Math.PI / 3) / Math.ceil(r / step);

                    for (double a = 0; a < 2 * Math.PI - epsilon; a += astep)
                        f.accept(new Vector2(Math.sin(a) * r, Math.cos(a) * r * xyr));
                }
            }
            break;

            case UserDefined: {
                List<Vector2> points = d.get_user_defined_points();
                if (points != null && !points.isEmpty()) {
                    for (Vector2 v: points) {
                        f.accept(v);
                    }
                }
                break;
            }

            default: {
                Consumer<Vector2> f2 = (Vector2 v) -> {
                    // unobstructed pattern must be inside external
                    // radius
                    if (square(v.x())
                            + square(v.y() / xyr)
                            < square(tr))
                        f.accept(v);
                };
                get_pattern_base(shape, f2, d, unobstructed);
                break;
            }
        }
    }

    private static void get_pattern_rect(Rectangle shape, Consumer<Vector2> f,
                                 Distribution d, boolean unobstructed) {
        final double epsilon = 1e-8;
        Vector2 hs = shape.get_half_size().times(d.get_scaling());
        Vector2 step = hs.divide((double)(d.get_radial_density () / 2));

        switch (d.get_pattern()) {
            case MeridionalDist: {
                f.accept(Vector2.vector2_0);

                for (double y = step.y(); y < hs.y() + epsilon; y += step.y()) {
                    f.accept(new Vector2(0, y));
                    f.accept(new Vector2(0, -y));
                }
                break;
            }

            case SagittalDist: {
                f.accept(Vector2.vector2_0);

                for (double x = step.x(); x < hs.x() + epsilon; x += step.x()) {
                    f.accept(new Vector2(x, 0));
                    f.accept(new Vector2(-x, 0));
                }
                break;
            }

            case CrossDist: {
                f.accept(Vector2.vector2_0);

                for (double x = step.x(); x < hs.x() + epsilon; x += step.x()) {
                    f.accept(new Vector2(x, 0));
                    f.accept(new Vector2(-x, 0));
                }

                for (double y = step.y(); y < hs.y() + epsilon; y += step.y()) {
                    f.accept(new Vector2(0, y));
                    f.accept(new Vector2(0, -y));
                }
                break;
            }

            case DefaultDist:
            case SquareDist: {
                double x, y;

                f.accept(Vector2.vector2_0);

                for (x = step.x(); x < hs.x() + epsilon; x += step.x())
                    for (y = step.y(); y < hs.y() + epsilon; y += step.y()) {
                        f.accept(new Vector2(x, y));
                        f.accept(new Vector2(-x, y));
                        f.accept(new Vector2(x, -y));
                        f.accept(new Vector2(-x, -y));
                    }

                for (x = step.x(); x < hs.x() + epsilon; x += step.x()) {
                    f.accept(new Vector2(x, 0));
                    f.accept(new Vector2(-x, 0));
                }

                for (y = step.y(); y < hs.y() + epsilon; y += step.y()) {
                    f.accept(new Vector2(0, y));
                    f.accept(new Vector2(0, -y));
                }
                break;
            }

            case UserDefined: {
                List<Vector2> points = d.get_user_defined_points();
                if (points != null && !points.isEmpty()) {
                    for (Vector2 v: points) {
                        f.accept(v);
                    }
                }
                break;
            }

            default:
                get_pattern_base(shape, f, d, unobstructed);
        }
    }


}
