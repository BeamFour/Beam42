package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector2;

public class TraceFanDef {

    public final Vector2 start;
    public final Vector2 stop;
    public int num_rays;

    public TraceFanDef(Vector2 fan_start, Vector2 fan_stop, int num_rays) {
        this.start = fan_start;
        this.stop = fan_stop;
        this.num_rays = num_rays;
    }
}
