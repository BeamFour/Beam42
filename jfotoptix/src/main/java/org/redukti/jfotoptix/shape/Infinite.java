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


package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Triangle2;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import java.util.function.Consumer;

public class Infinite implements Shape {
    public static final Infinite infinite = new Infinite();

    @Override
    public boolean inside(Vector2 point) {
        return true;
    }

//    @Override
//    public void get_pattern(Consumer<Vector2> f, Distribution d, boolean unobstructed) {
//        throw new IllegalArgumentException ("can not distribute rays across an infinite surface shape");
//    }

    @Override
    public double max_radius() {
        return 0;
    }

    @Override
    public double min_radius() {
        return 0;
    }

    @Override
    public double get_outter_radius(Vector2 dir) {
        return 0;
    }

    @Override
    public double get_hole_radius(Vector2 dir) {
        return 0;
    }

    @Override
    public Vector2Pair get_bounding_box() {
        return Vector2Pair.vector2_pair_00;
    }

    @Override
    public int get_contour_count() {
        return 0;
    }

    @Override
    public void get_contour(int contour, Consumer<Vector2> f, double resolution) {

    }

    @Override
    public void get_triangles(Consumer<Triangle2> f, double resolution) {

    }
}
