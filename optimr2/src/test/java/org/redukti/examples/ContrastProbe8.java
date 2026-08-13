package org.redukti.examples;

import org.redukti.rayoptics.analysis.ContrastAnalysis;

/**
 * REVIEW.md &sect;1 — proof that vignetting rescales the shear.
 *
 * <p>Pushes a base pupil point and its sheared partners through
 * {@code Field.apply_vignetting} and reports the displacement that actually survives,
 * expressed as the effective spatial frequency. No ray tracing, so there is no confound
 * with aperture extent (unlike {@link ContrastProbe7}).
 *
 * <p>At full field the requested 40 cyc/mm tangential shear arrives as 12.5-14.6 cyc/mm,
 * and the upper and lower pupil halves get different scale factors — so the shear is not
 * even a rigid translation.
 */
public class ContrastProbe8 {
    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;
        double wvl = 587.5618;
        double requested = ContrastAnalysis.normalized_entry_pupil_shift(m, wvl, 40.0);
        System.out.printf("requested shear for 40 cyc/mm = %.5f pupil radii%n%n", requested);
        System.out.println("what Trace actually applies, after Field.apply_vignetting:");
        System.out.println("field   base p        sag shear   -> eff freq   tan shear   -> eff freq");
        for (var f : m.optical_spec.fov.fields) {
            // representative base point: the grid is centred at (-d/2,-d/2)
            for (double py : new double[]{0.4, -0.4}) {
                double[] p = {0.0, py};
                double[] ps = {requested, py};
                double[] pt = {0.0, py + requested};
                double[] vp = f.apply_vignetting(p);
                double[] vps = f.apply_vignetting(ps);
                double[] vpt = f.apply_vignetting(pt);
                double ds = vps[0] - vp[0];
                double dt = vpt[1] - vp[1];
                System.out.printf("y=%.2f  p=(0,%+.1f)   %.5f      %5.1f      %.5f      %5.1f%n",
                        f.y, py, ds, 40 * ds / requested, dt, 40 * dt / requested);
            }
        }
    }
}
