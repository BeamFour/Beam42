package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

public record DummyInterface(int surfaceIndex, Interface surface, String label) implements Element {
    @Override public ElementType type() { return ElementType.DUMMY_INTERFACE; }
}
