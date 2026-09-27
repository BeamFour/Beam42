// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

public record DummyInterface(int surfaceIndex, Interface surface, String label) implements Element {
    @Override public ElementType type() { return ElementType.DUMMY_INTERFACE; }
}
