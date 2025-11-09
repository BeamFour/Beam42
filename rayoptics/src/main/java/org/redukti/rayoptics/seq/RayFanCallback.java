// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.specs.Field;

public interface RayFanCallback {
    Double apply(OpticalModel opt_model, Vector2 p, int wi, RayPkg ray_pkg, Field fld, double wvl, double foc);
}
