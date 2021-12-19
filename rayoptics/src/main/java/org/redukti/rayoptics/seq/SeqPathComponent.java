package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.util.ZDir;

public class SeqPathComponent {
    public final Interface ifc;
    public final Gap gap;
    public final Transform3 transform3;
    public final Double rndx;
    public final ZDir z_dir;

    public SeqPathComponent(Interface ifc, Gap gap, Transform3 transform3, Double rndx, ZDir z_dir) {
        this.ifc = ifc;
        this.gap = gap;
        this.transform3 = transform3;
        this.rndx = rndx;
        this.z_dir = z_dir;
    }
}
