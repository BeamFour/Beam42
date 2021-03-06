package org.redukti.jfotoptix.sys;

import java.util.ArrayList;
import java.util.List;

public class OpticalSystem implements Container {
    private final ArrayList<Element> elements;
    private final Transform3Cache transform3Cache = new Transform3Cache();

    @Override
    public List<Element> elements() {
        return elements;
    }

    public OpticalSystem(ArrayList<Element> elements) {
        this.elements = elements;
    }

    public static class Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();

        public Builder add(Element.Builder element) {
            this.elements.add(element);
            return this;
        }

        public OpticalSystem build() {
            return new OpticalSystem(null);
        }

    }

}
