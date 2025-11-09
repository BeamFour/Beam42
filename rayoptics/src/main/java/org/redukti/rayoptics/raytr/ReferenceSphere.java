// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Tfm3d;

public class ReferenceSphere {
    public final Vector3 image_pt;
    public final Vector3 ref_dir;
    public final double ref_sphere_radius;
    public final Tfm3d lcl_tfrm_last;

    public ReferenceSphere(Vector3 image_pt, Vector3 ref_dir, double ref_sphere_radius, Tfm3d lcl_tfrm_last) {
        this.image_pt = image_pt;
        this.ref_dir = ref_dir;
        this.ref_sphere_radius = ref_sphere_radius;
        this.lcl_tfrm_last = lcl_tfrm_last;
    }
}
