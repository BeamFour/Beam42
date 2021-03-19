package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import static org.redukti.jfotoptix.math.MathUtils.square;

public class Disk extends Round {

    double _radius;

    public Disk(double radius) {
        super(false);
        this._radius = radius;
    }

    @Override
    double get_xy_ratio() {
        return 1.0;
    }

    @Override
    double get_external_xradius() {
        return _radius;
    }

    @Override
    double get_internal_xradius() {
        return 0;
    }

    @Override
    public boolean inside(Vector2 point) {
        return (square(point.x()) + square(point.y())
                <= square(_radius));
    }

    @Override
    public double max_radius() {
        return _radius;
    }

    @Override
    public double min_radius() {
        return _radius;
    }

    @Override
    public double get_outter_radius(Vector2 dir) {
        return _radius;
    }

    @Override
    public Vector2Pair get_bounding_box() {
        Vector2 hs = new Vector2(_radius, _radius);
        return new Vector2Pair(hs.negate(), hs);
    }

    @Override
    public String toString() {
        return "Disk{" +
                "radius=" + _radius +
                '}';
    }
}
