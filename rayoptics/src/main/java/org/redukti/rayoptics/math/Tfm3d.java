package org.redukti.rayoptics.math;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;

public class Tfm3d {
    public final Matrix3 rot_mat;
    public final Vector3 vec;

    public Tfm3d(Matrix3 rot_mat, Vector3 vec) {
        this.rot_mat = rot_mat;
        this.vec = vec;
    }
}
