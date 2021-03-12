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
}
