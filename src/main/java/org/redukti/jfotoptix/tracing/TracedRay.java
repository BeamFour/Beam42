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


package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.light.LightRay;
import org.redukti.jfotoptix.material.Medium;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.sys.Element;

/**
 * Propagated light ray class
 * <p>
 * This class is used to describe a LightRay with all
 * tracing and propagation information attached.
 */
public class TracedRay extends LightRay {

    Vector3 _point; // ray intersection point (intersect surface local)
    double _intercept_intensity;     // intersection point intensity
    double _len;                     // ray length
    Element _creator;    // element which generated this ray
    Medium _material; // material
    Element _i_element;        // intersect element
    TracedRay _parent;                    // ray which generated this one
    TracedRay _child;                     // pointer to generated ray
    TracedRay _next;                      // pointer to sibling generated ray
    boolean _lost;                      // does the ray intersect with an element ?

    public TracedRay(Vector3 origin, Vector3 direction) {
        super(new Vector3Pair(origin, direction));
        _len = Double.MAX_VALUE;
        _creator = null;
        _parent = null;
        _child = null;
        _lost = true;
    }

    public void add_generated(TracedRay r) {
        assert (r._parent == null);
        r._parent = this;
        r._next = _child;
        _child = r;
    }

    public void set_intercept(Element e, Vector3 point) {
        _i_element = e;
        _point = point;
        _lost = false;
    }

    public void set_creator(Element e) {
        this._creator = e;
    }

    public Element get_creator() {
        return _creator;
    }

    public void set_len(double v) {
        this._len = v;
    }

    public double get_len() {
        return _len;
    }

    public Vector3 get_position(Element e) {
        return _creator.get_transform_to(e).transform(_ray.origin());
    }

    public Vector3 get_direction(Element e) {
        return _creator.get_transform_to(e).apply_rotation(_ray.direction());
    }

    public Vector3 get_position() {
        return _creator.get_global_transform().transform(_ray.origin());
    }

    public Vector3 get_direction() {
        return _creator.get_global_transform().apply_rotation(_ray.direction());
    }

    public void set_intercept_intensity(double v) {
        this._intercept_intensity = v;
    }

    public Medium get_material() {
        return _material;
    }

    public void set_material(Medium mat) {
        _material = mat;
    }

    public boolean is_lost() {
        return _lost;
    }

    public Element get_intercept_element() {
        return _i_element;
    }

    public Vector3 get_intercept_point() {
        return _point;
    }

    public TracedRay get_next_child() {
        return _next;
    }

    public TracedRay get_first_child() {
        return _child;
    }

    public String toString() {
        return "TracedRay{src=" + _creator.id() + ",wavelen=" + _wavelen + ",origin=" + _ray.origin() + ",direction=" + _ray.direction() + ",len=" + _len + '}';
    }
}
