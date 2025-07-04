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

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.curve.Sphere;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.Medium;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Lens extends Group {

    public enum LensEdge {
        StraightEdge,
        SlopeEdge,
    }

    protected List<OpticalSurface> _surfaces;
    // There can be more than stop, but one of them must be an
    // ApertureStop, rest FlareStops
    protected final List<Stop> _stops;

    public Lens(int id, Vector3Pair position, Transform3 transform, List<OpticalSurface> surfaces, List<Element> elementList, List<Stop> stops) {
        super(id, position, transform, elementList);
        this._stops = stops;
        this._surfaces = surfaces;
    }

    public List<Stop> stops() {
        return _stops;
    }

    public List<OpticalSurface> surfaces() {
        return _surfaces;
    }

    @Override
    void set_system(OpticalSystem system) {
        super.set_system(system);
        for (Stop stop: _stops)
            stop.set_system(system);
    }

    @Override
    public String toString() {
        return "Lens{"+ super.toString() + "}";
    }

    public static class Builder extends Group.Builder {
        double _last_pos = 0;
        Medium _next_mat = Air.air;

        @Override
        public Element build() {
            ArrayList<Element> elements = getElements();
            List<Stop> stops = elements.stream().filter(e -> e instanceof Stop)
                    .map(e->(Stop)e)
                    .collect(Collectors.toList());
            List<OpticalSurface> surfaces = elements.stream().filter(e -> e instanceof OpticalSurface)
                    .map (e -> (OpticalSurface)e)
                    .collect(Collectors.toList());
            return new Lens(_id, _position, _transform, surfaces, elements, stops);
        }

        /**
         * Add an optical surface
         *
         * @param curvature curvature of the surface, 1/r
         * @param radius    the radius of the disk
         * @param thickness the thickness after this surface
         * @param glass     the material after this surface
         */
        public Lens.Builder add_surface(double curvature, double radius, double thickness, Medium glass) {
            Curve curve;
            if (curvature == 0.)
                curve = Flat.flat;
            else
                curve = new Sphere(curvature);
            return add_surface(curve, new Disk(radius), thickness,
                    glass);
        }

        public Lens.Builder add_surface(double curvature, double radius, double thickness) {
            return add_surface(curvature, radius, thickness, null);
        }

        public Lens.Builder add_surface(Curve curve, Shape shape, double thickness, Medium glass) {
            assert (thickness >= 0.);
            if (glass == null) {
                glass = Air.air;
            }
            OpticalSurface.Builder surface = new OpticalSurface.Builder()
                    .position(new Vector3Pair(new Vector3(0, 0, _last_pos), Vector3.vector3_001))
                    .curve(curve)
                    .shape(shape)
                    .leftMaterial(_next_mat)
                    .rightMaterial(glass)
                    .thickness(thickness);
            _next_mat = glass;
            _last_pos += thickness;
            add(surface);
            return this;
        }

        public Lens.Builder add_stop(double radius, double thickness, boolean is_aperturestop) {
            return add_stop(new Disk(radius), thickness, is_aperturestop);
        }
        public Lens.Builder add_stop(Shape shape, double thickness, boolean is_aperturestop) {
            Stop.Builder stop = new Stop.Builder()
                    .position(new Vector3Pair(new Vector3(0, 0, _last_pos), Vector3.vector3_001))
                    .curve(Flat.flat)
                    .shape(shape)
                    .thickness(thickness)
                    .as(is_aperturestop);
            _last_pos += thickness;
            add(stop);
            return this;
        }

        @Override
        public void compute_global_transforms(Transform3Cache tcache) {
            super.compute_global_transforms(tcache);
        }

        @Override
        public Lens.Builder position(Vector3Pair position) {
            return (Lens.Builder) super.position(position);
        }
    }
}
