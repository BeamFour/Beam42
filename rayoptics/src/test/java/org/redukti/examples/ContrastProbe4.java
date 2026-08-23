package org.redukti.examples;

import org.redukti.optim.*;
import org.redukti.rayoptics.analysis.*;
import org.redukti.spec.Prescription;

import java.util.Arrays;

import static org.redukti.optim.OptimizationBuilder.contrast;

/**
 * REVIEW.md &sect;2 and &sect;4 — the four-way re-optimization comparison table.
 *
 * <p>Runs a full solve at four sampling/frequency configurations and reports the
 * resulting lens measured with the independent spot/MTF analysis. Shows that 6x12
 * sampling gives a materially better lens than the 3x6 default, that 8x16 reproduces
 * 6x12 (so 6x12 is converged), and that a single frequency matches three frequencies
 * at 40% of the cost.
 *
 * <p>This is the slowest probe — roughly five minutes for all four solves.
 */
public class ContrastProbe4 {

    static OptimizationBuilder.OptimizationSetup setup(Prescription p, int rings, int spokes, int[] freqs) {
        double[] w = {1.0, 1.0, 1.0, 1.0};
        var b = OptimizationBuilder.builder(p)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 20, 40)
                .varyCurvatures(0, 1, 2, 3, 4, 5, 6, 8, 9,
                        11, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23)
                .varyThicknesses(25)
                .varyExistingAspherics()
                .contrastSampling(rings, spokes);
        var goals = Arrays.stream(freqs).mapToObj(f -> contrast(f, w))
                .toArray(OptimizationBuilder.ContrastGoals[]::new);
        return b.contrastGoals(goals).build();
    }

    static void run(String label, int rings, int spokes, int[] freqs) throws Exception {
        var prescription = ZeissOtusML50mm.getPrescription(ContrastProbes.inputPath(), false, false);
        var s = setup(prescription, rings, spokes, freqs);
        var a = s.analysis();
        a.compute();
        var mf = s.meritFunction(false);
        long t0 = System.nanoTime();
        int status = mf.getSolver().solve();
        double secs = (System.nanoTime() - t0) / 1e9;
        a.required_analyses(true, true, true);
        a.compute();
        System.out.printf("%n### %s (%dx%d, freqs=%s) status=%d  %.1f s  m=%d%n",
                label, rings, spokes, Arrays.toString(freqs), status, secs, s.goals().length);
        System.out.println("  spotRms  = " + Arrays.toString(Arrays.stream(a._spots)
                .mapToDouble(x -> x.get_mean_radius()).toArray()));
        System.out.println("  sag@40   = " + Arrays.toString(a._mtfs[2].sag_mtf_by_field));
        System.out.println("  tan@40   = " + Arrays.toString(a._mtfs[2].tan_mtf_by_field));
        // converged measurement of the surrogate itself
        var c = ContrastAnalysis.eval(a._opt_model, new ContrastOptions(40.0).num_rings(20).num_spokes(40));
        StringBuilder sb = new StringBuilder("  converged sigma(dW)@40:");
        for (var f : c.fields) {
            double sw = 0, mt = 0, t2 = 0;
            for (var w : f.wavelengths())
                for (var x : w.samples()) {
                    sw += x.weight();
                    mt += x.weight() * x.tangentialDifference();
                    t2 += x.weight() * x.tangentialDifference() * x.tangentialDifference();
                }
            mt /= sw; t2 /= sw;
            sb.append(String.format(" %.4f", Math.sqrt(Math.max(0, t2 - mt * mt))));
        }
        System.out.println(sb);
    }

    public static void main(String[] args) throws Exception {
        run("baseline", 3, 6, new int[]{10, 20, 40});
        run("denser sampling", 6, 12, new int[]{10, 20, 40});
        run("denser, single freq", 6, 12, new int[]{40});
        run("denser, single freq", 8, 16, new int[]{40});
    }
}
