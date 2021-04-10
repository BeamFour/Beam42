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


package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.rendering.Renderer;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class Stop extends Surface {

    double _external_radius;

    public Stop(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform, curve, shape);
        _external_radius = shape.max_radius () * 2.0;
    }

    public void draw_2d_e (Renderer r, Element ref)
    {
        Vector3 mr = new Vector3(0, _external_radius, 0);
        Vector3 top = new Vector3(0, get_shape ().get_outter_radius (Vector2.vector2_01), 0);
        Vector3 bot = new Vector3 (0, -get_shape ().get_outter_radius (Vector2.vector2_01.negate()),
            0);

        Transform3 t = get_transform_to (ref);
        Rgb color = r.get_style_color(get_style());
        r.group_begin ("");
        r.draw_segment (t.transform (top), t.transform (mr), color);
        r.draw_segment (t.transform (bot), t.transform (mr.negate()), color);
        r.group_end ();
    }

    @Override
    public String toString() {
        return "Stop{" +
                super.toString() +
                ", external_radius=" + _external_radius +
                '}';
    }

    public double get_external_radius() {
        return _external_radius;
    }

    public static class Builder extends Surface.Builder {
        @Override
        public Stop.Builder position(Vector3Pair position) {
            return (Stop.Builder) super.position(position);
        }

        public Stop.Builder shape(Shape shape) {
            return (Stop.Builder) super.shape(shape);
        }

        public Stop.Builder curve(Curve curve) {
            return (Stop.Builder) super.curve(curve);
        }

        public Stop build() {
            return new Stop(id, position, transform, curve, shape);
        }
    }
}
