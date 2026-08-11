package org.redukti.examples;

import org.redukti.rayoptics.analysis.*;

/**
 * REVIEW.md &sect;5 — high-frequency degeneracy.
 *
 * <p>Sweeps the spatial frequency towards the cutoff and reports the largest sampled
 * pupil radius. Once {@code 0.707*shift} reaches 1 the overlap radius clamps to zero,
 * every quadrature point collapses onto the grid centre, and that centre is itself
 * outside the unit pupil (maxSampleRadius &gt; 1) — so the block returns BIGVAL and the
 * solve aborts with no diagnostic. The clamp is per-wavelength, so the longest
 * wavelength fails first.
 */
public class ContrastProbe5 {
    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        double lambda = a._opt_model.nm_to_sys_units(587.5618);
        double fno = a._pfo[org.redukti.optim.ParaxHelper.Fno];
        System.out.printf("cutoff 1/(lambda*F#) = %.1f cyc/mm; 0.707*cutoff = %.1f%n",
                1.0 / (lambda * fno), 0.707 / (lambda * fno));
        for (double f : new double[]{40, 400, 800, 830, 850, 1000}) {
            var c = ContrastAnalysis.eval(a._opt_model, new ContrastOptions(f).num_rings(3).num_spokes(6));
            double shift = ContrastAnalysis.normalized_pupil_shift(a._opt_model, 587.5618, f);
            double maxAbs = 0, maxR = 0;
            for (var fr : c.fields)
                for (var w : fr.wavelengths())
                    for (var s : w.samples()) {
                        maxAbs = Math.max(maxAbs, Math.abs(s.tangentialDifference()));
                        maxR = Math.max(maxR, s.pupil().len());
                    }
            System.out.printf("freq=%6.0f shift=%.4f  maxSampleRadius=%.5f  max|dW|=%.6f%n",
                    f, shift, maxR, maxAbs);
        }
    }
}
