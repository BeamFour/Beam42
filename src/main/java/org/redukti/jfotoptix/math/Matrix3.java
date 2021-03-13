package org.redukti.jfotoptix.math;

public class Matrix3 {
    final double values[];

    private Matrix3(double[] values) {
        this.values = values;
    }

    static int idx(int row, int col) {
        return row * 3 + col;
    }

    public static Matrix3 diag(double x, double y, double z) {
        double values[] = new double[9];
        values[idx(0, 0)] = x;
        values[idx(1, 1)] = y;
        values[idx(2, 2)] = z;
        return new Matrix3(values);
    }

    // Quaternion to Rotation Matrix
    // Q = (x, y, z, w)
    // 1-2y^2-2z^2      2xy-2wz         2xz+2wy
    // 2xy+2zw          1-2x^2-2z^2     2yz-2xw
    // 2xz-2yw          2yz+2xw         1-2x^2-2y^2
    //
    // see https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation
    public static Matrix3 rotation(Quaternion q) {
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

    Vector3 times(Vector3 v) {
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            double s = 0;
            for (int k = 0; k < 3; k++) {
                s += values[idx(i, k)] * v.values[k];
            }
            r[i] = s;
        }
        return new Vector3(r[0], r[1], r[2]);
    }

    Matrix3 times(Matrix3 m) {
        double[] r = new double[9];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double s = 0;
                for (int k = 0; k < 3; k++)
                    s += this.values[idx(i, k)] * m.values[idx(k, j)];

                r[idx(i, j)] = s;
            }
        }
        return new Matrix3(r);
    }

    Matrix3 inverse() {
        // inverse = adjugate / determinant
        double s1 = values[idx(1, 1)] * values[idx(2, 2)] - values[idx(2, 1)] * values[idx(1, 2)];
        double s2 = values[idx(1, 0)] * values[idx(2, 2)] - values[idx(2, 0)] * values[idx(1, 2)];
        double s3 = values[idx(1, 0)] * values[idx(2, 1)] - values[idx(2, 0)] * values[idx(1, 1)];

        double det = values[idx(0, 0)] * s1 - values[idx(0, 1)] * s2 + values[idx(0, 2)] * s3;

        assert (det != 0.0);

        double[] r = new double[9];
        r[idx(0, 0)] = +s1 / det;
        r[idx(0, 1)]
                = -(values[idx(0, 1)] * values[idx(2, 2)] - values[idx(0, 2)] * values[idx(2, 1)]) / det;
        r[idx(0, 2)]
                = +(values[idx(0, 1)] * values[idx(1, 2)] - values[idx(0, 2)] * values[idx(1, 1)]) / det;

        r[idx(1, 0)] = -s2 / det;
        r[idx(1, 1)]
                = +(values[idx(0, 0)] * values[idx(2, 2)] - values[idx(0, 2)] * values[idx(2, 0)]) / det;
        r[idx(1, 2)]
                = -(values[idx(0, 0)] * values[idx(1, 2)] - values[idx(0, 2)] * values[idx(1, 0)]) / det;

        r[idx(2, 0)] = +s3 / det;
        r[idx(2, 1)]
                = -(values[idx(0, 0)] * values[idx(2, 1)] - values[idx(0, 1)] * values[idx(2, 0)]) / det;
        r[idx(2, 2)]
                = +(values[idx(0, 0)] * values[idx(1, 1)] - values[idx(0, 1)] * values[idx(1, 0)]) / det;

        return new Matrix3(r);
    }

    public static Matrix3 getRotationMatrix(int axis, double a) {
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
                r[idx(1, 1)] = Math.cos(a);
                r[idx(1, 2)] = Math.sin(a);
                r[idx(2, 0)] = 0;
                r[idx(2, 1)] = -Math.sin(a);
                r[idx(2, 2)] = Math.cos(a);
                break;

            case 1:
                // rotation counter clockwise around the Y axis
                r[idx(0, 0)] = Math.cos(a);
                r[idx(0, 1)] = 0;
                r[idx(0, 2)] = -Math.sin(a);
                r[idx(1, 0)] = 0;
                r[idx(1, 1)] = 1;
                r[idx(1, 2)] = 0;
                r[idx(2, 0)] = Math.sin(a);
                r[idx(2, 1)] = 0;
                r[idx(2, 2)] = Math.cos(a);
                break;

            case 2:
                // rotation counter clockwise around the Z axis
                r[idx(0, 0)] = Math.cos(a);
                r[idx(0, 1)] = Math.sin(a);
                r[idx(0, 2)] = 0;
                r[idx(1, 0)] = -Math.sin(a);
                r[idx(1, 1)] = Math.cos(a);
                r[idx(1, 2)] = 0;
                r[idx(2, 0)] = 0;
                r[idx(2, 1)] = 0;
                r[idx(2, 2)] = 1;
                break;
            default:
                throw new IllegalArgumentException();
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
                sb.append(values[idx(i, j)]);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
