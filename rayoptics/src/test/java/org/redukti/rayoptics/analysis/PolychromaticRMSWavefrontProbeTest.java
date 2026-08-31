package org.redukti.rayoptics.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.GridItem;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.TraceRingsDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only probes used while reviewing PolychromaticRMSWavefrontAnalysis.
 * These print measurements rather than lock behaviour.
 */
class PolychromaticRMSWavefrontProbeTest {

    /** Re-trace the same samples the analysis uses, keeping the raw OPDs. */
    private static List<double[]> opdSamples(OpticalModel model, int fieldIndex,
                                             int numRings, int numSpokes) {
        var osp = model.optical_spec;
        var field = osp.fov.fields[fieldIndex];
        double focus = osp.defocus().get_focus();
        var definition = new TraceRingsDef();
        definition.num_rings = numRings;
        var primary = Trace.setup_pupil_coords(
                model, field, osp.wvls.central_wvl(), focus, null, null);
        var imagePoint = primary.ref_sphere.image_pt.project_xy();
        var out = new ArrayList<double[]>();
        var traceOptions = new TraceOptions();
        traceOptions.check_apertures = true;
        for (double wavelength : osp.wvls.wavelengths) {
            var coords = Trace.setup_pupil_coords(
                    model, field, wavelength, focus, imagePoint, null);
            field.chief_ray = coords.chief_ray_pkg;
            field.ref_sphere = coords.ref_sphere;
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
                    out.add(new double[]{wavelength, s.weight, s.result});
            }
        }
        return out;
    }

    @Test
    void probe_pistonIsNotRemoved() {
        var model = MtfTest.buildTestModel();
        System.out.println("--- piston probe: reference_wvl index = "
                + model.optical_spec.wvls.reference_wvl);
        var result = PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));
        for (int fi = 0; fi < model.optical_spec.fov.fields.length; fi++) {
            var samples = opdSamples(model, fi, 6, 12);
            double w = 0, sum = 0, sumsq = 0;
            for (var s : samples) { w += s[1]; sum += s[1] * s[2]; sumsq += s[1] * s[2] * s[2]; }
            double mean = sum / w;
            double aboutZero = Math.sqrt(sumsq / w);
            double aboutMean = Math.sqrt(Math.max(0.0, sumsq / w - mean * mean));
            System.out.printf(
                    "field %d  analysis=%.6f  aboutZero=%.6f  mean(piston)=%.6f  aboutMean=%.6f  ratio=%.3f%n",
                    fi, result.fields().get(fi).rmsWaves(), aboutZero, mean, aboutMean,
                    aboutMean > 0 ? aboutZero / aboutMean : Double.NaN);
        }
    }

    @Test
    void probe_resultDependsOnIncomingFieldState() {
        var fresh = MtfTest.buildTestModel();
        var a = PolychromaticRMSWavefrontAnalysis.eval(fresh,
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));
        // second call on the same (now dirty) model
        var b = PolychromaticRMSWavefrontAnalysis.eval(fresh,
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));

        // a model whose fields were last touched by an analysis at the LAST wavelength
        var seeded = MtfTest.buildTestModel();
        double focus = seeded.optical_spec.defocus().get_focus();
        double last = seeded.optical_spec.wvls.wavelengths[
                seeded.optical_spec.wvls.wavelengths.length - 1];
        for (var f : seeded.optical_spec.fov.fields) {
            var c = Trace.setup_pupil_coords(seeded, f, last, focus, null, null);
            f.chief_ray = c.chief_ray_pkg;
            f.ref_sphere = c.ref_sphere;
        }
        var c = PolychromaticRMSWavefrontAnalysis.eval(seeded,
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));

        System.out.println("--- incoming-state probe (fresh / repeat / pre-seeded-at-last-wvl)");
        for (int fi = 0; fi < a.fields().size(); fi++) {
            System.out.printf("field %d  fresh=%.9f  repeat=%.9f  seeded=%.9f  d(seeded)=%.3e%n",
                    fi, a.fields().get(fi).rmsWaves(), b.fields().get(fi).rmsWaves(),
                    c.fields().get(fi).rmsWaves(),
                    c.fields().get(fi).rmsWaves() - a.fields().get(fi).rmsWaves());
        }
        System.out.println("aim_info after eval, per field:");
        for (var f : fresh.optical_spec.fov.fields)
            System.out.println("   chief_ray wvl left on field = " + f.chief_ray.chief_ray.wvl
                    + "  (central = " + fresh.optical_spec.wvls.central_wvl() + ")");
    }

    @Test
    void probe_pupilWeightBudget() {
        var model = MtfTest.buildTestModel();
        var result = PolychromaticRMSWavefrontAnalysis.eval(model,
                new PolychromaticRMSWavefrontOptions().num_rings(6).num_spokes(12));
        System.out.println("--- pupil weight budget (1.0 == every quadrature sample survived)");
        for (int fi = 0; fi < result.fields().size(); fi++) {
            var f = result.fields().get(fi);
            for (var w : f.wavelengths())
                System.out.printf("field %d wvl %.1f  pupilWeight=%.6f  n=%d  rms=%.6f%n",
                        fi, w.wavelength(), w.pupilWeight(), w.validSamples(), w.rmsWaves());
            System.out.printf("field %d  poly=%.6f%n", fi, f.rmsWaves());
        }
    }

    @Test
    void probe_optionsValidationGaps() {
        System.out.println("--- options validation");
        try {
            new PolychromaticRMSWavefrontOptions().num_spokes(2);
            System.out.println("num_spokes(2) accepted by options");
        } catch (RuntimeException e) {
            System.out.println("num_spokes(2) rejected by options: " + e.getMessage());
        }
        try {
            PolychromaticRMSWavefrontAnalysis.eval(MtfTest.buildTestModel(),
                    new PolychromaticRMSWavefrontOptions().num_rings(2).num_spokes(2));
            System.out.println("eval with 2 spokes succeeded");
        } catch (RuntimeException e) {
            System.out.println("eval with 2 spokes threw " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
        }
        var shared = new TraceOptions();
        shared.check_apertures = false;
        new PolychromaticRMSWavefrontOptions().trace_options(shared).check_apertures(true);
        System.out.println("caller's TraceOptions.check_apertures after builder = "
                + shared.check_apertures);
    }
}
