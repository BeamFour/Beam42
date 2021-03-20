package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.material.Mirror;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

public class MirrorSurface extends OpticalSurface {

    public MirrorSurface(int id, Vector3Pair p, Transform3 transform, Curve curve, Shape shape, MaterialBase left, MaterialBase right) {
        super(id, p, transform, curve, shape, left, right);
    }

    public static class Builder extends OpticalSurface.Builder {

        boolean _light_from_left = true;

        public Builder(boolean _light_from_left) {
            this._light_from_left = _light_from_left;
            this.left = Mirror.mirror;
            this.right = null; /* none */
        }

        @Override
        public MirrorSurface.Builder position(Vector3Pair position) {
            return (MirrorSurface.Builder) super.position(position);
        }

        @Override
        public MirrorSurface.Builder shape(Shape shape) {
            return (MirrorSurface.Builder) super.shape(shape);
        }

        @Override
        public MirrorSurface.Builder curve(Curve curve) {
            return (MirrorSurface.Builder) super.curve(curve);
        }

        @Override
        public MirrorSurface.Builder leftMaterial(MaterialBase left) {
            return (MirrorSurface.Builder)super.leftMaterial(left);
        }
        public MirrorSurface.Builder metal(MaterialBase left) {
            return (MirrorSurface.Builder)super.leftMaterial(left);
        }

        @Override
        public MirrorSurface.Builder rightMaterial(MaterialBase right) {
            return (MirrorSurface.Builder) super.rightMaterial(right);
        }
        public MirrorSurface.Builder air(MaterialBase right) {
            return (MirrorSurface.Builder) super.rightMaterial(right);
        }

        public MirrorSurface.Builder light_from_left() {
            this._light_from_left = true;
            return this;
        }

        public MirrorSurface.Builder light_from_right() {
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
            return new MirrorSurface(id, position, transform, curve, shape,
                    _light_from_left ?  air() : metal(),
                    _light_from_left ? metal() : air() );
        }
    }
}
