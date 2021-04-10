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


package org.redukti.jfotoptix.rendering;

import org.redukti.jfotoptix.math.*;

import java.util.EnumSet;

/**
 Base class for rendering drivers

 This class define the interface for graphical rendering drivers
 and provide a default implementation for some functions.
 */
public abstract class Renderer {
    /** Specifies light ray intensity rendering mode */
    public enum IntensityMode
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
    public enum PointStyle
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
        public static PointStyle of(int i) {
            switch (i) {
                case 0: return PointStyleDot;
                case 1: return PointStyleCross;
                case 2: return PointStyleRound;
                case 3: return PointStyleSquare;
                case 4: return PointStyleTriangle;
                default: throw new IllegalArgumentException();
            }
        }
    }

    /** Specifies rendered text alignment */
    public enum TextAlignMask
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
    public Rgb get_style_color (Style s)
    {
        return _styles_color[s.value];
    }
    public double get_feature_size() { return _feature_size; }

    /** Draw a point in 2d */
    public abstract void draw_point (Vector2 p, Rgb rgb, PointStyle s);
    public abstract void draw_text (Vector2 pos, Vector2 dir,
                                    String str, EnumSet<TextAlignMask> a, int size,
                                    Rgb rgb);
    public abstract void draw_segment (Vector2Pair s, Rgb rgb);
    public abstract void draw_segment(Vector3Pair s, Rgb rgb);

    public void draw_point(Vector2 p) {
        draw_point(p, Rgb.rgb_gray, PointStyle.PointStyleDot);
    }

    /** Draw a line segment in 2d */
    public void draw_segment (Vector2Pair s) {
        draw_segment(s, Rgb.rgb_gray);
    }

    /**  Draw a line segment in 2d */
    public void draw_segment (Vector2 a, Vector2 b, Rgb rgb)
    {
        draw_segment (new Vector2Pair (a, b), rgb);
    }
    public void draw_segment (Vector2 a, Vector2 b)
    {
        draw_segment (a, b, Rgb.rgb_gray);
    }

    public void draw_segment (Vector3 a, Vector3 b, Rgb rgb)
    {
        draw_segment (new Vector3Pair(a, b), rgb);
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
            p[i] = v.plus(new Vector2 (r * Math.cos (a), r * Math.sin (a)));

        draw_polygon (p, rgb, filled, true);
    }

    public void draw_triangle (Triangle2 t, boolean filled, Rgb rgb)
    {
        draw_polygon (t._v, rgb, filled, true);
    }

    public void draw_box (Vector2Pair c, Rgb rgb)
    {
        draw_segment (new Vector2 (c.v0.x (), c.v0.y ()), new Vector2 (c.v1.x (), c.v0.y ()), rgb);
        draw_segment (new Vector2 (c.v1.x (), c.v1.y ()), new Vector2 (c.v1.x (), c.v0.y ()), rgb);
        draw_segment (new Vector2 (c.v1.x (), c.v1.y ()), new Vector2 (c.v0.x (), c.v1.y ()), rgb);
        draw_segment (new Vector2 (c.v0.x (), c.v0.y ()), new Vector2 (c.v0.x (), c.v1.y ()), rgb);
    }

    public void group_begin (String name)
    {
    }

    public void group_end ()
    {
    }
}
