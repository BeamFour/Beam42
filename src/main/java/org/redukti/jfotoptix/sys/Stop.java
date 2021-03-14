package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class Stop extends Surface {

    public Stop(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape) {
        super(id, p, transform, curve, shape);
    }

    public static class Builder extends Surface.Builder {
        @Override
        public Stop.Builder position(Vector3Pair position) {
            return (Stop.Builder) super.position(position);
        }

        public Stop.Builder shape(Shape shape) {
            return (Stop.Builder) super.shape(shape);
        }

        public Stop.Builder curve(Curve curve) {
            return (Stop.Builder) super.curve(curve);
        }

        public Stop build() {
            return new Stop(id, position, transform, curve, shape);
        }
    }
}
