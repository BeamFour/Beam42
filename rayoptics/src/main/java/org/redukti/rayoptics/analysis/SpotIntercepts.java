package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.TraceGridByWvl;

public class SpotIntercepts {
    public final double wvl;
    public final double[] x;
    public final double[] y;
    public final TraceGridByWvl trace_data;

    public SpotIntercepts(TraceGridByWvl trace_data) {
        this.trace_data = trace_data;
        this.wvl = trace_data.wvl;
        this.x = new double[trace_data.grid.size()];
        this.y = new double[trace_data.grid.size()];
        for (int i = 0; i < trace_data.grid.size(); i++) {
            this.x[i] = trace_data.grid.get(i).pupil.x();
            this.y[i] = trace_data.grid.get(i).pupil.y();
        }
    }

    public Vector2 compute_centroid() {
        double cx = 0, cy = 0;
        for (int i = 0; i < trace_data.grid.size(); i++) {
            cx += this.x[i];
            cy += this.y[i];
        }
        cx = cx / this.x.length;
        cy = cy / this.y.length;
        return new Vector2(cx,cy);
    }

    public void adjust_to_centroid(Vector2 centroid) {
        for (int i = 0; i < trace_data.grid.size(); i++) {
            this.x[i] -= centroid.x;
            this.y[i] -= centroid.y;
        }
    }
}
