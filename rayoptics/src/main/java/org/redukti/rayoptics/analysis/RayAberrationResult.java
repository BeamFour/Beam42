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

    public String list_ray_fans() {
        StringBuilder sb = new StringBuilder();
        for (var result: results) {
            sb.append(result.fld.toString()).append(" xy=").append(result.xy).append("\n");
            for (int i = 0; i < result.fans.size(); i++) {
                var fan = result.fans.get(i);
                if (i > 0)
                    sb.append(",");
                sb.append(fan.wvl);
            }
            sb.append("\n");
            int row_count = result.fans.get(0).fan_y.size();
            for (int i = 0; i < row_count; i++) {
                for (int j = 0; j < result.fans.size(); j++) {
                    var fan = result.fans.get(j);
                    if (j > 0)
                        sb.append(",");
                    sb.append(fan.fan_y.get(i));
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}
