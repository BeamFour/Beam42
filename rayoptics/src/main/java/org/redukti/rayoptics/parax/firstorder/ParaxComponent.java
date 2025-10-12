// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax.firstorder;

public class ParaxComponent {
    public final double ht;
    public final double slp;
    public final double aoi;

    public ParaxComponent(double ht, double slp, double aoi) {
        this.ht = ht;
        this.slp = slp;
        this.aoi = aoi;
    }
}
