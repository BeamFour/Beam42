package org.redukti.exporters;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.Prescription.FieldVignetting;
import org.redukti.tools.LensTool2;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZemaxVignettingExportTest {

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

    private static final String SINGLE = INPUT.replace("scenarios\t0\t1\nnames\tWide\tLong\n", "");

    private static Prescription prescription(String input) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(input);
        return LensTool2.createPrescription(specs, true, false);
    }

    /** The numbers following {@code key} on its line. */
    private static double[] values(String zmx, String key) {
        for (String line : zmx.split("\n"))
            if (line.startsWith(key + " ")) {
                var words = line.trim().split("\\s+");
                var v = new double[words.length - 1];
                for (int i = 1; i < words.length; i++)
                    v[i - 1] = Double.parseDouble(words[i]);
                return v;
            }
        throw new AssertionError(key + " not found");
    }

    /** The value of the multi-configuration operand {@code op} for field and config, 1-based. */
    private static double operand(String zmx, String op, int field, int config) {
        for (String line : zmx.split("\n")) {
            var words = line.trim().split("\\s+");
            if (words[0].equals(op) && Integer.parseInt(words[1]) == field && Integer.parseInt(words[2]) == config)
                return Double.parseDouble(words[3]);
        }
        throw new AssertionError(op + " " + field + " " + config + " not found");
    }

    @Test
    void withoutVignettingTheFieldsAreUnchanged() throws Exception {
        String zmx = new ZemaxExporter().generate(prescription(SINGLE), false);
        assertTrue(zmx.contains("SDMA 0 1 0\nFTYP 3 0 9 3 0 0 0 9\nROPD 2\nHYPR 0\nPICB 1\n"
                + "XFLN 0 0 0 0 0 0 0 0 0 0 0 0\nYFLN 0 2.16 4.33 6.5 8.65 10.8 12.98 15.14 "));
        assertTrue(zmx.contains("VCYN 0 0 0 0 0 0 0 0 0 0 0 0\n"));
        assertFalse(zmx.contains("FVDY"));
    }

    @Test
    void anglesAndFactorsOfOneConfiguration() throws Exception {
        var p = prescription(SINGLE);
        p._vignetting = List.of(
                new FieldVignetting(0, 0.0, 0, 0, 0, 0),
                new FieldVignetting(0, 0.5, 0.02, 0.02, 0.1, 0.08),
                new FieldVignetting(0, 1.0, 0.1, 0.1, 0.3, 0.2));
        String zmx = new ZemaxExporter().generate(p, false);
        assertTrue(zmx.contains("\nFTYP 0 0 3 3 0 0 0 3\n"));
        var y = values(zmx, "YFLN");
        assertEquals(12, y.length);
        assertArrayEquals(new double[]{0, 10, 20}, java.util.Arrays.copyOf(y, 3), 1e-12);
        for (int i = 3; i < 12; i++)
            assertEquals(0.0, y[i]);
        var vdx = values(zmx, "VDXN");
        var vdy = values(zmx, "VDYN");
        var vcx = values(zmx, "VCXN");
        var vcy = values(zmx, "VCYN");
        // Zemax maps P to VD + P(1 - VC): the pupil edges land where our factors put them
        for (int i = 0; i < 3; i++) {
            var v = p._vignetting.get(i);
            assertEquals(1 - v.vuy(), vdy[i] + (1 - vcy[i]), 1e-12);
            assertEquals(-1 + v.vly(), vdy[i] - (1 - vcy[i]), 1e-12);
            assertEquals(1 - v.vux(), vdx[i] + (1 - vcx[i]), 1e-12);
            assertEquals(-1 + v.vlx(), vdx[i] - (1 - vcx[i]), 1e-12);
        }
        assertFalse(zmx.contains("MNUM"));
    }

    @Test
    void eachConfigurationHasItsOwnAnglesAndFactors() throws Exception {
        var p = prescription(INPUT);
        p._vignetting = List.of(
                new FieldVignetting(0, 0.0, 0, 0, 0, 0),
                new FieldVignetting(0, 1.0, 0.1, 0.1, 0.3, 0.2),
                new FieldVignetting(1, 0.0, 0, 0, 0, 0),
                new FieldVignetting(1, 1.0, 0.05, 0.05, 0.4, 0.1));
        String zmx = new ZemaxExporter().generate(p, false);
        assertTrue(zmx.contains("\nMNUM 2 1\n"));
        // half of 40 and of 35 degrees
        assertEquals(20.0, operand(zmx, "YFIE", 2, 1), 1e-12);
        assertEquals(17.5, operand(zmx, "YFIE", 2, 2), 1e-12);
        assertEquals(0.0, operand(zmx, "YFIE", 1, 2), 1e-12);
        assertEquals((0.2 - 0.3) / 2, operand(zmx, "FVDY", 2, 1), 1e-12);
        assertEquals((0.1 - 0.4) / 2, operand(zmx, "FVDY", 2, 2), 1e-12);
        assertEquals((0.4 + 0.1) / 2, operand(zmx, "FVCY", 2, 2), 1e-12);
        assertEquals(0.05, operand(zmx, "FVCX", 2, 2), 1e-12);
        assertEquals(0.0, operand(zmx, "FVDX", 2, 2), 1e-12);
        // one operand row holds all its configurations, in order
        assertTrue(zmx.contains("YFIE    2   1 20.0  0 0 0 1 1 1 0 0\nYFIE    2   2 17.5  0 0 0 1 1 1 0 0\n"));
    }

    @Test
    void rejectsConfigurationsWithDifferentFields() throws Exception {
        var p = prescription(INPUT);
        var rows = new ArrayList<FieldVignetting>();
        rows.add(new FieldVignetting(0, 0.0, 0, 0, 0, 0));
        rows.add(new FieldVignetting(0, 1.0, 0, 0, 0, 0));
        rows.add(new FieldVignetting(1, 0.0, 0, 0, 0, 0));
        p._vignetting = rows;
        assertThrows(IllegalArgumentException.class, () -> new ZemaxExporter().generate(p, false));
        rows.add(new FieldVignetting(1, 0.7, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ZemaxExporter().generate(p, false));
    }
}
