package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.exceptions.TraceException;

/** Three rays used by a contrast-optimization pupil sample. */
public record ContrastRayTriplet(
        Vector2 pupil,
        RayPkg reference,
        RayPkg sagittal,
        RayPkg tangential,
        TraceException referenceError,
        TraceException sagittalError,
        TraceException tangentialError,
        double weight) {
}
