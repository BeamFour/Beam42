package org.redukti.jfotoptix.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuaternionTest {

    @Test
    public void test() {
        for (int i = 1; i <= 21; i++) {
            Matrix3 r = Matrix3.get_rotation_matrix(0, i);
            Vector3 dir = r.times(Vector3.vector3_001);
            Quaternion q = Quaternion.get_rotation_between(Vector3.vector3_001, dir);
            Matrix3 r2 = Matrix3.to_rotation_matrix(q);
            Assertions.assertTrue(r.isEquals(r2, 1e-12));
        }
    }

    // test taken from https://github.com/toji/gl-matrix
    @Test
    public void testAtRightAngle() {
        Quaternion q = Quaternion.get_rotation_between(Vector3.vector3_010, Vector3.vector3_100);
        Assertions.assertEquals(q, new Quaternion(0.0,0.0,-0.7071067811865475,0.7071067811865475));
    }
}
