package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.util.Lists;

/**
 * REVIEW.md &sect;7 — is the shear applied at the exit pupil, as the OTF requires, or at
 * the entrance pupil where {@code REL_PUPIL} coordinates live?
 *
 * <p>A pair of rays produces image-plane fringes of frequency {@code nu} exactly when
 * their image-space direction cosines differ by {@code lambda*nu}. That needs no
 * knowledge of where the exit pupil sits, so it tests the shear directly. Were the pupil
 * imaging aberration free, a rigid entrance-pupil shift would give a constant ratio of 1;
 * departures are pupil aberration, and they mean the sample is being evaluated at the
 * wrong spatial frequency.
 *
 * <p>See {@link ContrastProbe13} for the calibration that removes the field-dependent
 * part of the error.
 */
public class ContrastProbe12 {

    /** Image-space direction cosines: the segment before the image surface. */
    static double[] imageDirection(RayPkg pkg) {
        if (pkg == null || pkg.ray == null || pkg.ray.size() < 2) return null;
        var seg = Lists.get(pkg.ray, -2);
        return new double[]{seg.d.x, seg.d.y};
    }

    public static void main(String[] args) throws Exception {
        var p = GenericContrastOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(p, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;
        double wvl = m.seq_model.central_wavelength();
        double lambdaMm = m.nm_to_sys_units(wvl);
        var fields = m.optical_spec.fov.fields;
        var focus = m.optical_spec.defocus().get_focus();

        var def = new TraceRingsDef();
        def.num_rings = 6;

        for (int freq : new int[]{10, 30, 50}) {
            double shift = ContrastAnalysis.normalized_entry_pupil_shift(m, wvl, freq);
            double required = lambdaMm * freq;
            System.out.printf("%n=== %d cyc/mm: EP shift=%.5f, required d(cos)=%.6f ===%n",
                    freq, shift, required);
            System.out.println("fld |   sag: min/mean/max ratio   |   tan: min/mean/max ratio");
            for (int fi = 0; fi < fields.length; fi++) {
                var fld = fields[fi];
                var pc = Trace.setup_pupil_coords(m, fld, wvl, focus, null, null);
                fld.chief_ray = pc.chief_ray_pkg;
                fld.ref_sphere = pc.ref_sphere;
                var triplets = Trace.trace_contrast(m, def, 12,
                        new Vector2(shift, 0.0), new Vector2(0.0, shift),
                        fld, wvl, new TraceOptions());

                double sMin = 9, sMax = -9, sSum = 0, tMin = 9, tMax = -9, tSum = 0;
                int n = 0;
                for (var t : triplets) {
                    var ref = imageDirection(t.reference());
                    var sag = imageDirection(t.sagittal());
                    var tan = imageDirection(t.tangential());
                    if (ref == null || sag == null || tan == null) continue;
                    double rs = Math.abs(sag[0] - ref[0]) / required;
                    double rt = Math.abs(tan[1] - ref[1]) / required;
                    sMin = Math.min(sMin, rs); sMax = Math.max(sMax, rs); sSum += rs;
                    tMin = Math.min(tMin, rt); tMax = Math.max(tMax, rt); tSum += rt;
                    n++;
                }
                System.out.printf("%3d | %8.4f %8.4f %8.4f | %8.4f %8.4f %8.4f%n",
                        fi, sMin, sSum / n, sMax, tMin, tSum / n, tMax);
            }
        }
    }
}
