package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

public class SpotOptions {
    TraceOptions traceOptions = new TraceOptions();
    boolean use_grid = false;
    boolean use_gaussian_quadrature = false;
    boolean use_hexapolar = true;
    boolean use_centroid = true;
    int num_rays = 21;
    Integer num_spokes = null;

    public SpotOptions num_rays(int rays) {
        this.num_rays = rays;
        return this;
    }
    public SpotOptions use_grid(boolean value) {
        this.use_grid = value;
        if (value)
            this.use_gaussian_quadrature = false;
        return this;
    }
    public SpotOptions use_gaussian_quadrature(boolean value) {
        this.use_gaussian_quadrature = value;
        if (value)
            this.use_grid = false;
        return this;
    }
    public SpotOptions num_spokes(Integer spokes) {
        this.num_spokes = spokes;
        return this;
    }
    public SpotOptions use_centroid(boolean value) {
        this.use_centroid = value;
        return this;
    }
}
