package org.redukti.examples;

import org.redukti.rayoptics.analysis.*;

import java.util.*;

/**
 * REVIEW.md &sect;4 (frequency collinearity) and &sect;3 (mean fraction).
 *
 * <p>Computes the cosine between the residual vectors of the three frequency blocks,
 * reports what fraction of the contrast sum-of-squares is the MTF-irrelevant mean
 * (tilt) term, then solves and compares the small-phase MTF estimate against the
 * geometric MTF.
 *
 * <p>The timing split in this probe is JIT-polluted and was superseded by
 * {@link ContrastProbe3}, which warms up first.
 */
public class ContrastProbe2 {

    static double[] residualVector(ContrastAnalysisResult c) {
        List<Double> v = new ArrayList<>();
        for (var f : c.fields)
            for (var w : f.wavelengths())
                for (var s : w.samples()) {
                    v.add(Math.sqrt(s.weight()) * s.sagittalDifference());
                    v.add(Math.sqrt(s.weight()) * s.tangentialDifference());
                }
        return v.stream().mapToDouble(Double::doubleValue).toArray();
    }

    static double cosine(double[] a, double[] b) {
        double d = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { d += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return d / Math.sqrt(na * nb);
    }

    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var analysis = setup.analysis();
        analysis.compute();

        // --- collinearity of the three frequency residual blocks -------------
        var r10 = residualVector(analysis._contrasts[0]);
        var r20 = residualVector(analysis._contrasts[1]);
        var r40 = residualVector(analysis._contrasts[2]);
        System.out.printf("cos(r10,r20)=%.6f  cos(r20,r40)=%.6f  cos(r10,r40)=%.6f%n",
                cosine(r10, r20), cosine(r20, r40), cosine(r10, r40));

        // --- cost split (see ContrastProbe3 for warmed-up numbers) ----------
        int reps = 5;
        analysis.required_analyses(false, false, false);
        long t = System.nanoTime();
        for (int i = 0; i < reps; i++) analysis.compute();
        double contrastOnly = (System.nanoTime() - t) / 1e6 / reps;
        analysis.required_analyses(false, true, false);
        t = System.nanoTime();
        for (int i = 0; i < reps; i++) analysis.compute();
        double withFans = (System.nanoTime() - t) / 1e6 / reps;
        System.out.printf("compute(): contrast-only=%.1f ms, +ray-fans=%.1f ms (JIT-polluted)%n",
                contrastOnly, withFans);

        // --- how much of the contrast SOS is the (MTF-irrelevant) mean? -----
        double sosTotal = 0, sosMean = 0;
        for (var c : analysis._contrasts)
            for (var f : c.fields)
                for (var w : f.wavelengths()) {
                    double sw = 0, ms = 0, mt = 0, s2 = 0, t2 = 0;
                    for (var s : w.samples()) {
                        sw += s.weight();
                        ms += s.weight() * s.sagittalDifference();
                        mt += s.weight() * s.tangentialDifference();
                        s2 += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                        t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    }
                    sosTotal += s2 + t2;
                    sosMean += (ms * ms + mt * mt) / sw;
                }
        System.out.printf("contrast SOS=%.5f, of which mean(tilt) term=%.5f (%.1f%%)%n",
                sosTotal, sosMean, 100.0 * sosMean / sosTotal);

        // --- solve, then compare surrogate against real MTF -----------------
        analysis.required_analyses(false, true, false);
        var mf = setup.meritFunction(false);
        long t0 = System.nanoTime();
        int status = mf.getSolver().solve();
        System.out.printf("solve status=%d elapsed=%.1f s finalRms=%.8f%n",
                status, (System.nanoTime() - t0) / 1e9, mf.getRMS());

        analysis.required_analyses(true, true, true);
        analysis.compute();
        System.out.println("\nfreq fld | sigma(dW) waves | 1-2pi^2 sigma^2 | exp(-2pi^2 sigma^2) | geometric MTF");
        int[] freqs = {10, 20, 40};
        for (int fi = 0; fi < 3; fi++) {
            var c = analysis._contrasts[fi];
            for (int f = 0; f < c.fields.size(); f++) {
                double sw = 0, mt = 0, t2 = 0;
                for (var w : c.fields.get(f).wavelengths())
                    for (var s : w.samples()) {
                        sw += s.weight(); mt += s.weight() * s.tangentialDifference();
                        t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    }
                mt /= sw; t2 /= sw;
                double var = Math.max(0, t2 - mt * mt);
                double phi2 = 4 * Math.PI * Math.PI * var;
                System.out.printf("%4d %3d | %15.5f | %15.4f | %19.4f | %13.4f%n",
                        freqs[fi], f, Math.sqrt(var), 1 - phi2 / 2, Math.exp(-phi2 / 2),
                        analysis._mtfs[fi].tan_mtf_by_field[f]);
            }
        }
    }
}
