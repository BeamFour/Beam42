package org.redukti.jfotoptix.math;

import org.junit.jupiter.api.Test;

public class QuaternionTest {

    @Test
    public void test() {
        Matrix3 r = Matrix3.getRotationMatrix (0, 21);
        Vector3 dir = r.times(Vector3.vector3_001);
        Quaternion q = new Quaternion(Vector3.vector3_001, dir);
        Matrix3 r2 = Matrix3.rotation(q);
        System.out.println();
    }

}
