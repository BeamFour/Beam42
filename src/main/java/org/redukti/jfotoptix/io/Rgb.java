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


package org.redukti.jfotoptix.io;

public class Rgb {
    public final double r;
    public final double g;
    public final double b;
    public final double a;

    public Rgb (double red, double green, double blue, double alpha) {
        this.r = red;
        this.g = green;
        this.b = blue;
        this.a = alpha;
    }

    Rgb negate ()
    {
        return new Rgb (1. - r, 1. - g, 1. - b, a);
    }

    public static final Rgb rgb_black = new Rgb (0.0f, 0.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_red = new Rgb (1.0f, 0.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_green = new Rgb(0.0f, 1.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_blue = new Rgb(0.0f, 0.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_yellow = new Rgb(1.0f, 1.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_cyan = new Rgb(0.0f, 1.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_magenta = new Rgb(1.0f, 0.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_gray = new Rgb(0.5f, 0.5f, 0.5f, 1.0f);
    public static final  Rgb rgb_white = new Rgb(1.0f, 1.0f, 1.0f, 1.0f);
}
