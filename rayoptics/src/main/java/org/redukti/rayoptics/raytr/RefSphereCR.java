// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

public class RefSphereCR {
    public final ReferenceSphere ref_sphere;
    public final ChiefRayPkg chief_ray_pkg;

    public RefSphereCR(ReferenceSphere ref_sphere, ChiefRayPkg chief_ray_pkg) {
        this.ref_sphere = ref_sphere;
        this.chief_ray_pkg = chief_ray_pkg;
    }
}
