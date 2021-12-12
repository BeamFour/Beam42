package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Vector2;

public class Circular extends Aperture {
    public double radius = 1.0;

    public Circular(double radius) {
        super();
        this.radius = radius;
    }

    public Circular(double x_offset, double y_offset, double rotation, double radius) {
        super(x_offset, y_offset, rotation);
        this.radius = radius;
    }

    @Override
    public Vector2 dimension() {
        return new Vector2(radius, radius);
    }

    @Override
    public void set_dimension(double x, double y) {
        radius = Math.sqrt(x*x + y*y);
    }

    @Override
    public double max_dimension() {
        return radius;
    }

    @Override
    public boolean point_inside(double x, double y) {
        Vector2 v = tform(x, y);
        return Math.sqrt(v.x*v.x + v.y*v.y) <= radius;
    }

    @Override
    public void apply_scale_factor(double scale_factor) {
        super.apply_scale_factor(scale_factor);
        radius *= scale_factor;
    }
}
