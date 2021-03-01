package org.redukti.jfotopix.sys;

import org.redukti.jfotopix.math.Transform3;
import org.redukti.jfotopix.math.Vector3Pair;

public abstract class Element {

    OpticalSystem system;
    Group group;
    Transform3 transform;

    public Element(Vector3Pair p) {
        this.transform = new Transform3(p.translation(), p.direction());
    }

}
