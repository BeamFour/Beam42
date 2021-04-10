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
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Group extends Element implements Container {
    private final List<? extends Element> elements;

    public Group(int id, Vector3Pair p, Transform3 transform3, List<? extends Element> elements) {
        super(id, p, transform3);
        this.elements = elements;
    }

    @Override
    public List<? extends Element> elements() {
        return elements;
    }

    public Element getElement(int pos) {
        if (pos >= 0 && pos < elements.size()) {
            return elements.get(pos);
        }
        return null;
    }

    public Group getGroup(int pos) {
        if (pos >= 0 && pos < elements.size() && elements.get(pos) instanceof Group) {
            return (Group)elements.get(pos);
        }
        return null;
    }

    public Surface getSurface(int pos) {
        if (pos >= 0 && pos < elements.size()  && elements.get(pos) instanceof Surface) {
            return (Surface) elements.get(pos);
        }
        return null;
    }

    void set_system(OpticalSystem system) {
        this._system = system;
        for (Element e: elements()) {
            e.set_system(system);
        }
    }

    public Vector3Pair get_bounding_box ()
    {
        return Element.get_bounding_box(elements);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=" + _id +
                ", position=" + _position +
                ", transform=" + _transform)
                .append(System.lineSeparator());
        for (Element e: elements) {
            sb.append('\t').append(e.toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static class Builder extends Element.Builder {
        protected final ArrayList<Element.Builder> elements = new ArrayList<>();

        public Group.Builder position(Vector3Pair position) {
            return (Builder) super.position(position);
        }

        public Group.Builder add(Element.Builder element) {
            this.elements.add(element);
            element.parent(this);
            return this;
        }

        public Group.Builder setId(AtomicInteger id) {
            this.id = id.incrementAndGet();
            for (Element.Builder e: elements) {
                e.setId(id);
            }
            return this;
        }

        @Override
        public void computeGlobalTransform(Transform3Cache tcache) {
            super.computeGlobalTransform(tcache);
            for (Element.Builder e: elements) {
                e.computeGlobalTransform(tcache);
            }
        }

        @Override
        public Element build() {
            return new Group(id, position, transform, getElements());
        }

        protected ArrayList<Element> getElements() {
            ArrayList<Element> myels = new ArrayList<>();
            for (Element.Builder e: elements) {
                myels.add(e.build());
            }
            return myels;
        }
    }
}
