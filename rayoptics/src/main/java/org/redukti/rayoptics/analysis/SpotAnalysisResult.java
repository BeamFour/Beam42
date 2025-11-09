package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

public class SpotAnalysisResult {

    public static class SpotResultsByField {
        Field fld;
        Vector3 image_pt;
        List<List<GridItem>> trace_results;
        double max_radius;
        double mean_radius;

        public SpotResultsByField(Field fld, List<List<GridItem>> trace_results) {
            this.fld = fld;
            this.image_pt = fld.ref_sphere.image_pt;
            this.trace_results = trace_results;
            computeMeanMax();
        }

        private void computeMeanMax() {
            max_radius = 0;
            mean_radius = 0;
            int count = 0;
            for (int wl = 0; wl < trace_results.size(); wl++) {
                var grids = trace_results.get(wl);
                for (var grid: grids) {
                    //System.out.println("pupil = " + grid.pupil.toString());
                    var l = grid.pupil.len();
                    //System.out.println("len = " + l);
                    if (l > max_radius) {
                        max_radius = l;
                    }
                    mean_radius += (l*l);
                }
                count += grids.size();
            }
            mean_radius = Math.sqrt(mean_radius/count);
        }

        @Override
        public String toString() {
            return "Field angle " + fld.y + "\n" + " mean radius " + mean_radius * 1000 + "\n" + "  max radius " + max_radius * 1000 + "\n";
        }
    }

    List<SpotResultsByField> spot_results = new ArrayList<>();
    public SpotAnalysisResult add(Field fld, List<List<GridItem>> trace_results) {
        spot_results.add(new SpotResultsByField(fld, trace_results));
        return this;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (var result: spot_results) {
            sb.append(result.toString());
        }
        return sb.toString();
    }
}
