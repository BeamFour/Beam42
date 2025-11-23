package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.specs.Field;

import java.util.List;

public class TraceFanResult {

    public RayFanType type;
    public Field fld;
    public int fi;
    /**
     * xy determines whether x (=0) or y (=1) fan
     */
    public int xy;
    public List<TraceFanPoints> fans;
    public double max_rho_val;
    public double max_y_val;

    public TraceFanResult(Field fld,int fi, int xy, List<TraceFanPoints> fans, double max_rho_val, double max_y_val) {
        this.fld = fld;
        this.fi = fi;
        this.xy = xy;
        this.fans = fans;
        this.max_rho_val = max_rho_val;
        this.max_y_val = max_y_val;
    }
    public TraceFanResult setFanType(RayFanType type) {
        this.type = type;
        return this;
    }
}
