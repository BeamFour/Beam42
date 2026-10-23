package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.analysis.ContrastAnalysis;
import org.redukti.analysis.ContrastRayTriplet;
import org.redukti.analysis.ExitPupilAiming;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Fixed-lens check of the reference-sphere target, not of exit-pupil integration weights. */
class SummicronExitPupilDiagnosticTest {
    @Test
    void comparesOrdinaryAndAimedReferenceSphereShear() throws Exception {
        var prescription = GenericContrastOpt.getPrescription(ExampleFinder.geoPathToExample(
                "Examples/jfotoptix/leica-m-summicron-50mm-f2/US004123144_Example09P.txt"), false, false);
        double[] fields = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        var model = new RayOpticsModelBuilder(prescription).build_optical_model(
                true, fields, false, VigType.SetVig, true, 0);
        var options = new TraceOptions();
        options.check_apertures = false;
        StringBuilder csv = new StringBuilder("aimed,field,wavelength_nm,frequency,axis,attempted,reference_failures,partner_failures,coordinate_failures,valid,target_mm,ratio_min,ratio_mean,ratio_max,max_cross_mm,max_vector_error_mm\n");
        double aimedMaxError = 0;
        int aimedFailures = 0;
        for (int frequency : new int[]{10, 20, 40})
            for (int fi = 0; fi < fields.length; fi++)
                for (int wi = 0; wi < model.optical_spec.wvls.wavelengths.length; wi++) {
                    double wavelength = model.optical_spec.wvls.wavelengths[wi];
                    double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wavelength, frequency);
                    for (boolean aimed : new boolean[]{false, true}) {
                        var rows = ContrastAnalysis.trace_by_wavelength(model,
                                (rays, field, wvl, focus) -> new Measurement(rays,
                                        ExitPupilAiming.sphere_coord(rays.reference(), field.chief_ray, field.ref_sphere),
                                        ExitPupilAiming.sphere_coord(rays.sagittal(), field.chief_ray, field.ref_sphere),
                                        ExitPupilAiming.sphere_coord(rays.tangential(), field.chief_ray, field.ref_sphere)),
                                fi, wi, 6, 12, new Vector2(shift, 0), new Vector2(0, shift),
                                frequency, options, aimed).get(0).samples();
                        double target = ExitPupilAiming.referenceSphereShift(model,
                                model.optical_spec.fov.fields[fi], wavelength, frequency);
                        assertEquals(72, rows.size());
                        for (int axis = 0; axis < 2; axis++) {
                            int referenceFailures = 0, partnerFailures = 0, coordinateFailures = 0, valid = 0;
                            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                            double sum = 0, cross = 0, error = 0;
                            for (var row : rows) {
                                var rays = row.rays();
                                if (rays.referenceError() != null || rays.reference() == null) {
                                    referenceFailures++;
                                    continue;
                                }
                                if ((axis == 0 ? rays.sagittalError() : rays.tangentialError()) != null
                                        || (axis == 0 ? rays.sagittal() : rays.tangential()) == null) {
                                    partnerFailures++;
                                    continue;
                                }
                                var partner = axis == 0 ? row.sagittal() : row.tangential();
                                var reference = row.reference();
                                if (reference == null || partner == null) {
                                    coordinateFailures++;
                                    continue;
                                }
                                double dx = partner.x - reference.x, dy = partner.y - reference.y;
                                double along = axis == 0 ? dx : dy;
                                double across = axis == 0 ? dy : dx;
                                double ratio = along / target;
                                assertTrue(Double.isFinite(ratio) && Double.isFinite(across));
                                min = Math.min(min, ratio);
                                max = Math.max(max, ratio);
                                sum += ratio;
                                cross = Math.max(cross, Math.abs(across));
                                error = Math.max(error, Math.hypot(along - target, across));
                                valid++;
                            }
                            assertEquals(rows.size(), valid + referenceFailures + partnerFailures + coordinateFailures);
                            assertTrue(valid > 0);
                            csv.append(String.format(Locale.ROOT,
                                    "%s,%.1f,%.4f,%d,%s,%d,%d,%d,%d,%d,%.12g,%.12g,%.12g,%.12g,%.12g,%.12g%n",
                                    aimed, fields[fi], wavelength, frequency, axis == 0 ? "sag" : "tan",
                                    rows.size(), referenceFailures, partnerFailures, coordinateFailures, valid,
                                    target, min, sum / valid, max, cross, error));
                            if (aimed) {
                                aimedMaxError = Math.max(aimedMaxError, error);
                                aimedFailures += referenceFailures + partnerFailures + coordinateFailures;
                            }
                        }
                    }
                }
        Path output = Path.of("target", "summicron-exit-pupil-diagnostic.csv");
        Files.writeString(output, csv);
        System.out.printf(Locale.ROOT, "Summicron aimed failures=%d maxVectorErrorMm=%.12g report=%s%n",
                aimedFailures, aimedMaxError, output);
        assertEquals(0, aimedFailures, "See diagnostic CSV for failed rays");
        assertTrue(aimedMaxError <= Math.max(1e-10,
                Math.abs(model.optical_spec.parax_data.fod.exp_radius) * 1e-6),
                "Aimed shear exceeds the aiming acceptance tolerance");
    }

    private record Measurement(ContrastRayTriplet rays, Vector3 reference, Vector3 sagittal, Vector3 tangential) {}
}
