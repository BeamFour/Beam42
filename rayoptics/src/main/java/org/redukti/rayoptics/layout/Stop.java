package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

public record Stop(int surfaceIndex, Interface referenceSurface) implements Element {
    @Override public String label() { return "Stop"; }
    @Override public ElementType type() { return ElementType.STOP; }
}
