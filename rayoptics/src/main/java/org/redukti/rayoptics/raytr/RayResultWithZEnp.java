// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class RayResultWithZEnp {
    public final double z_enp;
    public final RayResult rr;

    public RayResultWithZEnp(double z_enp, RayResult rr) {
        this.z_enp = z_enp;
        this.rr = rr;
    }
}
