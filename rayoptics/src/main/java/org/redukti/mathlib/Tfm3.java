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


package org.redukti.mathlib;

public class Tfm3 {

    public final Vector3 translation;
    /* Rotation matrix to rotate a unit vector toward z to the required direction */
    public final Matrix3 rotation_matrix;
    /** Whether to use rotation matrix */
    public final boolean use_rotation_matrix;

    public Tfm3() {
        this.rotation_matrix = Matrix3.diag(1.0, 1.0, 1.0);
        this.use_rotation_matrix = false;
        this.translation = Vector3.vector3_0;
    }

    public Tfm3(Vec3Pair position) {
        this.translation = position.point();
        if (position.direction().x() == 0 && position.direction().y() == 0) {
            if (position.direction().z() < 0.0) {
                this.rotation_matrix = Matrix3.diag(1.0, 1.0, -1.0);
                this.use_rotation_matrix = true;
            } else {
                this.rotation_matrix = Matrix3.diag(1.0, 1.0, 1.0);
                this.use_rotation_matrix = false;
            }
        } else {
            // Get a rotation matrix representing the rotation of unit vector in z
            // to the direction vector.
            this.rotation_matrix = Matrix3.get_rotation_between(Vector3.vector3_001, position.direction());
            this.use_rotation_matrix = true;
        }
    }

    public Tfm3(Vector3 translation, Matrix3 rotation_matrix, boolean use_rotation_matrix) {
        this.translation = translation;
        this.rotation_matrix = rotation_matrix;
        this.use_rotation_matrix = use_rotation_matrix;
    }

    /**
     * Apply this transforms rotation to given vector
     */
    public final Vector3 apply_rotation(Vector3 v) {
        if (use_rotation_matrix)
            return this.rotation_matrix.multiply(v);
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
    public static Tfm3 compose(Tfm3 p, Tfm3 c) {
        Vector3 translation = p.apply_rotation(c.translation).plus(p.translation);
        boolean use_rotation_matrix = p.use_rotation_matrix || c.use_rotation_matrix;
        Matrix3 rotation_matrix = p.rotation_matrix.multiply(c.rotation_matrix);
        return new Tfm3(translation, rotation_matrix, use_rotation_matrix);
    }

    /**
     * Transform given vector - vector is rotated and then translated.
     */
    public final Vector3 transform(Vector3 v) {
        return apply_rotation(v).plus(translation);
    }

    /**
     * Create an inverse of the transform
     */
    public final Tfm3 inverse() {
        Matrix3 rotation_matrix = this.rotation_matrix.inverse();
        Vector3 translation = rotation_matrix.multiply(this.translation.negate());
        return new Tfm3(translation, rotation_matrix, true);
    }

    /**
     * Rotate by x, y, and z axis.
     *
     * @param v Vector with angles per axis
     */
    public Tfm3 rotate_axis_by_angles(Vector3 v) {
        Tfm3 t = this;
        for (int i = 0; i < 3; i++) { // i stands for x,y,z axis
            if (v.v(i) != 0.0) {
                t = t.rotate_axis_by_angle(i, v.v(i));
            }
        }
        return t;
    }

    /**
     * Rotate around specified axis
     *
     * @param axis   0=x, 1=y, 2=z
     * @param dangle Angle of rotation in degrees
     */
    public final Tfm3 rotate_axis_by_angle(int axis, double dangle) {
        return rotate_axis_by_radian(axis, Math.toRadians(dangle));
    }

    /**
     * Rotate around specified axis
     *
     * @param axis   0=x, 1=y, 2=z
     * @param rangle Angle of rotation in radians
     */
    public final Tfm3 rotate_axis_by_radian(int axis, double rangle) {
        Matrix3 r = Matrix3.get_rotation_matrix(axis, rangle);
        r = r.multiply(this.rotation_matrix);
        return new Tfm3(this.translation, r, true);
    }

    public Vec3Pair transform_pair(Vec3Pair p) {
        return new Vec3Pair(transform(p.v0), transform(p.v1));
    }

    public Tfm3 set_translation(Vector3 translation) {
        return new Tfm3(translation, rotation_matrix, use_rotation_matrix);
    }

    public Tfm3 set_direction(Vector3 direction) {
        if (direction.x() == 0.0 && direction.y() == 0.0) {
            if (direction.z() < 0.0) {
                return new Tfm3(translation, Matrix3.diag(1.0, 1.0, -1.0), true);
            } else {
                return new Tfm3(translation, Matrix3.diag(1.0, 1.0, 1.0), false);
            }
        } else {
            return new Tfm3(translation, Matrix3.get_rotation_between(Vector3.vector3_001, direction), true);
        }
    }

    public Vec3Pair transform_line (Vec3Pair v)
    {
        return new Vec3Pair(transform (v.origin ()), apply_rotation(v.direction ()));
    }

    public String toString() {
        return "{translation=" + this.translation.toString()+",rmat="+this.rotation_matrix +",use_rmat="+this.use_rotation_matrix +"}";
    }
}
