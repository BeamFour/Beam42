package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class Image extends Surface {

    public Image(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform, curve, shape);
    }

    public static class Builder extends Surface.Builder {
        @Override
        public Image.Builder position(Vector3Pair position) {
            return (Image.Builder) super.position(position);
        }

        public Image.Builder shape(Shape shape) {
            return (Image.Builder) super.shape(shape);
        }

        public Image.Builder curve(Curve curve) {
            return (Image.Builder) super.curve(curve);
        }

        public Image build() {
            return new Image(id, position, transform, curve, shape);
        }
    }
}
