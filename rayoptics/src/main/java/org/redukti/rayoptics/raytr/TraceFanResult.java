package org.redukti.rayoptics.raytr;

import java.util.List;

public class TraceFanResult {

    public List<List<Double>> fans_x;
    public List<List<Double>> fans_y;
    public double max_rho_val;
    public double max_y_val;

    public TraceFanResult(List<List<Double>> fans_x, List<List<Double>> fans_y, double max_rho_val, double max_y_val) {
        this.fans_x = fans_x;
        this.fans_y = fans_y;
        this.max_rho_val = max_rho_val;
        this.max_y_val = max_y_val;
    }
}
