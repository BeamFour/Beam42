package org.redukti.jfotoptix.io;

import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import java.util.EnumSet;

import static org.redukti.jfotoptix.io.Renderer.Style.StyleForeground;
import static org.redukti.jfotoptix.io.Renderer.TextAlignMask.*;

public class TestRendererSvg {

    void test_circle(Renderer r)
    {
        for (double ra = 0; ra < 80.0 ; ra += 10.0)
        {
            r.draw_circle(Vector2.vector2_0, ra, Rgb.rgb_green, false);
        }

        for (double x = -90.0; x < 90.0 + 1e-6 ; x += 30.0)
        {
            Vector2 v = new Vector2(x, 0.);
            r.draw_circle(v, 8, Rgb.rgb_gray, true);
        }
    }

    void test_polygon(Renderer r)
    {
        Vector2[] v = new Vector2[5];

        v[0] = new Vector2(-10, -50);
        v[1] = new Vector2(-5, -40);
        v[2] = new Vector2(-15, -30);
        v[3] = new Vector2(10, -25);
        v[4] = new Vector2(5, -50);

        r.draw_polygon(v, Rgb.rgb_magenta, false, false);

        for (int i = 0; i < 5; i++)
            v[i] = v[i].plus(new Vector2(30, 0));

        r.draw_polygon(v, Rgb.rgb_magenta, true, false);

        for (int i = 0; i < 5; i++)
            v[i] = v[i].plus(new Vector2(30, 0));

        r.draw_polygon(v, Rgb.rgb_magenta, false, true);

        for (int i = 0; i < 5; i++)
            v[i] = v[i].plus(new Vector2(30, 0));

        r.draw_polygon(v, Rgb.rgb_magenta, true, true);
    }

    void test_point(Renderer r)
    {
        for (int i = 0; i < 5; i++) {
            for (double x = -90; x < 90 + 1e-6; x += 5) {
                r.draw_point(new Vector2(x, -90 + i * 5),
                        r.get_style_color(StyleForeground), Renderer.PointStyle.of(i));
            }
        }
    }

    void test_text_(Renderer r, Vector2 pos, EnumSet<Renderer.TextAlignMask> a)
    {
        for (double ra = 0; ra < 2.*Math.PI - 1.e-6; ra += Math.PI/6.)
        {
            Vector2 dir = new Vector2(Math.cos(ra), Math.sin(ra));

            r.draw_segment(new Vector2Pair(pos, pos.plus(dir.times(30))), Rgb.rgb_blue);
            r.draw_text(pos, dir, "A long long long test string",
                    a, 12, Rgb.rgb_red);
        }
    }

    void test_text(Renderer r)
    {
        test_text_(r, new Vector2(-90, 90), EnumSet.of(TextAlignLeft, TextAlignMiddle));
        test_text_(r, new Vector2(-30, 90), EnumSet.of(TextAlignCenter,TextAlignMiddle));
        test_text_(r, new Vector2(30,  90), EnumSet.of(TextAlignRight,TextAlignMiddle));
        test_text_(r, new Vector2(90,  90), EnumSet.of(TextAlignCenter,TextAlignTop));
        test_text_(r, new Vector2(-90, 30), EnumSet.of(TextAlignCenter,TextAlignMiddle));
        test_text_(r, new Vector2(-30, 30), EnumSet.of(TextAlignCenter,TextAlignBottom));
    }

    @Test
    public void test() {
        RendererSvg r = new RendererSvg(1600, 1200);
        r.set_window(new Vector2Pair(new Vector2(-100, -100), new Vector2(100, 100)), true);
        test_circle(r);
        test_polygon(r);
        test_point(r);
        test_text(r);
        r.draw_frame_2d();
        System.out.println(r.write(new StringBuilder()).toString());
    }
}
