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
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.shape.Shape;

public class Stop extends OpticalSurface {

    protected double _external_radius;

    Stop(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, double thickness) {
        super(id, p, transform, curve, shape, Air.air, Air.air, thickness);
        _external_radius = shape.max_radius () * 2.0;
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

    public static class ApertureStop extends Stop {
        public ApertureStop(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, double thickness) {
            super(id, p, transform, curve, shape, thickness);
        }
    }
    public static class FlareStop extends Stop {
        public FlareStop(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, double thickness) {
            super(id, p, transform, curve, shape, thickness);
        }
    }

    public static class Builder extends Surface.Builder {
        private boolean _as;
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

        public Stop.Builder thickness(double t) {
            super.thickness(t);
            return this;
        }
        public Stop.Builder as(boolean as) {
            this._as = as;
            return this;
        }
        public Stop build() {
            return _as ?  new ApertureStop(_id, _position, _transform, curve, shape, _thickness)
                    : new FlareStop(_id, _position, _transform, curve, shape, _thickness);
        }
    }
}
