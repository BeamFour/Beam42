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
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.medium.Mirror;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class MirrorSurface extends OpticalSurface {

    public MirrorSurface(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, Medium left, Medium right) {
        super(id, p, transform, curve, shape, left, right);
    }

    public static class Builder extends OpticalSurface.Builder {

        boolean _light_from_left = true;

        public Builder(boolean _light_from_left) {
            this._light_from_left = _light_from_left;
            this.left = Mirror.mirror;
            this.right = null; /* none */
        }

        @Override
        public MirrorSurface.Builder position(Vector3Pair position) {
            return (MirrorSurface.Builder) super.position(position);
        }

        @Override
        public MirrorSurface.Builder shape(Shape shape) {
            return (MirrorSurface.Builder) super.shape(shape);
        }

        @Override
        public MirrorSurface.Builder curve(Curve curve) {
            return (MirrorSurface.Builder) super.curve(curve);
        }

        @Override
        public MirrorSurface.Builder leftMaterial(Medium left) {
            return (MirrorSurface.Builder)super.leftMaterial(left);
        }
        public MirrorSurface.Builder metal(Medium left) {
            return (MirrorSurface.Builder)super.leftMaterial(left);
        }

        @Override
        public MirrorSurface.Builder rightMaterial(Medium right) {
            return (MirrorSurface.Builder) super.rightMaterial(right);
        }
        public MirrorSurface.Builder air(Medium right) {
            return (MirrorSurface.Builder) super.rightMaterial(right);
        }

        public MirrorSurface.Builder light_from_left() {
            this._light_from_left = true;
            return this;
        }

        public MirrorSurface.Builder light_from_right() {
            this._light_from_left = false;
            return this;
        }

        Medium metal() {
            return this.left;
        }
        Medium air() {
            return this.right;
        }

        @Override
        public OpticalSurface build() {
            return new MirrorSurface(id, position, transform, curve, shape,
                    _light_from_left ?  air() : metal(),
                    _light_from_left ? metal() : air() );
        }
    }
}
