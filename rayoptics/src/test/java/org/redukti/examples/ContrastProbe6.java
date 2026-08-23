package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.PupilType;

/**
 * Supporting evidence for REVIEW.md &sect;1.
 *
 * <p>Measures how much of the unit pupil survives the clear apertures with and without
 * aperture checking (contrast tracing deliberately disables it), and prints the per-field
 * vignetting factors. The vignetting factors printed here are what exposed &sect;1: they
 * are large (vuy=0.636, vly=0.687 at full field), so applying the shear in
 * vignetted-relative coordinates rescales it substantially.
 */
public class ContrastProbe6 {
    public static void main(String[] args) throws Exception {
        String input = ContrastProbes.inputPath();
        var prescription = ZeissOtusML50mm.getPrescription(input, false, false);
        var setup = ZeissOtusML50mm.createContrastSetup(prescription, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;
        var fields = m.optical_spec.fov.fields;
        double wvl = m.seq_model.central_wavelength();

        System.out.println("fraction of the unit pupil that survives the clear apertures");
        for (boolean check : new boolean[]{false, true}) {
            var to = new TraceOptions();
            to.check_apertures = check;
            to.apply_vignetting = true;
            to.pupil_type = PupilType.REL_PUPIL;
            System.out.print("check_apertures=" + check + " :");
            for (int fi = 0; fi < fields.length; fi++) {
                var fld = fields[fi];
                Trace.setup_pupil_coords(m, fld, wvl, 0.0, null, null);
                int ok = 0, n = 0;
                for (int i = -20; i <= 20; i++)
                    for (int j = -20; j <= 20; j++) {
                        double x = i / 20.0, y = j / 20.0;
                        if (x * x + y * y > 1.0) continue;
                        n++;
                        if (Trace.trace_safe(m, new Vector2(x, y), fld, wvl, to).pkg != null) ok++;
                    }
                System.out.printf("  fld%d=%.3f", fi, (double) ok / n);
            }
            System.out.println();
        }
        for (var f : fields)
            System.out.printf("field y=%.3f vux=%.4f vlx=%.4f vuy=%.4f vly=%.4f%n",
                    f.y, f.vux, f.vlx, f.vuy, f.vly);
    }
}
