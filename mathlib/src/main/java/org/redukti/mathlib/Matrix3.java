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
        return diag(1.,1.,1.);
    }
    /* Create a diagonal matrix with given values */
    public static Matrix3 diag(double x, double y, double z) {
        return new Matrix3(
                x, 0.0, 0.0,
                0.0, y, 0.0,
                0.0, 0.0, z
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
        double n00 = 1.0 - 2.0 * (q.y * q.y + q.z * q.z);
        double n10 = 2.0 * (q.x * q.y + q.z * q.w);
        double n20 = 2.0 * (q.x * q.z - q.y * q.w);

        double n01 = 2.0 * (q.x * q.y - q.z * q.w);
        double n11 = 1.0 - 2.0 * (q.x * q.x + q.z * q.z);
        double n21 = 2.0 * (q.z * q.y + q.x * q.w);

        double n02 = 2.0 * (q.x * q.z + q.y * q.w);
        double n12 = 2.0 * (q.y * q.z - q.x * q.w);
        double n22 = 1.0 - 2.0 * (q.x * q.x + q.y * q.y);
        return new Matrix3(
                n00, n01, n02,
                n10, n11, n12,
                n20, n21, n22);
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
        double n00,n01,n02,n10,n11,n12,n20,n21,n22;
        switch (axis) {
            case 0:
                // rotation counter clockwise around the X axis
                n00 = 1;
                n01 = 0;
                n02 = 0;
                n10 = 0;
                n11 = Math.cos(angleInRadians);
                n12 = Math.sin(angleInRadians);
                n20 = 0;
                n21 = -Math.sin(angleInRadians);
                n22 = Math.cos(angleInRadians);
                break;

            case 1:
                // rotation counter clockwise around the Y axis
                n00 = Math.cos(angleInRadians);
                n01 = 0;
                n02 = -Math.sin(angleInRadians);
                n10 = 0;
                n11 = 1;
                n12 = 0;
                n20 = Math.sin(angleInRadians);
                n21 = 0;
                n22 = Math.cos(angleInRadians);
                break;

            case 2:
                // rotation counter clockwise around the Z axis
                n00 = Math.cos(angleInRadians);
                n01 = Math.sin(angleInRadians);
                n02 = 0;
                n10 = -Math.sin(angleInRadians);
                n11 = Math.cos(angleInRadians);
                n12 = 0;
                n20 = 0;
                n21 = 0;
                n22 = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid rotation axis, must be 0=x, 1=y or 2=z");
        }
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
