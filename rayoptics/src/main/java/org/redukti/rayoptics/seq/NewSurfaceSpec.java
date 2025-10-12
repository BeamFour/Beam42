// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.util.ZDir;

public class NewSurfaceSpec {
    public final Surface surface;
    public final Gap gap;
    public final double rndx;
    public final Tfm3d tfrm;
    public final ZDir z_dir;

    public NewSurfaceSpec(Surface surface, Gap gap, double rndx, Tfm3d tfrm, ZDir z_dir) {
        this.surface = surface;
        this.gap = gap;
        this.rndx = rndx;
        this.tfrm = tfrm;
        this.z_dir = z_dir;
    }
}
