package org.redukti.examples;

import org.redukti.analysis.DiffractionMTF;
import org.redukti.analysis.PolyDiffractionMTF;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.Vector2;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

/**
 * Diffraction MTF curve review, P1 and P2, on the Sigma 40mm f/1.4.
 *
 * <ul>
 *   <li>{@code SIGMA}: polychromatic MTF at fields 0, 0.7 and 1 with 64, 128 and 256 pupil rays.
 *   <li>{@code SHIFT}: a requested OTF phase reference displaced in y on axis. The modulus must
 *       not change, and the phase at 40 cycles/mm must be {@code -2 pi nu (-offset)} modulo
 *       2 pi, also for offsets beyond the 64-ray window half-width (P1).
 *   <li>{@code DEFOCUS}: 1 mm of defocus on axis, which no listed sampling holds (P2).
 * </ul>
 */
public class DiffractionProbe {

    public static void main(String[] args) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(ExampleFinder.geoPathToExample("Examples/jfotoptix/sigma-40mm-f1.4/Sigma-40mmf1.4.txt"));
        var prescription = Prescription.build_prescription(specs, true, false, false);
        var opm = new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, new double[]{0, .7, 1}, false, VigType.SetVig, true, 0);
        var wvls = opm.optical_spec.wvls;

        for (int fi = 0; fi < 3; fi++)
            for (int n : new int[]{64, 128, 256}) {
                var poly = new PolyDiffractionMTF(opm, opm.optical_spec.fov.fields[fi],
                        wvls.wavelengths, wvls.spectral_wts, 0, n, 2 * n);
                System.out.printf("SIGMA fi=%d n=%d s40=%.9f t40=%.9f s10=%.9f t10=%.9f%n", fi, n,
                        poly.sagittal(40), poly.tangential(40), poly.sagittal(10), poly.tangential(10));
            }

        var fld = opm.optical_spec.fov.fields[0];
        double wvl = wvls.central_wvl();
        var point = DiffractionMTF.image_point(opm, fld, wvl, 0);
        for (double offset : new double[]{0, .01, .03, .05}) {
            var mono = new DiffractionMTF(opm, fld, wvl, 0, 64, 128, new Vector2(point.x, point.y + offset));
            var otf = mono.tangential_otf(40);
            System.out.printf("SHIFT offset=%.4f modulus40=%.9f phase40=%.9f halfWidth=%.9f%n", offset,
                    Math.hypot(otf[0], otf[1]), Math.atan2(otf[1], otf[0]), 63 / (2 * mono.nu_cutoff_y));
        }
        var mono128 = new DiffractionMTF(opm, fld, wvl, 0, 128, 256, new Vector2(point.x, point.y + .03));
        var otf128 = mono128.tangential_otf(40);
        System.out.printf("SHIFT128 offset=.03 modulus=%.9f phase=%.9f%n",
                Math.hypot(otf128[0], otf128[1]), Math.atan2(otf128[1], otf128[0]));

        for (int n : new int[]{64, 128, 256}) {
            var mono = new DiffractionMTF(opm, fld, wvl, 1.0, n, 2 * n);
            var a = mono.tangential_otf(10);
            var b = mono.tangential_otf(40);
            System.out.printf("DEFOCUS n=%d t10=%.9f t40=%.9f%n", n, Math.hypot(a[0], a[1]), Math.hypot(b[0], b[1]));
        }
    }
}
