package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.io.Renderer;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class Surface extends Element {

    final Shape shape;
    final Curve curve;

    public Surface(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform);
        this.curve = curve;
        this.shape = shape;
    }

    public Shape get_shape() { return shape; }

    public Curve get_curve() { return curve; }
    public Renderer.Style get_style() { return Renderer.Style.StyleSurface; }

    public static class Builder extends Element.Builder {
        Shape shape;
        Curve curve;

        @Override
        public Surface.Builder position(Vector3Pair position) {
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
