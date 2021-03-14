package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Triangle2;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;
import org.redukti.jfotoptix.patterns.Distribution;

import java.util.function.Consumer;
import java.util.function.Function;

public class Infinite implements Shape {
    public static final Infinite infinite = new Infinite();

    @Override
    public boolean inside(Vector2 point) {
        return true;
    }

    @Override
    public void get_pattern(Consumer<Vector2> f, Distribution d, boolean unobstructed) {
        throw new IllegalArgumentException ("can not distribute rays across an infinite surface shape");
    }

    @Override
    public double max_radius() {
        return 0;
    }

    @Override
    public double min_radius() {
        return 0;
    }

    @Override
    public double get_outter_radius(Vector2 dir) {
        return 0;
    }

    @Override
    public double get_hole_radius(Vector2 dir) {
        return 0;
    }

    @Override
    public Vector2Pair get_bounding_box() {
        return Vector2Pair.vector2_pair_00;
    }

    @Override
    public int get_contour_count() {
        return 0;
    }

    @Override
    public void get_contour(int contour, Consumer<Vector2> f, double resolution) {

    }

    @Override
    public void get_triangles(Consumer<Triangle2> f, double resolution) {

    }
}
