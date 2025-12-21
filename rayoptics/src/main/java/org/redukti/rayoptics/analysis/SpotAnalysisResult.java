package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.TraceGridByWvl;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

public class SpotAnalysisResult {

    public final boolean use_centroid;
    public List<SpotResultsByField> spot_results = new ArrayList<>();

    public SpotAnalysisResult(boolean use_centroid) {
        this.use_centroid = use_centroid;
    }

    public static class SpotResultsByField {
        public Field fld;
        public Vector3 image_pt;
        public List<TraceGridByWvl> trace_results;
        public List<SpotIntercepts> intercepts = new ArrayList<>();
        public double max_radius;
        public double mean_radius;

        public SpotResultsByField(Field fld, List<TraceGridByWvl> trace_results, boolean use_centroid) {
            this.fld = fld;
            this.image_pt = fld.ref_sphere.image_pt;
            this.trace_results = trace_results;
            for (var result: trace_results) {
                intercepts.add(new SpotIntercepts(result,use_centroid));
            }
            computeMeanMax();
        }

        private void computeMeanMax() {
            max_radius = 0;
            mean_radius = 0;
            int count = 0;
            for (var results: intercepts) {
                for (int i = 0; i < results.x.length; i++) {
                    double r = results.x[i] * results.x[i] + results.y[i] * results.y[i];
                    double l = Math.sqrt(r);
                    if (l > max_radius) {
                        max_radius = l;
                    }
                    mean_radius += (l * l);
                }
                count += results.x.length;
            }
            mean_radius = Math.sqrt(mean_radius/count);
        }

        @Override
        public String toString() {
            return "Field " + fld + " mean radius " + get_mean_radius() + " max radius " + get_max_radius();
        }

        public double get_max_radius() {
            return max_radius * 1000;
        }

        public double get_mean_radius() {
            return mean_radius * 1000;
        }
    }

    public SpotAnalysisResult add(Field fld, List<TraceGridByWvl> trace_results) {
        spot_results.add(new SpotResultsByField(fld, trace_results, use_centroid));
        return this;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (var result: spot_results) {
            sb.append(result.toString()).append("\n");
        }
        return sb.toString();
    }
}
