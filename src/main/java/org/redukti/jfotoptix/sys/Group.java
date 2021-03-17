package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.io.Renderer;
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



    @Override
    public void draw_2d_e(Renderer r, Element ref) {
        for (Element e: elements) {
            e.draw_element_2d(r, ref);
        }
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
