package org.redukti.jfotoptix.io;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import static org.redukti.jfotoptix.io.Renderer.Style.StyleForeground;

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
    protected abstract void set_orthographic();

    void set_page(int page) {
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

    Vector2Pair get_window() {
        return _window2d_fit;
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
}
