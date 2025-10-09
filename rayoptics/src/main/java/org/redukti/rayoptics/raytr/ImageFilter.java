package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public interface ImageFilter {
    GridItem apply(Vector2 pupil, RayPkg pkg);
}
