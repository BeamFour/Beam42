package org.redukti.examples;

import org.redukti.optim.*;

import java.util.*;

/**
 * REVIEW.md &sect;3 and the residual breakdown in "Smaller items".
 *
 * <p>Reports the goal count and sum-of-squares by goal type, then for every
 * (frequency, field, wavelength) the weighted mean, rms and variance of the pupil
 * phase difference. The mean column is the evidence that the residual is the
 * un-centred second moment: the sagittal mean is zero by symmetry, the tangential
 * mean is not, and it scales linearly with the shear (pure wavefront tilt).
 */
public class ContrastProbe {
    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        System.out.println("wavelengths = " + Arrays.toString(prescription._wvls));
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var analysis = setup.analysis();
        long t0 = System.nanoTime();
        analysis.compute();
        long t1 = System.nanoTime();
        System.out.println("analysis.compute() ms = " + (t1 - t0) / 1e6);
        System.out.println("compute_spots=" + analysis._compute_spots
                + " compute_mtf=" + analysis._compute_mtf
                + " compute_ray_ab=" + analysis._compute_ray_aberrations);
        System.out.println("fno = " + analysis._pfo[ParaxHelper.Fno]);

        var goals = setup.goals();
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Double> sos = new TreeMap<>();
        for (var g : goals) {
            String k = g.getClass().getSimpleName();
            counts.merge(k, 1, Integer::sum);
            double v = g.value();
            double r = (v - g._target) * Math.sqrt(g._weight);
            sos.merge(k, r * r, Double::sum);
        }
        System.out.println("goal counts = " + counts);
        System.out.println("goal SOS    = " + sos);
        double total = sos.values().stream().mapToDouble(Double::doubleValue).sum();
        System.out.println("total SOS = " + total + "  m = " + goals.length
                + "  rms = " + Math.sqrt(total / goals.length));

        // Per-frequency: overlap radius, mean and rms of the raw differences
        for (var c : analysis._contrasts) {
            System.out.println("\n=== frequency " + c.spatialFrequency + " cyc/mm ===");
            for (int f = 0; f < c.fields.size(); f++) {
                var fr = c.fields.get(f);
                for (var w : fr.wavelengths()) {
                    double sumw = 0, ms = 0, mt = 0, s2 = 0, t2 = 0, rmax = 0;
                    for (var s : w.samples()) {
                        sumw += s.weight();
                        ms += s.weight() * s.sagittalDifference();
                        mt += s.weight() * s.tangentialDifference();
                        s2 += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                        t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                        rmax = Math.max(rmax, s.pupil().len());
                    }
                    ms /= sumw; mt /= sumw; s2 /= sumw; t2 /= sumw;
                    System.out.printf(
                        "fld=%d wvl=%7.2f shift=%.5f maxPupilR=%.4f | SAG mean=%+.5f rms=%.5f var=%.5f"
                      + " | TAN mean=%+.5f rms=%.5f var=%.5f%n",
                        f, w.wavelength(), w.normalizedPupilShift(), rmax,
                        ms, Math.sqrt(s2), Math.sqrt(Math.max(0, s2 - ms * ms)),
                        mt, Math.sqrt(t2), Math.sqrt(Math.max(0, t2 - mt * mt)));
                }
            }
        }
    }
}
