package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastAnalysisResult;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.importers.obench.OpticalBenchDataImporter;

/**
 * REVIEW.md &sect;9 follow-up - does {@code normalize_frequency} work?
 *
 * <p>The option rescales each wavefront difference by
 * {@code frequency_requested / frequency_realized}. That rests on two things being true,
 * and they are independent of each other:
 *
 * <ol>
 *   <li>the rescaling itself - that {@code dW} is proportional to the shear over the few
 *       percent the correction spans, so multiplying a difference measured at one shear
 *       reconstructs the difference at another;</li>
 *   <li>the target - that {@code frequency_realized} is the frequency the pair actually
 *       sampled in Hopkins' OTF coordinates, which finding 9 disputes.</li>
 * </ol>
 *
 * <p><b>Part A</b> tests the rescaling directly. It traces each pair at the nominal shear
 * and again at shears scaled by a few percent, and compares the wavefront difference that
 * actually results against the one the rescaling assumes. This is the assumption's error,
 * measured rather than argued.
 *
 * <p><b>Part B</b> tests the target. It forms the normalized residuals twice, once driven
 * by the direction-cosine metric the option currently uses and once by the exit-pupil
 * separation, and compares the block sums of squares. This is what switching metrics would
 * cost or gain.
 *
 * <p><b>Part C</b> is a consistency check on the metrics themselves. The entrance-to-exit
 * pupil mapping is a property of the lens, not of the frequency being requested, so the
 * ratio of realized to requested frequency should be very nearly the same at 10 and at 40
 * cycles/mm. A metric that drifts with frequency is measuring something else as well.
 */
public class ContrastProbe20 {

    private static final double[] FIELDS = {0.0, 0.5, 0.7, 1.0};
    private static final int[] FREQS = {10, 40};
    private static final double[] STRETCH = {0.02, 0.05, 0.10};

    public static void main(String[] args) throws Exception {
        report("Leica R APO 75/2", ContrastProbes.leicaInputPath());
        report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
    }

    private static void report(String label, String specfile) throws Exception {
        OpticalModel model = build(specfile);
        System.out.println();
        System.out.println("================================================================");
        System.out.println(label);
        System.out.println("================================================================");
        partA(model);
        partB(model);
        partC(model);
    }

    private static OpticalModel build(String specfile) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        return new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);
    }

    /**
     * Is dW proportional to the shear? Trace a pair at shear s and at s(1+e), and compare
     * the measured ratio of wavefront differences against the assumed (1+e).
     */
    private static void partA(OpticalModel model) {
        System.out.println();
        System.out.println("A. is the rescaling valid? measured dW ratio vs assumed (1+e)");
        System.out.printf("%4s %5s %4s | %8s %8s %8s   (mean |error| in the assumed ratio)%n",
                "freq", "fld", "dir", "e=2%", "e=5%", "e=10%");

        var traceOptions = new TraceOptions();
        traceOptions.pupil_type = PupilType.REL_PUPIL;
        traceOptions.apply_vignetting = false;
        traceOptions.check_apertures = false;

        double wvl = model.seq_model.central_wavelength();
        double focus = model.optical_spec.defocus().get_focus();

        for (int freq : FREQS) {
            double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wvl, freq);
            for (int fi = 0; fi < FIELDS.length; fi++) {
                var field = model.optical_spec.fov.fields[fi];
                var coords = Trace.setup_pupil_coords(model, field, wvl, focus, null, null);
                field.chief_ray = coords.chief_ray_pkg;
                field.ref_sphere = coords.ref_sphere;

                for (int t = 0; t < 2; t++) {
                    boolean tangential = t == 1;
                    var errors = new Stats[STRETCH.length];
                    for (int i = 0; i < errors.length; i++) errors[i] = new Stats();

                    // A ring of base points well inside the pupil so the stretched
                    // partner is not pushed past the rim.
                    for (double r = -0.5; r <= 0.51; r += 0.25) {
                        var base = tangential ? new Vector2(0.0, r) : new Vector2(r, 0.0);
                        Double w0 = opd(model, base, field, wvl, focus, traceOptions);
                        if (w0 == null) continue;
                        Double wNominal = shifted(model, base, shift, tangential,
                                field, wvl, focus, traceOptions);
                        if (wNominal == null) continue;
                        double dwNominal = wNominal - w0;
                        if (Math.abs(dwNominal) < 1.0e-9) continue;

                        for (int i = 0; i < STRETCH.length; i++) {
                            Double wStretched = shifted(model, base, shift * (1.0 + STRETCH[i]),
                                    tangential, field, wvl, focus, traceOptions);
                            if (wStretched == null) continue;
                            double measured = (wStretched - w0) / dwNominal;
                            double assumed = 1.0 + STRETCH[i];
                            errors[i].add(Math.abs(measured - assumed) / assumed);
                        }
                    }
                    if (errors[0].n < 2) continue;
                    System.out.printf("%4d %5.2f %4s | %7.3f%% %7.3f%% %7.3f%%%n",
                            freq, FIELDS[fi], tangential ? "tan" : "sag",
                            100.0 * errors[0].mean(), 100.0 * errors[1].mean(),
                            100.0 * errors[2].mean());
                }
            }
        }
    }

    /** Normalizing by the direction metric against normalizing by the pupil metric. */
    private static void partB(OpticalModel model) {
        System.out.println();
        System.out.println("B. block sum of squares under each normalization metric");
        System.out.printf("%4s %5s %4s | %11s %11s %11s | %9s %9s%n",
                "freq", "fld", "dir", "SOS raw", "SOS by dir", "SOS by pup",
                "dir vs raw", "pup vs dir");

        for (int freq : FREQS) {
            var result = ContrastAnalysis.eval(model, new ContrastOptions(freq)
                    .num_rings(6).num_spokes(12)
                    .calibrate_frequency(true).measure_frequency(true));
            for (int fi = 0; fi < FIELDS.length; fi++) {
                for (int t = 0; t < 2; t++) {
                    boolean tangential = t == 1;
                    double raw = 0.0, byDir = 0.0, byPupil = 0.0;
                    for (var wavelength : result.fields.get(fi).wavelengths()) {
                        for (var sample : wavelength.samples()) {
                            if (!sample.valid() || sample.shear() == null) continue;
                            double dw = tangential
                                    ? sample.tangentialDifference() : sample.sagittalDifference();
                            double nuDir = tangential
                                    ? sample.shear().tangentialFrequency()
                                    : sample.shear().sagittalFrequency();
                            double nuPupil = tangential
                                    ? sample.shear().tangentialPupilFrequencyOnAxis()
                                    : sample.shear().sagittalPupilFrequencyOnAxis();
                            if (!Double.isFinite(nuDir) || !Double.isFinite(nuPupil)) continue;
                            if (!(nuDir > 0.0) || !(nuPupil > 0.0)) continue;
                            double w = sample.weight();
                            raw += w * dw * dw;
                            byDir += w * sq(dw * freq / nuDir);
                            byPupil += w * sq(dw * freq / nuPupil);
                        }
                    }
                    if (!(raw > 0.0)) continue;
                    System.out.printf("%4d %5.2f %4s | %11.5e %11.5e %11.5e | %+8.2f%% %+8.2f%%%n",
                            freq, FIELDS[fi], tangential ? "tan" : "sag",
                            raw, byDir, byPupil,
                            100.0 * (byDir - raw) / raw, 100.0 * (byPupil - byDir) / byDir);
                }
            }
        }
    }

    /** The pupil mapping is a lens property; a metric measuring it should not drift with frequency. */
    private static void partC(OpticalModel model) {
        System.out.println();
        System.out.println("C. is the metric frequency-independent? ratio realized/requested");
        System.out.printf("%5s %4s | %19s | %19s%n",
                "fld", "dir", "direction @ 10 / 40", "pupil @ 10 / 40");

        double[][] dir = new double[FIELDS.length * 2][FREQS.length];
        double[][] pup = new double[FIELDS.length * 2][FREQS.length];

        for (int f = 0; f < FREQS.length; f++) {
            var result = ContrastAnalysis.eval(model, new ContrastOptions(FREQS[f])
                    .num_rings(6).num_spokes(12)
                    .calibrate_frequency(false).measure_frequency(true));
            for (int fi = 0; fi < FIELDS.length; fi++) {
                for (int t = 0; t < 2; t++) {
                    var d = new Stats();
                    var p = new Stats();
                    for (var wavelength : result.fields.get(fi).wavelengths()) {
                        for (var sample : wavelength.samples()) {
                            if (!sample.valid() || sample.shear() == null) continue;
                            d.add((t == 1 ? sample.shear().tangentialFrequency()
                                    : sample.shear().sagittalFrequency()) / FREQS[f]);
                            p.add((t == 1 ? sample.shear().tangentialPupilFrequencyOnAxis()
                                    : sample.shear().sagittalPupilFrequencyOnAxis()) / FREQS[f]);
                        }
                    }
                    dir[fi * 2 + t][f] = d.mean();
                    pup[fi * 2 + t][f] = p.mean();
                }
            }
        }
        for (int fi = 0; fi < FIELDS.length; fi++) {
            for (int t = 0; t < 2; t++) {
                int row = fi * 2 + t;
                System.out.printf("%5.2f %4s | %8.4f %8.4f  | %8.4f %8.4f%n",
                        FIELDS[fi], t == 1 ? "tan" : "sag",
                        dir[row][0], dir[row][1], pup[row][0], pup[row][1]);
            }
        }
    }

    private static Double shifted(OpticalModel model, Vector2 base, double shift,
                                  boolean tangential,
                                  org.redukti.rayoptics.specs.Field field,
                                  double wvl, double focus, TraceOptions traceOptions) {
        var pupil = tangential
                ? new Vector2(base.x, base.y + shift)
                : new Vector2(base.x + shift, base.y);
        return opd(model, pupil, field, wvl, focus, traceOptions);
    }

    private static Double opd(OpticalModel model, Vector2 pupil,
                              org.redukti.rayoptics.specs.Field field,
                              double wvl, double focus, TraceOptions traceOptions) {
        var pkg = Trace.trace_safe(model, pupil, field, wvl, traceOptions).pkg;
        if (pkg == null) return null;
        var value = WavefrontAberrationAnalysis.opd(model, pupil, 0, pkg, field, wvl, focus);
        return value == null || !Double.isFinite(value) ? null : value;
    }

    private static double sq(double v) { return v * v; }

    private static final class Stats {
        int n;
        double sum;
        void add(double v) { if (Double.isFinite(v)) { n++; sum += v; } }
        double mean() { return n > 0 ? sum / n : Double.NaN; }
    }
}
