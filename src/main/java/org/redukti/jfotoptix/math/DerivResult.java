package org.redukti.jfotoptix.math;

public class DerivResult {
    public final double result;
    public final double abserr;

    public DerivResult(double result, double abserr) {
        this.result = result;
        this.abserr = abserr;
    }
}
