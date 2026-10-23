package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.analysis.*;
import org.redukti.optim.ParaxHelper;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.spec.Prescription;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.redukti.optim.OptimizationBuilder.contrast;

/** Paired experiment using the example factory's current merit, without retuning it. */
class LeicaApo75mmMandlerTest {
    private static final int[] FREQUENCIES = {10, 20, 40};

    @Test
    void optimizesUsingFactoryContractedQuadrature() throws Exception {
        optimize(false);
    }

    @Test
    void optimizesUsingFactoryOverlapQuadrature() throws Exception {
        optimize(true);
    }

    private static void optimize(boolean overlap) throws Exception {
        optimize(overlap, false);
    }

    @Test
    void optimizesUsingUniformContractedQuadrature() throws Exception {
        optimize(false, true);
    }

    @Test
    void optimizesUsingUniformOverlapQuadrature() throws Exception {
        optimize(true, true);
    }

    @Test
    void optimizesUsingDenseUniformContractedQuadrature() throws Exception {
        optimize(false, true, 12);
    }

    @Test
    void optimizesUsingDenseUniformOverlapQuadrature() throws Exception {
        optimize(true, true, 12);
    }

    // Mirrors the factory settings, replacing only its orientation/field weights.
    private static OptimizationBuilder.OptimizationSetup uniformSetup(Prescription prescription, int rings) {
        double[] fields = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] weights = new double[fields.length];
        Arrays.fill(weights, 1.0);
        boolean[] balance = {true, true, true, true, true, true, true, true, true, true, false};
        return OptimizationBuilder.builder(prescription)
                .fields(fields).varyAllCurvatures().varyAllThicknesses()
                .applyCurvatureConstraints().applyThicknessConstraints(5.0)
                .contrastSampling(rings, 2 * rings)
                .contrastGoals(contrast(10, weights, weights), contrast(20, weights, weights),
                        contrast(40, weights, weights))
                .calibrateContrastFrequency(false).centerContrastResiduals(false)
                .aimContrastAtExitPupil(false).vignetting(VigType.SetVig).freezeVignetting()
                .checkSpotApertures(false).applyEdgeThicknessConstraints()
                .contrastBalanceGoals(balance, 4.0).build();
    }

    private static void optimize(boolean overlap, boolean uniform) throws Exception {
        optimize(overlap, uniform, 6);
    }

    private static void optimize(boolean overlap, boolean uniform, int rings) throws Exception {
        var prescription = LeicaApo75mmMandler.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = uniform ? uniformSetup(prescription, rings)
                : LeicaApo75mmMandler.createContrastSetup(prescription, false, false,
                new double[]{1, 1, 1, 1});
        var analysis = setup.analysis();
        // Goals read this flag through the shared Analysis; no goals or weights are rebuilt.
        analysis.using_contrast_overlap_quadrature(overlap);
        assertEquals(rings, analysis._contrast_num_rings);
        assertEquals(2 * rings, analysis._contrast_num_spokes);
        assertTrue(analysis._freeze_vignetting);
        assertEquals(VigType.SetVig, analysis._vig_type);
        assertFalse(analysis._contrast_center_residuals);
        assertFalse(analysis._contrast_aim_exit_pupil);
        assertFalse(analysis._contrast_calibrate_frequency);
        String label = "Mandler" + (uniform ? "Uniform" : "") + (overlap ? "Overlap" : "Contracted");
        if (rings != 6) label += rings + "x" + 2 * rings;
        System.out.println(label + " START: variables=" + setup.variables().length
                + " goals=" + setup.goals().length);
        analysis.compute();
        var initialPhysical = new RayOpticsModelBuilder(prescription).build_optical_model(
                true, analysis._fields, false, VigType.SetVig, true, 0);
        reportPhysical(label + " initial", initialPhysical);
        var merit = setup.meritFunction(false);
        double initialRms = merit.getRMS();
        long started = System.nanoTime();
        int status = merit.getSolver().solve();
        long elapsed = (System.nanoTime() - started) / 1_000_000;
        double finalRms = merit.getRMS();
        System.out.println(label + " solve: status=" + status + " elapsedMs=" + elapsed
                + " initialRms=" + initialRms + " finalRms=" + finalRms);
        assertTrue(status > 0, "Optimizer failed with status " + status);
        assertTrue(Double.isFinite(finalRms) && finalRms < initialRms);

        Path directory = Path.of("target", "mandler-overlap-comparison");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(label + ".txt"),
                prescription.to_opt_bench_str(new StringBuilder()).toString());
        analysis.required_analyses(true, false, true);
        analysis._freqs = FREQUENCIES.clone();
        analysis.compute();
        System.out.println(label + " paraxial: efl=" + analysis._pfo[ParaxHelper.Effective_focal_length]
                + " fno=" + analysis._pfo[ParaxHelper.Fno]);
        for (var mtf : analysis._mtfs)
            printMtf(label + " frozen-unclipped", mtf);
        for (int evaluationRings : new int[]{6, 12, 24, 32}) {
            for (int frequency : FREQUENCIES) {
                var contrast = ContrastAnalysis.eval(analysis._opt_model,
                        new ContrastOptions(frequency).num_rings(evaluationRings).num_spokes(2 * evaluationRings)
                                .overlap_quadrature(true));
                double sum = 0;
                int blocks = 0;
                for (var field : contrast.fields)
                    for (var block : field.wavelengths())
                        for (int axis : new int[]{0, 1}) {
                            double rms = ContrastReport.rms(block, axis);
                            assertTrue(Double.isFinite(rms));
                            sum += rms * rms;
                            blocks++;
                        }
                System.out.println(label + " overlap score " + frequency + " " + evaluationRings + "x" + 2 * evaluationRings
                        + " blockRms=" + Math.sqrt(sum / blocks));
            }
        }
        var physical = new RayOpticsModelBuilder(prescription).build_optical_model(
                true, analysis._fields, false, VigType.SetVig, true, 0);
        reportPhysical(label + " final", physical);
        for (int n : new int[]{128, 256})
            for (int fi : new int[]{0, 3, 5, 7, 10}) {
                var field = physical.optical_spec.fov.fields[fi];
                double wavelength = physical.optical_spec.wvls.central_wvl();
                var mtf = new DiffractionMTF(physical, field, wavelength, 0, n, 2 * n);
                for (int frequency : FREQUENCIES)
                    System.out.printf(Locale.ROOT, "%s diffraction n=%d field=%.1f wavelength=%.4f freq=%d sag=%.8f tan=%.8f%n",
                            label, n, analysis._fields[fi], wavelength, frequency,
                            mtf.sagittal(frequency), mtf.tangential(frequency));
            }
    }

    private static void reportPhysical(String label, OpticalModel model) {
        var spots = SpotAnalysis.eval(model, new SpotOptions().use_hexapolar().num_rays(64));
        for (var mtf : spots.computeMTFs(FREQUENCIES))
            printMtf(label + " physical-clipped", mtf);
        System.out.println(label + " physical spot=" + Arrays.toString(spots.spot_results.stream()
                .mapToDouble(spot -> spot.get_mean_radius()).toArray()));
    }

    private static void printMtf(String label, MTFResultByFreq mtf) {
        System.out.println(label + " MTF " + mtf.freq + " sag=" + Arrays.toString(mtf.sag_mtf_by_field)
                + " tan=" + Arrays.toString(mtf.tan_mtf_by_field));
        for (double value : mtf.sag_mtf_by_field) assertTrue(Double.isFinite(value));
        for (double value : mtf.tan_mtf_by_field) assertTrue(Double.isFinite(value));
    }
}
