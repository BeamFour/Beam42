package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.seq.Interface;

/** A zero-thickness, non-stop aperture interface in air. */
public record Aperture(int surfaceIndex, Interface referenceSurface) implements Element {
    @Override public String label() { return "Aperture " + surfaceIndex; }
    @Override public ElementType type() { return ElementType.APERTURE; }
}