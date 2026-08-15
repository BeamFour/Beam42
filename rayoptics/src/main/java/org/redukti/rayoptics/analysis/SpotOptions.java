package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceOptions;

public class SpotOptions {

    public static final int PATTERN_HEXAPOLAR = 1;
    public static final int PATTERN_GAUSS_QUADRATURE = 2;
    public static final int PATTERN_GRID = 3;

    TraceOptions _trace_options = new TraceOptions();
    int _pattern;
    boolean _use_centroid = true;
    boolean _append_failed_rays = false;
    int _num_rays_or_rings;
    Integer _num_spokes = null;

    public SpotOptions(boolean useGaussGuadrature) {
        // Spot analysis historically checked the physical surface apertures.
        _trace_options.check_apertures = true;
        if (useGaussGuadrature)
            use_gaussian_quadrature();
        else
            use_hexapolar();
    }
    public SpotOptions() {
        this(false);
    }
    public SpotOptions num_rays(int rays) {
        this._num_rays_or_rings = rays;
        return this;
    }
    public SpotOptions num_rings(int rings) {
        this._num_rays_or_rings = rings;
        return this;
    }
    public SpotOptions use_hexapolar() {
        _pattern = PATTERN_HEXAPOLAR;
        _num_rays_or_rings = 64;
        return this;
    }
    public SpotOptions use_grid() {
        _pattern = PATTERN_GRID;
        _num_rays_or_rings = 64;
        return this;
    }
    public SpotOptions use_gaussian_quadrature() {
        _pattern = PATTERN_GAUSS_QUADRATURE;
        _num_rays_or_rings = 14;
        _num_spokes = 20;
        return this;
    }
    public SpotOptions num_spokes(Integer spokes) {
        this._num_spokes = spokes;
        return this;
    }
    public SpotOptions use_centroid(boolean value) {
        this._use_centroid = value;
        return this;
    }
    public SpotOptions append_failed_rays(boolean value) {
        this._append_failed_rays = value;
        return this;
    }
    /**
     * Whether Gaussian-quadrature spot rays are rejected by surface apertures.
     * Grid and ring/hexapolar spot analyses always check their physical apertures.
     */
    public SpotOptions check_apertures(boolean value) {
        this._trace_options.check_apertures = value;
        return this;
    }
    public boolean is_gauss_quadrature() {
        return _pattern == PATTERN_GAUSS_QUADRATURE;
    }
    public boolean is_hexapolar() {
        return _pattern == PATTERN_HEXAPOLAR;
    }
    public boolean is_grid() {
        return _pattern == PATTERN_GRID;
    }
}
