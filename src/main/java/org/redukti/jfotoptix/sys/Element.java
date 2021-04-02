package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Element {

    final int id;
    final Vector3Pair position;
    final Transform3 transform;
    OpticalSystem _system;

    public Element(int id, Vector3Pair p, Transform3 transform) {
        this.id = id;
        this.position = p;
        this.transform = transform;
    }

    public Vector3 getLocalPosition() {
        return this.transform.translation;
    }

    public int id() {
        return id;
    }

    public Vector3Pair get_bounding_box() {
        return new Vector3Pair(Vector3.vector3_0, Vector3.vector3_0);
    }

    public Transform3 get_transform() {
        return transform;
    }

    public void draw_element_2d(Renderer r, Element ref) {
        r.group_begin("element");
        draw_2d_e(r, ref);
        r.group_end();
    }

    public void draw_2d_e(Renderer r, Element ref) {
    }

    public Transform3 get_transform_to(Element e) {
        assert (_system != null);
        return e != null
                ? _system.get_transform(this, e)
                : _system.get_global_transform(this);
    }

    public Transform3 get_global_transform ()
    {
        assert (_system != null);
        return _system.get_global_transform (this);
    }

//    public Transform3 get_local_transform ()
//    {
//        assert (_system != null);
//        return _system.get_local_transform (this);
//    }

    void set_system(OpticalSystem system) {
        this._system = system;
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", position=" + position +
                ", transform=" + transform;
    }

    public static Vector3Pair get_bounding_box(List<? extends Element> elementList) {
        Vector3 a = new Vector3(Double.MAX_VALUE);
        Vector3 b = new Vector3(Double.MIN_VALUE);

        for (Element e : elementList) {
            Vector3Pair bi = e.get_bounding_box();

            if (bi.v0 == bi.v1)
                continue;

            bi = e.get_transform().transform_pair(bi);

            for (int j = 0; j < 3; j++) {
                if (bi.v0.v(j) > bi.v1.v(j))
                    bi = Vector3Pair.swapElement(bi, j);

                if (bi.v0.v(j) < a.v(j))
                    a = a.v(j, bi.v0.v(j));

                if (bi.v1.v(j) > b.v(j))
                    b = b.v(j, bi.v1.v(j));
            }
        }
        return new Vector3Pair(a, b);
    }

    public static abstract class Builder {
        int id;
        Vector3Pair position;
        Transform3 transform;
        Element.Builder parent;

        public Builder position(Vector3Pair position) {
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
            this.transform = this.transform.linearRotation(new Vector3(x, y, z));
            return this;
        }

        public Transform3 transform() {
            return transform;
        }

        public Element.Builder transform(Transform3 transform3) {
            this.transform = transform3;
            return this;
        }

        public int id() {
            return id;
        }

        public void computeGlobalTransform(Transform3Cache tcache) {
            //System.err.println("Computing coordinate for " + this);

            Transform3 t = transform; // local transform
            Element.Builder p = this.parent;
            while (p != null) {
                t = Transform3.compose(p.transform, t);
                p = p.parent;
            }
            tcache.putLocal2GlobalTransform(this.id, t);  // Local to global
            tcache.putGlobal2LocalTransform(this.id, t.inverse()); // Global to local
        }

        public abstract Element build();

        public String toString() {
            return getClass().getName() + ",id=" + id;
        }
    }
}
