package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Vector3Position;

import java.util.ArrayList;
import java.util.List;

public class Group extends Element implements Container {
    private final ArrayList<Element> elements = new ArrayList<>();

    public Group(OpticalSystem opticalSystem, Group group, Vector3Position p) {
        super(opticalSystem, group, p);
    }

    @Override
    public List<Element> elements() {
        return elements;
    }

    public static class Builder extends Element.Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();

        public Group build() {
            return new Group(opticalSystem.build(), group.build(), position);
        }

        public Group.Builder system(OpticalSystem.Builder system) {
            return (Builder) super.system(system);
        }

        public Group.Builder group(Group.Builder group) {
            return (Builder) super.group(group);
        }

        public Group.Builder position(Vector3Position position) {
            return (Builder) super.position(position);
        }

        public Group.Builder add(Element.Builder element) {
            this.elements.add(element);
            element.group(this);
            return this;
        }
    }

}
