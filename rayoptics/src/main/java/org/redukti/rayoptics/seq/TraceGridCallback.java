package org.redukti.rayoptics.seq;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.specs.Field;

public interface TraceGridCallback {
    GridItem apply(Vector2 p, int wi, RayPkg ray_pkg, Field fld, double wvl, double foc);
}
