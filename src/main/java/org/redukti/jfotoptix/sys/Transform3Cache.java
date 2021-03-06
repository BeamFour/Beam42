package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Transform3Cache {

    Map<ElementPair, Transform3> cache = new HashMap<>();

    public Transform3 get(Element from, Element to) {
        ElementPair pair = new ElementPair(from, to);
        return cache.get(pair);
    }

    public void put(Element from, Element to, Transform3 transform) {
        ElementPair pair = new ElementPair(from, to);
        cache.put(pair, transform);
    }

    static final class ElementPair {
        final Element from;
        final Element to;

        public ElementPair(Element from, Element to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElementPair that = (ElementPair) o;
            // Identity based equality
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

}
