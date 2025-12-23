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
