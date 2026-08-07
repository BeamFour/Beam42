package com.stellarsoftware.beam.core.analysis;

/** UI-independent, uniformly binned one-dimensional histogram. */
public final class Histogram1D {
    private final double min;
    private final double max;
    private final int[] bins;
    private double sum;
    private int count;

    public Histogram1D(int numberOfBins, double min, double max) {
        if (numberOfBins < 1 || !Double.isFinite(min) || !Double.isFinite(max) || max <= min)
            throw new IllegalArgumentException("Invalid histogram bounds or bin count");
        this.min = min;
        this.max = max;
        this.bins = new int[numberOfBins];
    }

    public boolean add(double value) {
        if (!Double.isFinite(value))
            return false;
        sum += value;
        count++;
        int bin = (int) Math.floor(bins.length * (value - min) / (max - min));
        if (bin < 0 || bin >= bins.length)
            return false;
        bins[bin]++;
        return true;
    }

    public int[] bins() { return bins; }
    public int count() { return count; }
    public double sum() { return sum; }
    public double average() { return count == 0 ? 0.0 : sum / count; }
    public double span() { return max - min; }
    public double binWidth() { return span() / bins.length; }
}
