package org.redukti.jfotoptix.io;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

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

    void set_window(Vector2 center, Vector2 size, boolean keep_aspect) {
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
        _window2d_fit = new Vector2Pair(center.subtract(sby2), center.add(sby2));

        Vector2 ms0 = sby2;
        Vector2 ms1 = sby2;

        switch (_margin_type) {
            case MarginLocal:
//                ms[0] = ms[0] + _margin[0];
//                ms[1] = ms[1] + _margin[1];
                ms0 = ms0.add(_margin.a());
                ms1 = ms1.add(_margin.a());
                break;
            case MarginRatio:
//                ms[0] = ms[0] + s.mul (_margin[0]);
//                ms[1] = ms[1] + s.mul (_margin[1]);
                ms0 = ms0.add(s.mul(_margin.a()));
                ms1 = ms1.add(s.mul(_margin.b()));
                break;
            case MarginOutput:
//                ms[0] = ms[0] / (math::vector2_1 - _margin[0] / _2d_output_res * 2);
//                ms[1] = ms[1] / (math::vector2_1 - _margin[1] / _2d_output_res * 2);
                ms0 = ms0.divide(Vector2.vector2_1.subtract(_margin.a().divide(_2d_output_res.multiply(2))));
                ms1 = ms1.divide(Vector2.vector2_1.subtract(_margin.b().divide(_2d_output_res.multiply(2))));
                break;
        }

        _window2d = new Vector2Pair(center.subtract(ms0), center.add(ms1));

        update_2d_window();
        set_orthographic();
        set_page(_pageid);
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

        Vector2 size = new Vector2(_window2d.b().x() - _window2d.a().x(),
                _window2d.b().y() - _window2d.a().y());

        Vector2 a = new Vector2(_window2d.a().x() - size.x() * col,
                _window2d.a().y() - size.y() * (_rows - 1 - row));

        Vector2 b = new Vector2(a.x() + size.x() * _cols, a.y() + size.y() * _rows);

        _page = new Vector2Pair(a, b);
    }

    double x_scale(double x) {
        return ((x / (_page.b().x() - _page.a().x())) * _2d_output_res.x());
    }

    double y_scale(double y) {
        return ((y / (_page.b().y() - _page.a().y())) * _2d_output_res.y());
    }

    double x_trans_pos(double x) {
        return x_scale(x - _page.a().x());
    }

    double y_trans_pos(double y) {
        return y_scale(y - _page.a().y());
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

    void set_margin (double left, double bottom, double right,
                                  double top)
    {
        _margin_type = margin_type_e.MarginLocal;
        _margin = new Vector2Pair (new Vector2(left, bottom), new Vector2(right, top));
        set_window (_window2d_fit, false);
    }

    void set_margin_ratio (double left, double bottom, double right,
                                        double top)
    {
        _margin_type = margin_type_e.MarginRatio;
        _margin = new Vector2Pair (new Vector2(left, bottom), new Vector2(right, top));
        set_window (_window2d_fit, false);
    }

    void set_margin_output (double left, double bottom, double right,
                                         double top)
    {
        _margin_type = margin_type_e.MarginOutput;
        _margin = new Vector2Pair (new Vector2(left, bottom), new Vector2(right, top));
        set_window (_window2d_fit, false);
    }

    void set_window (Vector2Pair window, boolean keep_aspect)
    {
        Vector2 center = window.a().add(window.b()).divide(2);
        Vector2 size = new Vector2(window.a().x () - window.a().x (),
            window.b().y () - window.b().y ());
        set_window (center, size, keep_aspect);
    }
}
