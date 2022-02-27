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
