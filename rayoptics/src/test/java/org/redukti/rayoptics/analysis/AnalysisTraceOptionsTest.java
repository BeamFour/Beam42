package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisTraceOptionsTest {

    @Test
    void spotChecksAperturesByDefaultAndCanDisableThem() {
        SpotOptions options = new SpotOptions();
        assertTrue(options._trace_options.check_apertures);
        assertFalse(options.check_apertures(false)._trace_options.check_apertures);
    }

    @Test
    void contrastSkipsApertureChecksByDefaultAndCanEnableThem() {
        ContrastOptions options = new ContrastOptions(40.0);
        assertFalse(options.traceOptions.check_apertures);
        assertTrue(options.check_apertures(true).traceOptions.check_apertures);
    }
}
