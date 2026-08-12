package org.redukti.examples;

import org.redukti.optim.Analysis;
import org.redukti.rayoptics.analysis.*;

/**
 * REVIEW.md &sect;6 — the core evidence.
 *
 * <p>For every field and orientation this compares three numbers computed from the very
 * same contrast ray samples:
 * <ul>
 *   <li>{@code sigma} - rms of the pupil phase difference, mean removed. What the
 *       least-squares merit actually minimises.</li>
 *   <li>{@code LSQ pred} - {@code exp(-2 pi^2 sigma^2)}, the MTF that merit implies.</li>
 *   <li>{@code phasor} - {@code |sum w exp(i 2 pi dW)|}, the OTF modulus those same
 *       samples encode.</li>
 * </ul>
 * against the geometric MTF from the independent spot analysis.
 *
 * <p>The phasor tracks the geometric MTF everywhere; the LSQ prediction only tracks it
 * while sigma stays under roughly 0.1 waves, and beyond that errs in both directions.
 * So the traced rays hold the right answer and the squaring is what discards it.
 */
public class ContrastProbe10 {

    static final int[] FREQS = {10, 30, 50};

    static void table(String label, Analysis a) {
        System.out.println("\n" + label);
        System.out.println("freq fld dir |   sigma   LSQ pred    phasor |  geo MTF");
        for (int fi = 0; fi < FREQS.length; fi++) {
            var c = ContrastAnalysis.eval(a._opt_model,
                    new ContrastOptions(FREQS[fi]).num_rings(12).num_spokes(24));
            for (int f = 0; f < c.fields.size(); f++) {
                for (int dir = 0; dir < 2; dir++) {
                    double sw = 0, m = 0, m2 = 0, re = 0, im = 0;
                    for (var w : c.fields.get(f).wavelengths())
                        for (var s : w.samples()) {
                            double d = dir == 0 ? s.sagittalDifference() : s.tangentialDifference();
                            sw += s.weight();
                            m += s.weight() * d;
                            m2 += s.weight() * d * d;
                            re += s.weight() * Math.cos(2 * Math.PI * d);
                            im += s.weight() * Math.sin(2 * Math.PI * d);
                        }
                    m /= sw; m2 /= sw; re /= sw; im /= sw;
                    double var = Math.max(0, m2 - m * m);
                    double lsq = Math.exp(-2 * Math.PI * Math.PI * var);
                    double phasor = Math.hypot(re, im);
                    double geo = dir == 0 ? a._mtfs[fi].sag_mtf_by_field[f]
                                          : a._mtfs[fi].tan_mtf_by_field[f];
                    System.out.printf("%4d %3d %s | %7.4f  %8.4f  %8.4f | %8.4f%s%n",
                            FREQS[fi], f, dir == 0 ? "sag" : "tan",
                            Math.sqrt(var), lsq, phasor, geo,
                            Math.abs(lsq - phasor) > 0.25 ? "   <-- LSQ far off" : "");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var prescription = GenericContrastOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(true, true, true);
        a.compute();
        table("=== BEFORE ===", a);

        setup.meritFunction(false).getSolver().solve();
        a.required_analyses(true, true, true);
        a.compute();
        table("=== AFTER ===", a);
    }
}
