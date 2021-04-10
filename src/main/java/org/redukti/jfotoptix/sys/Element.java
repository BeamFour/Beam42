/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet
 */


package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Element {

    final int _id;
    final Vector3Pair _position;
    final Transform3 _transform;
    OpticalSystem _system;

    public Element(int id, Vector3Pair p, Transform3 transform) {
        this._id = id;
        this._position = p;
        this._transform = transform;
    }

    public Vector3 getLocalPosition() {
        return this._transform.translation;
    }

    public int id() {
        return _id;
    }

    public Vector3Pair get_bounding_box() {
        return new Vector3Pair(Vector3.vector3_0, Vector3.vector3_0);
    }

    public Transform3 get_transform() {
        return _transform;
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

    public Vector3 get_position (Element e)
    {
        return _system.get_transform (this, e).transform (Vector3.vector3_0);
    }

    public Vector3 get_position ()
    {
        return _system.get_global_transform (this).transform (Vector3.vector3_0);
    }

    @Override
    public String toString() {
        return "id=" + _id +
                ", position=" + _position +
                ", transform=" + _transform;
    }

    public static Vector3Pair get_bounding_box(List<? extends Element> elementList) {
        Vector3 a = new Vector3(Double.MAX_VALUE);
        Vector3 b = new Vector3(Double.MIN_VALUE);

        for (Element e : elementList) {
            Vector3Pair bi = e.get_bounding_box();
            if (bi == null) // FIXME - this is a temp solution to failure
                continue;

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
            this.transform = new Transform3(v, this.transform.rotation_matrix, this.transform.use_rotation_matrix);
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
            this.transform = this.transform.rotate_axis_by_angles(new Vector3(x, y, z));
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
