package org.redukti.examples;

import org.redukti.rayoptics.analysis.*;

/**
 * INCONCLUSIVE — superseded by {@link ContrastProbe8}. Kept as a record of a dead end.
 *
 * <p>This was an attempt to confirm REVIEW.md &sect;1 by zeroing the vignetting factors
 * and re-measuring sigma(dW). It does not isolate the effect: zeroing the factors also
 * expands the traced aperture out to the full unvignetted pupil, admitting heavily
 * aberrated rays (sigma jumps from 0.80 to 49 waves at full field). The result therefore
 * conflates the shear scale with the aperture extent and proves nothing on its own.
 *
 * <p>{@link ContrastProbe8} measures the realized shear directly through
 * {@code Field.apply_vignetting} instead, with no ray tracing and no confound.
 */
public class ContrastProbe7 {

    static void show(String label, org.redukti.rayoptics.optical.OpticalModel m, double freq) {
        var c = ContrastAnalysis.eval(m, new ContrastOptions(freq).num_rings(12).num_spokes(24));
        StringBuilder sb = new StringBuilder(String.format("%-22s freq=%5.0f |", label, freq));
        for (var f : c.fields) {
            double sw = 0, mt = 0, t2 = 0, ms = 0, s2 = 0;
            for (var w : f.wavelengths())
                for (var s : w.samples()) {
                    sw += s.weight();
                    mt += s.weight() * s.tangentialDifference();
                    t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    ms += s.weight() * s.sagittalDifference();
                    s2 += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                }
            mt /= sw; t2 /= sw; ms /= sw; s2 /= sw;
            sb.append(String.format("  sag=%.4f tan=%.4f",
                    Math.sqrt(Math.max(0, s2 - ms * ms)), Math.sqrt(Math.max(0, t2 - mt * mt))));
        }
        System.out.println(sb);
    }

    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;

        System.out.println("sigma(dW) in waves, per field, 12x24 sampling, START design");
        show("as-is (vignetted)", m, 40);
        for (var f : m.optical_spec.fov.fields) {
            System.out.printf("  field y=%.2f: tangential shear is scaled by %.3f/%.3f (upper/lower)%n",
                    f.y, 1 - f.vuy, 1 - f.vly);
        }
        for (var f : m.optical_spec.fov.fields) { f.vux = f.vlx = f.vuy = f.vly = 0.0; f.chief_ray = null; }
        show("vig factors zeroed", m, 40);
        System.out.println("\nNOTE: the comparison below is confounded — zeroing the vignetting");
        System.out.println("factors also enlarges the traced aperture. See ContrastProbe8.");
        show("vig zeroed", m, 15);
        show("vig zeroed", m, 25);
        show("vig zeroed", m, 31);
    }
}
