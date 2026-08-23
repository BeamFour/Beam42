package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

/**
 *
 * @param ray_pkg A test aperture ray
 * @param chief_ray_pkg The chief ray
 * @param ref_sphere The reference sphere
 * @param e1 Object space geometric difference between chief ray and aperture ray from entry pupil to first surface.
 * @param ekp Image space geometric difference between chief ray amd aperture ray from last surface to exit pupil excluding impact of focus.
 * @param ep Image space geometric adjustment for focus
 * @param ray_exit_pupil_coord coordinate of B′, the test ray’s intersection
 *                             with the reference sphere, in the coordinate
 *                             system whose origin is the chief-ray exit-pupil
 *                             point Ē′
 * @param ray_op optical path length of aperture ray inside the lens
 * @param cr_op optical path length of chief ray inside the lens
 */
public record FinitePupilWaveAberrationResult(
        RayPkg ray_pkg,
        ChiefRayPkg chief_ray_pkg,
        ReferenceSphere ref_sphere,
        double e1,
        double ekp,
        double ep,
        Vector3 ray_exit_pupil_coord,
        double ray_op,
        double cr_op ) {
}
