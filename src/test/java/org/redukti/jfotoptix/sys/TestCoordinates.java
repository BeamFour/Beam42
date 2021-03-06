package org.redukti.jfotoptix.sys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;
import org.redukti.jfotoptix.shape.Infinite;

public class TestCoordinates {

    @Test
    public void testBasics() {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();

        Group.Builder g1 = new Group.Builder().position(Vector3Position.position_000_001);
        Group.Builder g21 = new Group.Builder().position(Vector3Position.position_000_001);
        Group.Builder g22 = new Group.Builder().position(Vector3Position.position_000_001);

        Surface.Builder s211 = new Surface.Builder().position(Vector3Position.position_000_001)
                .curve(Flat.flat)
                .shape(Infinite.infinite);
        Surface.Builder s221 = new Surface.Builder().position(Vector3Position.position_000_001)
                .curve(Flat.flat)
                .shape(Infinite.infinite);

        g22.add(s221);
        g1.add(g21);
        g1.add(g22);
        g21.add(s211);
        sys.add(g1);

        {
            OpticalSystem system1 = sys.build();
            Element s211e = system1.getGroup(0).getGroup(0).getSurface(0);
            Assertions.assertTrue(s211e.getLocalPosition().isEqual(Vector3.vector3_0, 1e-10));
            Assertions.assertTrue(system1.getAbsPosition(s211e).isEqual(Vector3.vector3_0, 1e-10));
        }

        g21.localPosition(new Vector3(1, 2, 3));

        {
            OpticalSystem system1 = sys.build();
            Element s211e = system1.getGroup(0).getGroup(0).getSurface(0);
            Assertions.assertTrue(s211e.getLocalPosition().isEqual(Vector3.vector3_0, 1e-10));
            Assertions.assertTrue(system1.getAbsPosition(s211e).isEqual(new Vector3(1,2,3), 1e-10));
        }

        g1.localPosition(new Vector3(3, 2, 1));

        {
            OpticalSystem system1 = sys.build();
            Element s211e = system1.getGroup(0).getGroup(0).getSurface(0);
            Assertions.assertTrue(system1.getAbsPosition(s211e).isEqual(new Vector3(4,4,4), 1e-10));
        }

        s211.localPosition(new Vector3(7, 7, 7));

        {
            OpticalSystem system1 = sys.build();
            Element s211e = system1.getGroup(0).getGroup(0).getSurface(0);
            Assertions.assertTrue(system1.getAbsPosition(s211e).isEqual(new Vector3(11, 11, 11), 1e-10));
        }

//        s211.set_position(math::Vector3(9, 9, 9));
//
//        if (!COMPARE_VECTOR3(s211->get_position(), math::Vector3(9, 9, 9)))
//        fail(__LINE__);
//
//        if (!COMPARE_VECTOR3(s211->get_local_position(), math::Vector3(5, 5, 5)))
//        fail(__LINE__);

    }

}
