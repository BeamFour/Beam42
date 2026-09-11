package org.redukti.mathlib;

import org.junit.jupiter.api.Test;

class Matrix3Test {

    @Test
    public void testRotation2() {
        Vector3 euler1 = new Vector3(30.0, 40.0, 50.0);
        Vector3 euler2 = euler1.deg2rad();
        System.out.println(euler2);
        Matrix3 m = Matrix3.euler2mat(euler2.x, euler2.y, euler2.z);
        System.out.println(m);

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