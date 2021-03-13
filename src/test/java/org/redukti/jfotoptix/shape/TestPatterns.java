package org.redukti.jfotoptix.shape;

import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.io.RendererSvg;
import org.redukti.jfotoptix.io.RendererViewport;
import org.redukti.jfotoptix.io.Rgb;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.function.Function;

import static org.redukti.jfotoptix.io.Renderer.PointStyle.PointStyleCross;
import static org.redukti.jfotoptix.io.Renderer.TextAlignMask.TextAlignBottom;
import static org.redukti.jfotoptix.io.Renderer.TextAlignMask.TextAlignCenter;


public class TestPatterns {
    static class shape_test_s {
        String name;
        Shape s;
        boolean unobstructed;

        public shape_test_s(String name, Shape s, boolean unobstructed) {
            this.name = name;
            this.s = s;
            this.unobstructed = unobstructed;
        }
    }

    TestPatterns.shape_test_s st[] = {
            new TestPatterns.shape_test_s("disk", new Disk(30), false)
    };

    @Test
    public void testPatterns() {
        String pname[] = {"default", "sagittal", "tangential", "cross",
                "square", "triangular", "hexpolar", "random"};
        Pattern patterns[] = {Pattern.DefaultDist,
                Pattern.SagittalDist,
                Pattern.MeridionalDist,
                Pattern.CrossDist,
                Pattern.SquareDist,
                Pattern.TriangularDist,
                Pattern.HexaPolarDist,
                Pattern.RandomDist};
        int err = 0;
        for (int i = 0; i < st.length; i++) {
            shape_test_s s = st[i];

            String fname = String.format("test_pattern_%s.svg", s.name);

            RendererSvg rsvg = new RendererSvg(800, 400, Rgb.rgb_white);
            RendererViewport r = rsvg;

            r.set_page_layout(4, 2);

            for (int j = 0; j < patterns.length; j++) {
                Pattern p = patterns[j];
                if (p == Pattern.SquareDist || p == Pattern.TriangularDist)
                    continue;

                r.set_page(j);

                System.out.println(s.name + " " + pname[j] + System.lineSeparator());

                r.set_window(Vector2.vector2_0, 70, true);
                r.set_feature_size(.1);

                ArrayList<Vector2> pts = new ArrayList<>();

                try {
                    Distribution dist = new Distribution(p, 5, 0.999);
                    Function<Vector2, Void> de = (Vector2 v2d) -> {
                        pts.add(v2d);
                        return null;
                    };
                    s.s.get_pattern(de, dist, s.unobstructed);
                } catch (Exception e) {
                    continue;
                }

                if (pts.size() < 4)
                    err++;

                boolean first = true;

                for (Vector2 v : pts) {
                    r.draw_point(v, Rgb.rgb_red, PointStyleCross);

                    // Chief ray must be the first ray in list, some analysis do rely
                    // on this
                    if (!first && v.isEqual(Vector2.vector2_0, 1)
                            && p != Pattern.RandomDist) {
                        System.err.println("-- chief !first " + v + "\n");
                        err++;
                    }

                    if (!s.unobstructed && !s.s.inside(v)) {
                        System.err.println("-- !inside " + v + "\n");
                        err++;
                    }

                    if (p != Pattern.RandomDist) {
                        // check for duplicates
                        for (Vector2 w : pts)
                            if (v != w && v.isEqual(w, 1)) {
                                System.err.println("-- dup " + w + v + "\n");
                                err++;
                            }
                    }

                    first = false;
                }

                for (int c = 0; c < s.s.get_contour_count(); c++) {
                    ArrayList<Vector2> poly = new ArrayList<>();
                    Function<Vector2, Void> de = (Vector2 v2d) -> {
                        poly.add(v2d);
                        return null;
                    };
                    s.s.get_contour(c, de, 10.);
                    r.draw_polygon(poly.toArray(new Vector2[poly.size()]), Rgb.rgb_black, false, true);
                }

                r.draw_text(new Vector2(0, -43), Vector2.vector2_10,
                        String.format("%s: %d points", pname[j], pts.size()),
                        EnumSet.of(TextAlignCenter, TextAlignBottom), 18, Rgb.rgb_gray);
            }

            //      r.draw_pages_grid(io::rgb_black);
            System.out.println(rsvg.write(new StringBuilder()).toString());

        }
    }
}
