package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Position;
import org.redukti.jfotoptix.shape.Shape;

public class Surface extends Element {

    final Shape shape;
    final Curve curve;

    public Surface(int id, Vector3Position p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform);
        this.curve = curve;
        this.shape = shape;
    }

    public static class Builder extends Element.Builder {
        Shape shape;
        Curve curve;

        @Override
        public Surface.Builder position(Vector3Position position) {
            return (Builder) super.position(position);
        }

        @Override
        public Element build() {
            return new Surface(id, position, transform, curve, shape);
        }

        public Builder shape(Shape shape) {
            this.shape = shape;
            return this;
        }

        public Builder curve(Curve curve) {
            this.curve = curve;
            return this;
        }
    }

}
