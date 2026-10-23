package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.M;
import org.redukti.rayoptics.util.Orientation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrastReportTest {

    @Test
    void tabulatesOneRowPerFrequencyWavelengthAndOrientation() {
        var model = MtfTest.buildTestModel();
        var options = new ContrastOptions(0.0).num_rings(2).num_spokes(6);

        String report = ContrastReport.report(model, new int[]{10, 40}, options);

        var lines = report.lines().filter(line -> line.startsWith(" ")).toList();
        var header = lines.get(0).trim().split("\\s+");
        int fields = model.optical_spec.fov.fields.length;
        int wavelengths = model.optical_spec.wvls.wavelengths.length;
        assertEquals(3 + fields, header.length);
        assertEquals("freq", header[0]);
        assertEquals(1 + 2 * 2 * wavelengths, lines.size());

        // First row: lowest frequency, first wavelength, tangential.
        var row = lines.get(1).trim().split("\\s+");
        assertEquals("10", row[0]);
        assertEquals("tan", row[2]);
        assertEquals("sag", lines.get(2).trim().split("\\s+")[2]);

        var result = ContrastAnalysis.eval(model, options.with_frequency(10));
        var format = M.decimal_format(4);
        for (int fi = 0; fi < fields; fi++) {
            var block = result.fields.get(fi).wavelengths().get(0);
            assertEquals(format.format(ContrastReport.rms(block, Orientation.TANGENTIAL)), row[3 + fi]);
        }
    }

    @Test
    void rmsIsNormalizedByTheWeightOfValidSamples() {
        var model = MtfTest.buildTestModel();
        var result = ContrastAnalysis.eval(model, new ContrastOptions(40.0).num_rings(2).num_spokes(6));
        var block = result.fields.get(1).wavelengths().get(0);

        double sum = 0.0;
        for (var sample : block.samples())
            sum += sample.weight() * sample.tangentialOpdDifference() * sample.tangentialOpdDifference();

        double rms = ContrastReport.rms(block, Orientation.TANGENTIAL);
        assertTrue(rms > 0.0);
        assertEquals(Math.sqrt(sum), rms, 1.0e-12);
    }
}
