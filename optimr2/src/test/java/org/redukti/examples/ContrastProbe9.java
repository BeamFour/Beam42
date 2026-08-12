package org.redukti.examples;

import org.redukti.optim.Analysis;

import java.util.Arrays;

/**
 * REVIEW.md &sect;6 — reproduces the mid-field MTF drop, and rules out &sect;3 as its cause.
 *
 * <p>Runs the GenericOpt contrast setup on the Leica 75/2 and prints MTF and spot RMS
 * before and after the solve, alongside a decomposition of every contrast residual into
 * the part that costs MTF (the variance of the pupil phase difference) and the part that
 * does not (its mean — a pure image displacement).
 *
 * <p>The result: sagittal mean share is 0.0% everywhere, by symmetry, while the drop is
 * sagittal. So the un-centred second moment of &sect;3 cannot explain it. See
 * {@link ContrastProbe10} for what does.
 */
public class ContrastProbe9 {

    static final int[] FREQS = {10, 30, 50};

    static void mtfTable(String label, Analysis a) {
        System.out.println("  " + label);
        for (int i = 0; i < FREQS.length; i++)
            System.out.printf("    %2d cyc/mm  sag %s%n                tan %s%n", FREQS[i],
                    fmt(a._mtfs[i].sag_mtf_by_field), fmt(a._mtfs[i].tan_mtf_by_field));
        System.out.println("    spot RMS   " + fmt(Arrays.stream(a._spots)
                .mapToDouble(s -> s.get_mean_radius()).toArray()));
    }

    static String fmt(double[] v) {
        StringBuilder sb = new StringBuilder();
        for (double d : v) sb.append(String.format("%9.4f", d));
        return sb.toString();
    }

    static void decompose(String label, Analysis a) {
        System.out.println("  " + label + "  (mean is the MTF-irrelevant tilt term)");
        for (var c : a._contrasts) {
            System.out.printf("    freq %2.0f:%n", c.spatialFrequency);
            for (int f = 0; f < c.fields.size(); f++) {
                double sw = 0, ms = 0, mt = 0, s2 = 0, t2 = 0;
                for (var w : c.fields.get(f).wavelengths())
                    for (var s : w.samples()) {
                        sw += s.weight();
                        ms += s.weight() * s.sagittalDifference();
                        mt += s.weight() * s.tangentialDifference();
                        s2 += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                        t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    }
                ms /= sw; mt /= sw; s2 /= sw; t2 /= sw;
                System.out.printf(
                    "      fld%d | SAG rms=%.4f mean=%+.4f var=%.4f meanShare=%4.1f%%"
                  + " | TAN rms=%.4f mean=%+.4f var=%.4f meanShare=%4.1f%%%n",
                    f, Math.sqrt(s2), ms, Math.sqrt(Math.max(0, s2 - ms * ms)), 100 * ms * ms / s2,
                    Math.sqrt(t2), mt, Math.sqrt(Math.max(0, t2 - mt * mt)), 100 * mt * mt / t2);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var prescription = GenericContrastOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(prescription, false, false);
        var a = setup.analysis();

        a.required_analyses(true, true, true);
        a.compute();
        System.out.println("=== BEFORE ===");
        mtfTable("MTF / spot", a);
        decompose("contrast residual", a);

        var mf = setup.meritFunction(false);
        double before = mf.getRMS();
        long t0 = System.nanoTime();
        int status = mf.getSolver().solve();
        System.out.printf("%nsolve status=%d  %.1f s  merit %.8f -> %.8f%n",
                status, (System.nanoTime() - t0) / 1e9, before, mf.getRMS());

        a.required_analyses(true, true, true);
        a.compute();
        System.out.println("\n=== AFTER ===");
        mtfTable("MTF / spot", a);
        decompose("contrast residual", a);
    }
}
