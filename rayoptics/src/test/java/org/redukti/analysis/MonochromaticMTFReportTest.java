package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.M;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonochromaticMTFReportTest {

    @Test
    void tabulatesOneRowPerFrequencyWavelengthAndOrientation() {
        var model = MtfTest.buildTestModel();
        var spotAnalysis = SpotAnalysis.eval(model, new SpotOptions().use_hexapolar().num_rings(21));
        var mtfs = new ArrayList<List<MonochromaticGeometricMTF>>();
        var fields = new double[spotAnalysis.spot_results.size()];
        for (int i = 0; i < fields.length; i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            fields[i] = spotFld.fld.y;
            var cfg = spotFld.mtfHistogramConfig();
            var field = new ArrayList<MonochromaticGeometricMTF>();
            for (var intercepts : spotFld.intercepts)
                field.add(new MonochromaticGeometricMTF(intercepts, cfg));
            mtfs.add(field);
        }

        String report = MonochromaticMTFReport.report(fields, mtfs, new int[]{10, 30});

        var lines = report.lines().filter(line -> line.startsWith(" ")).toList();
        int wavelengths = mtfs.get(0).size();
        assertEquals(3 + fields.length, lines.get(0).trim().split("\\s+").length);
        assertEquals(1 + 2 * 2 * wavelengths, lines.size());

        // Second row: lowest frequency, first wavelength, sagittal.
        var row = lines.get(2).trim().split("\\s+");
        assertEquals("10", row[0]);
        assertEquals("sag", row[2]);
        var format = M.decimal_format(3);
        for (int fi = 0; fi < fields.length; fi++) {
            var mtf = mtfs.get(fi).get(0).mtf;
            assertEquals(format.format(MTFResultByFreq.interpolate(mtf.freq, mtf.mag_x, 10)), row[3 + fi]);
        }
    }
}
