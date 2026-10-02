package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LensTool2Test {
    @Test
    void referencedDistancesRejectMissingAndUndefinedSelectedValues() throws Exception {
        for (String values : new String[]{"45", "45\tundefined", "45\t", "45\tNaN", "45\tnonsense"}) {
            var specs = new OpticalBenchDataImporter.LensSpecifications();
            specs.parse_buffer(INPUT.replace("Bf\t45\t55", "Bf\t" + values));
            var error = assertThrows(IllegalArgumentException.class,
                    () -> LensTool2.createPrescription(specs, true, false));
            assertTrue(error.getMessage().contains("Bf"));
            assertTrue(error.getMessage().contains("scenario"));
        }
        var missing = new OpticalBenchDataImporter.LensSpecifications();
        var error = assertThrows(IllegalArgumentException.class,
                () -> missing.parse_buffer(INPUT.replace("Bf\t45\t55\n", "")));
        assertTrue(error.getMessage().contains("Referenced variable 'Bf'"));
    }

    @Test
    void referencedValuesValidateOnlySelectedScenariosAndAllowZero() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(INPUT.replace("Bf\t45\t55", "Bf\tundefined\t0")
                .replace("scenarios\t0\t1\nnames\tWide\tLong", "scenarios\t1\nnames\tLong"));
        var prescription = LensTool2.createPrescription(specs, true, false);
        assertEquals(0.0, prescription.get_surfaces()[1]._thickness);
    }

    private static Prescription stoppedPrescription() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(INPUT.replace("2\t-50\tBf\t\t20",
                "2\t-50\t2\t\t20\n3\tAS\tBf\t\t10"));
        return LensTool2.createPrescription(specs, true, false);
    }

    @Test
    void apertureDiameterRejectsMissingSelectedValue() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(stoppedPrescription().to_opt_bench_str(new StringBuilder()).toString()
                .replace("[variable distances]", "[variable distances]\nAperture Diameter\t10\tundefined"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> LensTool2.createPrescription(specs, true, false));
        assertTrue(error.getMessage().contains("Aperture Diameter"));
        assertTrue(error.getMessage().contains("scenario 1"));
    }

    @Test
    void savedAnalysisAperturesKeepConfigurationStopsAndLargestSharedDiameter() throws Exception {
        var prescription = stoppedPrescription();
        var models = new OpticalModel[2];
        for (int config = 0; config < 2; config++) {
            models[config] = LensTool2.createSystem(prescription, true, VigType.None,
                    false, new double[]{0.0, 1.0}, config);
            models[config].seq_model.ifcs.get(1).max_aperture = config == 0 ? 12 : 11;
            models[config].seq_model.ifcs.get(3).max_aperture = config == 0 ? 4 : 5;
        }
        var saved = LensTool2.prescriptionWithAnalysisApertures(prescription, models, VigType.SetApertures);
        assertEquals(24.0, saved.get_surfaces()[0]._diameter);
        assertArrayEquals(new double[]{8, 10}, saved.get_surfaces()[2]._diameter_by_scenario);
        assertEquals(8.0, saved.get_surfaces()[2]._diameter);
        assertEquals(20.0, prescription.get_surfaces()[0]._diameter);
        assertNull(prescription.get_surfaces()[2]._diameter_by_scenario);
        var restoredSpecs = new OpticalBenchDataImporter.LensSpecifications();
        restoredSpecs.parse_buffer(saved.to_opt_bench_str(new StringBuilder()).toString());
        var restored = LensTool2.createPrescription(restoredSpecs, true, false);
        assertArrayEquals(new double[]{8, 10}, restored.get_surfaces()[2]._diameter_by_scenario);
        assertEquals(24.0, restored.get_surfaces()[0]._diameter);
        var stopOnly = LensTool2.prescriptionWithAnalysisApertures(prescription, models, VigType.SetStopAperture);
        assertEquals(20.0, stopOnly.get_surfaces()[0]._diameter);
        assertArrayEquals(new double[]{8, 10}, stopOnly.get_surfaces()[2]._diameter_by_scenario);
        var unchanged = LensTool2.prescriptionWithAnalysisApertures(prescription, models, VigType.SetPupil);
        assertEquals(10.0, unchanged.get_surfaces()[2]._diameter);
    }

    @Test
    void actualApertureSizingIsSaved() throws Exception {
        for (var mode : new VigType[]{VigType.SetStopAperture, VigType.SetApertures, VigType.SetFnum}) {
            var prescription = stoppedPrescription();
            var models = new OpticalModel[2];
            for (int config = 0; config < 2; config++)
                models[config] = LensTool2.createSystem(prescription, true, mode,
                        false, new double[]{0.0, 1.0}, config);
            var saved = LensTool2.prescriptionWithAnalysisApertures(prescription, models, mode);
            for (int config = 0; config < 2; config++) {
                double expected = models[config].seq_model.ifcs.get(3).surface_od() * 2;
                assertEquals(expected, saved.get_surfaces()[2].get_diameter_by_scenario(config), 0.00005);
            }
            if (mode != VigType.SetApertures)
                assertNotEquals(prescription.get_surfaces()[2]._diameter, saved.get_surfaces()[2]._diameter);
        }
    }

    @Test
    void notesKeepOnlySourceFilenameAndDoNotChangeImportedGeometry() throws Exception {
        var prescription = stoppedPrescription();
        // Built with the platform separator, so the path is stripped on every OS
        Path specFile = Path.of("lenses", "test lens-trial3.txt").toAbsolutePath();
        String output = LensTool2.prescriptionOutput(prescription, specFile.toString());
        assertTrue(output.contains("[notes]\n"));
        assertTrue(output.contains("source prescription\ttest lens-trial3.txt\n"));
        assertFalse(output.contains(specFile.getParent().toString()));
        assertFalse(output.contains("argument "));
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(output);
        var restored = LensTool2.createPrescription(specs, true, false);
        assertEquals(prescription.get_surfaces()[2]._thickness, restored.get_surfaces()[2]._thickness);
    }

    private static final String INPUT = """
            [descriptive data]
            title	Test lens
            [variable distances]
            Focal Length	50	60
            F-Number	4	5
            Angle of View	40	35
            Bf	45	55
            [lens data]
            1	50	4	1.5	20	60
            2	-50	Bf		20
            [report data]
            lens name	Test lens
            scenarios	0	1
            names	Wide	Long
            """;

    @Test
    void firstSelectedScenarioSuppliesDefaultGeometryAndOpticalSpecs() throws Exception {
        for (String report : new String[]{"scenarios\t1\nnames\tLong",
                "scenarios\t1\t0\nnames\tLong\tWide"}) {
            String input = INPUT.replace("scenarios\t0\t1\nnames\tWide\tLong", report);
            assertNotEquals(INPUT, input, "fixture no longer contains the report being replaced");
            var specs = new OpticalBenchDataImporter.LensSpecifications();
            specs.parse_buffer(input);
            var prescription = LensTool2.createPrescription(specs, true, false);
            assertEquals(60.0, prescription._focal_length);
            assertEquals(5.0, prescription._fno);
            assertEquals(35.0, prescription._angle_of_view_in_degrees);
            assertEquals(55.0, prescription.get_surfaces()[1]._thickness);
            assertEquals(55.0, prescription.get_surfaces()[1].get_thickness_by_scenario(0));
            var model = LensTool2.createSystem(prescription, true, VigType.None,
                    false, new double[]{0.0, 1.0}, 0);
            assertEquals(5.0, model.optical_spec.pupil.value);
            assertEquals(17.5, model.optical_spec.fov.value);
            var weighted = LensTool2.createWeightedPrescription(prescription, false);
            assertEquals(60.0, weighted._focal_length);
            assertEquals(5.0, weighted._fno);
            assertEquals(35.0, weighted._angle_of_view_in_degrees);
        }
    }

    @Test
    void configurationIndexResolvesThroughSelectedScenarios() throws Exception {
        var reordered = new OpticalBenchDataImporter.LensSpecifications();
        reordered.parse_buffer(INPUT.replace("scenarios\t0\t1\nnames\tWide\tLong",
                "scenarios\t1\t0\nnames\tLong\tWide"));
        assertEquals(1, Prescription.scenario_of_configuration(reordered, 0));
        assertEquals(0, Prescription.scenario_of_configuration(reordered, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Prescription.scenario_of_configuration(reordered, 2));
        assertThrows(IllegalArgumentException.class,
                () -> Prescription.scenario_of_configuration(reordered, -1));
        assertThrows(IllegalArgumentException.class,
                () -> Prescription.build_prescription(reordered, true, new double[]{587.5618},
                        new double[]{1.0}, 2));

        // Without selected configurations the index is the scenario itself
        var unconfigured = new OpticalBenchDataImporter.LensSpecifications();
        unconfigured.parse_buffer(INPUT.replace("scenarios\t0\t1\nnames\tWide\tLong\n", ""));
        assertEquals(1, Prescription.scenario_of_configuration(unconfigured, 1));
    }

    @Test
    void reportsAndWeightedSpectrumKeepFinalAirspaces() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(INPUT);
        var prescription = LensTool2.createPrescription(specs, true, false);
        String originalReport = LensTool2.startREADME(prescription).toString();
        // Simulate the in-place airspace changes made by the optimizer.
        prescription.get_surfaces()[1]._thickness = 43.25;
        prescription.get_surfaces()[1]._thickness_by_scenario = new double[]{43.25, 52.75};

        var weighted = LensTool2.createWeightedPrescription(prescription, false);
        assertArrayEquals(new double[]{43.25, 52.75},
                weighted.get_surfaces()[1]._thickness_by_scenario);
        assertArrayEquals(new double[]{Glass.d, Glass.C, Glass.e, Glass.F, Glass.g}, weighted._wvls);
        assertArrayEquals(new double[]{1.0, 0.475, 0.98, 0.49, 0.15}, weighted._wts);
        String report = LensTool2.startREADME(prescription).toString();
        assertNotEquals(originalReport, report);
        assertTrue(report.contains("43.25"));
        assertTrue(report.contains("52.75"));
    }

    @Test
    void weightedDLineKeepsFinalPrimeBackFocusAndIgnoredGlassTypes() throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(INPUT.substring(0, INPUT.indexOf("[report data]"))
                .replace("1.5\t20\t60", "1.5\t20\t60\tN-BK7\tSchott"));
        var prescription = LensTool2.createPrescription(specs, false, true);
        prescription.get_surfaces()[1]._thickness = 43.25;
        var weighted = LensTool2.createWeightedPrescription(prescription, true);
        assertEquals(43.25, weighted.get_surfaces()[1]._thickness);
        assertNull(weighted.get_surfaces()[0]._glass_name);
        assertEquals(1.5, weighted.get_surfaces()[0].get_refractive_index());
        assertArrayEquals(new double[]{Glass.d}, weighted._wvls);
        assertArrayEquals(new double[]{1.0}, weighted._wts);
    }
}
