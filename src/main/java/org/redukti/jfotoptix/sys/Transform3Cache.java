package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Transform3Cache {

    Map<ElementPair, Transform3> cache = new HashMap<>();

    public Transform3 get(int from, int to) {
        ElementPair pair = new ElementPair(from, to);
        return cache.get(pair);
    }

    private void put(int from, int to, Transform3 transform) {
        ElementPair pair = new ElementPair(from, to);
        cache.put(pair, transform);
    }

    public void putLocal2GlobalTransform(int id, Transform3 t) {
        put(id, 0, t);
    }

    public Transform3 getLocal2GlobalTransform(int id) {
        return get(id, 0);
    }

    public void putGlobal2LocalTransform(int id, Transform3 t) {
        put(0, id, t);
    }

    public Transform3 getGlobal2LocalTransform(int id) {
        return get(0, id);
    }

    public Transform3 transform_cache_update(int from, int to) {
        assert (from != to);
        Transform3 e = get(from, to);
        if (e == null) {
            Transform3 t1 = getLocal2GlobalTransform(from);
            Transform3 t2 = getLocal2GlobalTransform(to);
            e = Transform3.compose(t1, t2.inverse());
            put(from, to, e);
        }
        return e;
    }

    static final class ElementPair {
        final int from;
        final int to;

        public ElementPair(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElementPair that = (ElementPair) o;
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

}
