// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class AimInfo {
    /** aim_pt is used for paraxial aiming */
    public final double[] aim_pt;
    /** z_enp is the actual entrance pupil distance with respect to 1st ifc for a field */
    public final Double z_enp;

    public AimInfo(double[] aim_pt, Double z_enp) {
        this.aim_pt = aim_pt;
        this.z_enp = z_enp;
    }
}
