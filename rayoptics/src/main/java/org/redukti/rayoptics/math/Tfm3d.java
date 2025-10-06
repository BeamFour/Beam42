package org.redukti.rayoptics.math;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;

public class Tfm3d {
    public final Matrix3 rt;
    public final Vector3 t;

    public Tfm3d(Matrix3 rt, Vector3 t) {
        this.rt = rt;
        this.t = t;
    }
}
