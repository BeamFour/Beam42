package org.redukti.rayoptics.raytr;

import java.util.List;

/**
 * Ray and optical path length, plus wavelength
 */
public class RayPkg {
    /**
     * list of RaySegs
     */
    public List<RaySeg> ray;
    /**
     * optical path length between pupils
     */
    public double op_delta;
    /**
     * wavelength (in nm) that the ray was traced in
     */
    public double wvl;

    public RayPkg(List<RaySeg> ray, double op_delta, double wvl) {
        this.ray = ray;
        this.op_delta = op_delta;
        this.wvl = wvl;
    }
}
