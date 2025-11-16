package org.redukti.rayoptics.raytr;

import java.util.List;

public class TraceFanPoints {
    /**
     * Wavelength
     */
    public final double wvl;
    /**
     * X values - with vignetting applied
     */
    public final List<Double> fan_x;
    /**
     * Aberration result
     */
    public final List<Double> fan_y;

    public TraceFanPoints(double wvl, List<Double> fan_x, List<Double> fan_y) {
        this.wvl = wvl;
        this.fan_x = fan_x;
        this.fan_y = fan_y;
    }
}
