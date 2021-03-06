package org.redukti.jfotoptix.sys;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OpticalSystem implements Container {
    private final ArrayList<Element> elements;
    private final Transform3Cache transform3Cache;

    @Override
    public List<Element> elements() {
        return elements;
    }

    public OpticalSystem(ArrayList<Element> elements, Transform3Cache transform3Cache) {
        this.elements = elements;
        this.transform3Cache = transform3Cache;
    }

    public static class Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();
        private Transform3Cache transform3Cache = new Transform3Cache();

        public Builder add(Element.Builder element) {
            this.elements.add(element);
            return this;
        }

        public OpticalSystem build() {
            generateIds();
            setCoordinates();
            ArrayList<Element> elements = buildElements();
            return null;
        }

        private ArrayList<Element> buildElements() {
            ArrayList<Element> els = new ArrayList<>();
            for (Element.Builder e: elements) {
                els.add(e.build());
            }
            return els;
        }


        private void setCoordinates() {
            transform3Cache = new Transform3Cache();
            List<Group.Builder> parents = new ArrayList<>();
            for (Element.Builder e: elements) {
                e.computeGlobalTransform(parents, transform3Cache);
            }
        }

        private void generateIds() {
            AtomicInteger id = new AtomicInteger(0);
            for (Element.Builder e: elements) {
                e.setId(id);
            }
        }

    }

}
