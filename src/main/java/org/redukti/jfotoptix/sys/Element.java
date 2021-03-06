package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Element {

    final int id;
    final Vector3Position position;
    final Transform3 transform;

    public Element(int id, Vector3Position p, Transform3 transform) {
        this.id = id;
        this.position = p;
        this.transform = transform;
    }

    public Vector3 getLocalPosition() {
        return this.transform.translation;
    }
    public int id() { return id; }

    public static abstract class Builder {
        int id;
        Vector3Position position;
        Transform3 transform;
        Element.Builder parent;

        public Builder position(Vector3Position position) {
            this.position = position;
            this.transform = new Transform3(position);
            return this;
        }

        public Builder localPosition(Vector3 v) {
            this.transform = new Transform3(v, this.transform.linear, this.transform.useLinear);
            return this;
        }

        public Builder parent(Element.Builder parent) {
            this.parent = parent;
            return this;
        }

        public Builder setId(AtomicInteger id) {
            this.id = id.incrementAndGet();
            return this;
        }

        public int id() { return id;}

        public void computeGlobalTransform(List<Group.Builder> parents, Transform3Cache tcache) {
            Transform3 t = transform; // local transform
            for (int i = parents.size()-1; i >= 0; i--) {
                Group.Builder g = parents.get(i);
                t = Transform3.compose(t, g.transform);
            }
            tcache.put(this.id, 0, t);
            tcache.put(0, this.id, t.inverse());
        }

        public abstract Element build();
    }
}
