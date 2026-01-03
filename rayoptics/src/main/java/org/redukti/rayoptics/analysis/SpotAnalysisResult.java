package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.TraceGridByWvl;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

public class SpotAnalysisResult {

    public final boolean use_centroid;
    public List<SpotResultsForField> spot_results = new ArrayList<>();

    public SpotAnalysisResult(boolean use_centroid) {
        this.use_centroid = use_centroid;
    }

    public static class SpotResultsForField {
        public Field fld;
        public Vector3 image_pt;
        public List<TraceGridByWvl> trace_results;
        public List<SpotIntercepts> intercepts = new ArrayList<>();
        public double max_radius;
        public double mean_radius;

        public SpotResultsForField(Field fld, List<TraceGridByWvl> trace_results, double ref_wvl, boolean use_centroid) {
            this.fld = fld;
            this.image_pt = fld.ref_sphere.image_pt;
            this.trace_results = trace_results;
            Vector2 centroid = null;
            // To preserve chromatic aberration when applying centroid
            // adjust to reference wvl
            for (var result: trace_results) {
                var s = new SpotIntercepts(result);
                if (result.wvl == ref_wvl && use_centroid)
                    centroid = s.compute_centroid();
                intercepts.add(s);
            }
            if (centroid != null) {
                for (var intercept: intercepts)
                    intercept.adjust_to_centroid(centroid);
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

    public SpotAnalysisResult add(Field fld, List<TraceGridByWvl> trace_results, double ref_wvl) {
        spot_results.add(new SpotResultsForField(fld, trace_results, ref_wvl, use_centroid));
        return this;
    }

    public double[] fields() {
        // Here we assume that the y component of the field is set
        double[] fields = new double[spot_results.size()];
        for (int i = 0; i < spot_results.size(); i++)
            fields[i] = spot_results.get(i).fld.y;
        return fields;
    }

    /**
     * Compute geometric MTF for given frequencies
     */
    public MTFResultByFreq[] computeMTFs(int[] freqs) {
        var mtfs = new ArrayList<PolyMTF>();
        for (int i = 0; i < spot_results.size(); i++) {
            var spotFld = spot_results.get(i);
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                var mtf = new MonochromaticGeometricMTF(intercepts);
                if (polyMtfForField == null)
                    polyMtfForField = new PolyMTF(mtf.mtf.fft_size,mtf.h2d.pixel_size);
                polyMtfForField.add(mtf.mtf, 1.0);
            }
            if (polyMtfForField != null) {
                polyMtfForField.compute();
                mtfs.add(polyMtfForField);
            }
        }
        var mtfResults = new ArrayList<MTFResultByFreq>();
        for (var freq: freqs)
            mtfResults.add(new MTFResultByFreq(mtfs,freq));
        return mtfResults.toArray(new MTFResultByFreq[0]);
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
