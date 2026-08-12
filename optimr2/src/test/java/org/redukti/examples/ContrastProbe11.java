package org.redukti.examples;

import org.redukti.rayoptics.analysis.*;

/**
 * REVIEW.md &sect;6 — the sampling constraint on a phasor-based MTF goal.
 *
 * <p>{@code |sum w exp(i 2 pi dW)|} oscillates where the least-squares residual only
 * grows, so it needs finer pupil sampling to integrate accurately. On the unoptimized
 * Leica 75/2 - the worst phases in play - 6x12 is not adequate, 12x24 is close, and
 * 24x48 matches 40x80. Anyone adopting the phasor as an optimization goal has to raise
 * the sampling accordingly, or only trust it once the design is in the small-phase
 * regime.
 */
public class ContrastProbe11 {

    public static void main(String[] args) throws Exception {
        var prescription = GenericOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericOpt.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(true, true, true);
        a.compute();

        System.out.println("START design (worst phases). phasor MTF vs pupil sampling:");
        System.out.println("freq fld dir |    6x12    12x24    24x48    40x80 |  geo MTF");
        int[] freqs = {10, 30, 50};
        int[][] rs = {{6, 12}, {12, 24}, {24, 48}, {40, 80}};
        for (int fi = 0; fi < freqs.length; fi++) {
            var results = new ContrastAnalysisResult[rs.length];
            for (int k = 0; k < rs.length; k++)
                results[k] = ContrastAnalysis.eval(a._opt_model,
                        new ContrastOptions(freqs[fi]).num_rings(rs[k][0]).num_spokes(rs[k][1]));
            for (int f = 0; f < results[0].fields.size(); f++) {
                for (int dir = 0; dir < 2; dir++) {
                    StringBuilder sb = new StringBuilder(
                            String.format("%4d %3d %s |", freqs[fi], f, dir == 0 ? "sag" : "tan"));
                    for (var c : results) {
                        double sw = 0, re = 0, im = 0;
                        for (var w : c.fields.get(f).wavelengths())
                            for (var s : w.samples()) {
                                double d = dir == 0 ? s.sagittalDifference() : s.tangentialDifference();
                                sw += s.weight();
                                re += s.weight() * Math.cos(2 * Math.PI * d);
                                im += s.weight() * Math.sin(2 * Math.PI * d);
                            }
                        sb.append(String.format(" %8.4f", Math.hypot(re / sw, im / sw)));
                    }
                    double geo = dir == 0 ? a._mtfs[fi].sag_mtf_by_field[f]
                                          : a._mtfs[fi].tan_mtf_by_field[f];
                    System.out.println(sb + String.format(" | %8.4f", geo));
                }
            }
        }
    }
}
