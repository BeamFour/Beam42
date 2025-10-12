// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.util.ZDir;

public class PathSeg {
    public final Interface ifc;
    public final Gap gap;
    public final Tfm3d Tfrm;
    public final Double Indx;
    public final ZDir Zdir;

    public PathSeg(Interface ifc, Gap gap, Tfm3d Tfrm, Double Indx, ZDir Zdir) {
        this.ifc = ifc;
        this.gap = gap;
        this.Tfrm = Tfrm;
        this.Indx = Indx;
        this.Zdir = Zdir;
    }
}
