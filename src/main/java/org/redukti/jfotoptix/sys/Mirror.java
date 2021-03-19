package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class Mirror extends OpticalSurface {

    public Mirror(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, MaterialBase left, MaterialBase right) {
        super(id, p, transform, curve, shape, left, right);
    }

    public static class Builder extends OpticalSurface.Builder {

        boolean _light_from_left = true;

        @Override
        public Mirror.Builder position(Vector3Pair position) {
            return (Mirror.Builder) super.position(position);
        }

        @Override
        public Mirror.Builder shape(Shape shape) {
            return (Mirror.Builder) super.shape(shape);
        }

        @Override
        public Mirror.Builder curve(Curve curve) {
            return (Mirror.Builder) super.curve(curve);
        }

        @Override
        public Mirror.Builder leftMaterial(MaterialBase left) {
            return (Mirror.Builder)super.leftMaterial(left);
        }
        public Mirror.Builder metal(MaterialBase left) {
            return (Mirror.Builder)super.leftMaterial(left);
        }

        @Override
        public Mirror.Builder rightMaterial(MaterialBase right) {
            return (Mirror.Builder) super.rightMaterial(right);
        }
        public Mirror.Builder air(MaterialBase right) {
            return (Mirror.Builder) super.rightMaterial(right);
        }

        public Mirror.Builder light_from_left() {
            this._light_from_left = true;
            return this;
        }

        public Mirror.Builder light_from_right() {
            this._light_from_left = false;
            return this;
        }

        MaterialBase metal() {
            return this.left;
        }
        MaterialBase air() {
            return this.right;
        }

        @Override
        public OpticalSurface build() {
            return new Mirror(id, position, transform, curve, shape,
                    _light_from_left ?  air() : metal(),
                    _light_from_left ? metal() : air() );
        }
    }
}
