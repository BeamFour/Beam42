package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

public class SpotOptions {

    public static final int PATTERN_HEXAPOLAR = 1;
    public static final int PATTERN_GAUSS_QUADRATURE = 2;
    public static final int PATTERN_GRID = 3;

    TraceOptions traceOptions = new TraceOptions();
    int pattern;
    boolean use_centroid = true;
    int num_rays;
    Integer num_spokes = null;

    public SpotOptions(boolean useGaussGuadrature) {
        if (useGaussGuadrature)
            use_gaussian_quadrature();
        else
            use_hexapolar();
    }
    public SpotOptions() {
        this(false);
    }
    public SpotOptions num_rays(int rays) {
        this.num_rays = rays;
        return this;
    }
    public SpotOptions num_rings(int rings) {
        this.num_rays = rings;
        return this;
    }
    public SpotOptions use_hexapolar() {
        pattern = PATTERN_HEXAPOLAR;
        num_rays = 64;
        return this;
    }
    public SpotOptions use_grid() {
        pattern = PATTERN_GRID;
        num_rays = 64;
        return this;
    }
    public SpotOptions use_gaussian_quadrature() {
        pattern = PATTERN_GAUSS_QUADRATURE;
        num_rays = 14;
        num_spokes = 20;
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
    public boolean is_gauss_quadrature() {
        return pattern == PATTERN_GAUSS_QUADRATURE;
    }
    public boolean is_hexapolar() {
        return pattern == PATTERN_HEXAPOLAR;
    }
    public boolean is_grid() {
        return pattern == PATTERN_GRID;
    }
}
