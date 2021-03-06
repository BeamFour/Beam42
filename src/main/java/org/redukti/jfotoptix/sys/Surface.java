package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.math.Vector3Position;
import org.redukti.jfotoptix.shape.Shape;

public class Surface extends Element {

    public Surface(OpticalSystem system, Group group, Vector3Position p) {
        super(system, group, p);
    }


    public static class Builder extends Element.Builder {
        Shape shape;
        Curve curve;

        @Override
        public Surface.Builder system(OpticalSystem.Builder system) {
            return (Builder) super.system(system);
        }

        @Override
        public Surface.Builder group(Group.Builder group) {
            return (Builder) super.group(group);
        }

        @Override
        public Surface.Builder position(Vector3Position position) {
            return (Builder) super.position(position);
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
