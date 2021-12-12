package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.elem.Surface;
import org.redukti.rayoptics.math.Transform3;

public class NewSurfaceSpec {
    public final Surface surface;
    public final Gap gap;
    public final double rndx;
    public final Transform3 tfrm;

    public NewSurfaceSpec(Surface surface, Gap gap, double rndx, Transform3 tfrm) {
        this.surface = surface;
        this.gap = gap;
        this.rndx = rndx;
        this.tfrm = tfrm;
    }
}
