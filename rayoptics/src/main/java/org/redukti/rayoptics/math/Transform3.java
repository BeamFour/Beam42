package org.redukti.rayoptics.math;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;

public class Transform3 {
    public final Matrix3 rot_mat;
    public final Vector3 vec;

    public Transform3(Matrix3 rot_mat, Vector3 vec) {
        this.rot_mat = rot_mat;
        this.vec = vec;
    }

    public Transform3() {
        this.rot_mat = Matrix3.IDENTITY;
        this.vec = Vector3.ZERO;
    }
}
