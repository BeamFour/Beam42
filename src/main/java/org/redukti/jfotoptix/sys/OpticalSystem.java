package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OpticalSystem implements Container {
    private final List<Element> elements;
    private final Transform3Cache transform3Cache;

    @Override
    public List<Element> elements() {
        return elements;
    }

    public OpticalSystem(List<Element> elements, Transform3Cache transform3Cache) {
        this.elements = elements;
        this.transform3Cache = transform3Cache;
    }

    public Element getElement(int pos) {
        if (pos >= 0 && pos < elements.size()) {
            return elements.get(pos);
        }
        return null;
    }

    public Group getGroup(int pos) {
        if (pos >= 0 && pos < elements.size() && elements.get(pos) instanceof Group) {
            return (Group)elements.get(pos);
        }
        return null;
    }

    public Transform3 getGlobalTransform(Element e) {
        return transform3Cache.get(e.id(), 0);
    }

    public Transform3 getLocalTransform(Element e) {
        return transform3Cache.get(0, e.id());
    }

    public Vector3 getAbsPosition(Element e) {
        return getGlobalTransform(e).transform(Vector3.vector3_0);
    }



    public static class Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();

        public Builder add(Element.Builder element) {
            this.elements.add(element);
            return this;
        }

        public OpticalSystem build() {
            generateIds();
            Transform3Cache transform3Cache = setCoordinates();
            List<Element> elements = buildElements();
            return new OpticalSystem(elements, transform3Cache);
        }

        private List<Element> buildElements() {
            List<Element> els = new ArrayList<>();
            for (Element.Builder e: elements) {
                els.add(e.build());
            }
            return els;
        }

        private Transform3Cache setCoordinates() {
            Transform3Cache transform3Cache = new Transform3Cache();
            List<Group.Builder> parents = new ArrayList<>();
            for (Element.Builder e: elements) {
                e.computeGlobalTransform(parents, transform3Cache);
            }
            return transform3Cache;
        }

        private void generateIds() {
            AtomicInteger id = new AtomicInteger(0);
            for (Element.Builder e: elements) {
                e.setId(id);
            }
        }

    }

}
