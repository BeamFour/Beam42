// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.plotter;

import org.redukti.analysis.ContrastPupilMap;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector2Pair;
import org.redukti.render.rendering.Renderer;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.Rgb;
import org.redukti.rayoptics.specs.FieldSnapshot;
import org.redukti.rayoptics.util.Orientation;

import java.util.EnumSet;

/**
 * Draws {@link ContrastPupilMap}s for one field: a row per frequency, sagittal on the
 * left and tangential on the right.
 *
 * <p>Each contrast sample is a circle at its reference ray's pupil position. The circle's
 * radius is proportional to the sample's contrast loss, from nothing when the pair is in
 * phase with the block to the full radius at half a wave. The line through each sample is
 * a clock hand at the pair's average phase, so neighbouring hands turning shows the shape of
 * the wavefront. The outline is the vignetted pupil, scaled in each panel to fill it.
 */
public class ContrastPupilMapPlot {

    private static final Rgb LOSS = new Rgb(0.84f, 0.15f, 0.16f, 1.0f);
    private static final Rgb HAND = new Rgb(0.12f, 0.47f, 0.71f, 1.0f);
    private static final Rgb OUTLINE = Rgb.rgb_black;
    private static final double PANEL_WIDTH = 2.6;
    private static final double PANEL_HEIGHT = 2.9;

    public final FieldSnapshot fld;
    public final int[] frequencies;
    /** [frequency][orientation] */
    public final ContrastPupilMap[][] maps;

    public ContrastPupilMapPlot(FieldSnapshot fld, int[] frequencies, ContrastPupilMap[][] maps) {
        if (frequencies.length != maps.length)
            throw new IllegalArgumentException("one row of maps is needed per frequency");
        this.fld = fld;
        this.frequencies = frequencies;
        this.maps = maps;
    }

    public String plot() {
        int rows = frequencies.length;
        RendererSvg r = new RendererSvg(660, 370 * rows, Rgb.rgb_white);
        r.set_window(new Vector2Pair(
                new Vector2(-PANEL_WIDTH / 2, -(rows - 1) * PANEL_HEIGHT - 1.3),
                new Vector2(1.5 * PANEL_WIDTH, 1.6)), true);
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < 2; column++) {
                var map = maps[row][column];
                if (map != null)
                    panel(r, map, frequencies[row],
                            new Vector2(column * PANEL_WIDTH, -row * PANEL_HEIGHT));
            }
        return r.write(new StringBuilder()).toString();
    }

    private void panel(RendererSvg r, ContrastPupilMap map, int frequency, Vector2 centre) {
        // Scale the vignetted pupil to fill the panel, so off-axis maps stay legible.
        double x0 = -half_width(fld.vlx), x1 = half_width(fld.vux);
        double y0 = -half_width(fld.vly), y1 = half_width(fld.vuy);
        double zoom = 1 / Math.max((x1 - x0) / 2, (y1 - y0) / 2);
        var middle = new Vector2((x0 + x1) / 2, (y0 + y1) / 2);
        outline(r, centre, middle, zoom);
        // Scale circles to the mean sample spacing so that full-size ones only just touch.
        double radius = 0.5 * zoom * Math.sqrt(Math.PI * scale(fld.vlx, fld.vux) * scale(fld.vly, fld.vuy)
                / Math.max(1, map.points.size()));
        for (var p : map.points) {
            var at = centre.plus(p.pupil().minus(middle).times(zoom));
            if (p.loss() > 0)
                r.draw_circle(at, radius * p.loss(), LOSS, false);
            if (Double.isFinite(p.average_phase())) {
                var hand = new Vector2(Math.cos(p.average_phase()), Math.sin(p.average_phase()))
                        .times(0.8 * radius);
                r.draw_segment(at, at.plus(hand), HAND);
            }
        }
        var left = EnumSet.of(Renderer.TextAlignMask.TextAlignLeft);
        double x = centre.x - 1.2;
        r.draw_text(new Vector2(x, centre.y + 1.42), Vector2.vector2_10,
                String.format("field %.2f  %d cyc/mm  %s", fld.y, frequency, Orientation.name(map.orientation)),
                left, 13, Rgb.rgb_black);
        r.draw_text(new Vector2(x, centre.y + 1.22), Vector2.vector2_10,
                String.format("phasor %.3f   rms %.3f waves", map.phasor, map.rms),
                left, 11, Rgb.rgb_gray);
    }

    /** The vignetted pupil, as the unit circle mapped through the piecewise scales. */
    private void outline(RendererSvg r, Vector2 centre, Vector2 middle, double zoom) {
        final int steps = 180;
        Vector2 previous = null;
        for (int i = 0; i <= steps; i++) {
            double t = 2 * Math.PI * i / steps;
            double x = Math.cos(t), y = Math.sin(t);
            var pupil = new Vector2(
                    x * half_width(x < 0 ? fld.vlx : fld.vux),
                    y * half_width(y < 0 ? fld.vly : fld.vuy));
            var point = centre.plus(pupil.minus(middle).times(zoom));
            if (previous != null)
                r.draw_segment(previous, point, OUTLINE);
            previous = point;
        }
    }

    /** Mean of the two pupil half-widths along an axis, from the vignetting factors. */
    private static double scale(double lower, double upper) {
        return 0.5 * (half_width(lower) + half_width(upper));
    }

    private static double half_width(double factor) {
        return factor == 0.0 ? 1.0 : 1.0 - factor;
    }
}
