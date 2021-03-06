package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Position;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Element {

    final Vector3Position position;
    final Transform3 transform;

    public Element(Vector3Position p, Transform3 transform) {
        this.position = p;
        this.transform = transform;
    }

    public Vector3 getLocalPosition() {
        return this.transform.translation;
    }

    public static abstract class Builder {
        int id;
        Vector3Position position;
        Transform3 transform;

        public Builder position(Vector3Position position) {
            this.position = position;
            this.transform = new Transform3(position);
            return this;
        }

        public Builder setId(AtomicInteger id) {
            this.id = id.incrementAndGet();
            return this;
        }

        public void computeGlobalTransform(List<Group.Builder> parents, Transform3Cache tcache) {
            Transform3 t = transform; // local transform
            for (int i = parents.size()-1; i >= 0; i--) {
                Group.Builder g = parents.get(i);
                t = Transform3.compose(t, g.transform);
            }
            tcache.put(this.id, 0, t);
        }

        public abstract Element build();
    }
}
