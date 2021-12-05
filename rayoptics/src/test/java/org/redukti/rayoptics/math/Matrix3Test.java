package org.redukti.rayoptics.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}