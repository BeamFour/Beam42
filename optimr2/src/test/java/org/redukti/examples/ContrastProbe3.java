package org.redukti.examples;

import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.optical.OpticalModel;

/**
 * REVIEW.md &sect;2 (sampling convergence) and the measured cost split in "Verdict".
 *
 * <p>Evaluates the contrast surrogate at increasing pupil sampling on the starting
 * design and again on the optimized design. The starting design is converged at 3x6;
 * the optimized design is not, which is the signature of the optimizer fitting the
 * quadrature grid rather than the wavefront.
 *
 * <p>Also reports the warmed-up cost of contrast vs contrast+fans vs full spot/MTF.
 */
public class ContrastProbe3 {

    /** weighted rms and mean of the tangential difference, all fields/wavelengths. */
    static void report(String label, OpticalModel m, int rings, int spokes) {
        var c = ContrastAnalysis.eval(m, new ContrastOptions(40.0).num_rings(rings).num_spokes(spokes));
        StringBuilder sb = new StringBuilder(String.format("%-26s %2dx%-3d |", label, rings, spokes));
        for (var f : c.fields) {
            double sw = 0, t2 = 0, mt = 0, s2 = 0;
            for (var w : f.wavelengths())
                for (var s : w.samples()) {
                    sw += s.weight();
                    mt += s.weight() * s.tangentialDifference();
                    t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    s2 += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                }
            mt /= sw; t2 /= sw; s2 /= sw;
            sb.append(String.format("  tanVar=%.4f sagRms=%.4f", Math.sqrt(Math.max(0, t2 - mt * mt)), Math.sqrt(s2)));
        }
        System.out.println(sb);
    }

    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var analysis = setup.analysis();
        analysis.compute();

        System.out.println("--- START design, 40 cyc/mm, increasing pupil sampling ---");
        for (int[] rs : new int[][]{{3, 6}, {6, 12}, {12, 24}, {20, 40}})
            report("start", analysis._opt_model, rs[0], rs[1]);

        // warmed-up cost split
        analysis.required_analyses(false, false, false);
        for (int i = 0; i < 20; i++) analysis.compute();
        long t = System.nanoTime();
        for (int i = 0; i < 20; i++) analysis.compute();
        double contrastOnly = (System.nanoTime() - t) / 1e6 / 20;
        analysis.required_analyses(false, true, false);
        for (int i = 0; i < 20; i++) analysis.compute();
        t = System.nanoTime();
        for (int i = 0; i < 20; i++) analysis.compute();
        double withFans = (System.nanoTime() - t) / 1e6 / 20;
        analysis.required_analyses(true, true, true);
        for (int i = 0; i < 10; i++) analysis.compute();
        t = System.nanoTime();
        for (int i = 0; i < 10; i++) analysis.compute();
        double full = (System.nanoTime() - t) / 1e6 / 10;
        System.out.printf("%nwarm compute(): contrast=%.1f ms  contrast+fans=%.1f ms  contrast+fans+spot/MTF=%.1f ms%n",
                contrastOnly, withFans, full);

        analysis.required_analyses(false, true, false);
        var mf = setup.meritFunction(false);
        long t0 = System.nanoTime();
        int status = mf.getSolver().solve();
        System.out.printf("solve status=%d elapsed=%.1f s finalRms=%.8f%n",
                status, (System.nanoTime() - t0) / 1e9, mf.getRMS());

        analysis.required_analyses(true, true, true);
        analysis.compute();
        System.out.println("\n--- OPTIMIZED design, 40 cyc/mm, increasing pupil sampling ---");
        for (int[] rs : new int[][]{{3, 6}, {6, 12}, {12, 24}, {20, 40}})
            report("optimized", analysis._opt_model, rs[0], rs[1]);
        System.out.println("geometric tan MTF@40 = " + java.util.Arrays.toString(analysis._mtfs[2].tan_mtf_by_field));
        System.out.println("geometric sag MTF@40 = " + java.util.Arrays.toString(analysis._mtfs[2].sag_mtf_by_field));
    }
}
