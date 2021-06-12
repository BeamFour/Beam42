package org.redukti.jfotoptix.shape;

import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.patterns.PatternGenerator;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.RendererViewport;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.math.Triangle2;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.patterns.Distribution;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.redukti.jfotoptix.rendering.Renderer.PointStyle.PointStyleCross;
import static org.redukti.jfotoptix.rendering.Renderer.PointStyle.PointStyleDot;

public class TestShapes {

    class shape_test_s {
        final String name;
        final Shape s;

        public shape_test_s(String name, Shape s) {
            this.name = name;
            this.s = s;
        }
    }

    shape_test_s st[] = {
            new shape_test_s("disk", new Disk(30.)),
            new shape_test_s("rectangle", new Rectangle(70., 40.)),
            new shape_test_s("ellipse1", new Ellipse(20, 35)),
            new shape_test_s("ellipse2", new Ellipse(35, 20)),
    };

    @Test
    public void testShapes() {
        for (int i = 0; i < st.length; i++) {
            shape_test_s s = st[i];

            String fname = String.format("test_shape_%s.svg", s.name);

            RendererSvg rsvg = new RendererSvg(800, 600, Rgb.rgb_black);
            RendererViewport r = rsvg;

            r.set_window(Vector2.vector2_0, 70., true);

            {
                Consumer<Triangle2> d = (Triangle2 t) -> {
                    r.draw_triangle(t, true, new Rgb(.2, .2, .2, 1.0));
                    r.draw_triangle(t, false, Rgb.rgb_gray);
                };
                s.s.get_triangles(d, 10.);
            }

            for (int c = 0; c < s.s.get_contour_count(); c++) {

                final ArrayList<Vector2> poly = new ArrayList<>();
                Consumer<Vector2> d = (Vector2 v) -> {
                    poly.add(v);
                };
                s.s.get_contour(c, d, 10.);
                r.draw_polygon(poly.toArray(new Vector2[poly.size()]), Rgb.rgb_yellow, false, true);
            }

            for (double a = 0; a < 2.0 * Math.PI - 1e-8; a += 2.0 * Math.PI / 50.0) {
                Vector2 d = new Vector2(Math.cos(a), Math.sin(a));

                double ro = s.s.get_outter_radius(d);
                r.draw_point(d.times(ro), Rgb.rgb_magenta, PointStyleCross);
                double rh = s.s.get_hole_radius(d);
                r.draw_point(d.times(rh), Rgb.rgb_cyan, PointStyleCross);
            }

            r.draw_circle(Vector2.vector2_0, s.s.max_radius(), Rgb.rgb_red, false);
            r.draw_circle(Vector2.vector2_0, s.s.min_radius(), Rgb.rgb_blue, false);

            r.draw_box(s.s.get_bounding_box(), Rgb.rgb_cyan);

            {
                Consumer<Vector2> d = (Vector2 v) -> {
                    r.draw_point(v, Rgb.rgb_green, PointStyleDot);
                };
                Distribution dist = new Distribution();
                PatternGenerator.get_pattern_base((ShapeBase) s.s, d, dist, false);
            }
            System.out.println(fname);
            System.out.println(rsvg.write(new StringBuilder()).toString());
        }

    }

}
