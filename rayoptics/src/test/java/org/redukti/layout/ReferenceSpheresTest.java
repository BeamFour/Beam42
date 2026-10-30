package org.redukti.layout;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.util.Args;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceSpheresTest {
    private static final double TOLERANCE = 1e-7;

    private static OpticalModel model(String specfile, double field) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        var prescription = Prescription.build_prescription(specs, true,
                new double[]{587.5618}, new double[]{1.0}, 0);
        return new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, new double[]{0.0, field}, false, VigType.SetPupil, true, 0);
    }

    private static final String LENS = "../Examples/jfotoptix/leica-m-35mm-f1.2/prescription.txt";

    private static ReferenceSpheres spheres(OpticalModel model, int field) {
        TraceOptions options = new TraceOptions();
        options.rayerr_filter = "full";
        return ReferenceSpheres.compute(model, model.optical_spec.fov.fields[field],
                model.seq_model.central_wavelength(), options);
    }

    private static double distance(Vector2 a, Vector2 b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    @Test
    public void rimRaysEndOnTheExitSphere() throws Exception {
        var spheres = spheres(model(LENS, 1.0), 1);
        assertEquals(2, spheres.rims.size());
        assertEquals(0.0, spheres.exit.pupil().y(), TOLERANCE);
        assertEquals(spheres.exit.radius(), distance(spheres.exit.centre(), spheres.exit.pupil()), TOLERANCE);
        assertEquals(0.0, distance(spheres.chief.exitHit(), spheres.exit.pupil()), TOLERANCE);
        for (var rim : spheres.rims)
            assertEquals(spheres.exit.radius(), distance(spheres.exit.centre(), rim.exitHit()), TOLERANCE);
    }

    @Test
    public void entrancePupilIsOnTheAxisAndAgreesWithTheAim() throws Exception {
        var model = model(LENS, 1.0);
        var spheres = spheres(model, 1);
        var field = model.optical_spec.fov.fields[1];
        assertNotNull(field.z_enp);
        assertTrue(spheres.entrance.isPlane());
        assertEquals(0.0, spheres.entrance.pupil().y(), TOLERANCE);
        assertEquals(field.z_enp, spheres.entrance.pupil().x() - model.seq_model.gbl_tfrms.get(1).t.z, 1e-12);
    }

    @Test
    public void rimRaysEndOnTheEntrancePlane() throws Exception {
        var spheres = spheres(model(LENS, 1.0), 1);
        var direction = spheres.chief.incidentDir();
        for (var rim : spheres.rims) {
            var hit = rim.entranceHit();
            var pupil = spheres.entrance.pupil();
            double offset = (hit.x() - pupil.x()) * direction.x() + (hit.y() - pupil.y()) * direction.y();
            assertEquals(0.0, offset, TOLERANCE);
        }
    }

    @Test
    public void finiteObjectHasASphereCentredOnIt() throws Exception {
        var model = model(LENS, 1.0);
        model.seq_model.gaps.get(0).thi = 500.0;
        model.update_model();
        var spheres = spheres(model, 1);
        var entrance = spheres.entrance;
        assertFalse(entrance.isPlane());
        assertEquals(0.0, entrance.pupil().y(), TOLERANCE);
        assertEquals(entrance.radius(), distance(entrance.centre(), entrance.pupil()), TOLERANCE);
        assertEquals(0.0, distance(spheres.chief.entranceHit(), entrance.pupil()), TOLERANCE);
        assertEquals(2, spheres.rims.size());
        for (var rim : spheres.rims)
            assertEquals(entrance.radius(), distance(entrance.centre(), rim.entranceHit()), TOLERANCE);
        Path output = Path.of("target", "layout-tests");
        Files.createDirectories(output);
        Files.writeString(output.resolve("reference-spheres-finite.svg"), new Layout2D().renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false).referenceSphereField(1)));
    }

    @Test
    public void axialFieldUsesTheParaxialPupils() throws Exception {
        var spheres = spheres(model(LENS, 1.0), 0);
        assertEquals(spheres.entrance.paraxialPupil().x(), spheres.entrance.pupil().x(), TOLERANCE);
        assertEquals(spheres.exit.paraxialPupil().x(), spheres.exit.pupil().x(), TOLERANCE);
    }

    @Test
    public void rendersAtFullAndPartialField() throws Exception {
        Path output = Path.of("target", "layout-tests");
        Files.createDirectories(output);
        for (double field : new double[]{1.0, 0.7, 0.0}) {
            var model = model(LENS, field);
            String svg = new Layout2D().renderSvg(model, 1000, 500,
                    new LayoutOptions().drawReferenceRays(false).referenceSphereField(1));
            assertFalse(svg.contains("NaN"));
            assertTrue(svg.contains("E'"));
            Files.writeString(output.resolve("reference-spheres-" + field + ".svg"), svg);
        }
    }

    @Test
    public void relativeFieldIsValidated() {
        assertEquals(1.0, Args.parseArguments(new String[]{"--output-reference-spheres"}).reference_sphere_field);
        assertEquals(0.7, Args.parseArguments(new String[]{"--reference-sphere-field", "0.7"}).reference_sphere_field);
        for (String value : new String[]{"-0.1", "1.1", "NaN", "abc"})
            assertThrows(IllegalArgumentException.class,
                    () -> Args.parseArguments(new String[]{"--reference-sphere-field", value}));
    }
}
