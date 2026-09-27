// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.specs.Field;

@FunctionalInterface
public interface ContrastTraceCallback<T> {
    T apply(ContrastRayTriplet rays, Field field, double wavelength, double focus);
}
