package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

public record FinitePupilWaveAberrationResult(RayPkg ray_pkg, ChiefRayPkg chief_ray_pkg, ReferenceSphere ref_sphere,
                                              double e1, double ekp, double ep, Vector3 ray_exit_pupil_pt, double ray_op, double cr_op ) {
}
