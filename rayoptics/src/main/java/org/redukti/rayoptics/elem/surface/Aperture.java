// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.elem.surface;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.util.Pair;

public abstract class Aperture {
    public double x_offset = 0.0;
    public double y_offset = 0.0;
    public double rotation = 0.0;
    public boolean is_obscuration = false;

    public Aperture(double x_offset, double y_offset, double rotation, boolean is_obscuration) {
        this.x_offset = x_offset;
        this.y_offset = y_offset;
        this.rotation = rotation;
        this.is_obscuration = is_obscuration;
    }

    public abstract Vector2 dimension();

    public abstract void set_dimension(double x, double y);

    public double max_dimension() {
        Vector2 d = dimension();
        return Math.sqrt(d.x * d.x + d.y * d.y);
    }

    public abstract boolean point_inside(double x, double y,Double fuzz);
    public abstract Vector2 edge_pt_target(Vector2 rel_dir);

    public Pair<Vector2, Vector2> bounding_box() {
        Vector2 center = new Vector2(x_offset, y_offset);
        Vector2 extent = dimension();
        return new Pair<>(center.minus(extent), center.plus(extent));
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
