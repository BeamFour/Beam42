package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Position;

import java.util.List;

public class Lens extends Group {

    public Lens(Vector3Position position, Transform3 transform, List<Element> elementList) {
        super(position, transform, elementList);
    }

    public static class Builder extends Group.Builder {
        @Override
        public Element build() {
            return new Lens(position, transform, getElements());
        }
    }
}
