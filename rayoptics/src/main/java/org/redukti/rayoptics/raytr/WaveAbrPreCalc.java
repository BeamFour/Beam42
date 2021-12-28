package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.math.Vector3;

final class WaveAbrPreCalc {
    double pre_opd;
    Vector3 p_coord;
    Vector3 b4_pt;
    Vector3 b4_dir;

    public WaveAbrPreCalc(double pre_opd, Vector3 p_coord, Vector3 b4_pt, Vector3 b4_dir) {
        this.pre_opd = pre_opd;
        this.p_coord = p_coord;
        this.b4_pt = b4_pt;
        this.b4_dir = b4_dir;
    }
}
