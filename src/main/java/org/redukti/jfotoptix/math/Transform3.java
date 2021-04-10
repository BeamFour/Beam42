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


package org.redukti.jfotoptix.math;

public class Transform3 {

    public final Vector3 translation;
    public final Matrix3 linear;
    public final boolean useLinear;

    public Transform3() {
        this.linear = Matrix3.diag(1.0, 1.0, 1.0);
        this.useLinear = false;
        this.translation = Vector3.vector3_0;
    }

    public Transform3(Vector3Pair position) {
        this.translation = position.point();
        if (position.direction().x() == 0 && position.direction().y() == 0) {
            if (position.direction().z() < 0.0) {
                this.linear = Matrix3.diag(1.0, 1.0, -1.0);
                this.useLinear = true;
            } else {
                this.linear = Matrix3.diag(1.0, 1.0, 1.0);
                this.useLinear = false;
            }
        } else {
            Quaternion q = new Quaternion(Vector3.vector3_001, position.direction());
            this.linear = Matrix3.rotation(q);
            this.useLinear = true;
        }
    }

    public Transform3(Vector3 translation, Matrix3 linear, boolean useLinear) {
        this.translation = translation;
        this.linear = linear;
        this.useLinear = useLinear;
    }

    public Vector3 transform_linear(Vector3 v) {
        if (useLinear)
            return this.linear.times(v);
        else
            return v;
    }

    /**
     * Composition. New translation is set to: apply parent's linear transformation on child translation and add parent translation.
     * New linear matrix -s the product of the parent and child matrices.
     * TODO check terminology is correct
     *
     * @param p Parent component
     * @param c Child component
     */
    public static Transform3 compose(Transform3 p, Transform3 c) {
        Vector3 translation = p.transform_linear(c.translation).plus(p.translation);
        boolean useLinear = p.useLinear || c.useLinear;
        Matrix3 linear = p.linear.times(c.linear);
        return new Transform3(translation, linear, useLinear);
    }

    public Vector3 transform(Vector3 v) {
        return transform_linear(v).plus(translation);
    }

    public Transform3 inverse() {
        Matrix3 linear = this.linear.inverse();
        Vector3 translation = linear.times(this.translation.negate());
        return new Transform3(translation, linear, true);
    }

    /**
     * Rotate by x, y, and z axis.
     *
     * @param v Vector with angles per axis
     */
    public Transform3 linearRotation(Vector3 v) {
        Transform3 t = this;
        for (int i = 0; i < 3; i++) { // i stands for x,y,z axis
            if (v.v(i) != 0.0) {
                t = t.linearRotation(i, v.v(i));
            }
        }
        return t;
    }

    /**
     * Rotate around specified axis
     *
     * @param axis   0=x, 1=y, 2=z
     * @param dangle Angle of rotation
     */
    Transform3 linearRotation(int axis, double dangle) {
        return linearRotationRadians(axis, Math.toRadians(dangle));
    }

    Transform3 linearRotationRadians(int axis, double rangle) {
        Matrix3 r = Matrix3.get_rotation_matrix(axis, rangle);
        Matrix3 linear = r.times(this.linear);
        return new Transform3(this.translation, linear, true);
    }

    public Vector3Pair transform_pair(Vector3Pair p) {
        return new Vector3Pair(transform(p.v0), transform(p.v1));
    }

    public Transform3 set_translation(Vector3 translation) {
        return new Transform3(translation, linear, useLinear);
    }

    public Transform3 set_direction(Vector3 direction) {
        if (direction.x() == 0.0 && direction.y() == 0.0) {
            if (direction.z() < 0.0) {
                return new Transform3(translation, Matrix3.diag(1.0, 1.0, -1.0), true);
            } else {
                return new Transform3(translation, Matrix3.diag(1.0, 1.0, 1.0), false);
            }
        } else {
            return new Transform3(translation, Matrix3.rotation(new Quaternion(Vector3.vector3_001, direction)), true);
        }
    }

    public Vector3Pair transform_line (Vector3Pair v)
    {
        return new Vector3Pair (transform (v.origin ()),
            transform_linear (v.direction ()));
    }

    public String toString() {
        return "{translation=" + this.translation.toString()+",matrix="+this.linear+",useLinear="+this.useLinear+"}";
    }
}
