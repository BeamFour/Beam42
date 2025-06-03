package org.redukti.mathlib;

import org.junit.jupiter.api.Test;

class Matrix3Test {

    @Test
    public void testInverse() {
        Matrix3 m = new Matrix3(
                0.50362327, 0.49201708, 0.31560728,
                0.76278702, 0.90001429, 0.66901699,
                0.18806893, 0.54642545, 0.07530182);
        Matrix3 inverse = m.inverse();
        // [[7.7945543408888565,-3.5441470288658663,-1.1808946579357469],
        // [-1.7898464419918447,0.5609702645807123,2.5177336920333677],
        // [-6.479203918597579,4.780966917152172,-2.0406435181620153]]
//        [[ 7.79455398, -3.54414685, -1.18089458],
//       [-1.78984638,  0.56097025,  2.51773367],
//       [-6.47920352,  4.78096669, -2.04064356]]
        System.out.println(m.transpose());
        System.out.println(inverse);
    }

//    @Test
//    public void testRotation() {
//        Vector3 euler1 = new Vector3(30.0, 40.0, 50.0);
//        Vector3 euler2 = euler1.deg2rad();
//        System.out.println(euler2);
//        Matrix3 m = Matrix3.rotation(euler2.z, euler2.y, euler2.x);
//        System.out.println(m);
//
//        Matrix3 y_rot = Matrix3.pitch(euler2.y);
//        Matrix3 x_rot = Matrix3.roll(euler2.x);
//        Matrix3 z_rot = Matrix3.yaw(euler2.z);
//
//        Matrix3 m2 = x_rot.multiply(y_rot.multiply(z_rot));
//        System.out.println(m2);
//
//        Vector3 rotated = m.multiply(new Vector3(1,1,1));
//        System.out.println(rotated);
//        Vector3 rotationReversed = m.transpose().multiply(rotated);
//        System.out.println(rotationReversed);
//    }

    @Test
    public void testRotation2() {
        Vector3 euler1 = new Vector3(30.0, 40.0, 50.0);
        Vector3 euler2 = euler1.deg2rad();
        System.out.println(euler2);
        Matrix3 m = Matrix3.euler2mat(euler2.x, euler2.y, euler2.z);
        System.out.println(m);

        Matrix3 pitch = Matrix3.pitch(euler2.y);
        Matrix3 roll = Matrix3.roll(euler2.x);
        Matrix3 yaw = Matrix3.yaw(euler2.z);

        Matrix3 m2 = yaw.multiply(pitch.multiply(roll));
        System.out.println(m2);

        System.out.println(m.multiply(new Vector3(1,1,1)).normalize());
    }

    @Test
    public void testMatrixDot() {
        Matrix3 M = new Matrix3(
                0.17515668, 0.73612839, 0.85686732,
                0.26718692, 0.85310982, 0.86961188,
                0.49203219, 0.84370621, 0.50712345);
        Vector3 V = new Vector3(0.38998996, 0.91619505, 0.8556259);
        System.out.println(M.multiply(V));
        //>>> M.dot(V)
        //array([1.47590441, 1.62987766, 1.39879503])
        Matrix3 M2 = new Matrix3(
                0.09170845, 0.10099766, 0.95648792,
                0.87324276, 0.26369822, 0.23328569,
                0.96453837, 0.24997789, 0.72227048);
        System.out.println(M.multiply(M2));
        //>>> M.dot(M2)
        //array([[1.48536355, 0.42600405, 0.95815345],
        //       [1.6082493 , 0.46933255, 1.08267437],
        //       [1.27102388, 0.39894758, 1.03372773]])
    }
}