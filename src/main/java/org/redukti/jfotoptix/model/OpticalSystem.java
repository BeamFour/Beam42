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


package org.redukti.jfotoptix.model;

import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OpticalSystem implements Container {
    protected final List<Element> _elements;
    protected final Transform3Cache _transform3Cache;
    protected boolean keep_aspect = true;

    @Override
    public List<Element> elements() {
        return _elements;
    }

    public OpticalSystem(List<Element> elements, Transform3Cache transform3Cache) {
        this._elements = elements;
        this._transform3Cache = transform3Cache;
    }

    public Element getElement(int pos) {
        if (pos >= 0 && pos < _elements.size()) {
            return _elements.get(pos);
        }
        return null;
    }

    public Group getGroup(int pos) {
        if (pos >= 0 && pos < _elements.size() && _elements.get(pos) instanceof Group) {
            return (Group) _elements.get(pos);
        }
        return null;
    }

    public Vector3 getPosition(Element e) {
        return _transform3Cache.local_2_global_transform(e.id()).transform(Vector3.vector3_0);
    }

    public Vector3Pair get_bounding_box ()
    {
        return Element.get_bounding_box(_elements);
    }

    Transform3 get_transform (Element from, Element to)
    {
        return _transform3Cache.transform_cache_update (from.id (), to.id ());
    }

    Transform3 get_global_transform(Element e) {
        return _transform3Cache.local_2_global_transform(e.id());
    }

//    Transform3 get_local_transform(Element e) {
//        return transform3Cache.getGlobal2LocalTransform(e.id());
//    }

    @Override
    public String toString() {
        return "OpticalSystem{" +
                "elements=" + _elements +
                ", transform3Cache=" + _transform3Cache +
                ", keep_aspect=" + keep_aspect +
                '}';
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
            OpticalSystem system = new OpticalSystem(elements, transform3Cache);
            for (Element e: system.elements()) {
                e.set_system(system);
            }
            return system;
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
            for (Element.Builder e: elements) {
                e.compute_global_transforms(transform3Cache);
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
                e.localPosition(transform3Cache.global_2_local_transform(e.parent.id()).transform(v));
            }
            else {
                e.localPosition(v);
            }
            return build();
        }
    }

}
