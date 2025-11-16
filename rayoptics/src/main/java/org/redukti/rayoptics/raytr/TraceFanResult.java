package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.specs.Field;

import java.util.List;

public class TraceFanResult {

    public Field fld;
    /**
     * xy determines whether x (=0) or y (=1) fan
     */
    public int xy;
    public List<TraceFanPoints> fans;
    public double max_rho_val;
    public double max_y_val;

    public TraceFanResult(Field fld, int xy, List<TraceFanPoints> fans, double max_rho_val, double max_y_val) {
        this.fld = fld;
        this.xy = xy;
        this.fans = fans;
        this.max_rho_val = max_rho_val;
        this.max_y_val = max_y_val;
    }
}
