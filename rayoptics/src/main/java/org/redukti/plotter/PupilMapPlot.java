package org.redukti.plotter;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector2Pair;
import org.redukti.rayoptics.analysis.PupilMapAnalysis;
import org.redukti.rayoptics.analysis.PupilMapAnalysis.PupilMapForField;
import org.redukti.rayoptics.analysis.PupilMapAnalysis.Sample;
import org.redukti.render.rendering.Renderer;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws a {@link PupilMapAnalysis} map: the part of the pupil a field can use, in green,
 * with the surfaces that block the rest in colour, and the nominal pupil and the two
 * candidate vignetting regions drawn over it.
 *
 * <p>What to look for: green outside the black circle is light a sampling pattern confined
 * to the nominal pupil would never trace, and the dashed outlines show whether the four
 * measured factors put their region where the green actually is.
 */
public class PupilMapPlot {

    public final PupilMapForField map;

    public PupilMapPlot(PupilMapForField map) {
        this.map = map;
    }

    private static final Rgb PASSED = new Rgb(0.30f, 0.69f, 0.31f, 1.0f);
    private static final Rgb FAILED = new Rgb(0.93f, 0.93f, 0.93f, 1.0f);
    private static final Rgb NOMINAL = Rgb.rgb_black;
    private static final Rgb PIECEWISE = new Rgb(0.76f, 0.07f, 0.12f, 1.0f);
    private static final Rgb ELLIPSE = new Rgb(0.12f, 0.47f, 0.71f, 1.0f);

    /** A colour per blocking surface, assigned in the order the surfaces turn up. */
    private static final Rgb[] PALETTE = {
            new Rgb(0.84f, 0.15f, 0.16f, 1.0f), new Rgb(1.00f, 0.50f, 0.05f, 1.0f),
            new Rgb(0.58f, 0.40f, 0.74f, 1.0f), new Rgb(0.55f, 0.34f, 0.29f, 1.0f),
            new Rgb(0.89f, 0.47f, 0.76f, 1.0f), new Rgb(0.74f, 0.74f, 0.13f, 1.0f),
            new Rgb(0.09f, 0.75f, 0.81f, 1.0f), new Rgb(0.68f, 0.78f, 0.91f, 1.0f),
            new Rgb(1.00f, 0.73f, 0.47f, 1.0f), new Rgb(0.77f, 0.69f, 0.84f, 1.0f)};

    public String plot() {
        return plot(640);
    }

    public String plot(int size) {
        RendererSvg r = new RendererSvg(size, size, Rgb.rgb_white);
        double reach = map.reach;
        r.set_window(new Vector2Pair(new Vector2(-reach, -reach), new Vector2(reach, reach)), true);

        Map<Integer, Rgb> colours = new LinkedHashMap<>();
        for (Sample s : map.samples)
            if (!s.passed() && s.blocked_by() >= 0)
                colours.computeIfAbsent(s.blocked_by(), k -> PALETTE[colours.size() % PALETTE.length]);

        // The grid is regular and its regions are contiguous, so each row is drawn as a few
        // filled runs rather than as tens of thousands of points.
        double cell = 2 * reach / (map.num_samples - 1.0);
        for (int j = 0; j < map.num_samples; j++) {
            int start = 0;
            for (int i = 1; i <= map.num_samples; i++) {
                Rgb run = colour_of(map.sample(start, j), colours);
                Rgb here = i < map.num_samples ? colour_of(map.sample(i, j), colours) : null;
                if (here != null && same(run, here))
                    continue;
                double x0 = map.coordinate(start) - cell / 2;
                double x1 = map.coordinate(i - 1) + cell / 2;
                double y0 = map.coordinate(j) - cell / 2;
                double y1 = map.coordinate(j) + cell / 2;
                r.draw_polygon(new Vector2[]{new Vector2(x0, y0), new Vector2(x1, y0),
                        new Vector2(x1, y1), new Vector2(x0, y1)}, run, true, true);
                start = i;
            }
        }

        r.draw_circle(Vector2.vector2_0, 1.0, NOMINAL, false);
        draw_region(r, PIECEWISE, false);
        draw_region(r, ELLIPSE, true);

        var left = EnumSet.of(Renderer.TextAlignMask.TextAlignLeft);
        double top = reach * 0.93;
        r.draw_text(new Vector2(-reach * 0.97, top), Vector2.vector2_10,
                String.format("field %.2f", map.fld.yv()), left, 16, Rgb.rgb_black);
        r.draw_text(new Vector2(-reach * 0.97, top - reach * 0.09), Vector2.vector2_10,
                String.format("vig scales  x %.3f/%.3f  y %.3f/%.3f",
                        PupilMapAnalysis.scale(map.fld.vlx), PupilMapAnalysis.scale(map.fld.vux),
                        PupilMapAnalysis.scale(map.fld.vly), PupilMapAnalysis.scale(map.fld.vuy)),
                left, 12, Rgb.rgb_gray);
        r.draw_text(new Vector2(-reach * 0.97, -top + reach * 0.09), Vector2.vector2_10,
                String.format("piecewise sampled %.0f%% covered %.0f%%",
                        100 * map.piecewise_quality.sampled(),
                        100 * map.piecewise_quality.covered()), left, 12, PIECEWISE);
        r.draw_text(new Vector2(-reach * 0.97, -top), Vector2.vector2_10,
                String.format("ellipse   sampled %.0f%% covered %.0f%%",
                        100 * map.ellipse_quality.sampled(),
                        100 * map.ellipse_quality.covered()), left, 12, ELLIPSE);

        var right = EnumSet.of(Renderer.TextAlignMask.TextAlignRight);
        int row = 0;
        for (var entry : colours.entrySet()) {
            r.draw_text(new Vector2(reach * 0.97, top - row * reach * 0.075), Vector2.vector2_10,
                    "blocked by s" + entry.getKey(), right, 12, entry.getValue());
            row++;
        }
        return r.write(new StringBuilder()).toString();
    }

    /** The boundary of a candidate region, as the unit circle mapped through it. */
    private void draw_region(RendererSvg r, Rgb rgb, boolean ellipse) {
        final int steps = 180;
        Vector2 previous = null;
        for (int i = 0; i <= steps; i++) {
            double t = 2 * Math.PI * i / steps;
            double x = Math.cos(t), y = Math.sin(t);
            Vector2 point;
            if (ellipse)
                point = new Vector2(
                        PupilMapAnalysis.ellipse_offset(map.fld.vlx, map.fld.vux)
                                + x * PupilMapAnalysis.ellipse_scale(map.fld.vlx, map.fld.vux),
                        PupilMapAnalysis.ellipse_offset(map.fld.vly, map.fld.vuy)
                                + y * PupilMapAnalysis.ellipse_scale(map.fld.vly, map.fld.vuy));
            else
                point = new Vector2(x * map.fld.vignetting_scale_x(x),
                        y * map.fld.vignetting_scale_y(y));
            if (previous != null)
                r.draw_segment(previous, point, rgb);
            previous = point;
        }
    }

    private static Rgb colour_of(Sample s, Map<Integer, Rgb> colours) {
        if (s.passed())
            return PASSED;
        return s.blocked_by() >= 0 ? colours.get(s.blocked_by()) : FAILED;
    }

    private static boolean same(Rgb a, Rgb b) {
        return a.r == b.r && a.g == b.g && a.b == b.b;
    }
}
