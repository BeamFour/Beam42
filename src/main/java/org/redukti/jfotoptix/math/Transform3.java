package org.redukti.jfotoptix.math;

public class Transform3 {

    public final Vector3 translation;
    public final Matrix3 linear;
    public final boolean useLinear;

    public Transform3(Vector3Position position) {
        this.translation = position.translation();
        if (position.direction().x() == 0 && position.direction().y() == 0) {
            if (position.direction().z() < 0.0) {
                this.linear = Matrix3.diag(1.0, 1.0, -1.0);
                this.useLinear = true;
            } else {
                this.linear = Matrix3.diag(1.0, 1.0, 1.0);
                this.useLinear = false;
            }
        } else {
            Quaternion q = new Quaternion(Vector3.vector3_001, position.direction());
            this.linear = Matrix3.rotation(q);
            this.useLinear = true;
        }
    }

    public Transform3(Vector3 translation, Matrix3 linear, boolean useLinear) {
        this.translation = translation;
        this.linear = linear;
        this.useLinear = useLinear;
    }

    Vector3 transformLinear(Vector3 v) {
        if (useLinear)
            return this.linear.times(v);
        else
            return v;
    }

    /**
     * Composition. New translation is set to: apply parent's linear transformation on child translation and add parent translation.
     * New linear matrix -s the product of the parent and child matrices.
     * TODO check terminology is correct
     * @param p Parent component
     * @param c Child component
     */
    public static Transform3 compose(Transform3 p, Transform3 c) {
        Vector3 translation = p.transformLinear(c.translation).plus(p.translation);
        boolean useLinear = p.useLinear || c.useLinear;
        Matrix3 linear = p.linear.times(c.linear);
        return new Transform3(translation, linear, useLinear);
    }

    public Vector3 transform(Vector3 v) {
        return transformLinear(v).plus(translation);
    }

    public Transform3 inverse() {
        Matrix3 linear = this.linear.inverse();
        Vector3 translation = linear.times(this.translation.negate());
        return new Transform3(translation, linear, true);
    }

    /**
     * Rotate by x, y, and z axis.
     * @param v Vector with angles per axis
     */
    public Transform3 linearRotation(Vector3 v) {
        Transform3 t = this;
        for (int i = 0; i < 3; i++) { // i stands for x,y,z axis
            if (v.get(i) != 0.0) {
                t = t.linearRotation(i, v.get(i));
            }
        }
        return t;
    }

    /**
     * Rotate around specified axis
     * @param axis 0=x, 1=y, 2=z
     * @param dangle Angle of rotation
     */
    Transform3 linearRotation(int axis, double dangle) {
        return linearRotationRadians(axis, Math.toRadians(dangle));
    }

    Transform3 linearRotationRadians(int axis, double rangle) {
        Matrix3 r = Matrix3.getRotationMatrix(axis, rangle);
        Matrix3 linear = r.times(this.linear);
        return new Transform3(this.translation, linear, true);
    }


}
