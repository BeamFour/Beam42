package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.util.ZDir;

public class PathSeg {
    public final Interface ifc;
    public final Gap gap;
    public final Tfm3d transform3;
    public final Double rndx;
    public final ZDir z_dir;

    public PathSeg(Interface ifc, Gap gap, Tfm3d transform3, Double rndx, ZDir z_dir) {
        this.ifc = ifc;
        this.gap = gap;
        this.transform3 = transform3;
        this.rndx = rndx;
        this.z_dir = z_dir;
    }
}
