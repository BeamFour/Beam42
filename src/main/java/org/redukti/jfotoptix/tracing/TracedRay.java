package org.redukti.jfotoptix.tracing;

import org.redukti.jfotoptix.light.LightRay;
import org.redukti.jfotoptix.material.MaterialBase;
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
    MaterialBase _material; // material
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

    public TracedRay(LightRay r) {
        super(r);
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
        return _creator.get_transform_to(e).transform_linear(_ray.direction());
    }

    public Vector3 get_position() {
        return _creator.get_global_transform().transform(_ray.origin());
    }

    public Vector3 get_direction() {
        return _creator.get_global_transform().transform_linear(_ray.direction());
    }


    public void set_intercept_intensity(double v) {
        this._intercept_intensity = v;
    }

    public MaterialBase get_material() {
        return _material;
    }

    public void set_material(MaterialBase mat) {
        _material = mat;
    }
}
