package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.seq.Glass;

import static org.junit.jupiter.api.Assertions.*;

class LensTool2Test {
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
