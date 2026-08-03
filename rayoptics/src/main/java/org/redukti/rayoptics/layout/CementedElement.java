package org.redukti.rayoptics.layout;

import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.seq.Gap;

import java.util.List;

/** Two or more consecutive material-filled gaps sharing their boundary surfaces. */
public record CementedElement(List<Integer> surfaceIndices, List<Surface> surfaces,
                              List<Gap> gaps) implements Element {
    public CementedElement {
        surfaceIndices = List.copyOf(surfaceIndices);
        surfaces = List.copyOf(surfaces);
        gaps = List.copyOf(gaps);
        if (gaps.size() < 2 || surfaces.size() != gaps.size() + 1
                || surfaceIndices.size() != surfaces.size())
            throw new IllegalArgumentException("a cemented element requires n gaps and n + 1 surfaces");
    }

    @Override public String label() { return "CE" + surfaceIndices.get(0); }
    @Override public ElementType type() { return ElementType.CEMENTED_LENS; }
}
