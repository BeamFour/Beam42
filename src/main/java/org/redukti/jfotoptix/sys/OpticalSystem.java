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

    public Vector3 getPosition(Element e) {
        return transform3Cache.getLocal2GlobalTransform(e.id()).transform(Vector3.vector3_0);
    }

    public static class Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();
        private Transform3Cache transform3Cache;

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
            transform3Cache = new Transform3Cache();
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

        /**
         * Sets element position using global coordinate system
         * Needs a prior call to build so we have the transformations needed
         */
        public OpticalSystem updatePosition(Element.Builder e, Vector3 v) {
            // FIXME
            if (transform3Cache == null)
                throw new IllegalStateException("build() must be called prior to updating position");
            if (e.parent != null) {
                e.localPosition(transform3Cache.getGlobal2LocalTransform(e.parent.id()).transform(v));
            }
            else {
                e.localPosition(v);
            }
            return build();
        }

    }

}
