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

}
