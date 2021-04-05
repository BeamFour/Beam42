package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.material.Air;
import org.redukti.jfotoptix.material.MaterialBase;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Shape;

import java.util.Objects;

public class OpticalSurface extends Surface {
    MaterialBase[] mat = new MaterialBase[2];

    public OpticalSurface(int id,
                          Vector3Pair p,
                          Transform3 transform,
                          Curve curve,
                          Shape shape,
                          MaterialBase left,
                          MaterialBase right) {
        super(id, p, transform, curve, shape);
        mat[0] = left;
        mat[1] = right;
    }

    public MaterialBase get_material(int i) {
        return mat[i];
    }

    public String toString() {
        return "OpticalSurface{" +
                super.toString() +
                ", left material=" + Objects.toString(mat[0]) +
                ", right material=" + Objects.toString(mat[1]) +
                '}';
    }

    public static class Builder extends Surface.Builder {
        MaterialBase left = Air.air;
        MaterialBase right = Air.air;

        @Override
        public OpticalSurface.Builder position(Vector3Pair position) {
            return (OpticalSurface.Builder) super.position(position);
        }

        public OpticalSurface.Builder shape(Shape shape) {
            return (OpticalSurface.Builder) super.shape(shape);
        }

        public OpticalSurface.Builder curve(Curve curve) {
            return (OpticalSurface.Builder) super.curve(curve);
        }

        public OpticalSurface.Builder leftMaterial(MaterialBase left) {
            this.left = left;
            return this;
        }

        public OpticalSurface.Builder rightMaterial(MaterialBase right) {
            this.right = right;
            return this;
        }

        public OpticalSurface build() {
            return new OpticalSurface(id, position, transform, curve, shape, left, right);
        }

    }
}
