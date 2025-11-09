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


package org.redukti.output.rendering;

import org.redukti.output.data.Range;
import org.redukti.output.data.Set1d;
import org.redukti.output.math.*;
import org.redukti.output.plotting.Plot;
import org.redukti.output.plotting.PlotAxes;
import org.redukti.output.plotting.PlotData;
import org.redukti.rayoptics.util.Pair;

import java.util.EnumSet;

import static org.redukti.output.plotting.PlotStyleMask.*;
import static org.redukti.output.rendering.Renderer.PointStyle.PointStyleCross;
import static org.redukti.output.rendering.Renderer.Style.StyleForeground;
import static org.redukti.output.rendering.Renderer.TextAlignMask.*;

public abstract class RendererViewport extends Renderer {

    /**
     * Current 2d viewport window
     */
    Vector2Pair _window2d_fit;

    /**
     * Current 2d viewport window (with margins)
     */
    Vector2Pair _window2d;

    /**
     * 2d device resolution
     */
    Vector2 _2d_output_res;

    enum margin_type_e {
        /**
         * _margin contains a size ratio
         */
        MarginRatio,
        /**
         * _margin contains the width in window size units
         */
        MarginLocal,
        /**
         * _margin contains the width in output size units
         */
        MarginOutput,
    }

    margin_type_e _margin_type;

    /**
     * Margin size or ratio
     */
    Vector2Pair _margin;

    /**
     * Current layout rows and columns counts
     */
    int _rows, _cols;

    /**
     * Current page id
     */
    int _pageid;

    /**
     * Current 2d page window
     */
    Vector2Pair _page;

    double _fov;

    RendererViewport() {
        _margin_type = margin_type_e.MarginRatio;
        _margin = new Vector2Pair(new Vector2(0.13, 0.13), new Vector2(0.13, 0.13));
        _rows = 1;
        _cols = 1;
        _pageid = 0;
        _fov = 45.;
        _page = Vector2Pair.vector2_pair_00;
        _window2d = Vector2Pair.vector2_pair_00;
        _window2d_fit = Vector2Pair.vector2_pair_00;
        _2d_output_res = Vector2.vector2_0;
        //_precision (3), _format ()
    }

    void set_2d_size(double width, double height) {
        _2d_output_res = new Vector2(width, height);
    }

    public void set_window(Vector2 center, Vector2 size, boolean keep_aspect) {
        Vector2 s = size;

        if (keep_aspect) {
            double out_ratio
                    = (_2d_output_res.x() / _cols) / (_2d_output_res.y() / _rows);
            if (Math.abs(s.x() / s.y()) < out_ratio)
                //s.x () = s.y () * out_ratio;
                s = new Vector2(s.y() * out_ratio, s.y());
            else
                //s.y () = s.x () / out_ratio;
                s = new Vector2(s.x(), s.x() / out_ratio);
        }

        Vector2 sby2 = s.divide(2.0);
        //  (center - s / 2., center + s / 2.)
        _window2d_fit = new Vector2Pair(center.minus(sby2), center.plus(sby2));

        Vector2 ms0 = sby2;
        Vector2 ms1 = sby2;

        switch (_margin_type) {
            case MarginLocal:
//                ms[0] = ms[0] + _margin[0];
//                ms[1] = ms[1] + _margin[1];
                ms0 = ms0.plus(_margin.v0);
                ms1 = ms1.plus(_margin.v1);
                break;
            case MarginRatio:
//                ms[0] = ms[0] + s.mul (_margin[0]);
//                ms[1] = ms[1] + s.mul (_margin[1]);
                ms0 = ms0.plus(s.ebeTimes(_margin.v0));
                ms1 = ms1.plus(s.ebeTimes(_margin.v1));
                break;
            case MarginOutput:
//                ms[0] = ms[0] / (math::vector2_1 - _margin[0] / _2d_output_res * 2);
//                ms[1] = ms[1] / (math::vector2_1 - _margin[1] / _2d_output_res * 2);
                ms0 = ms0.ebeDivide(Vector2.vector2_1.minus(_margin.v0.ebeDivide(_2d_output_res.times(2.0))));
                ms1 = ms1.ebeDivide(Vector2.vector2_1.minus(_margin.v1.ebeDivide(_2d_output_res.times(2.0))));
                break;
        }

        //(center - ms[0], center + ms[1])
        _window2d = new Vector2Pair(center.minus(ms0), center.plus(ms1));

        update_2d_window();
        set_orthographic();
        set_page(_pageid);
    }

    public void set_window (Vector2 center, double radius,
                                  boolean keep_aspect)
    {
        Vector2 size = new Vector2(radius, radius);
        set_window (center, size, keep_aspect);
    }

    void update_2d_window() {
    }

    /**
     * Set 3d projection to orthographic, called from @mref set_window.
     */
    public abstract void set_orthographic();

    /** Set 3d perspective projection mode. This function reset the
     viewport window to (-1,1). @see set_window @see set_fov */
    public abstract void set_perspective ();

    public void set_page(int page) {
        if (page >= _cols * _rows)
            throw new IllegalArgumentException("set_page: no such page number in current layout");

        _pageid = page;
        int row = page / _cols;
        int col = page % _cols;

        Vector2 size = new Vector2(_window2d.v1.x() - _window2d.v0.x(),
                _window2d.v1.y() - _window2d.v0.y());

        Vector2 a = new Vector2(_window2d.v0.x() - size.x() * col,
                _window2d.v0.y() - size.y() * (_rows - 1 - row));

        Vector2 b = new Vector2(a.x() + size.x() * _cols, a.y() + size.y() * _rows);

        _page = new Vector2Pair(a, b);
    }

    double x_scale(double x) {
        return ((x / (_page.v1.x() - _page.v0.x())) * _2d_output_res.x());
    }

    double y_scale(double y) {
        return ((y / (_page.v1.y() - _page.v0.y())) * _2d_output_res.y());
    }

    double x_trans_pos(double x) {
        return x_scale(x - _page.v0.x());
    }

    double y_trans_pos(double y) {
        return y_scale(y - _page.v0.y());
    }

    public Vector2Pair get_window2d_fit() {
        return _window2d_fit;
    }
    public Vector2Pair get_window2d() {
        return _window2d;
    }

    public Vector2 get_2d_output_res() {
        return _2d_output_res;
    }

    void set_margin_output(double width, double height) {
        set_margin_output(width, height, width, height);
    }

    void set_margin(double width, double height) {
        set_margin(width, height, width, height);
    }

    void set_margin_ratio(double width, double height) {
        set_margin_ratio(width, height, width, height);
    }

    void set_margin(double left, double bottom, double right,
                    double top) {
        _margin_type = margin_type_e.MarginLocal;
        _margin = new Vector2Pair(new Vector2(left, bottom), new Vector2(right, top));
        set_window(_window2d_fit, false);
    }

    void set_margin_ratio(double left, double bottom, double right,
                          double top) {
        _margin_type = margin_type_e.MarginRatio;
        _margin = new Vector2Pair(new Vector2(left, bottom), new Vector2(right, top));
        set_window(_window2d_fit, false);
    }

    void set_margin_output(double left, double bottom, double right,
                           double top) {
        _margin_type = margin_type_e.MarginOutput;
        _margin = new Vector2Pair(new Vector2(left, bottom), new Vector2(right, top));
        set_window(_window2d_fit, false);
    }

    public void set_window(Vector2Pair window, boolean keep_aspect) {
        //(window[0] + window[1]) / 2
        Vector2 center = window.v0.plus(window.v1).divide(2.0);
        //(window[1].x () - window[0].x (),
        //window[1].y () - window[0].y ());
        Vector2 size = new Vector2(window.v1.x() - window.v0.x(),
                window.v1.y() - window.v0.y());
        set_window(center, size, keep_aspect);
    }

    void draw_frame_2d() {
        Vector2[] fr = new Vector2[4];

        fr[0] = _window2d_fit.v0;
        fr[1] = new Vector2(_window2d_fit.v0.x(), _window2d_fit.v1.y());
        fr[2] = _window2d_fit.v1;
        fr[3] = new Vector2(_window2d_fit.v1.x(), _window2d_fit.v0.y());

        draw_polygon(fr, get_style_color(StyleForeground), false, true);
    }

    public void set_page_layout (int cols, int rows)
    {
        _cols = cols;
        _rows = rows;
        set_page (0);
    }

    public void set_feature_size(double v) {
        _feature_size = v;
    }

    public void set_camera_direction (Vector3 dir)
    {
        Transform3 t = get_camera_transform ();
        t = t.set_direction (dir);
        set_camera_transform (t);
    }

    public void set_camera_position (Vector3 pos)
    {
        Transform3 t = get_camera_transform ();
        t = t.set_translation (pos);
        set_camera_transform (t);
    }

    /** Get reference to 3d camera transform */
    public abstract Transform3 get_camera_transform ();
    /** Get modifiable reference to 3d camera transform */
    public abstract void set_camera_transform (Transform3 t);

    public void set_2d_plot_window (Plot plot)
    {
        var x_range = plot.get_axes ()._axes[0]._range;

        if (x_range.first == x_range.second)
            x_range = plot.get_x_data_range (0);

        var y_range = plot.get_axes ()._axes[1]._range;

        if (y_range.first == y_range.second)
            y_range = plot.get_y_data_range ();

        set_window (
            new Vector2Pair (new Vector2 (x_range.first, y_range.first),
                         new Vector2 (x_range.second, y_range.second)),
      false);
    }

    static double trunc(double x) {
        return (x >= 0) ? Math.floor(x) : Math.ceil(x);
    }


    void draw_axes (PlotAxes a)
    {
        int N = 2;
        Vector3 p = a.get_position ();
        int pow10;
        int[] max = new int[N];
        int[] min = new int[N];
        double[] step = new double[N];

        if (a._frame && N == 2)
            draw_frame_2d ();

        for (int i = 0; i < N; i++)
        {
            PlotAxes.Axis ax = a._axes[i];
            Range r = new Range(_window2d_fit.v0.v(i), _window2d_fit.v1.v(i));

            double s = step[i] = Math.abs (a.get_tics_step (i, r));

            min[i] = (int) trunc ((r.first - p.v(i)) / s);
            max[i] = (int) trunc ((r.second - p.v(i)) / s);

            pow10 = ax._pow10_scale ? (int)Math.floor (Math.log10 (s)) : 0;

            String si_unit = "";

            if (ax._si_prefix)
            {
                String[] sc
                    = { "y", "z", "a", "f", "p", "n", "u", "m", "",
                        "k", "M", "G", "T", "P", "E", "Z", "Y" };
                int u = (24 + pow10 + ax._pow10) / 3;
                if (u >= 0 && u < 17)
                {
                    si_unit = sc[u] + ax._unit;
                    pow10 = (u - 8) * 3 - ax._pow10;
                }
            }

            Vector2 lp = null;
            Vector2 ld = null;

            switch (i)
            {
                case 0:
                    lp = new Vector2 (
                        (_window2d.v0.x () + _window2d.v1.x ()) / 2.,
                        (_window2d_fit.v0.y () * .50 + _window2d.v0.y () * 1.50) / 2.);
                    ld = Vector2.vector2_10;
                    break;
                case 1:
                    lp = new Vector2 (
                        (_window2d_fit.v0.x () * .50 + _window2d.v0.x () * 1.50) / 2.,
                        (_window2d.v0.y () + _window2d.v1.y ()) / 2.);
                    ld = Vector2.vector2_01;
                    break;
            }

            // axis label
            {
                String lx = ax._label;
                boolean useunit = !ax._unit.isEmpty ();
                boolean usep10 = pow10 != 0;

                if (!si_unit.isEmpty ())
                    lx += " (" + si_unit + ")";
                else if (useunit || usep10)
                {
                    lx += " (";
                    if (usep10)
                        lx += String.format("x10^%d", pow10);
                    if (useunit && usep10)
                        lx += " ";
                    if (useunit)
                        lx += ax._unit;
                    lx += ")";
                }

                draw_text (lp, ld, lx, EnumSet.of(TextAlignCenter, TextAlignMiddle), 12,
                    get_style_color (StyleForeground));
            }

            // skip out of range axis
            boolean oor = false;
            for (int j = 0; j < N; j++)
                oor |= (   j != i
                        && (    (p.v(j) <= Math.min (_window2d_fit.v0.v(j), _window2d_fit.v1.v(j)))
                             || (p.v(j) >= Math.max(_window2d_fit.v0.v(j), _window2d_fit.v1.v(j)))));

            // draw axis
            if (!oor && ax._axis)
            {
                Vector3Pair seg = new Vector3Pair (p.v(i,r.first), p.v(i,r.second));
                draw_segment (seg, get_style_color (StyleForeground));
            }

            // draw tics on axis
            if (ax._tics && (ax._axis || a._frame))
            {
                for (int j = min[i]; j <= max[i]; j++)
                    draw_axes_tic(a, i, pow10, oor, j * s);
            }
        }

        if (a._grid)
        {
            // draw grid
            for (int x = min[0]; x <= max[0]; x++)
                for (int y = min[1]; y <= max[1]; y++)
                {
                    draw_point (
                       new Vector2 (p.x() + x * step[0], p.y() + y * step[1]),
                          get_style_color (StyleForeground), PointStyle.PointStyleDot);
                      break;
                }
        }
    }

    void draw_axes_tic (PlotAxes a, int i, int pow10, boolean oor, double x)
    {
        Vector3 p = a.get_position ();
        PlotAxes.Axis ax = a._axes[i];
        Vector2 vtic = null;

        if (!oor && ax._axis)
        {
            vtic = p.project_xy();
            vtic = vtic.set(i, x + p.v(i));
            draw_point (vtic, get_style_color (StyleForeground), PointStyleCross);
        }

        if (a._frame)
        {
            vtic = _window2d_fit.v1;
            vtic = vtic.set(i,x + p.v(i));
            draw_point (vtic, get_style_color (StyleForeground), PointStyleCross);

            vtic = _window2d_fit.v0;
            vtic = vtic.set(i,x + p.v(i));
            draw_point (vtic, get_style_color (StyleForeground), PointStyleCross);
        }

        // draw tic value text
        if (ax._values)
        {
            EnumSet<TextAlignMask>[] align = new EnumSet[]  {
                EnumSet.of(TextAlignCenter, TextAlignTop),
                EnumSet.of(TextAlignRight, TextAlignMiddle),
                EnumSet.of(TextAlignTop, TextAlignCenter)};

            // Use good old C printf - TODO check output format is correct
            String s = String.format("%.3g",
                (x + p.v(i) - a._origin.v(i)) / Math.pow (10., pow10));

            draw_text (vtic, Vector2.vector2_10, s, align[i], 12,
                     get_style_color (StyleForeground));
        }
    }

    @Override
    public void draw_plot (Plot plot)
    {
        switch (plot.get_dimensions ())
        {
            case 1:

            set_2d_plot_window (plot);
            draw_axes (plot.get_axes ());

            // plot title
            draw_text (
                new Vector2 ((_window2d.v0.x () + _window2d.v1.x ()) / 2.,
                             (_window2d_fit.v1.y () + _window2d.v1.y ()) / 2.),
                Vector2.vector2_10, plot.get_title (),
                EnumSet.of(TextAlignCenter,TextAlignMiddle), 18,
                get_style_color (StyleForeground));

            // plot data
            for (int i = 0; i < plot.get_plot_count (); i++)
            {
                PlotData d = plot.get_plot_data (i);
                draw_plot_data_2d ((Set1d) d.get_set (), d);
            }
            break;
        }
    }

    void draw_plot_data_2d (Set1d data, PlotData style)
    {
        // spline interpolated curve between points

        if ((style.get_style () & InterpolatePlot.value()) != 0)
        {
            final double x_step = (_window2d.v1.x () - _window2d.v0.x ()) / _2d_output_res.x ();
            Range xr = data.get_x_range ();
            double x_low = Math.max(_window2d_fit.v0.x (), xr.first);
            double x_high = Math.min(_window2d_fit.v1.x (), xr.second);
            double y1 = data.interpolate (x_low);

            for (double x = x_low + x_step; x < x_high + x_step / 2; x += x_step)
            {
                double y2 = data.interpolate (x);

                draw_segment (new Vector3Pair (
                                new Vector3 (x - x_step, y1, 0),
                                new Vector3 (x, y2, 0)),
                        style.get_color ());

                y1 = y2;
            }
        }

        // line plot

        if ((style.get_style () & LinePlot.value()) != 0)
        {
            Pair<Double, Double> p1 = new Pair<> (data.get_x_value (0),
                                    data.get_y_value (0));

            for (int j = 1; j < data.get_count (); j++)
            {
                Pair<Double, Double> p2 = new Pair<> (data.get_x_value (j),
                                        data.get_y_value (j));

                draw_segment (
                    new Vector3Pair (new Vector3 (p1.first, p1.second, 0),
                                 new Vector3 (p2.first, p2.second, 0)),
                style.get_color ());

                p1 = p2;
            }
        }

        // draw cross tic for each point

        if ((style.get_style () & PointPlot.value()) != 0)
        {
            for (int j = 0; j < data.get_count (); j++)
            {
                Vector2 p = new Vector2 (data.get_x_value (j), data.get_y_value (j));
                draw_point (p, style.get_color (), PointStyleCross);
            }
        }

        // print value for each point

        if ((style.get_style () & ValuePlot.value()) != 0)
        {
            for (int j = 0; j < data.get_count (); j++)
            {
                EnumSet<TextAlignMask> a;
                // FIXME remove use of data pair
                Pair<Double, Double> p = new Pair<> (data.get_x_value (j),
                                                     data.get_y_value (j));

                double prev = j > 0 ? data.get_y_value (j - 1) : p.second;
                double next = j + 1 < data.get_count () ? data.get_y_value (j + 1)
                                                  : p.second;

                if (p.second > prev) // FIXME use derivative to find best text position
                {
                    if (p.second > next)
                        a = EnumSet.of(TextAlignBottom, TextAlignCenter);
                    else
                        a = EnumSet.of(TextAlignBottom, TextAlignRight);
                }
                else
                {
                    if (p.second > next)
                        a = EnumSet.of(TextAlignTop, TextAlignRight);
                    else
                        a = EnumSet.of(TextAlignBottom, TextAlignLeft);
                }

                var s = String.format("%.02f", p.second);

                draw_text (new Vector2 (p.first, p.second), Vector2.vector2_10,
                     s, a, 12, style.get_color ());
            }
        }
    }
}
