package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.TraceGridByWvl;

public class SpotIntercepts {
    public final double wvl;
    public final double[] x;
    public final double[] y;
    public final double[] weights;
    public final boolean[] valid;

    public SpotIntercepts(TraceGridByWvl trace_data) {
        this.wvl = trace_data.wvl;
        this.x = new double[trace_data.grid.size()];
        this.y = new double[trace_data.grid.size()];
        this.weights = new double[trace_data.grid.size()];
        this.valid = new boolean[trace_data.grid.size()];
        for (int i = 0; i < trace_data.grid.size(); i++) {
            var item = trace_data.grid.get(i);
            this.valid[i] = item.valid;
            this.x[i] = valid[i] ? item.pupil.x() : Double.NaN;
            this.y[i] = valid[i] ? item.pupil.y() : Double.NaN;
            this.weights[i] = item.weight;
        }
    }

    public Vector2 compute_centroid() {
        double cx = 0, cy = 0;
        double totalWeight = 0.0;
        for (int i = 0; i < x.length; i++) {
            if (!valid[i]) continue;
            cx += this.weights[i] * this.x[i];
            cy += this.weights[i] * this.y[i];
            totalWeight += this.weights[i];
        }
        if (!(totalWeight > 0.0)) return new Vector2(Double.NaN, Double.NaN);
        cx = cx / totalWeight;
        cy = cy / totalWeight;
        return new Vector2(cx,cy);
    }

    public void adjust_to_centroid(Vector2 centroid) {
        for (int i = 0; i < x.length; i++) {
            if (!valid[i]) continue;
            this.x[i] -= centroid.x;
            this.y[i] -= centroid.y;
        }
    }
}
