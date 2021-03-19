package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.io.RendererSvg;
import org.redukti.jfotoptix.io.RendererViewport;
import org.redukti.jfotoptix.io.Rgb;
import org.redukti.jfotoptix.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OpticalSystem implements Container {
    private final List<Element> elements;
    private final Transform3Cache transform3Cache;
    private boolean keep_aspect = true;

    @Override
    public List<Element> elements() {
        return elements;
    }

    public OpticalSystem(List<Element> elements, Transform3Cache transform3Cache) {
        this.elements = elements;
        this.transform3Cache = transform3Cache;
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

    public Vector3 getPosition(Element e) {
        return transform3Cache.getLocal2GlobalTransform(e.id()).transform(Vector3.vector3_0);
    }

    @Override
    public void draw_2d_fit(RendererViewport r, boolean keep_aspect) {
        Vector3Pair b = get_bounding_box ();

        r.set_window (Vector2Pair.from(b, 2, 1), keep_aspect);
        r.set_camera_direction (Vector3.vector3_100);
        r.set_camera_position (Vector3.vector3_0);

        r.set_feature_size (b.v1.y () - b.v0.y () / 20.);
    }

    @Override
    public void draw_2d(Renderer r) {
        // optical axis
        Vector3Pair b = get_bounding_box ();
        r.draw_segment (new Vector2Pair (new Vector2(b.z0 (), 0.), new Vector2(b.z1 (), 0.)), Rgb.rgb_gray);

        for (Element e : elements())
        {
            e.draw_element_2d(r, null);
        }
    }

    public void draw_2d_fit(RendererSvg r) {
        draw_2d_fit(r, keep_aspect);
    }

    public Vector3Pair get_bounding_box ()
    {
        return Element.get_bounding_box(elements);
    }

    public void draw_2d(RendererSvg r) {
        // optical axis
        Vector3Pair b = get_bounding_box ();
        r.draw_segment (new Vector2Pair (new Vector2(b.v0.z(), 0.), new Vector2(b.v1.z (), 0.)), Rgb.rgb_gray);

        for (Element e : elements())
        {
            e.draw_element_2d (r, null);
        }
    }

    Transform3 get_transform (Element from, Element to)
    {
        return transform3Cache.transform_cache_update (from.id (), to.id ());
    }

    Transform3 get_global_transform(Element e) {
        return transform3Cache.getLocal2GlobalTransform(e.id());
    }

    @Override
    public String toString() {
        return "OpticalSystem{" +
                "elements=" + elements +
                ", transform3Cache=" + transform3Cache +
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
                e.computeGlobalTransform(transform3Cache);
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
                e.localPosition(transform3Cache.getGlobal2LocalTransform(e.parent.id()).transform(v));
            }
            else {
                e.localPosition(v);
            }
            return build();
        }
    }

}
