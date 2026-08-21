package org.redukti.rayoptics.raytr;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.integration.US003549241Example05;
import org.redukti.rayoptics.util.Lists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the finite-reference-sphere correction in {@link WaveAbr}. */
class WaveAbrFinitePupilTest {

    @Test
    void hopkinsDiscriminantPlacesChordPointOnReferenceSphere() {
        var fixture = tracedOffAxisRay();
        var geometry = sphereGeometry(fixture.ray, fixture.setup.chief_ray_pkg,
                fixture.setup.ref_sphere);

        double ep = sphereDistance(geometry, false);
        double residual = sphereResidual(geometry, ep);

        assertEquals(0.0, residual, 1.0e-11,
                "F^2 - J/R must put the displaced chord point on the reference sphere");

        // This is the expression presently used by wave_abr_full_calc_finite_pup().
        // It is retained here to demonstrate that changing '-' to '+' is not an
        // equivalent sign convention: the resulting point is not on the sphere.
        double upstreamEp = sphereDistance(geometry, true);
        double upstreamResidual = sphereResidual(geometry, upstreamEp);
        assertTrue(Math.abs(upstreamResidual) > 1.0e-6,
                "F^2 + J/R should expose a measurable sphere-equation residual");
    }

    @Test
    void finitePupilOpdUsesTheGeometricReferenceSphereIntersection() {
        var fixture = tracedOffAxisRay();
        var chief = fixture.setup.chief_ray_pkg;
        var sphere = fixture.setup.ref_sphere;
        var fod = fixture.model.optical_spec.parax_data.fod;
        var geometry = sphereGeometry(fixture.ray, chief, sphere);

        double e1 = WaveAbr.eic_distance(
                new RayData(fixture.ray.ray.get(1).p, fixture.ray.ray.get(0).d),
                new RayData(chief.chief_ray.ray.get(1).p, chief.chief_ray.ray.get(0).d));
        double ep = sphereDistance(geometry, false);
        double expected = -Math.abs(fod.n_obj) * e1
                - fixture.ray.op_delta
                + Math.abs(fod.n_img) * geometry.ekp
                + chief.chief_ray.op_delta
                - Math.abs(fod.n_img) * ep;

        double actual = WaveAbr.wave_abr_full_calc(fod, fixture.field, fixture.wavelength,
                fixture.focus, fixture.ray, chief, sphere);

        // This assertion intentionally fails while WaveAbr uses F^2 + J/R. It directly
        // connects the geometric invariant above to the public OPD calculation.
        assertEquals(expected, actual, 1.0e-12,
                "finite-pupil OPD must terminate the ray on the reference sphere");
    }

    private static Fixture tracedOffAxisRay() {
        var model = US003549241Example05.build();
        var field = model.optical_spec.fov.fields[2];
        double wavelength = model.seq_model.central_wavelength();
        double focus = model.optical_spec.defocus().get_focus();
        var setup = Trace.setup_pupil_coords(model, field, wavelength, focus, null, null);
        field.chief_ray = setup.chief_ray_pkg;
        field.ref_sphere = setup.ref_sphere;
        var ray = Trace.trace_base(model, new double[]{0.65, 0.45}, field, wavelength,
                new TraceOptions());
        return new Fixture(model, field, wavelength, focus, setup, ray);
    }

    private static SphereGeometry sphereGeometry(
            RayPkg rayPkg, ChiefRayPkg chiefRayPkg, ReferenceSphere sphere) {
        int lastRealSurface = -2;
        var ray = rayPkg.ray;
        var chiefRay = chiefRayPkg.chief_ray.ray;
        double ekp = WaveAbr.eic_distance(
                new RayData(Lists.get(ray, lastRealSurface).p,
                        Lists.get(ray, lastRealSurface).d),
                new RayData(Lists.get(chiefRay, lastRealSurface).p,
                        Lists.get(chiefRay, lastRealSurface).d));
        var after = Transform.transform_after_surface(chiefRayPkg.cr_exp_seg.ifc,
                new RayData(Lists.get(ray, lastRealSurface).p,
                        Lists.get(ray, lastRealSurface).d));
        double distance = ekp - chiefRayPkg.cr_exp_seg.exp_dst;
        var chordCoordinate = after.pt.minus(after.dir.times(distance))
                .minus(chiefRayPkg.cr_exp_seg.exp_pt);
        double radius = sphere.ref_sphere_radius;
        double f = sphere.ref_dir.dot(after.dir)
                - after.dir.dot(chordCoordinate) / radius;
        double j = chordCoordinate.dot(chordCoordinate) / radius
                - 2.0 * sphere.ref_dir.dot(chordCoordinate);
        double solutionSign = sphere.ref_dir.z * Lists.get(chiefRay, -1).d.z < 0.0
                ? -1.0 : 1.0;
        return new SphereGeometry(radius, f, j, solutionSign, ekp);
    }

    private static double sphereDistance(SphereGeometry geometry, boolean upstreamPlusSign) {
        double signedTerm = geometry.j / geometry.radius;
        double discriminant = geometry.f * geometry.f
                + (upstreamPlusSign ? signedTerm : -signedTerm);
        assertTrue(discriminant >= 0.0, "ray must intersect the reference sphere");
        double denominator = geometry.f
                + geometry.solutionSign * Math.sqrt(discriminant);
        return denominator == 0.0 ? 0.0 : geometry.j / denominator;
    }

    private static double sphereResidual(SphereGeometry geometry, double ep) {
        return ep * ep - 2.0 * geometry.radius * geometry.f * ep
                + geometry.radius * geometry.j;
    }

    private record SphereGeometry(
            double radius, double f, double j, double solutionSign, double ekp) {}

    private record Fixture(
            org.redukti.rayoptics.optical.OpticalModel model,
            org.redukti.rayoptics.specs.Field field,
            double wavelength,
            double focus,
            RefSphereCR setup,
            RayPkg ray) {}
}
