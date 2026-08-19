package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.exceptions.TraceException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

/** Inverse entrance-to-exit-pupil mapping used by contrast tracing. */
public final class ExitPupilAiming {

    private static final int MAX_ITERATIONS = 10;
    private static final double ENTRANCE_STEP = 1.0e-4;

    private ExitPupilAiming() {
    }

    /** Result of aiming one ray at a transverse coordinate on the reference sphere. */
    public record Result(Vector2 pupil, RayResult ray, Vector3 exitCoordinate,
                         int iterations, double error) {
    }

    /**
     * Aim a ray so its transverse coordinate on the exit-pupil reference sphere equals
     * {@code target}. The initial entrance-pupil coordinate is normally the traditional
     * entrance-pupil displacement and is therefore already a close guess.
     */
    public static Result aim(
            OpticalModel opticalModel, Vector2 initialPupil, Vector2 target,
            Field field, double wavelength, TraceOptions traceOptions) {
        if (field.chief_ray == null || field.ref_sphere == null
                || M.is_kinda_big(field.ref_sphere.ref_sphere_radius)) {
            return failed(initialPupil,
                    new ExitPupilAimException("Finite exit-pupil reference sphere required"));
        }

        double pupilRadius = Math.abs(opticalModel.optical_spec.parax_data.fod.exp_radius);
        // Sub-micrometre accuracy on a typical photographic exit pupil is already
        // far below the frequency resolution useful to the contrast merit function.
        // A tighter threshold stalls on trace/intersection roundoff for off-axis rays.
        double tolerance = Math.max(1.0e-10, pupilRadius * 2.0e-7);
        Vector2 pupil = initialPupil;
        Evaluation current = evaluate(
                opticalModel, pupil, target, field, wavelength, traceOptions);
        if (current.ray.err != null) {
            return new Result(pupil, current.ray, current.coordinate, 0, Double.NaN);
        }
        if (current.coordinate == null)
            return failed(pupil, new ExitPupilAimException(
                    "Exit-pupil coordinate could not be computed"));

        for (int iteration = 0; iteration <= MAX_ITERATIONS; iteration++) {
            double error = current.residual.len();
            if (error <= tolerance) {
                return new Result(pupil, current.ray, current.coordinate, iteration, error);
            }
            if (iteration == MAX_ITERATIONS) break;

            Evaluation xProbe = evaluate(opticalModel,
                    pupil.plus(new Vector2(ENTRANCE_STEP, 0.0)), target,
                    field, wavelength, traceOptions);
            Evaluation yProbe = evaluate(opticalModel,
                    pupil.plus(new Vector2(0.0, ENTRANCE_STEP)), target,
                    field, wavelength, traceOptions);
            if (xProbe.coordinate == null || yProbe.coordinate == null) break;

            double j00 = (xProbe.coordinate.x - current.coordinate.x) / ENTRANCE_STEP;
            double j10 = (xProbe.coordinate.y - current.coordinate.y) / ENTRANCE_STEP;
            double j01 = (yProbe.coordinate.x - current.coordinate.x) / ENTRANCE_STEP;
            double j11 = (yProbe.coordinate.y - current.coordinate.y) / ENTRANCE_STEP;
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
                Evaluation trial = evaluate(opticalModel, trialPupil, target,
                        field, wavelength, traceOptions);
                if (trial.coordinate != null && trial.residual.len() < error) {
                    accepted = trial;
                    acceptedPupil = trialPupil;
                    break;
                }
            }
            if (accepted == null) break;
            pupil = acceptedPupil;
            current = accepted;
        }

        return failed(pupil, new ExitPupilAimException(
                "Exit-pupil aiming did not converge; residual=" + current.residual.len()
                        + ", pupil=" + pupil));
    }

    private static Result failed(Vector2 pupil, TraceException error) {
        error.surf = -1;
        return new Result(pupil, new RayResult(null, error), null, MAX_ITERATIONS, Double.NaN);
    }

    private record Evaluation(RayResult ray, Vector3 coordinate, Vector2 residual) {
    }

    private static Evaluation evaluate(
            OpticalModel opticalModel, Vector2 pupil, Vector2 target,
            Field field, double wavelength, TraceOptions traceOptions) {
        RayResult ray = Trace.trace_safe(opticalModel, pupil, field, wavelength, traceOptions);
        if (ray.pkg == null) return new Evaluation(ray, null, null);
        Vector3 coordinate = sphere_coord(ray.pkg, field.chief_ray, field.ref_sphere);
        if (coordinate == null) return new Evaluation(ray, null, null);
        return new Evaluation(ray, coordinate,
                new Vector2(coordinate.x - target.x, coordinate.y - target.y));
    }

    /** Equally-inclined-chord coordinate relative to the exit-pupil centre. */
    public static Vector3 chord_coord(
            RayPkg rayPkg, ChiefRayPkg chiefRayPkg, ReferenceSphere referenceSphere) {
        if (rayPkg == null || rayPkg.ray == null || rayPkg.ray.size() < 2) return null;
        if (chiefRayPkg == null || referenceSphere == null) return null;
        if (M.is_kinda_big(referenceSphere.ref_sphere_radius)) return null;

        var chiefRay = chiefRayPkg.chief_ray.ray;
        if (chiefRay == null || chiefRay.size() < 2) return null;
        var chiefExit = chiefRayPkg.cr_exp_seg;
        int k = -2;
        var ray = rayPkg.ray;
        double ekp = WaveAbr.eic_distance(
                new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d),
                new RayData(Lists.get(chiefRay, k).p, Lists.get(chiefRay, k).d));
        var after = Transform.transform_after_surface(
                chiefExit.ifc, new RayData(Lists.get(ray, k).p, Lists.get(ray, k).d));
        double distance = ekp - chiefExit.exp_dst;
        return after.pt.minus(after.dir.times(distance)).minus(chiefExit.exp_pt);
    }

    /** Coordinate on the exit-pupil reference sphere relative to its pupil centre. */
    public static Vector3 sphere_coord(
            RayPkg rayPkg, ChiefRayPkg chiefRayPkg, ReferenceSphere referenceSphere) {
        Vector3 coordinate = chord_coord(rayPkg, chiefRayPkg, referenceSphere);
        if (coordinate == null) return null;

        int k = -2;
        var segment = Lists.get(rayPkg.ray, k);
        Vector3 direction = Transform.transform_after_surface(
                chiefRayPkg.cr_exp_seg.ifc, new RayData(segment.p, segment.d)).dir;
        double radius = referenceSphere.ref_sphere_radius;
        Vector3 referenceDirection = referenceSphere.ref_dir;
        double f = referenceDirection.dot(direction) - direction.dot(coordinate) / radius;
        double j = coordinate.dot(coordinate) / radius
                - 2.0 * referenceDirection.dot(coordinate);
        double discriminant = f * f - j / radius;
        if (discriminant < 0.0) return null;
        double denominator = f + Math.sqrt(discriminant);
        double ep = denominator == 0.0 ? 0.0 : j / denominator;
        return coordinate.plus(direction.times(ep));
    }

    /** A ray traced successfully, but its requested exit-pupil coordinate was not found. */
    public static final class ExitPupilAimException extends TraceException {
        public ExitPupilAimException(String message) {
            super(message);
        }
    }
}
