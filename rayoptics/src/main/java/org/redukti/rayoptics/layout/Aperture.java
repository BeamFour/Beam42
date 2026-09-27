// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

/** A zero-thickness, non-stop aperture interface in air. */
public record Aperture(int surfaceIndex, Interface referenceSurface) implements Element {
    @Override public String label() { return "Aperture " + surfaceIndex; }
    @Override public ElementType type() { return ElementType.APERTURE; }
}