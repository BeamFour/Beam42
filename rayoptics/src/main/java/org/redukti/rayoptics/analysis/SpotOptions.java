package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

public class SpotOptions {

    public static final int PATTERN_HEXAPOLAR = 1;
    public static final int PATTEN_GAUSS_QUADRATURE = 2;
    public static final int PATTERN_GRID = 3;

    TraceOptions traceOptions = new TraceOptions();
    int pattern;
    boolean use_centroid = true;
    int num_rays;
    Integer num_spokes = null;

    public SpotOptions(boolean useGaussGuadrature) {
        if (useGaussGuadrature) {
            // num_rays is rings here
            num_rays = 14;
            num_spokes = 20;
            pattern = PATTEN_GAUSS_QUADRATURE;
        }
        else {
            num_rays = 64;
            pattern = PATTERN_HEXAPOLAR;
        }
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
        return this;
    }
    public SpotOptions use_grid() {
        pattern = PATTERN_GRID;
        return this;
    }
    public SpotOptions use_gaussian_quadrature() {
        pattern = PATTEN_GAUSS_QUADRATURE;
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
        return pattern == PATTEN_GAUSS_QUADRATURE;
    }
    public boolean is_hexapolar() {
        return pattern == PATTERN_HEXAPOLAR;
    }
    public boolean is_grid() {
        return pattern == PATTERN_GRID;
    }
}
