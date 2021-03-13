package org.redukti.jfotoptix.patterns;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.shape.Shape;

import java.util.Random;
import java.util.function.Function;

import static org.redukti.jfotoptix.util.MathUtils.square;

public class PatternGenerator {

    static Random random = new Random();

    static void add_pattern_point(Vector2 v, boolean unobstructed, Shape shape, Function<Vector2, Void> f) {
        if (unobstructed || shape.inside(v))
            f.apply(v);
    }

    public static void get_pattern(Distribution d, boolean unobstructed, Shape shape, Function<Vector2, Void> f) {
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

            default:
                throw new IllegalArgumentException("distribution pattern not supported for this shape");
        }
    }

}
