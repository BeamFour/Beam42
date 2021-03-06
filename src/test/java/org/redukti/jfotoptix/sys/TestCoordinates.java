package org.redukti.jfotoptix.sys;

import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.curve.Flat;
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
        //s221->set_parent(g22.get());
        g1.add(g21);
        //g21->set_parent(g1.get());
        g1.add(g22);
        //g22->set_parent(g1.get());
        g21.add(s211);
        //s211->set_parent(g21.get());

        //sys.add(g1);
    }

}
