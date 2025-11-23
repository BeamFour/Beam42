package org.redukti.mathlib;

public class Matrix2 {

    double[][] rows;

    public Matrix2(double v0, double v1, double v2, double v3) {
        rows = new double[2][2];
        rows[0][0] = v0;
        rows[0][1] = v1;
        rows[1][0] = v2;
        rows[1][1] = v3;
    }

    private Matrix2(double[][] values) {
        rows = values;
    }

    public Matrix2 multiply(Matrix2 m) {
        double[][] result = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double sum = 0;
                for (int k = 0; k < 2; k++) {
                    sum += rows[i][k] * m.rows[k][j];
                }
                result[i][j] = sum;
            }
        }
        return new Matrix2(result);
    }

    public Vector2 multiply(Vector2 v) {
        return new Vector2(rows[0][0] * v.x + rows[0][1] * v.y,
                              rows[1][0] * v.x + rows[1][1] * v.y);
    }
}
