package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.seq.Gap;

/** A material-filled gap bounded by two sequential surfaces. */
public record LensElement(int firstSurfaceIndex, int secondSurfaceIndex,
                          Surface surface1, Surface surface2, Gap gap) implements Element {
    @Override public String label() { return "L" + firstSurfaceIndex; }
    @Override public ElementType type() { return ElementType.LENS; }
}
