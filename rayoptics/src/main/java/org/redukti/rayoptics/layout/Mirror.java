package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

public record Mirror(int surfaceIndex, Interface surface) implements Element {
    @Override public String label() { return "Mirror " + surfaceIndex; }
    @Override public ElementType type() { return ElementType.MIRROR; }
}
