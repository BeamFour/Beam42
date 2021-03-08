package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

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

        public Builder rotate(double x, double y, double z) {
            this.transform = this.transform.linearRotation(new Vector3(x,y,z));
            return this;
        }

        public int id() { return id;}

        public void computeGlobalTransform(Transform3Cache tcache) {
            Transform3 t = transform; // local transform
            Element.Builder p = this.parent;
            while (p != null) {
                t = t.compose(p.transform);
                p = p.parent;
            }
            tcache.putLocal2GlobalTransform(this.id, t);  // Local to global
            tcache.putGlobal2LocalTransform(this.id, t.inverse()); // Global to local
        }

        public abstract Element build();
    }
}
