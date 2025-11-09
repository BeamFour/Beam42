// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import java.util.List;

/**
 * Ray and optical path length, plus wavelength
 */
public class RayPkg {
    /**
     * List of RaySegs
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "ray=" + ray +
                ", op_delta=" + op_delta +
                ", wvl=" + wvl +
                ')';
    }
}
