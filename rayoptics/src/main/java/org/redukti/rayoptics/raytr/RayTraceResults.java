package org.redukti.rayoptics.raytr;

import java.util.List;

public class RayTraceResults {
    public List<RayTraceElement> ray;
    public double op_delta;
    public double wvl;

    public RayTraceResults(List<RayTraceElement> ray, double op_delta, double wvl) {
        this.ray = ray;
        this.op_delta = op_delta;
        this.wvl = wvl;
    }
}
