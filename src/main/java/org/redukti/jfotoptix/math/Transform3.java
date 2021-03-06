package org.redukti.jfotoptix.math;

public class Transform3 {

    public final Vector3 translation;
    public final Matrix3 linear;
    final boolean useLinear;

    public Transform3(Vector3Position position) {
        this.translation = position.translation();
        if (position.direction().x() == 0 && position.direction().y() == 0) {
            if (position.direction().z () < 0.0) {
                this.linear = Matrix3.diag(1.0, 1.0, -1.0);
                this.useLinear = true;
            }
            else {
                this.linear = Matrix3.diag(1.0, 1.0, 1.0);
                this.useLinear = false;
            }
        }
        else {
            Quaternion q = new Quaternion(Vector3.vector3_001, position.direction());
            this.linear = Matrix3.rotation(q);
            this.useLinear = true;
        }
    }

    private Transform3(Vector3 translation, Matrix3 linear, boolean useLinear) {
        this.translation = translation;
        this.linear = linear;
        this.useLinear = useLinear;
    }

    Vector3 transformLinear(Vector3 v)
    {
        if (useLinear)
            return this.linear.multiply(v);
        else
            return v;
    }

    public static Transform3 compose(Transform3 t, Transform3 b) {
        Vector3 translation = t.transformLinear(b.translation).add(t.translation);
        boolean useLinear = t.useLinear || b.useLinear;
        Matrix3 linear = t.linear.multiply(b.linear);
        return new Transform3(translation, linear, useLinear);
    }

}
