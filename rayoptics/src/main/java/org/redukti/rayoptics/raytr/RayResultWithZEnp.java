// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class RayResultWithZEnp {
    /**
     * Entrance pupil distance wrt the 1st interface, or null if no pupil
     * location could be found for the field. Upstream returns None here.
     */
    public final Double z_enp;
    public final RayResult rr;

    public RayResultWithZEnp(Double z_enp, RayResult rr) {
        this.z_enp = z_enp;
        this.rr = rr;
    }
}
