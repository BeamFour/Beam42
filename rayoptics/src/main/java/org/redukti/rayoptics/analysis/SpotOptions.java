package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

public class SpotOptions {
    TraceOptions traceOptions = new TraceOptions();
    boolean use_grid = false;
    boolean use_hexapolar = true;
    boolean use_centroid = true;
    int num_rays = 21;
}
