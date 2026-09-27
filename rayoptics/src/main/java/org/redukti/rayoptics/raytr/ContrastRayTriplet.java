// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
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
