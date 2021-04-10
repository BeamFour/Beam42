package org.redukti.jfotoptix.math;

import org.junit.jupiter.api.Test;

public class QuaternionTest {

    @Test
    public void test() {
        Matrix3 r = Matrix3.get_rotation_matrix(0, 21);
        Vector3 dir = r.times(Vector3.vector3_001);
        Quaternion q = Quaternion.get_rotation_between(Vector3.vector3_001, dir);
        Matrix3 r2 = Matrix3.to_rotation_matrix(q);
        System.out.println(r);
        System.out.println(r2);
    }

}
