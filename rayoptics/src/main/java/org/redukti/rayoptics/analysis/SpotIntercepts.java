package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.TraceGridByWvl;

public class SpotIntercepts {
    public final double wvl;
    public final double[] x;
    public final double[] y;
    public final Vector2 centroid;
    public final TraceGridByWvl trace_data;

    public SpotIntercepts(TraceGridByWvl trace_data, boolean compute_centroid) {
        this.trace_data = trace_data;
        this.wvl = trace_data.wvl;
        this.x = new double[trace_data.grid.size()];
        this.y = new double[trace_data.grid.size()];
        double cx = 0, cy = 0;
        for (int i = 0; i < trace_data.grid.size(); i++) {
            this.x[i] = trace_data.grid.get(i).pupil.x();
            this.y[i] = trace_data.grid.get(i).pupil.y();
            if (compute_centroid) {
                cx += this.x[i];
                cy += this.y[i];
            }
        }
        if (compute_centroid) {
            cx = cx / this.x.length;
            cy = cy / this.y.length;
            this.centroid = new Vector2(cx,cy);
            for (int i = 0; i < this.x.length; i++) {
                this.x[i] = this.x[i] - cx;
                this.y[i] = this.y[i] - cy;
            }
        }
        else
            this.centroid = null;
    }
}
