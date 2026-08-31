package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.TraceRingsDef;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PolychromaticRMSWavefrontProbe2Test {

    private record Sample(double weight, double opd) {}

    /** Per-wavelength OPD samples plus the pupil-centre OPD. */
    private static List<List<Sample>> perWavelength(OpticalModel model, int fieldIndex,
                                                    int numRings, int numSpokes,
                                                    boolean checkApertures,
                                                    double[] centreOpdOut) {
        var osp = model.optical_spec;
        var field = osp.fov.fields[fieldIndex];
        double focus = osp.defocus().get_focus();
        var definition = new TraceRingsDef();
        definition.num_rings = numRings;
        var primary = Trace.setup_pupil_coords(
                model, field, osp.wvls.central_wvl(), focus, null, null);
        var imagePoint = primary.ref_sphere.image_pt.project_xy();
        var traceOptions = new TraceOptions();
        traceOptions.check_apertures = checkApertures;
        var out = new ArrayList<List<Sample>>();
        for (int wi = 0; wi < osp.wvls.wavelengths.length; wi++) {
            double wavelength = osp.wvls.wavelengths[wi];
            var coords = Trace.setup_pupil_coords(
                    model, field, wavelength, focus, imagePoint, null);
            field.chief_ray = coords.chief_ray_pkg;
            field.ref_sphere = coords.ref_sphere;
            var list = new ArrayList<Sample>();
            for (var s : Trace.trace_gaussian_quadrature(
                    model, definition, numSpokes, field, wavelength, focus,
                    (pupil, ray) -> {
                        if (ray == null) return null;
                        Double opd = WavefrontAberrationAnalysis.opd(
                                model, pupil, 0, ray, field, wavelength, focus);
                        return opd != null && Double.isFinite(opd)
                                ? new GridItem(pupil, ray, opd) : null;
                    }, true, traceOptions)) {
                if (s.valid && s.result != null && Double.isFinite(s.result) && s.weight > 0.0)
                    list.add(new Sample(s.weight, s.result));
            }
            out.add(list);
            if (centreOpdOut != null) {
                var centre = new TraceOptions();
                centre.check_apertures = checkApertures;
                centre.pupil_type = PupilType.REL_PUPIL;
                centre.apply_vignetting = true;
                var pkg = Trace.trace_safe(model, new Vector2(0, 0), field, wavelength, centre).pkg;
                Double opd = pkg == null ? null : WavefrontAberrationAnalysis.opd(
                        model, new Vector2(0, 0), 0, pkg, field, wavelength, focus);
                centreOpdOut[wi] = opd == null ? Double.NaN : opd;
            }
        }
        return out;
    }

    private static double[] stats(List<Sample> s) {
        double w = 0, sum = 0, sq = 0;
        for (var x : s) {
            w += x.weight();
            sum += x.weight() * x.opd();
            sq += x.weight() * x.opd() * x.opd();
        }
        double mean = sum / w;
        return new double[]{w, mean, Math.sqrt(sq / w), Math.sqrt(Math.max(0, sq / w - mean * mean))};
    }

    @Test
    void probe_perWavelengthPiston() {
        var model = MtfTest.buildTestModel();
        System.out.println("--- per-wavelength piston");
        for (int fi = 0; fi < model.optical_spec.fov.fields.length; fi++) {
            double[] centre = new double[model.optical_spec.wvls.wavelengths.length];
            var lists = perWavelength(model, fi, 6, 12, true, centre);
            double totalW = 0, pooledAboutOwnMean = 0, pooledAboutZero = 0;
            for (int wi = 0; wi < lists.size(); wi++) {
                var st = stats(lists.get(wi));
                System.out.printf("  field %d wvl %.1f  centreOPD=%9.6f  mean=%9.6f  rms0=%.6f  rmsAboutMean=%.6f%n",
                        fi, model.optical_spec.wvls.wavelengths[wi], centre[wi], st[1], st[2], st[3]);
                totalW += st[0];
                pooledAboutZero += st[0] * st[2] * st[2];
                pooledAboutOwnMean += st[0] * st[3] * st[3];
            }
            System.out.printf("  field %d  poly(as coded)=%.6f  poly(piston removed per wvl)=%.6f%n",
                    fi, Math.sqrt(pooledAboutZero / totalW),
                    Math.sqrt(pooledAboutOwnMean / totalW));
        }
    }

    @Test
    void probe_apertureCheckSensitivity() {
        System.out.println("--- check_apertures on vs off");
        var on = PolychromaticRMSWavefrontAnalysis.eval(MtfTest.buildTestModel(),
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));
        var off = PolychromaticRMSWavefrontAnalysis.eval(MtfTest.buildTestModel(),
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12)
                        .check_apertures(false));
        for (int fi = 0; fi < on.fields().size(); fi++)
            System.out.printf("  field %d  on=%.6f (n=%d)  off=%.6f (n=%d)  rel diff=%.2f%%%n",
                    fi, on.fields().get(fi).rmsWaves(), on.fields().get(fi).validSamples(),
                    off.fields().get(fi).rmsWaves(), off.fields().get(fi).validSamples(),
                    100.0 * (on.fields().get(fi).rmsWaves() - off.fields().get(fi).rmsWaves())
                            / off.fields().get(fi).rmsWaves());
    }

    @Test
    void probe_samplingConvergence() {
        System.out.println("--- sampling convergence");
        int[][] cases = {{2, 6}, {4, 8}, {6, 12}, {8, 16}, {14, 20}, {20, 32}};
        for (int[] c : cases) {
            var r = PolychromaticRMSWavefrontAnalysis.eval(MtfTest.buildTestModel(),
                    new PolychromaticRMSWavefrontOptions().num_rings(c[0]).num_spokes(c[1]));
            System.out.printf("  %2dx%-2d  f0=%.6f  f1=%.6f  f2=%.6f%n",
                    c[0], c[1], r.fields().get(0).rmsWaves(),
                    r.fields().get(1).rmsWaves(), r.fields().get(2).rmsWaves());
        }
    }

    @Test
    void probe_zeroSpectralWeights() {
        System.out.println("--- degenerate: all spectral weights zero");
        var model = MtfTest.buildTestModel();
        model.optical_spec.wvls.spectral_wts = new double[]{0.0, 0.0, 0.0};
        var error = assertThrows(IllegalArgumentException.class,
                () -> PolychromaticRMSWavefrontAnalysis.eval(model,
                        new PolychromaticRMSWavefrontOptions().num_rings(2).num_spokes(6)));
        System.out.println("  rejected: " + error.getMessage());
    }
}
