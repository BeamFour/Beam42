package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Gap;

public record AirGap(int gapIndex, Gap gap) implements Element {
    @Override public String label() { return "Air " + gapIndex; }
    @Override public ElementType type() { return ElementType.AIR_GAP; }
}
