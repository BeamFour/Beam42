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

    Vector3 multiply(Vector3 v) {
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

    Matrix3 multiply(Matrix3 m) {
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
}
