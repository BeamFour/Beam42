package org.redukti.jfotoptix.curve;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

public class TestAsphere {

    @Test
    public void test1() {

        Asphere surface = new Asphere(1.0 / 0.25284872, 1.0, -0.005,
                0.00001, -0.0000005, 0, 0, 0, 0, 0, 0, false);

        //  auto surface = std::make_shared<goptical::curve::Sphere> (1.0 /
        //  0.25284872);
        Vector3 pos_dir = new Vector3(0.0, 0.1736, 0.98481625);
        Vector3 origin = new Vector3(1.48, 0.0, 0.0);
        Vector3Pair ray = new Vector3Pair(origin, pos_dir);
        Vector3 point = new Vector3(0, 0, 0);
        point = surface.intersect(ray);

        // Vector3 pos_dir2 (0.98481625, 0.1736, 0.0);
        // Vector3 origin2 (0, 0.0, 1.48);
        Vector3Pair pointAndNormal = Asphere.compute_intersection(origin, pos_dir, surface);
        Vector3 point2 = pointAndNormal.point();
        Vector3 normal2 = pointAndNormal.normal();

        Assertions.assertEquals(point.x(), point2.x(), 1e-10);
        Assertions.assertEquals(point.y(), point2.y(), 1e-10);
        Assertions.assertEquals(point.z(), point2.z(), 1e-10);

    }

    @Test
    public void test2() {
        Asphere surface = new Asphere(108.488, 1.0, -3.82177e-07,
                -6.06486e-11, -3.80172e-15, -1.32266e-18, 0, 0, 0, 0,0, true);
        Vector3 pos_dir = new Vector3(0.0, 0.34169210791780597, 0.9398119510767493);
        Vector3 origin = new Vector3(0.0, -3.6357497887098503E9, -1.0E10);
        Vector3Pair ray = new Vector3Pair(origin, pos_dir);
        Vector3 point = surface.intersect(ray);

        System.out.println(point);
    }
}
