// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Gap;

public record AirGap(int gapIndex, Gap gap) implements Element {
    @Override public String label() { return "Air " + gapIndex; }
    @Override public ElementType type() { return ElementType.AIR_GAP; }
}
