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
import org.redukti.jfotoptix.patterns.Distribution;

import java.util.function.Consumer;

public interface Shape {

    /** Check if the (x,y) 2d point is inside 2d shape area */
    boolean inside (Vector2 point);

    /** Get points distributed on shape area with given pattern */
//    void get_pattern (Consumer<Vector2> f,
//                      Distribution d,
//                      boolean unobstructed);

    /** Get distance between origin and farthest shape edge */
    double max_radius ();

    /** Get distance between origin and nearest shape outter edge */
    double min_radius ();

    /** Get distance between origin and farthest shape edge in specified
     * direction */
    double get_outter_radius (Vector2 dir);

    /** Get distance between origin and nearest shape outter edge in specified
     * direction */
    double get_hole_radius (Vector2 dir);

    /** Get shape bounding box */
    Vector2Pair get_bounding_box ();

    /** Get number of contours polygones. This function returns
     value is greater than 1 if shape has hole(s). @see get_contour */
    int get_contour_count ();

    /** Get contour polygone points for specified contour id. First
     contour is always outter edge. @see get_contour_count */
    void get_contour (int contour,
                            Consumer<Vector2> f,
                              double resolution);

    /** Get shape teselation triangles */
    void get_triangles (Consumer<Triangle2> f, double resolution);

}
