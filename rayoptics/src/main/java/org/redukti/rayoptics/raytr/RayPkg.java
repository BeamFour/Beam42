package org.redukti.rayoptics.raytr;

import java.util.List;

/**
 * Ray and optical path length, plus wavelength
 */
public class RayPkg {
    /**
     * List of RaySegs for each interface in the path,
     * Each RaySeg contains
     * pt: the intersection point of the ray
     * after_dir: the ray direction cosine following the interface
     * after_dst: the geometric distance to the next interface
     * normal: the surface normal at the intersection point
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
