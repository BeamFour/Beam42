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

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OpticalSystem implements Container {
    protected final List<Element> _elements;
    protected final Transform3Cache _transform3Cache;
    protected double _angle_of_view;
    protected double _f_number;

    @Override
    public List<Element> elements() {
        return _elements;
    }

    public OpticalSystem(List<Element> elements, Transform3Cache transform3Cache, double angle_of_view, double f_number) {
        this._elements = elements;
        this._transform3Cache = transform3Cache;
        this._angle_of_view = angle_of_view;
        this._f_number = f_number;
    }

    public double get_angle_of_view() {
        return this._angle_of_view;
    }

    public double get_f_number() {
        return this._f_number;
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

    /**
     * Returns a flat sequence of elements, ordered by z
     */
    public List<Element> get_sequence() {
        List<Element> sequence = new ArrayList<>();
        for (Element e: elements()) {
            add(sequence, e);
        }
        sequence.sort((a,b) -> {
            double z1 = a.get_position().z();
            double z2 = b.get_position().z();
            if (z1 > z2)
                return 1;
            else if (z1 < z2)
                return -1;
            else
                return 0;
        });
        return sequence;
    }

    private void add(List<Element> sequence, Element e) {
        if (e instanceof Container) {
            Container c = (Container) e;
            for (Element e1: c.elements()) {
                add(sequence, e1);
            }
        }
        else
            sequence.add(e);
    }

    @Override
    public String toString() {
        return "OpticalSystem{" +
                "elements=" + _elements +
                ", transform3Cache=" + _transform3Cache +
                '}';
    }

    static final class GlassType {
        final String name;
        final double nd;

        public GlassType(String name, double nd) {
            this.name = name;
            this.nd = nd;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            GlassType glassType = (GlassType) o;
            return Double.compare(nd, glassType.nd) == 0 && Objects.equals(name, glassType.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, nd);
        }

        @Override
        public String toString() {
            return "(" + name + "," + nd + ')';
        }
    }

    static GlassType[] extractGlasses(OpticalSystem system) {
        var elements = system.get_sequence()
                .stream()
                .filter(e->e instanceof OpticalSurface)
                .map(e->(OpticalSurface)e)
                .toList();
        Set<GlassType> glassTypes = new HashSet<>();
        for (int i = 0; i < elements.size(); i++) {
            OpticalSurface opticalSurface = elements.get(i);
            var glass1 = opticalSurface.get_material(0);
            glassTypes.add(new GlassType(glass1.get_name(), glass1.get_refractive_index(SpectralLine.d)));
            var glass2 = opticalSurface.get_material(1);
            glassTypes.add(new GlassType(glass2.get_name(), glass2.get_refractive_index(SpectralLine.d)));
        }
        return glassTypes.toArray(new GlassType[0]);
    }

    public String[] glassNames() {
        var glassTypes = extractGlasses(this);
        return Arrays.stream(glassTypes).map(e->e.name).toList().toArray(new String[0]);
    }

    public double[] glassIndices() {
        var glassTypes = extractGlasses(this);
        return Arrays.stream(glassTypes).map(e->e.nd).mapToDouble(e->e.doubleValue()).toArray();
    }

    public static class Builder {
        private final ArrayList<Element.Builder> elements = new ArrayList<>();
        private Transform3Cache transform3Cache;
        private double _angle_of_view;
        private double _f_number;

        public Builder add(Element.Builder element) {
            this.elements.add(element);
            return this;
        }

        public OpticalSystem.Builder angle_of_view(double v) {
            this._angle_of_view = v;
            return this;
        }

        public OpticalSystem.Builder f_number(double v) {
            this._f_number = v;
            return this;
        }

        public OpticalSystem build() {
            generateIds();
            Transform3Cache transform3Cache = setCoordinates();
            List<Element> elements = buildElements();
            OpticalSystem system = new OpticalSystem(elements, transform3Cache, _angle_of_view, _f_number);
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
                e.set_id(id);
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
            if (e._parent != null) {
                e.localPosition(transform3Cache.global_2_local_transform(e._parent.id()).transform(v));
            }
            else {
                e.localPosition(v);
            }
            return build();
        }
    }

}
