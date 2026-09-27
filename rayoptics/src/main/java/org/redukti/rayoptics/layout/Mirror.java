// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

public record Mirror(int surfaceIndex, Interface surface) implements Element {
    @Override public String label() { return "Mirror " + surfaceIndex; }
    @Override public ElementType type() { return ElementType.MIRROR; }
}
