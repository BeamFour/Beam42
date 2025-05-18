package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.util.ZDir;

public class NewSurfaceSpec {
    public final Surface surface;
    public final Gap gap;
    public final double rndx;
    public final Transform3 tfrm;
    public final ZDir z_dir;

    public NewSurfaceSpec(Surface surface, Gap gap, double rndx, Transform3 tfrm, ZDir z_dir) {
        this.surface = surface;
        this.gap = gap;
        this.rndx = rndx;
        this.tfrm = tfrm;
        this.z_dir = z_dir;
    }
}
