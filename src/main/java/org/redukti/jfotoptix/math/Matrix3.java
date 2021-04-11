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

/**
 * 3D Matrix class - immutable implementation.
 */
public class Matrix3 {

    /* row major storage for 3d matrix */
    final double _values[];

    private Matrix3(double[] values) {
        this._values = values;
    }

    static int idx(int row, int col) {
        return row * 3 + col;
    }

    /* Create a diagonal matrix with given values */
    public static Matrix3 diag(double x, double y, double z) {
        double values[] = new double[9];
        values[idx(0, 0)] = x;
        values[idx(1, 1)] = y;
        values[idx(2, 2)] = z;
        return new Matrix3(values);
    }

    /*
    // Quaternion to Rotation Matrix
    // Q = (x, y, z, w)
    // 1-2y^2-2z^2      2xy-2wz         2xz+2wy
    // 2xy+2zw          1-2x^2-2z^2     2yz-2xw
    // 2xz-2yw          2yz+2xw         1-2x^2-2y^2
    //
    // see https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
    */
    public static Matrix3 to_rotation_matrix(Quaternion q) {
        double values[] = new double[9];
        values[idx(0, 0)] = 1.0 - 2.0 * (q.y * q.y + q.z * q.z);
        values[idx(1, 0)] = 2.0 * (q.x * q.y + q.z * q.w);
        values[idx(2, 0)] = 2.0 * (q.x * q.z - q.y * q.w);

        values[idx(0, 1)] = 2.0 * (q.x * q.y - q.z * q.w);
        values[idx(1, 1)] = 1.0 - 2.0 * (q.x * q.x + q.z * q.z);
        values[idx(2, 1)] = 2.0 * (q.z * q.y + q.x * q.w);

        values[idx(0, 2)] = 2.0 * (q.x * q.z + q.y * q.w);
        values[idx(1, 2)] = 2.0 * (q.y * q.z - q.x * q.w);
        values[idx(2, 2)] = 1.0 - 2.0 * (q.x * q.x + q.y * q.y);
        return new Matrix3(values);
    }

    /**
     * Get a rotation matrix that describes the rotation of vector a
     * to obtain vector 3.
     * @param from Unit vector
     * @param to Unit vector
     */
    public static Matrix3 get_rotation_between(Vector3 from, Vector3 to) {
        // Do not know the source of following equation
        // Believe it generates a Quaternion representing the rotation
        // of vector a to vector b
        // Closest match of the algo:
        // https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
        Quaternion q = Quaternion.get_rotation_between(from, to);
        return to_rotation_matrix(q);
    }

    /** Matrix times vector */
    public final Vector3 times(Vector3 v) {
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            double s = 0;
            for (int k = 0; k < 3; k++) {
                s += _values[idx(i, k)] * v.values[k];
            }
            r[i] = s;
        }
        return new Vector3(r[0], r[1], r[2]);
    }

    /** Martrix times matrix */
    public final Matrix3 times(Matrix3 m) {
        double[] r = new double[9];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double s = 0;
                for (int k = 0; k < 3; k++)
                    s += this._values[idx(i, k)] * m._values[idx(k, j)];

                r[idx(i, j)] = s;
            }
        }
        return new Matrix3(r);
    }

    /** Matrix inverse */
    public final Matrix3 inverse() {
        // inverse = adjugate / determinant
        double s1 = _values[idx(1, 1)] * _values[idx(2, 2)] - _values[idx(2, 1)] * _values[idx(1, 2)];
        double s2 = _values[idx(1, 0)] * _values[idx(2, 2)] - _values[idx(2, 0)] * _values[idx(1, 2)];
        double s3 = _values[idx(1, 0)] * _values[idx(2, 1)] - _values[idx(2, 0)] * _values[idx(1, 1)];

        double det = _values[idx(0, 0)] * s1 - _values[idx(0, 1)] * s2 + _values[idx(0, 2)] * s3;

        assert (det != 0.0);

        double[] r = new double[9];
        r[idx(0, 0)] = +s1 / det;
        r[idx(0, 1)]
                = -(_values[idx(0, 1)] * _values[idx(2, 2)] - _values[idx(0, 2)] * _values[idx(2, 1)]) / det;
        r[idx(0, 2)]
                = +(_values[idx(0, 1)] * _values[idx(1, 2)] - _values[idx(0, 2)] * _values[idx(1, 1)]) / det;

        r[idx(1, 0)] = -s2 / det;
        r[idx(1, 1)]
                = +(_values[idx(0, 0)] * _values[idx(2, 2)] - _values[idx(0, 2)] * _values[idx(2, 0)]) / det;
        r[idx(1, 2)]
                = -(_values[idx(0, 0)] * _values[idx(1, 2)] - _values[idx(0, 2)] * _values[idx(1, 0)]) / det;

        r[idx(2, 0)] = +s3 / det;
        r[idx(2, 1)]
                = -(_values[idx(0, 0)] * _values[idx(2, 1)] - _values[idx(0, 1)] * _values[idx(2, 0)]) / det;
        r[idx(2, 2)]
                = +(_values[idx(0, 0)] * _values[idx(1, 1)] - _values[idx(0, 1)] * _values[idx(1, 0)]) / det;

        return new Matrix3(r);
    }

    /** Get rotation matrix for rotation about axis.
     * @param axis the axis of rotation, x=0, y=1, z=2
     * @param angleInRadians the angle to rotate in radians
     */
    public static Matrix3 get_rotation_matrix(int axis, double angleInRadians) {
        assert (axis < 3 && axis >= 0);

        /*
         * Note on convention used below.
         *
         * See https://mathworld.wolfram.com/RotationMatrix.html
         * coordinate system rotations of the x-, y-, and z-axes in a
         * counterclockwise direction when looking towards the origin give the
         * matrices.
         *
         * This appears to correspond to xyz convention described in appendix A,
         * Classical Mechanics, Goldstein, 3rd Ed. 'It appears that most U.S. and
         * British aerodynamicists and pilots prefer the sequence in which the first
         * rotation is the yaw angle (phi) about a z-axis, the second is the pitch
         * angle (theta) about an intermediary y-axis, and the third is a bank or
         * roll angle (psi) about the final x-axis.'
         *
         * Also see https://youtu.be/wg9bI8-Qx2Q
         */
        double[] r = new double[9];
        switch (axis) {
            case 0:
                // rotation counter clockwise around the X axis
                r[idx(0, 0)] = 1;
                r[idx(0, 1)] = 0;
                r[idx(0, 2)] = 0;
                r[idx(1, 0)] = 0;
                r[idx(1, 1)] = Math.cos(angleInRadians);
                r[idx(1, 2)] = Math.sin(angleInRadians);
                r[idx(2, 0)] = 0;
                r[idx(2, 1)] = -Math.sin(angleInRadians);
                r[idx(2, 2)] = Math.cos(angleInRadians);
                break;

            case 1:
                // rotation counter clockwise around the Y axis
                r[idx(0, 0)] = Math.cos(angleInRadians);
                r[idx(0, 1)] = 0;
                r[idx(0, 2)] = -Math.sin(angleInRadians);
                r[idx(1, 0)] = 0;
                r[idx(1, 1)] = 1;
                r[idx(1, 2)] = 0;
                r[idx(2, 0)] = Math.sin(angleInRadians);
                r[idx(2, 1)] = 0;
                r[idx(2, 2)] = Math.cos(angleInRadians);
                break;

            case 2:
                // rotation counter clockwise around the Z axis
                r[idx(0, 0)] = Math.cos(angleInRadians);
                r[idx(0, 1)] = Math.sin(angleInRadians);
                r[idx(0, 2)] = 0;
                r[idx(1, 0)] = -Math.sin(angleInRadians);
                r[idx(1, 1)] = Math.cos(angleInRadians);
                r[idx(1, 2)] = 0;
                r[idx(2, 0)] = 0;
                r[idx(2, 1)] = 0;
                r[idx(2, 2)] = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid rotation axis, must be 0=x, 1=y or 2=z");
        }
        return new Matrix3(r);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('[');
            for (int j = 0; j < 3; j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append(_values[idx(i, j)]);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }

    public final boolean isEquals(Matrix3 other, double tolerance) {
        for (int i = 0; i < _values.length; i++) {
            if (Math.abs(_values[i]-other._values[i]) > tolerance)
                return false;
        }
        return true;
    }
}
