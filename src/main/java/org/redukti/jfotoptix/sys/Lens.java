package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;

public class Lens extends Group {

    public Lens(int id, Vector3Pair position, Transform3 transform, List<Element> elementList) {
        super(id, position, transform, elementList);
    }

    public static class Builder extends Group.Builder {
        @Override
        public Element build() {
            return new Lens(id, position, transform, getElements());
        }
    }
}
