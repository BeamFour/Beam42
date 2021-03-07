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
            return this.linear.multiply(v);
        else
            return v;
    }

    public Transform3 compose(Transform3 t) {
        Vector3 translation = t.transformLinear(this.translation).add(t.translation);
        boolean useLinear = t.useLinear || this.useLinear;
        Matrix3 linear = t.linear.multiply(this.linear);
        return new Transform3(translation, linear, useLinear);
    }

    public Vector3 transform(Vector3 v) {
        return transformLinear(v).add(translation);
    }

    public Transform3 inverse() {
        Matrix3 linear = this.linear.inverse();
        Vector3 translation = linear.multiply(this.translation.negate());
        return new Transform3(translation, linear, true);
    }

    public Transform3 linearRotation(Vector3 v) {
        Transform3 t = this;
        for (int i = 0; i < 3; i++) { // i stands for x,y,z axis
            if (v.get(i) != 0.0) {
                t = t.linearRotation(i, v.get(i));
            }
        }
        return t;
    }

    Transform3 linearRotation(int axis, double dangle) {
        return linearRotationRad(axis, Math.toRadians(dangle));
    }

    Transform3 linearRotationRad(int axis, double rangle) {
        Matrix3 r = Matrix3.getRotationMatrix(axis, rangle);

        Matrix3 linear = r.multiply(this.linear);
        boolean use_linear = true;

        return new Transform3(this.translation, linear, use_linear);
    }


}
