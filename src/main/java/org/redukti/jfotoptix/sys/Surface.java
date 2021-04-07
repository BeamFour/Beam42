package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.io.Rgb;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.shape.Shape;

import java.util.function.Consumer;

public class Surface extends Element {

    final Shape shape;
    final Curve curve;

    public Surface(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform);
        this.curve = curve;
        this.shape = shape;
    }

    public Shape get_shape() {
        return shape;
    }

    public Curve get_curve() {
        return curve;
    }

    public Renderer.Style get_style() {
        return Renderer.Style.StyleSurface;
    }

    public void get_pattern (Consumer<Vector3> f,
                             Distribution d, boolean unobstructed)
    {
        Consumer<Vector2> de = (v2d) -> {
            f.accept (new Vector3 (v2d.x(), v2d.y(), curve.sagitta (v2d)));
        };

        // get distribution from shape
        shape.get_pattern (de, d, unobstructed);
    }

    public void draw_2d_e(Renderer r, Element ref) {
        double top_edge = shape.get_outter_radius(Vector2.vector2_01);
        double top_hole = shape.get_hole_radius(Vector2.vector2_01);

        double bot_edge = -shape.get_outter_radius(Vector2.vector2_01.negate());
        double bot_hole = -shape.get_hole_radius(Vector2.vector2_01.negate());

        int res = Math.max(
                100,
                Math.min(4, (int) (Math.abs(top_edge - bot_edge) / r.get_feature_size())));

        Rgb color = r.get_style_color(get_style());

        if (Math.abs(bot_hole - top_hole) > 1e-6) {
            Vector2[] p = new Vector2[res / 2];

            get_2d_points(p, top_edge, top_hole, ref);
            r.draw_polygon(p, color, false, false);
            get_2d_points(p, bot_hole, bot_edge, ref);
            r.draw_polygon(p, color, false, false);
        } else {
            Vector2[] p = new Vector2[res];

            get_2d_points(p, top_edge, bot_edge, ref);
            r.draw_polygon(p, color, false, false);
        }
    }

    public void get_2d_points(Vector2[] array, double start,
                              double end, Element ref) {
        int count = array.length;
        assert (count > 1);

        double y1 = start;
        double step = (end - start) / (count - 1);
        int i;

        Transform3 t = get_transform_to(ref);

        for (i = 0; i < (int) count; i++) {
            Vector3 v = new Vector3(0., y1, 0.);
            v = v.z(curve.sagitta(v.project_xy()));

            array[i] = t.transform(v).project_zy();
            y1 += step;
        }
    }

    public Vector3Pair get_bounding_box() {
        Vector2Pair sb = shape.get_bounding_box();

        // FIXME we assume curve is symmetric here
        double z = 0;
        double ms = curve.sagitta(new Vector2(shape.max_radius()));

        if (z > ms) {
            double temp = z;
            z = ms;
            ms = temp;
        }
        return new Vector3Pair(new Vector3(sb.v0.x(), sb.v0.y(), z),
                new Vector3(sb.v1.x(), sb.v1.y(), ms));
    }

    @Override
    public String toString() {
        return super.toString()+
                ", shape=" + shape +
                ", curve=" + curve;
    }

    public static class Builder extends Element.Builder {
        Shape shape;
        Curve curve;

        @Override
        public Surface.Builder position(Vector3Pair position) {
            return (Builder) super.position(position);
        }

        @Override
        public Element build() {
            return new Surface(id, position, transform, curve, shape);
        }

        public Builder shape(Shape shape) {
            this.shape = shape;
            return this;
        }

        public Builder curve(Curve curve) {
            this.curve = curve;
            return this;
        }
    }

}
