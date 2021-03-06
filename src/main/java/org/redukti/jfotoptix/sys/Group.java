package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Group extends Element implements Container {
    private final List<Element> elements;

    public Group(Vector3Position p, Transform3 transform3, List<Element> elements) {
        super(p, transform3);
        this.elements = elements;
    }

    @Override
    public List<Element> elements() {
        return elements;
    }

    public static class Builder extends Element.Builder {
        protected final ArrayList<Element.Builder> elements = new ArrayList<>();

        public Group.Builder position(Vector3Position position) {
            return (Builder) super.position(position);
        }

        public Group.Builder add(Element.Builder element) {
            this.elements.add(element);
            return this;
        }

        public Group.Builder setId(AtomicInteger id) {
            this.id = id.incrementAndGet();
            for (Element.Builder e: elements) {
                e.setId(id);
            }
            return this;
        }

        @Override
        public void computeGlobalTransform(List<Builder> parents, Transform3Cache tcache) {
            List<Builder> list = new ArrayList<>(parents);
            list.add(this);
            for (Element.Builder e: elements) {
                e.computeGlobalTransform(list, tcache);
            }
        }

        @Override
        public Element build() {
            return new Group(position, transform, getElements());
        }

        protected ArrayList<Element> getElements() {
            ArrayList<Element> myels = new ArrayList<>();
            for (Element.Builder e: elements) {
                myels.add(e.build());
            }
            return myels;
        }
    }

}
