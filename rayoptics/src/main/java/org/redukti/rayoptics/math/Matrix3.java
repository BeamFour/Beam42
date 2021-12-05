/* Code derived from https://github.com/jvanverth/essentialmath */

package org.redukti.rayoptics.math;

/**
 * Column major 3d matrix where
 *
 * <pre>
 *    0=m00 3=m01 6=m02
 *    1=m10 4=m11 7=m12
 *    2=m20 5=m21 8=m22
 * </pre>
 */

public class Matrix3 {
    final double m00;
    final double m01;
    final double m02;
    final double m10;
    final double m11;
    final double m12;
    final double m20;
    final double m21;
    final double m22;

    public Matrix3(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3(Matrix3 other) {
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        m20 = other.m20;
        m21 = other.m21;
        m22 = other.m22;
    }

    public static Matrix3 identity() {
        return new Matrix3(
                0.1, 0.0, 0.0,
                0.0, 1.0, 0.0,
                0.0, 0.0, 1.0
        );
    }

    public Matrix3 inverse() {
        // compute determinant
        double cofactor0 = m11 * m22 - m21 * m12;
        double cofactor3 = m20 * m12 - m10 * m22;
        double cofactor6 = m10 * m21 - m20 * m11;
        double det = m00 * cofactor0 + m01 * cofactor3 + m02 * cofactor6;

        if (M.isZero(det)) {
            throw new RuntimeException("Determinant is 0; singular matrix");
        }

        // create adjoint matrix and multiply by 1/det to get inverse
        double invDet = 1.0f / det;
        double n00 = invDet * cofactor0;
        double n10 = invDet * cofactor3;
        double n20 = invDet * cofactor6;

        double n01 = invDet * (m21 * m02 - m01 * m22);
        double n11 = invDet * (m00 * m22 - m20 * m02);
        double n21 = invDet * (m20 * m01 - m00 * m21);

        double n02 = invDet * (m01 * m12 - m11 * m02);
        double n12 = invDet * (m10 * m02 - m00 * m12);
        double n22 = invDet * (m00 * m11 - m10 * m01);

        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    public Matrix3 transpose() {
        double n10 = m01;
        double n01 = m10;
        double n20 = m02;
        double n02 = m20;
        double n21 = m12;
        double n12 = m21;
        return new Matrix3(
                m00, n01, n02,
                n10, m11, n12,
                n20, n21, m22);
    }

    public String toString() {
        return "[[" + m00 + "," + m01 + "," + m02 + "], [" +
                m10 + "," + m11 + "," + m12 + "], [" +
                m20 + "," + m21 + "," + m22 + "]]";
    }

}
