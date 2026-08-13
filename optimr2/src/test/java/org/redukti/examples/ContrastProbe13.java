package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Lists;

/**
 * REVIEW.md &sect;7 — does {@code calibrate_frequency} drive the realised spatial
 * frequency to the requested one?
 *
 * <p>Reports, per field and direction, the correction the calibration computes and the
 * mean realised/requested frequency ratio across the sampled pattern before and after
 * applying it. Then confirms the production path honours the flag by evaluating
 * {@link ContrastAnalysis} both ways.
 */
public class ContrastProbe13 {

    static Double dir(OpticalModel m, Vector2 pupil, Field f, double wvl, TraceOptions to, int axis) {
        var pkg = Trace.trace_safe(m, pupil, f, wvl, to).pkg;
        if (pkg == null || pkg.ray == null || pkg.ray.size() < 2) return null;
        var s = Lists.get(pkg.ray, -2);
        return axis == 0 ? s.d.x : s.d.y;
    }

    /** Mean realised/requested frequency over the sampled pattern for a given shift. */
    static double meanRatio(OpticalModel m, Field f, double wvl, double applied,
                            int axis, double required, TraceOptions to) {
        var def = new TraceRingsDef();
        def.num_rings = 6;
        var sag = axis == 0 ? new Vector2(applied, 0) : Vector2.vector2_0;
        var tan = axis == 0 ? Vector2.vector2_0 : new Vector2(0, applied);
        var triplets = Trace.trace_contrast(m, def, 12, sag, tan, f, wvl, to);
        double sum = 0;
        int n = 0;
        for (var t : triplets) {
            var r = t.reference();
            var p = axis == 0 ? t.sagittal() : t.tangential();
            if (r == null || p == null || r.ray.size() < 2 || p.ray.size() < 2) continue;
            var rs = Lists.get(r.ray, -2);
            var ps = Lists.get(p.ray, -2);
            sum += (axis == 0 ? Math.abs(ps.d.x - rs.d.x) : Math.abs(ps.d.y - rs.d.y)) / required;
            n++;
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    public static void main(String[] args) throws Exception {
        var p = GenericContrastOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(p, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;
        double wvl = m.seq_model.central_wavelength();
        double lambda = m.nm_to_sys_units(wvl);
        var fields = m.optical_spec.fov.fields;
        double focus = m.optical_spec.defocus().get_focus();

        var to = new TraceOptions();
        to.check_apertures = false;
        to.pupil_type = PupilType.REL_PUPIL;
        to.apply_vignetting = false;

        for (int freq : new int[]{30, 50}) {
            double shift = ContrastAnalysis.normalized_entry_pupil_shift(m, wvl, freq);
            double required = lambda * freq;
            System.out.printf("%n=== %d cyc/mm ===%n", freq);
            System.out.println("fld dir |  scale | mean ratio before | mean ratio after");
            for (int fi = 0; fi < fields.length; fi++) {
                var f = fields[fi];
                for (int axis = 0; axis < 2; axis++) {
                    Trace.setup_pupil_coords(m, f, wvl, focus, null, null);
                    double half = 0.5 * shift;
                    var lo = axis == 0 ? new Vector2(-half, 0) : new Vector2(0, -half);
                    var hi = axis == 0 ? new Vector2(half, 0) : new Vector2(0, half);
                    double realized = Math.abs(dir(m, hi, f, wvl, to, axis) - dir(m, lo, f, wvl, to, axis));
                    double scale = required / realized;
                    System.out.printf("%3d %s | %.4f | %17.4f | %16.4f%n",
                            fi, axis == 0 ? "sag" : "tan", scale,
                            meanRatio(m, f, wvl, shift, axis, required, to),
                            meanRatio(m, f, wvl, shift * scale, axis, required, to));
                }
            }
        }

        System.out.println("\n=== through ContrastAnalysis.eval: sigma(dW) tangential, 50 cyc/mm ===");
        for (boolean calibrate : new boolean[]{false, true}) {
            var c = ContrastAnalysis.eval(m, new ContrastOptions(50)
                    .num_rings(6).num_spokes(12).calibrate_frequency(calibrate));
            StringBuilder sb = new StringBuilder(String.format("calibrate=%-5s |", calibrate));
            for (var fr : c.fields) {
                double sw = 0, mt = 0, t2 = 0;
                for (var w : fr.wavelengths())
                    for (var s : w.samples()) {
                        sw += s.weight();
                        mt += s.weight() * s.tangentialDifference();
                        t2 += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                    }
                mt /= sw; t2 /= sw;
                sb.append(String.format(" %8.5f", Math.sqrt(Math.max(0, t2 - mt * mt))));
            }
            System.out.println(sb);
        }
    }
}
