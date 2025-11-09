/* Code derived from https://github.com/jvanverth/essentialmath */
// Portions Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar

package org.redukti.mathlib;

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

    public static final Matrix3 IDENTITY = Matrix3.identity();

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
                1.0, 0.0, 0.0,
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

//    /**
//     * Create rotation matrix
//     *
//     * @param zRotation degree in radians
//     * @param yRotation degree in radians
//     * @param xRotation degree in radians
//     * @return Rotation matrix
//     */
//    public static Matrix3 rotation(double zRotation, double yRotation, double xRotation) {
//        double Cx = Math.cos(xRotation);
//        double Sx = Math.sin(xRotation);
//
//        double Cy = Math.cos(yRotation);
//        double Sy = Math.sin(yRotation);
//
//        double Cz = Math.cos(zRotation);
//        double Sz = Math.sin(zRotation);
//
//        double n00 = (Cy * Cz);
//        double n01 = -(Cy * Sz);
//        double n02 = Sy;
//
//        double n10 = (Sx * Sy * Cz) + (Cx * Sz);
//        double n11 = -(Sx * Sy * Sz) + (Cx * Cz);
//        double n12 = -(Sx * Cy);
//
//        double n20 = -(Cx * Sy * Cz) + (Sx * Sz);
//        double n21 = (Cx * Sy * Sz) + (Sx * Cz);
//        double n22 = (Cx * Cy);
//
//        return new Matrix3(
//                n00, n01, n02,
//                n10, n11, n12,
//                n20, n21, n22);
//    }

    public static Matrix3 euler2mat(double roll_angle, double pitch_angle, double yaw_angle) {
        double si = Math.sin(roll_angle),
                sj = Math.sin(pitch_angle),
                sk = Math.sin(yaw_angle);
        double ci = Math.cos(roll_angle),
                cj = Math.cos(pitch_angle),
                ck = Math.cos(yaw_angle);
        double cc = ci * ck, cs = ci * sk;
        double sc = si * ck, ss = si * sk;

        // gamma (roll)
        // beta (pitch)
        // alpha (yaw)
        // see https://en.wikipedia.org/wiki/Rotation_matrix#In_three_dimensions
        // More formally, it is an intrinsic rotation whose Tait–Bryan angles are α, β, γ, about axes z, y, x, respectively.
        // The formula below corresponds to yaw.multiply(pitch.multiply(roll))
        // which means roll followed by pitch followed by yaw

        double n00 = cj * ck;    // Cos(y) * Cos(z)
        double n01 = sj * sc - cs; // Sin(y) * Sin(x) * Cos(z) - Cos(x) * Sin(z)
        double n02 = sj * cc + ss; // Sin(y) * Cos(x) * Cos(z) + Sin(x) * Sin(z)
        double n10 = cj * sk;    // Cos(y) * Sin(z)
        double n11 = sj * ss + cc; // Sin(y) * Sin(x) * Sin(z) + Cos(x) * Cos(z)
        double n12 = sj * cs - sc; // Sin(y) * Cos(x) * Sin(z) - Sin(x) * Cos(z)
        double n20 = -sj;      // -Sin(y)
        double n21 = cj * si;    // Cos(y) * Sin(x)
        double n22 = cj * ci;    // Cos(y) * Cos(x)

        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    public static Matrix3 euler2mat(Vector3 euler) {
        return euler2mat(euler.x, euler.y, euler.z);
    }

    /**
     * rotate around vert axis
     */
    public static Matrix3 yaw(double angle) {
        double sine_theta = Math.sin(angle),
                cos_theta = Math.cos(angle);

        double n00 = cos_theta;
        double n10 = sine_theta;
        double n20 = 0.0f;
        double n01 = -sine_theta;
        double n11 = cos_theta;
        double n21 = 0.0f;
        double n02 = 0.0f;
        double n12 = 0.0f;
        double n22 = 1.0f;
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    /**
     * rotate around sideways axis - i.e. tilt
     */
    public static Matrix3 pitch(double angle) {
        double sine_theta = Math.sin(angle),
                cos_theta = Math.cos(angle);

        double n00 = cos_theta;
        double n10 = 0.0f;
        double n20 = -sine_theta;
        double n01 = 0.0f;
        double n11 = 1.0f;
        double n21 = 0.0f;
        double n02 = sine_theta;
        double n12 = 0.0f;
        double n22 = cos_theta;
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    /**
     * Rotate around forward axis, i.e. turn
     */
    public static Matrix3 roll(double angle) {
        double sine_theta = Math.sin(angle),
                cos_theta = Math.cos(angle);

        double n00 = 1.0f;
        double n10 = 0.0f;
        double n20 = 0.0f;
        double n01 = 0.0f;
        double n11 = cos_theta;
        double n21 = sine_theta;
        double n02 = 0.0f;
        double n12 = -sine_theta;
        double n22 = cos_theta;
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    /**
     * rotate v1 into v2 using equivalent angle rotation.
     *
     * Compute a rotation matrix from v1 to v2.
     * Take the cross product of the input vectors to get
     * the rotation axis. The eqivalent angle rotation is
     * equation 2.80 from Introduction to Robotics, 2nd ed, by John J Craig.
     */
    public static Matrix3 rot_v1_into_v2(Vector3 v1, Vector3 v2) {
        var rot_axis = v1.cross(v2).negate();
        var s = rot_axis.length();
        var cosine_ang = v1.dot(v2);
        var c = cosine_ang;
        var v = 1.0 - cosine_ang;
        var ax = rot_axis.normalize();
        var n00 = ax.x*ax.x*v + c;
        var n01 = ax.x*ax.y*v - ax.z*s;
        var n02 = ax.x*ax.z*v + ax.y*s;
        var n10 = ax.x*ax.y*v + ax.z*s;
        var n11 = ax.y*ax.y*v + c;
        var n12 = ax.y*ax.z*v + ax.x*s;
        var n20 = ax.x*ax.z*v + ax.y*s;
        var n21 = ax.y*ax.z*v + ax.x*s;
        var n22 = ax.z*ax.z*v + c;
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    public Matrix3 multiply(Matrix3 other) {
        double n00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20;
        double n10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20;
        double n20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20;
        double n01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21;
        double n11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21;
        double n21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21;
        double n02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22;
        double n12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22;
        double n22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22;
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
    }

    public Vector3 multiply(Vector3 other) {
        double x = m00 * other.x + m01 * other.y + m02 * other.z;
        double y = m10 * other.x + m11 * other.y + m12 * other.z;
        double z = m20 * other.x + m21 * other.y + m22 * other.z;
        return new Vector3(x,y,z);
    }

    public String toString() {
        return "[[" + m00 + "," + m01 + "," + m02 + "],\n [" +
                m10 + "," + m11 + "," + m12 + "],\n [" +
                m20 + "," + m21 + "," + m22 + "]]";
    }

}
