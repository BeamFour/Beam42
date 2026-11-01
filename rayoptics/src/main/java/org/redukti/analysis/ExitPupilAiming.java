// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.specs.Field;

/**
 * Inverse entrance-to-exit-pupil mapping used by contrast tracing.
 *
 * <p><b>Why a linear shift on the reference sphere is the right target.</b> The OTF is the
 * autocorrelation of the exit-pupil function at a fixed separation, and the aberration
 * enters that function's phase, not the separation. The separation is measured in Hopkins'
 * <em>reduced</em> coordinates, which are the transverse direction cosines of the chord
 * from the pupil point to the image point. For a pupil point {@code B'} on the sphere of
 * radius {@code R} about the image point {@code O}, with {@code c = B' - Ebar'} the stored
 * coordinate relative to the chief-ray exit-pupil point and {@code r} the reference
 * direction,
 *
 * <pre>{@code u = (O - B')/R = r - c/R}</pre>
 *
 * <p>so the transverse components of {@code u} are linear in {@code c} with coefficient
 * {@code -1/R} exactly - no obliquity factor, whatever the field angle. A transverse shift
 * of {@code R*lambda*nu/n} therefore produces a chord-direction difference of exactly
 * {@code lambda*nu/n}, which is the separation the OTF at {@code nu} integrates over.
 * Measured on the Leica 75/2: the realised chord-direction difference is the requested one
 * to six decimals at every field, axis and pupil radius, and the two coordinates sit on
 * the sphere to nine. See {@code ContrastProbe19} and the 2026-11-01 measurement in
 * Documentation/REVIEW.md.
 *
 * <p><b>What it deliberately does not do.</b> It does not make each pair's own fringes
 * land on {@code nu}. A traced ray's direction is the chord direction plus
 * {@code grad W / n}, so the pair's fringe frequency differs from {@code nu} by the
 * wavefront-slope difference across the shear - up to 8% at the edge of the pupil off
 * axis on that lens, and almost independent of {@code nu}, because both terms scale with
 * the shear and the ratio is set by the local second derivative of the wavefront. That
 * spread is the aberration being measured, not an error in the shear; equalising it would
 * make the sampling geometry a function of the design under optimization.
 */
public final class ExitPupilAiming {

    private static final int MAX_ITERATIONS = 10;
    private static final double ENTRANCE_STEP = 1.0e-4;

    private ExitPupilAiming() {
    }

    /**
     * Physical separation on the finite reference sphere corresponding to an
     * image-space spatial frequency.
     *
     * <p>The OTF's separation is {@code lambda*nu/n} in reduced coordinates - the
     * transverse direction cosines of the pupil-to-image chord - and those are the stored
     * sphere coordinate divided by {@code R}, so the physical separation is
     * {@code R*lambda*nu/n}. See the class comment for the derivation and for what this
     * does not claim about the pair's own fringe frequency. Taking {@code R} from the
     * traced reference sphere avoids converting through the paraxial exit-pupil radius
     * and f-number.
     */
    public static double referenceSphereShift(
            OpticalModel opticalModel, Field field, double wavelength,
            double spatialFrequency) {
        if (field.ref_sphere == null
                || M.is_kinda_big(field.ref_sphere.ref_sphere_radius)) {
            throw new IllegalArgumentException("Finite reference sphere required");
        }
        double wavelengthInSystemUnits = opticalModel.nm_to_sys_units(wavelength);
        double imageIndex = Math.abs(opticalModel.optical_spec.parax_data.fod.n_img);
        if (!(imageIndex > 0.0)) {
            throw new IllegalArgumentException("Positive image-space index required");
        }
        return wavelengthInSystemUnits * Math.abs(spatialFrequency)
                * Math.abs(field.ref_sphere.ref_sphere_radius) / imageIndex;
    }

    /** Result of aiming one ray at a transverse coordinate on the reference sphere. */
    public record Result(Vector2 pupil, RayResult ray, Vector3 ref_sphere_intersect_in_exit_pupil_coord,
                         int iterations, double error) {
    }

    /**
     * Determine the entrance pupil coordinates for ray aiming such that the aimed
     * ray get as close as possible to the desired target reference sphere coordinate
     * in the coordinate system of the exit pupil.
     */
    public static Result aim(
            OpticalModel opticalModel,
            Vector2 initialEntrancePupilCoord,
            Vector2 targetExitPupilCoord,
            Field field,
            double wavelength,
            TraceOptions traceOptions) {
        if (field.chief_ray == null || field.ref_sphere == null
                || M.is_kinda_big(field.ref_sphere.ref_sphere_radius)) {
            return failed(initialEntrancePupilCoord,
                    new ExitPupilAimException("Finite exit-pupil reference sphere required"));
        }

        double pupilRadius = Math.abs(opticalModel.optical_spec.parax_data.fod.exp_radius);
        // Sub-micrometre accuracy on a typical photographic exit pupil is already
        // far below the frequency resolution useful to the contrast merit function.
        // A tighter threshold stalls on trace/intersection roundoff for off-axis rays.
        double targetTolerance = Math.max(1.0e-10, pupilRadius * 2.0e-7);
        double acceptableTolerance = Math.max(targetTolerance, pupilRadius * 1.0e-6);
        Vector2 pupil = initialEntrancePupilCoord;
        Evaluation current = evaluate(
                opticalModel, pupil, targetExitPupilCoord, field, wavelength, traceOptions);
        if (current.ray.err != null) {
            return new Result(pupil, current.ray, current.ref_sphere_intersect_in_exit_pupil_coord, 0, Double.NaN);
        }
        if (current.ref_sphere_intersect_in_exit_pupil_coord == null)
            return failed(pupil, new ExitPupilAimException(
                    "Exit-pupil coordinate could not be computed"));

        for (int iteration = 0; iteration <= MAX_ITERATIONS; iteration++) {
            double error = current.residual.len();
            if (error <= targetTolerance) {
                return new Result(pupil, current.ray, current.ref_sphere_intersect_in_exit_pupil_coord, iteration, error);
            }
            if (iteration == MAX_ITERATIONS) break;

            Evaluation xProbe = evaluate(opticalModel,
                    pupil.plus(new Vector2(ENTRANCE_STEP, 0.0)), targetExitPupilCoord,
                    field, wavelength, traceOptions);
            Evaluation yProbe = evaluate(opticalModel,
                    pupil.plus(new Vector2(0.0, ENTRANCE_STEP)), targetExitPupilCoord,
                    field, wavelength, traceOptions);
            if (xProbe.ref_sphere_intersect_in_exit_pupil_coord == null || yProbe.ref_sphere_intersect_in_exit_pupil_coord == null) break;

            double j00 = (xProbe.ref_sphere_intersect_in_exit_pupil_coord.x - current.ref_sphere_intersect_in_exit_pupil_coord.x) / ENTRANCE_STEP;
            double j10 = (xProbe.ref_sphere_intersect_in_exit_pupil_coord.y - current.ref_sphere_intersect_in_exit_pupil_coord.y) / ENTRANCE_STEP;
            double j01 = (yProbe.ref_sphere_intersect_in_exit_pupil_coord.x - current.ref_sphere_intersect_in_exit_pupil_coord.x) / ENTRANCE_STEP;
            double j11 = (yProbe.ref_sphere_intersect_in_exit_pupil_coord.y - current.ref_sphere_intersect_in_exit_pupil_coord.y) / ENTRANCE_STEP;
            double determinant = j00 * j11 - j01 * j10;
            if (!Double.isFinite(determinant) || Math.abs(determinant) < 1.0e-14) break;

            // J.dp = -residual
            double dx = (-j11 * current.residual.x + j01 * current.residual.y) / determinant;
            double dy = (j10 * current.residual.x - j00 * current.residual.y) / determinant;
            if (!Double.isFinite(dx) || !Double.isFinite(dy)) break;

            // Backtracking keeps the inverse map stable near a strongly aberrated edge.
            Evaluation accepted = null;
            Vector2 acceptedPupil = null;
            for (int lineSearch = 0; lineSearch < 8; lineSearch++) {
                double scale = Math.scalb(1.0, -lineSearch);
                Vector2 trialPupil = pupil.plus(new Vector2(dx * scale, dy * scale));
                Evaluation trial = evaluate(opticalModel, trialPupil, targetExitPupilCoord,
                        field, wavelength, traceOptions);
                if (trial.ref_sphere_intersect_in_exit_pupil_coord != null && trial.residual.len() < error) {
                    accepted = trial;
                    acceptedPupil = trialPupil;
                    break;
                }
            }
            if (accepted == null) break;
            pupil = acceptedPupil;
            current = accepted;
        }

        double finalError = current.residual.len();
        if (finalError <= acceptableTolerance)
            return new Result(pupil, current.ray, current.ref_sphere_intersect_in_exit_pupil_coord,
                    MAX_ITERATIONS, finalError);
        return failed(pupil, new ExitPupilAimException(
                "Exit-pupil aiming did not converge; residual=" + current.residual.len()
                        + ", pupil=" + pupil));
    }

    private static Result failed(Vector2 pupil, TraceException error) {
        error.surf = -1;
        return new Result(pupil, new RayResult(null, error), null, MAX_ITERATIONS, Double.NaN);
    }

    private record Evaluation(RayResult ray, Vector3 ref_sphere_intersect_in_exit_pupil_coord, Vector2 residual) {
    }

    /**
     * Traces a test ray for given pupil to determine the intersection of the test
     * ray with the reference sphere associated with the Field. Returns the difference
     * between the desired target and achieved intersection.
     */
    private static Evaluation evaluate(
            OpticalModel opticalModel, Vector2 pupil, Vector2 targetRefSphereIntersection,
            Field field, double wavelength, TraceOptions traceOptions) {
        RayResult ray = Trace.trace_safe(opticalModel, pupil, field, wavelength, traceOptions);
        if (ray.pkg == null) return new Evaluation(ray, null, null);
        Vector3 achievedRefSphereIntersection = ref_sphere_intersection(ray.pkg, field.chief_ray, field.ref_sphere);
        if (achievedRefSphereIntersection == null) return new Evaluation(ray, null, null);
        return new Evaluation(ray, achievedRefSphereIntersection,
                new Vector2(achievedRefSphereIntersection.x - targetRefSphereIntersection.x, achievedRefSphereIntersection.y - targetRefSphereIntersection.y));
    }

    /**
     * Computes the intersection of the test ray with the reference sphere, and returns
     * the intersection point in the exit pupil coordinate system.
     *
     * @param rayPkg test ray
     * @param chiefRayPkg chief ray
     * @param referenceSphere Reference Sphere
     */
    public static Vector3 ref_sphere_intersection(
            RayPkg rayPkg, ChiefRayPkg chiefRayPkg, ReferenceSphere referenceSphere) {
        if (rayPkg == null || rayPkg.ray == null) return null;
        if (chiefRayPkg == null || referenceSphere == null) return null;
        if (M.is_kinda_big(referenceSphere.ref_sphere_radius)) return null;
        // The checks below are just sanity checks, as they are inadequate.
        // We assume that chief ray must have reached the image plane or else
        // we could not have got here.
        var chiefRay = chiefRayPkg.chief_ray.ray;
        if (chiefRay == null || chiefRay.size() < 2) return null;
        var testRay = rayPkg.ray;
        if (testRay.size() != chiefRay.size()) return null;

        return WaveAbr.wave_abr_calc_finite_pupil(rayPkg, chiefRayPkg, referenceSphere).ref_sphere_intersect_in_cr_exp_coord();
    }

    /** A ray traced successfully, but its requested exit-pupil coordinate was not found. */
    public static final class ExitPupilAimException extends TraceException {
        public ExitPupilAimException(String message) {
            super(message);
        }
    }
}
