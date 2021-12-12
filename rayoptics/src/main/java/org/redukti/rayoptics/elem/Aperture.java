package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Vector2;
import org.redukti.rayoptics.util.Pair;

public abstract class Aperture {
    public double x_offset;
    public double y_offset;
    public double rotation;

    public Aperture() {}

    public Aperture(double x_offset, double y_offset, double rotation) {
        this.x_offset = x_offset;
        this.y_offset = y_offset;
        this.rotation = rotation;
    }

    public abstract Vector2 dimension();

    public abstract void set_dimension(double x, double y);

    public double max_dimension() {
        Vector2 d = dimension();
        return Math.sqrt(d.x * d.x + d.y * d.y);
    }

    public abstract boolean point_inside(double x, double y);

    public Pair<Vector2, Vector2> bounding_box() {
        Vector2 center = new Vector2(x_offset, y_offset);
        Vector2 extent = dimension();
        return new Pair<>(center.subtract(extent), center.add(extent));
    }

    public void apply_scale_factor(double scale_factor) {
        x_offset *= scale_factor;
        y_offset *= scale_factor;
    }

    public Vector2 tform(double x, double y) {
        x -= x_offset;
        y -= y_offset;
        return new Vector2(x, y);
    }
}
