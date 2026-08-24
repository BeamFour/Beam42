package org.redukti.mathlib;

public class RootResult {
    public final double root;
    public final boolean converged;
    public final int iterations;

    public RootResult(double root, boolean converged, int iterations) {
        this.root = root;
        this.converged = converged;
        this.iterations = iterations;
    }

    @Override
    public String toString() {
        return "RootResult{" +
                "root=" + root +
                ", converged=" + converged +
                ", iterations=" + iterations +
                '}';
    }
}
