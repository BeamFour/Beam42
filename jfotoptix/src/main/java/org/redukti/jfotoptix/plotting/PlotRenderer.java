/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet
 */

package org.redukti.jfotoptix.plotting;

import org.redukti.jfotoptix.data.Range;
import org.redukti.jfotoptix.data.Set1d;
import org.redukti.jfotoptix.math.*;
import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.RendererViewport;

import java.text.DecimalFormat;
import java.util.EnumSet;

import static org.redukti.jfotoptix.rendering.Renderer.PointStyle.PointStyleCross;
import static org.redukti.jfotoptix.rendering.Renderer.Style.StyleForeground;
import static org.redukti.jfotoptix.rendering.Renderer.TextAlignMask.*;

public class PlotRenderer {

    final DecimalFormat _decimal_format;

    public PlotRenderer() {
        _decimal_format = MathUtils.decimal_format(0);
    }

    void draw_plot(RendererViewport r, Plot plot) {
        switch (plot.get_dimensions()) {
            case 1: {
                set_2d_plot_window(r, plot);
                draw_axes_2d(r, plot.get_axes());

                // plot title
                Vector2Pair _window2d = r.get_window2d();
                Vector2Pair _window2d_fit = r.get_window2d_fit();
                r.draw_text(
                        new Vector2((_window2d.v0.x() + _window2d.v1.x()) / 2.,
                                (_window2d_fit.v1.y() + _window2d.v1.y()) / 2.),
                        Vector2.vector2_10, plot.get_title(),
                        EnumSet.of(TextAlignCenter, TextAlignMiddle), 18,
                        r.get_style_color(StyleForeground));

                // plot data
                for (int i = 0; i < plot.get_plot_count(); i++) {
                    PlotData d = plot.get_plot_data(i);
                    draw_plot_data_2d(r, (Set1d) d.get_set(), d);
                }

                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported dimensions " + plot.get_dimensions());
        }
    }

    void draw_plot_data_2d(RendererViewport r, Set1d data,
                           PlotData style) {
        // spline interpolated curve between points
        Vector2Pair _window2d_fit = r.get_window2d_fit();
        Vector2Pair _window2d = r.get_window2d();
        Vector2 _2d_output_res = r.get_2d_output_res();
        if ((style.get_style() & PlotStyleMask.InterpolatePlot.value()) != 0) {
            final double x_step
                    = (_window2d.v1.x() - _window2d.v0.x()) / _2d_output_res.x();
            Range xr = data.get_x_range(0);
            double x_low = Math.max(_window2d_fit.v0.x(), xr.first);
            double x_high = Math.min(_window2d_fit.v1.x(), xr.second);
            double y1 = data.interpolate(x_low);

            for (double x = x_low + x_step; x < x_high + x_step / 2; x += x_step) {
                double y2 = data.interpolate(x);

                r.draw_segment(new Vector3Pair(new Vector3(x - x_step, y1, 0),
                                new Vector3(x, y2, 0)),
                        style.get_color());

                y1 = y2;
            }
        }

        // line plot

        if ((style.get_style() & PlotStyleMask.LinePlot.value()) != 0) {
            Range p1 = new Range(data.get_x_value(0),
                    data.get_y_value(0));

            for (int j = 1; j < data.get_count(); j++) {
                Range p2 = new Range(data.get_x_value(j),
                        data.get_y_value(j));

                r.draw_segment(
                        new Vector3Pair(new Vector3(p1.first, p1.second, 0),
                                new Vector3(p2.first, p2.second, 0)),
                        style.get_color());

                p1 = p2;
            }
        }

        // draw cross tic for each point

        if ((style.get_style() & PlotStyleMask.PointPlot.value()) != 0) {
            for (int j = 0; j < data.get_count(); j++) {
                Vector2 p = new Vector2(data.get_x_value(j), data.get_y_value(j));

                r.draw_point(p, style.get_color(), PointStyleCross);
            }
        }

        // print value for each point

        if ((style.get_style() & PlotStyleMask.ValuePlot.value()) != 0) {
            for (int j = 0; j < data.get_count(); j++) {
                EnumSet<Renderer.TextAlignMask> a;
                // FIXME remove use of data pair
                Range p = new Range(data.get_x_value(j),
                        data.get_y_value(j));

                double prev = j > 0 ? data.get_y_value(j - 1) : p.second;
                double next = j + 1 < data.get_count() ? data.get_y_value(j + 1)
                        : p.second;

                if (p.second
                        > prev) // FIXME use derivative to find best text position
                {
                    if (p.second > next)
                        a = EnumSet.of(TextAlignBottom, TextAlignCenter);
                    else
                        a = EnumSet.of(TextAlignBottom, TextAlignRight);
                } else {
                    if (p.second > next)
                        a = EnumSet.of(TextAlignTop, TextAlignRight);
                    else
                        a = EnumSet.of(TextAlignBottom, TextAlignLeft);
                }

                String s = String.format(".2f", p.second);

                r.draw_text(new Vector2(p.first, p.second), Vector2.vector2_10,
                        s, a, 12, style.get_color());
            }
        }
    }

    void draw_frame_2d(RendererViewport r) {
        Vector2[] fr = new Vector2[4];
        Vector2Pair _window2d_fit = r.get_window2d_fit();

        fr[0] = _window2d_fit.v0;
        fr[1] = new Vector2(_window2d_fit.v0.x(), _window2d_fit.v1.y());
        fr[2] = _window2d_fit.v1;
        fr[3] = new Vector2(_window2d_fit.v1.x(), _window2d_fit.v0.y());

        r.draw_polygon(fr, r.get_style_color(StyleForeground), false, true);
    }

    void set_2d_plot_window(RendererViewport r, Plot plot) {
        Range x_range = plot.get_axes()._axes[0]._range;

        if (x_range.first == x_range.second)
            x_range = plot.get_x_data_range(0);

        Range y_range = plot.get_axes()._axes[1]._range;

        if (y_range.first == y_range.second)
            y_range = plot.get_y_data_range();

        r.set_window(new Vector2Pair(new Vector2(x_range.first, y_range.first),
                        new Vector2(x_range.second, y_range.second)),
                false);
    }

    static final String[] sc = {"y", "z", "a", "f", "p", "n", "u", "m", "",
                                "k", "M", "G", "T", "P", "E", "Z", "Y"};


    public void draw_axes_2d(RendererViewport renderer, PlotAxes a) {
        int N = 2;
        Vector2 p = new Vector2(a.get_position().x(), a.get_position().y());
        int pow10;
        int[] max = new int[N];
        int[] min = new int[N];
        double[] step = new double[N];
        Vector2Pair _window2d = renderer.get_window2d();
        Vector2Pair _window2d_fit = renderer.get_window2d_fit();

        if (a._frame)
            draw_frame_2d(renderer);

        for (int i = 0; i < N; i++) {
            PlotAxes.Axis ax = a._axes[i];
            Range r = new Range(_window2d_fit.v0.v(i), _window2d_fit.v1.v(i));

            double s = step[i] = Math.abs(a.get_tics_step(i, r));

            min[i] = MathUtils.trunc((r.first - p.v(i)) / s);
            max[i] = MathUtils.trunc((r.second - p.v(i)) / s);

            pow10 = ax._pow10_scale ? (int) Math.floor(Math.log10(s)) : 0;

            String si_unit = "";

            if (ax._si_prefix) {
                int u = (24 + pow10 + ax._pow10) / 3;
                if (u >= 0 && u < 17) {
                    si_unit = sc[u] + ax._unit;
                    pow10 = (u - 8) * 3 - ax._pow10;
                }
            }

            Vector2 lp = null;
            Vector2 ld = null;

            switch (i) {
                case 0:
                    lp = new Vector2(
                            (_window2d.v0.x() + _window2d.v1.x()) / 2.,
                            (_window2d_fit.v0.y() * .50 + _window2d.v0.y() * 1.50) / 2.);
                    ld = Vector2.vector2_10;
                    break;
                case 1:
                    lp = new Vector2(
                            (_window2d_fit.v0.x() * .50 + _window2d.v0.x() * 1.50) / 2.,
                            (_window2d.v0.y() + _window2d.v1.y()) / 2.);
                    ld = Vector2.vector2_01;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid axis " + i);
            }

            // axis label
            {
                String lx = ax._label;
                boolean useunit = !ax._unit.isEmpty();
                boolean usep10 = pow10 != 0;

                if (!si_unit.isEmpty())
                    lx += " (" + si_unit + ")";
                else if (useunit || usep10) {
                    lx += " (";
                    if (usep10)
                        //lx += String.format("x10^%i", pow10);
                        lx += String.format("x10^%d", pow10);
                    if (useunit && usep10)
                        lx += " ";
                    if (useunit)
                        lx += ax._unit;
                    lx += ")";
                }

                renderer.draw_text(lp, ld, lx, EnumSet.of(TextAlignCenter, TextAlignMiddle), 12,
                        renderer.get_style_color(StyleForeground));
            }

            // skip out of range axis
            boolean oor = false;
            for (int j = 0; j < N; j++)
                oor |= (j != i
                        && ((p.v(j) <= Math.min(_window2d_fit.v0.v(j), _window2d_fit.v1.v(j)))
                        || (p.v(j) >= Math.max(_window2d_fit.v0.v(j), _window2d_fit.v1.v(j)))));

            // draw axis
            if (!oor && ax._axis) {
                Vector2Pair seg = new Vector2Pair(p.set(i, r.first), p.set(i, r.second));
                renderer.draw_segment(seg, renderer.get_style_color(StyleForeground));
            }

            // draw tics on axis
            if (ax._tics && (ax._axis || a._frame)) {
                for (int j = min[i]; j <= max[i]; j++)
                    draw_axes_tic2(renderer, a, i, pow10, oor, j * s);
            }
        }

        if (a._grid) {
            // draw grid
            for (int x = min[0]; x <= max[0]; x++)
                for (int y = min[1]; y <= max[1]; y++) {
                    switch (N) {
//                        case 3:
//                            for (int z = min[2]; z <= max[2]; z++)
//                                renderer.draw_point (new Vector3 (p[0] + x * step[0],
//                                p[1] + y * step[1],
//                                p[2] + z * step[2]),
//                            renderer.get_style_color (StyleForeground));
//                            break;

                        case 2:
                            renderer.draw_point(
                                    new Vector2(p.v(0) + x * step[0], p.v(1) + y * step[1]),
                                    renderer.get_style_color(StyleForeground), Renderer.PointStyle.PointStyleDot);
                            break;
                    }
                }
        }
    }


    void draw_axes_tic2(RendererViewport r, PlotAxes a, int i,
                        int pow10, boolean oor, double x) {
        final int N = 2;
        Vector2 p = new Vector2(a.get_position().x(), a.get_position().y());
        PlotAxes.Axis ax = a._axes[i];
        Vector2 vtic = null;
        Vector2Pair _window2d_fit = r.get_window2d_fit();

        if (!oor && ax._axis) {
            vtic = p;
            vtic = vtic.set(i, x + p.v(i));
            r.draw_point(vtic, r.get_style_color(StyleForeground), PointStyleCross);
        }

        if (a._frame) {
            vtic = _window2d_fit.v1;
            vtic = vtic.set(i, x + p.v(i));
            r.draw_point(vtic, r.get_style_color(StyleForeground), PointStyleCross);

            vtic = _window2d_fit.v0;
            vtic = vtic.set(i, x + p.v(i));
            r.draw_point(vtic, r.get_style_color(StyleForeground), PointStyleCross);
        }

        // draw tic value text
        if (ax._values) {
            EnumSet<Renderer.TextAlignMask> align0 = EnumSet.of(TextAlignCenter, TextAlignTop);
            EnumSet<Renderer.TextAlignMask> align1 = EnumSet.of(TextAlignRight, TextAlignMiddle);
            EnumSet<Renderer.TextAlignMask> align2 = EnumSet.of(TextAlignTop, TextAlignCenter);

            String s = _decimal_format.format((x + p.v(i) - a._origin.v(i)) / Math.pow(10., pow10));
            switch (N) {
                case 2:
                    EnumSet<Renderer.TextAlignMask> align = i == 0 ? align0
                            : (i == 1 ? align1 : align2);
                    r.draw_text(vtic, Vector2.vector2_10, s, align, 12,
                            r.get_style_color(StyleForeground));
                    break;
            }
        }
    }
}
