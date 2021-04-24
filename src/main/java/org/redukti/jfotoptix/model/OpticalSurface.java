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


package org.redukti.jfotoptix.model;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

import java.util.Objects;

public class OpticalSurface extends Surface {
    protected Medium[] _mat = new Medium[2];

    public OpticalSurface(int id,
                          Vector3Pair p,
                          Transform3 transform,
                          Curve curve,
                          Shape shape,
                          Medium left,
                          Medium right) {
        super(id, p, transform, curve, shape);
        _mat[0] = left;
        _mat[1] = right;
    }

    public Medium get_material(int i) {
        return _mat[i];
    }

    public String toString() {
        return "OpticalSurface{" +
                super.toString() +
                ", left material=" + Objects.toString(_mat[0]) +
                ", right material=" + Objects.toString(_mat[1]) +
                '}';
    }

    public static class Builder extends Surface.Builder {
        Medium left = Air.air;
        Medium right = Air.air;

        @Override
        public OpticalSurface.Builder position(Vector3Pair position) {
            return (OpticalSurface.Builder) super.position(position);
        }

        public OpticalSurface.Builder shape(Shape shape) {
            return (OpticalSurface.Builder) super.shape(shape);
        }

        public OpticalSurface.Builder curve(Curve curve) {
            return (OpticalSurface.Builder) super.curve(curve);
        }

        public OpticalSurface.Builder leftMaterial(Medium left) {
            this.left = left;
            return this;
        }

        public OpticalSurface.Builder rightMaterial(Medium right) {
            this.right = right;
            return this;
        }

        public OpticalSurface build() {
            return new OpticalSurface(_id, _position, _transform, curve, shape, left, right);
        }

    }
}
