package org.redukti.rayoptics.analysis;

public class Histogram {
    /**
     * Defines the size of the grid used as histogram
     */
    public final int num_bins;
    public final double pixel_size;
    public final double hmin, hmax;
    // 2d histogram
    public double[][] h2d;
    // line spreads
    public double[] lsf_x;
    public double[] lsf_y;

    public Histogram(int num_bins,double pixel_size) {
        this.num_bins = num_bins;
        this.pixel_size = pixel_size;
        var width = pixel_size * num_bins;
        hmin = -width / 2;
        hmax = width / 2;
        // 2d histogram
        h2d = new double[num_bins][num_bins];
        // line spreads
        lsf_x = new double[num_bins];
        lsf_y = new double[num_bins];
    }

    public Histogram(Config cfg) {
        this(cfg.num_bins, cfg.pixel_size);
    }

    // Default grid used when no spot extent is available: a 0.512 mm window
    // (+/-0.256 mm) sampled at 1 micron. This reproduces the historical fixed grid.
    public static final double DEFAULT_PIXEL_SIZE = 0.001;
    public static final int DEFAULT_NUM_BINS = 512;

    /**
     * Bin count and bin size for the spot histogram. All monochromatic MTFs that
     * are combined into a single polychromatic {@link PolyMTF} for one field MUST
     * share the same Config, otherwise their FFT sizes and frequency axes differ
     * and the complex-OTF summation in {@link PolyMTF#add} is invalid.
     */
    public static final class Config {
        public final int num_bins;
        public final double pixel_size;

        public Config(int num_bins, double pixel_size) {
            this.num_bins = num_bins;
            this.pixel_size = pixel_size;
        }
    }

    /**
     * Adaptive grid sized to contain the geometric spot with margin, using the
     * default bin size and limits. See the overload for the meaning of the tuning
     * parameters.
     *
     * @param max_radius maximum spot radius (in lens units, i.e. mm) for the field,
     *                   taken across all wavelengths so every wavelength shares one grid
     */
    public static Config adaptiveConfig(double max_radius) {
        return adaptiveConfig(max_radius, DEFAULT_PIXEL_SIZE, 2.0,
                DEFAULT_PIXEL_SIZE * DEFAULT_NUM_BINS, 2048);
    }

    /**
     * Choose a histogram grid that:
     * <ul>
     *   <li>keeps {@code pixel_size} fine enough to reach the desired maximum
     *       spatial frequency (Nyquist = 1/(2*pixel_size)),</li>
     *   <li>makes the window wide enough to contain the spot (radius * margin), so
     *       rays are not clipped by {@link #accumulate} and the LSF tapers to zero,</li>
     *   <li>never shrinks the window below {@code min_window}, so the MTF frequency
     *       step (1/(2*window)) stays small enough to sample low frequencies, and</li>
     *   <li>caps the bin count at {@code max_bins} to bound the O(num_bins^2) memory,
     *       coarsening the bin size instead if the spot is very large.</li>
     * </ul>
     * When the spot is small this returns the historical default grid unchanged.
     *
     * @param max_radius maximum spot radius (mm) across all wavelengths of the field
     * @param pixel_size preferred bin size (mm)
     * @param margin     window half-width as a multiple of max_radius (>= 1)
     * @param min_window minimum full window width (mm)
     * @param max_bins   upper bound on bin count (power of two recommended)
     */
    public static Config adaptiveConfig(double max_radius, double pixel_size,
                                        double margin, double min_window, int max_bins) {
        if (!Double.isFinite(max_radius) || max_radius < 0)
            max_radius = 0;
        double half_width = Math.max(max_radius * margin, min_window / 2.0);
        double window = 2.0 * half_width;
        int bins = nextPow2((int) Math.ceil(window / pixel_size));
        if (bins > max_bins) {
            bins = max_bins;
            // keep the whole spot in the window by coarsening the bin size
            pixel_size = window / bins;
        }
        return new Config(bins, pixel_size);
    }

    private static int nextPow2(int n) {
        int p = 1;
        while (p < n)
            p <<= 1;
        return p;
    }

    public void accumulate(SpotIntercepts intercepts,double wt) {
        for (int i = 0; i < intercepts.x.length; i++) {
            var x = intercepts.x[i];
            var y = intercepts.y[i];
            if (x < hmin || x > hmax
                    || y < hmin || y > hmax)
                continue;
            int ix = (int) Math.floor(num_bins * (x - hmin) / (hmax - hmin));
            int iy = (int) Math.floor(num_bins * (y - hmin) / (hmax - hmin));
            if (ix < 0 || ix >= num_bins || iy < 0 || iy >= num_bins)
                continue;
            // ideally we should assign a weight here
            // For now we assign 1 for each hit
            h2d[ix][iy] += wt;
        }
    }
    private void normalize_histogram() {
        double sum = 0.0;
        for (int i = 0; i < num_bins; i++)
            for (int j = 0; j < num_bins; j++)
                sum += h2d[i][j];

        for (int i = 0; i < num_bins; i++)
            for (int j = 0; j < num_bins; j++)
                h2d[i][j] /= sum;
    }

    private void build_lsf(int xy) {
        double[] lsf = xy == 0 ? lsf_x : lsf_y;
        if (xy == 0) {
            // integrate over y → LSF(x)
            for (int i = 0; i < num_bins; i++) {
                double s = 0;
                for (int j = 0; j < num_bins; j++) {
                    s += h2d[i][j];
                }
                lsf[i] = s;
            }
        } else {
            // integrate over x → LSF(y)
            for (int j = 0; j < num_bins; j++) {
                double s = 0;
                for (int i = 0; i < num_bins; i++) {
                    s += h2d[i][j];
                }
                lsf[j] = s;
            }
        }
        // normalize lsf
        double lsfSum = 0;
        for (double v : lsf)
            lsfSum += v;
        for (int i = 0; i < num_bins; i++)
            lsf[i] /= lsfSum;
    }

    private void build_lsfs() {
        build_lsf(0);   // x
        build_lsf(1);   // y
    }

    public void compute() {
        normalize_histogram();
        build_lsfs();
    }
}
