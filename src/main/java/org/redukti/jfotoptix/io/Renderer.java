package org.redukti.jfotoptix.io;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import java.util.ArrayList;

/**
 Base class for rendering drivers

 This class define the interface for graphical rendering drivers
 and provide a default implementation for some functions.
 */
public abstract class Renderer {
    /** Specifies light ray intensity rendering mode */
    enum IntensityMode
    {
        /** light ray intensity is ignored, no blending is performed while rendering
         ray */
        IntensityIgnore,
        /** light ray intensity is used to blend rendered ray */
        IntensityShade,
        /** light ray intensity logarithm is used to blend rendered ray. This enable
         faint rays to remain visible. */
        IntensityLogShade
    }

    /** Specifies light ray color rendering */
    enum RayColorMode
    {
        /** Compute ray color from its wavelength */
        RayColorWavelen,
        /** Use fixed ray color */
        RayColorFixed
    }

    /** Specifies rendering elements which can have modified colors and style */
    public enum Style
    {
        StyleBackground(0),
        StyleForeground(1),
        StyleRay(2),
        StyleSurface(3),
        StyleGlass(4),
        StyleLast(5);

        final int value;
        Style(int value) {
            this.value = value;
        }
    }
    enum PointStyle
    {
        PointStyleDot(0),
        PointStyleCross(1),
        PointStyleRound(2),
        PointStyleSquare(3),
        PointStyleTriangle(4);

        final int value;
        PointStyle(int value) {
            this.value = value;
        }
    }

    /** Specifies rendered text alignment */
    enum TextAlignMask
    {
        TextAlignCenter(1), //< Vertically centered
        TextAlignLeft(2),
        TextAlignRight(4),
        TextAlignTop(8),
        TextAlignBottom(16),
        TextAlignMiddle(32); //< Horizontally centered

        final int value;
        TextAlignMask(int value) {
            this.value = value;
        }
    };

    double _feature_size;

    Rgb[] _styles_color = new Rgb[Style.StyleLast.value];
    RayColorMode _ray_color_mode;
    IntensityMode _intensity_mode;
    float _max_intensity; // max ray intensity updated on

    public Renderer()
    {
        this._feature_size = 20.;
        this._ray_color_mode = RayColorMode.RayColorWavelen;
        this._intensity_mode = IntensityMode.IntensityIgnore;
        _styles_color[Style.StyleForeground.value] = new Rgb (1.0, 1.0, 1.0, 1.0);
        _styles_color[Style.StyleBackground.value] = new Rgb (0.0, 0.0, 0.0, 1.0);
        _styles_color[Style.StyleRay.value] = new Rgb (1.0, 0.0, 0.0, 1.0);
        _styles_color[Style.StyleSurface.value] = new Rgb (0.5, 0.5, 1.0, 1.0);
        _styles_color[Style.StyleGlass.value] = new Rgb (0.8, 0.8, 1.0, 1.0);
    }
    Rgb get_style_color (Style s)
    {
        return _styles_color[s.value];
    }

    /** @internal Draw a point in 2d */
    public abstract void draw_point (Vector2 p, Rgb rgb, PointStyle s);
    public void draw_point(Vector2 p) {
        draw_point(p, Rgb.rgb_gray, PointStyle.PointStyleDot);
    }

    /** @internal Draw a line segment in 2d */
    public abstract void draw_segment (Vector2Pair s,
                                       Rgb rgb);
    public void draw_segment (Vector2Pair s) {
        draw_segment(s, Rgb.rgb_gray);
    }

    /** @internal Draw a line segment in 2d */
    public void draw_segment (Vector2 a, Vector2 b,
                        Rgb rgb)
    {
        draw_segment (new Vector2Pair (a, b), rgb);
    }
    public void draw_segment (Vector2 a, Vector2 b)
    {
        draw_segment (a, b, Rgb.rgb_gray);
    }

/**********************************************************************
 * Misc shapes 2d drawing
 */

    public void draw_polygon(Vector2[] array, Rgb rgb, boolean filled, boolean closed) {
        int i;

        if (array.length < 3)
            return;

        for (i = 0; i + 1 < array.length; i++)
            draw_segment(new Vector2Pair(array[i], array[i + 1]), rgb);

        if (closed)
            draw_segment(new Vector2Pair(array[i], array[0]), rgb);
    }

    public void draw_circle (Vector2 v, double r, Rgb rgb, boolean filled)
    {
        int count
            = Math.min (100, Math.max (6, (int)(2. * Math.PI * r / _feature_size)));

        Vector2[] p = new Vector2[count];
        double astep = 2. * Math.PI / count;
        double a = astep;
        p[0] = new Vector2 (r, 0);

        for (int i = 0; i < count; i++, a += astep)
            p[i] = v.add(new Vector2 (r * Math.cos (a), r * Math.sin (a)));

        draw_polygon (p, rgb, filled, true);
    }


//    /** @internal Draw a circle in 2d */
//    virtual void draw_circle (const math::Vector2 &v, double r,
//                            const Rgb &rgb = rgb_gray, bool filled = false);
//    /** @internal Draw text in 2d */
//    virtual void draw_text (const math::Vector2 &pos, const math::Vector2 &dir,
//                          const std::string &str, TextAlignMask a, int size,
//                          const Rgb &rgb = rgb_gray)
//      = 0;
//    /** @internal Draw polygon in 2d */
//    virtual void draw_polygon (const math::Vector2 *array, unsigned int count,
//                             const Rgb &rgb = rgb_gray, bool filled = false,
//                               bool closed = true);
//    /** @internal Draw a box in 2d */
//    void draw_box (const math::VectorPair2 &corners, const Rgb &rgb = rgb_gray);
//    /** @internal Draw a triangle in 2d */
//    void draw_triangle (const math::Triangle<2> &t, bool filled = false,
//                      const Rgb &rgb = rgb_gray);
//
//    /** @internal Draw a point in 3d */
//    virtual void draw_point (const math::Vector3 &p, const Rgb &rgb = rgb_gray,
//                           enum PointStyle s = PointStyleDot)
//      = 0;
//    /** @internal Draw a line segment in 3d */
//    virtual void draw_segment (const math::VectorPair3 &l,
//                             const Rgb &rgb = rgb_gray)
//      = 0;
//    /** @internal Draw a line segment in 3d */
//    inline void draw_segment (const math::Vector3 &a, const math::Vector3 &b,
//                            const Rgb &rgb = rgb_gray);
//    /** @internal Draw polygon in 3d */
//    virtual void draw_polygon (const math::Vector3 *array, unsigned int count,
//                             const Rgb &rgb = rgb_gray, bool filled = false,
//                               bool closed = true);
//    /** @internal Draw text in 3d */
//    virtual void draw_text (const math::Vector3 &pos, const math::Vector3 &dir,
//                          const std::string &str, TextAlignMask a, int size,
//                          const Rgb &rgb = rgb_gray)
//      = 0;
//
//    /** @internal Draw filled triangle in 3d */
//    virtual void draw_triangle (const math::Triangle<3> &t, const Rgb &rgb);
//    /** @internal Draw filled triangle in 3d */
//    virtual void draw_triangle (const math::Triangle<3> &t,
//                              const math::Triangle<3> &gradient,
//                              const Rgb &rgb);

}
