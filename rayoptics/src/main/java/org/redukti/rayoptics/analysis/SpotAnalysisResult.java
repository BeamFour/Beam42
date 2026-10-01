// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.TraceGridByWvl;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.FieldSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpotAnalysisResult {

    public final boolean use_centroid;
    public List<SpotResultsForField> spot_results = new ArrayList<>();

    public SpotAnalysisResult(boolean use_centroid) {
        this.use_centroid = use_centroid;
    }

    public static class SpotResultsForField {
        public final FieldSnapshot fld;
        public Vector3 image_pt;
        public List<TraceGridByWvl> trace_results;
        public List<SpotIntercepts> intercepts = new ArrayList<>();
        public double max_radius;
        public double mean_radius;
        /** Conversion captured with the intercepts, which remain in model units. */
        public final double system_units_to_micrometres;

        public SpotResultsForField(Field fld, List<TraceGridByWvl> trace_results, double ref_wvl, boolean use_centroid) {
            if (fld.fov == null) {
                // Standalone fields historically use millimetres.
                system_units_to_micrometres = 1000.0;
            } else {
                var model = fld.fov.optical_spec.opt_model;
                String units = model.system_spec.dimensions;
                if (!("m".equalsIgnoreCase(units) || "cm".equalsIgnoreCase(units)
                        || "mm".equalsIgnoreCase(units) || "in".equalsIgnoreCase(units)
                        || "ft".equalsIgnoreCase(units)))
                    throw new IllegalArgumentException("Unsupported spot model units: " + units);
                system_units_to_micrometres = 1.0 / model.nm_to_sys_units(1000.0);
            }
            this.fld = new FieldSnapshot(fld);
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
            double totalWeight = 0.0;
            for (var results: intercepts) {
                for (int i = 0; i < results.x.length; i++) {
                    if (!results.valid[i]) continue;
                    double r = results.x[i] * results.x[i] + results.y[i] * results.y[i];
                    double l = Math.sqrt(r);
                    if (l > max_radius) {
                        max_radius = l;
                    }
                    mean_radius += results.weights[i] * l * l;
                    totalWeight += results.weights[i];
                }
            }
            mean_radius = totalWeight > 0.0
                    ? Math.sqrt(mean_radius/totalWeight) : Double.NaN;
        }

        @Override
        public String toString() {
            return "Field " + fld + " mean radius " + get_mean_radius() + " max radius " + get_max_radius();
        }

        public double get_max_radius() {
            return max_radius * system_units_to_micrometres;
        }

        public double get_mean_radius() {
            return mean_radius * system_units_to_micrometres;
        }

        /**
         * Histogram grid for this field's geometric MTF, sized to the field's spot
         * extent (across all wavelengths). Shared by every wavelength so the
         * monochromatic MTFs can be combined into a {@link PolyMTF}.
         */
        public Histogram.Config mtfHistogramConfig() {
            return Histogram.adaptiveConfig(max_radius);
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
            var cfg = spotFld.mtfHistogramConfig();
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                var mtf = new MonochromaticGeometricMTF(intercepts, cfg);
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
