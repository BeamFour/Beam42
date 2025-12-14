package org.redukti.rayoptics.analysis;

import org.redukti.rayoptics.raytr.TraceFanPoints;
import org.redukti.rayoptics.raytr.TraceFanResult;

import java.util.ArrayList;
import java.util.List;

public class RayAberrationResult {

    public List<TraceFanResult> results = new ArrayList<>();

    public void add(TraceFanResult fan_result){
        results.add(fan_result);
    }

    /**
     * Gets the results for given field, xy, and wavelength
     */
    public TraceFanPoints get_fans(int fi, int xy, double wvl) {
        for (var result: results) {
            if (result.fi == fi &&
                    result.xy == xy) {
                for (var fan : result.fans) {
                    if (fan.wvl == wvl)
                        return fan;
                }
            }
        }
        return null;
    }
}
