// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
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
