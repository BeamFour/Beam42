package org.redukti.jfotoptix.io;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

public abstract class RendererViewport extends Renderer {

    /** Current 2d viewport window */
    Vector2Pair _window2d_fit;

    /** Current 2d viewport window (with margins) */
    Vector2Pair _window2d;

    /** 2d device resolution */
    Vector2 _2d_output_res;

    enum margin_type_e
    {
        /** _margin contains a size ratio */
        MarginRatio,
        /** _margin contains the width in window size units */
        MarginLocal,
        /** _margin contains the width in output size units */
        MarginOutput,
    }

    margin_type_e _margin_type;

    /** Margin size or ratio */
    Vector2Pair _margin;

    /** Current layout rows and columns counts */
    int _rows, _cols;

    /** Current page id */
    int _pageid;

    /** Current 2d page window */
    Vector2Pair _page;

    double _fov;

    double x_scale (double x)
    {
        return ((x / (_page.b().x () - _page.a().x ())) * _2d_output_res.x ());
    }

    double y_scale (double y)
    {
        return ((y / (_page.b().y () - _page.a().y ())) * _2d_output_res.y ());
    }

    double x_trans_pos (double x)
    {
        return x_scale (x - _page.a().x ());
    }

    double y_trans_pos (double y)
    {
        return y_scale (y - _page.a().y ());
    }

}
