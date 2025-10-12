// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

public class RefSphere {
    public Vector3 image_pt;
    public Vector3 ref_dir;
    public double ref_sphere_radius;

    public RefSphere(Vector3 image_pt, Vector3 ref_dir, double ref_sphere_radius) {
        this.image_pt = image_pt;
        this.ref_dir = ref_dir;
        this.ref_sphere_radius = ref_sphere_radius;
    }
}
