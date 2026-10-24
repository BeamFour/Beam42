package org.redukti.examples;

import org.redukti.analysis.DiffractionMTF;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.raytr.RayGrid;
import org.redukti.rayoptics.util.Lists;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Calibration of the diffraction MTF undersampling warning, {@link DiffractionMTF#spot_to_window}.
 *
 * <p>Per field, at 486.1 nm, nominal focus and 64 pupil rays: the fraction of grid rays landing
 * outside the PSF window, the 90th, 95th and 99th percentiles of each ray's distance from the
 * chief-ray image point relative to the window half-width, and the largest change in the
 * exact-frequency OTF modulus at 10, 30 and 50 cycles/mm, sagittal and tangential, on going to
 * 256 rays. The warning uses the 99th percentile.
 */
public class DiffractionProbe2 {

    private static final double WVL = 486.1327;
    private static final int N = 64;

    public static void main(String[] args) throws Exception {
        report("Examples/jfotoptix/leica-summicron-50mm-f2/US004123144_Example09P.txt");
        report("Examples/jfotoptix/sigma-40mm-f1.4/Sigma-40mmf1.4.txt");
    }

    private static void report(String specfile) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(ExampleFinder.geoPathToExample(specfile));
        var prescription = Prescription.build_prescription(specs, true, false, false);
        var opm = new RayOpticsModelBuilder(prescription).build_optical_model(true,
                new double[]{0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1}, false, VigType.SetPupil, true, 0);
        System.out.println("== " + specfile);
        System.out.println("field  frac_out  p90   p95   p99   maxerr");
        for (var fld : opm.optical_spec.fov.fields) {
            var m = new DiffractionMTF(opm, fld, WVL, 0.0, N, 2 * N);
            var r = new DiffractionMTF(opm, fld, WVL, 0.0, 256, 512);
            double err = 0;
            for (double nu : new double[]{10, 30, 50}) {
                err = Math.max(err, Math.abs(modulus(m.sagittal_otf(nu)) - modulus(r.sagittal_otf(nu))));
                err = Math.max(err, Math.abs(modulus(m.tangential_otf(nu)) - modulus(r.tangential_otf(nu))));
            }
            var grid = new RayGrid(opm, fld, WVL, 0.0, null, null, null, null, N, true, Double.NaN, 1.0);
            var chief = DiffractionMTF.image_point(opm, fld, WVL, 0.0);
            double hx = (N - 1) / (2 * m.nu_cutoff_x), hy = (N - 1) / (2 * m.nu_cutoff_y);
            var extents = new ArrayList<Double>();
            for (var row : grid.grid_pkg.grid())
                for (var item : row) {
                    if (item.ray_pkg == null) continue;
                    var last = Lists.get(item.ray_pkg.ray, -1);
                    extents.add(Math.max(Math.abs(last.p.x - chief.x) / hx, Math.abs(last.p.y - chief.y) / hy));
                }
            Collections.sort(extents);
            long outside = extents.stream().filter(v -> v > 1).count();
            System.out.printf("%.1f   %.4f   %.2f  %.2f  %.2f  %.4f%n", fld.y, outside / (double) extents.size(),
                    extents.get((int) (0.9 * extents.size())), extents.get((int) (0.95 * extents.size())),
                    extents.get((int) (0.99 * extents.size()) - 1), err);
        }
    }

    private static double modulus(double[] otf) {
        return Math.hypot(otf[0], otf[1]);
    }
}
