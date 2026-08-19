package org.redukti.examples;

import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastAnalysisResult;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.importers.obench.OpticalBenchDataImporter;

/**
 * Does the frequency each contrast sample actually probes vary across the pupil, and by
 * enough to matter?
 *
 * <p>Not one of the numbered REVIEW.md probes. This measures the variation that
 * {@link ContrastOptions#calibrate_frequency(boolean)} cannot see. Calibration
 * infers one scale per field, wavelength and direction from a single probe pair; if the
 * entrance-to-exit pupil mapping were linear that would be exact, and every sample in a
 * block would report the same realised frequency. The spread reported here is the part of
 * the error a single scale leaves behind.
 *
 * <p>Columns, per (frequency, field) block, pooled over wavelengths:
 * <ul>
 *   <li><b>bias</b> - mean realised frequency over requested, minus one. What calibration
 *       corrects. Negative means the pair probes a lower frequency than asked for.</li>
 *   <li><b>spread</b> - standard deviation of the same ratio across the pupil, as a
 *       percentage. What calibration cannot correct.</li>
 *   <li><b>range</b> - min to max of the ratio, which is what an outlier sample sees.</li>
 *   <li><b>dSOS</b> - the historical, hypothetical change in the block's sum of squares
 *       from rescaling each sample to its measured frequency. The production
 *       normalization option was removed because this finite-OPD rescaling is not exact.</li>
 * </ul>
 */
public class ContrastPupilShearProbe {

    private static final double[] FIELDS = {0.0, 0.3, 0.5, 0.7, 0.85, 1.0};
    private static final int[] FREQS = {10, 20, 40};

    /**
     * With no arguments, reports the two reference lenses. Otherwise each argument is the
     * path to a prescription to report on, so the decision can be made against the design
     * actually being optimized rather than against these two.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            report("Leica R APO 75/2", ContrastProbes.leicaInputPath());
            report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
            return;
        }
        for (String path : args) report(path, path);
    }

    private static void report(String label, String specfile) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        OpticalModel model = new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);

        for (boolean calibrated : new boolean[]{false, true}) {
            System.out.println();
            System.out.println("================================================================"
                    + "================");
            System.out.println(label + "   6x12 samples, all wavelengths pooled, "
                    + (calibrated ? "calibrate_frequency ON" : "calibrate_frequency OFF"));
            System.out.println("================================================================"
                    + "================");
            System.out.printf("%4s %5s %4s | %8s %8s | %-17s | %8s%n",
                    "freq", "fld", "dir", "bias", "spread", "range", "dSOS");

            for (int freq : FREQS) {
                var options = new ContrastOptions(freq)
                        .num_rings(6).num_spokes(12)
                        .calibrate_frequency(calibrated)
                        .measure_frequency(true);
                var result = ContrastAnalysis.eval(model, options);
                for (int fi = 0; fi < FIELDS.length; fi++) {
                    row(freq, FIELDS[fi], "sag", result.fields.get(fi), freq, true);
                    row(freq, FIELDS[fi], "tan", result.fields.get(fi), freq, false);
                }
            }
        }
    }

    private static void row(int freq, double field,
                            String direction,
                            ContrastAnalysisResult.FieldResult fieldResult,
                            double requested, boolean sagittal) {
        double n = 0.0, sum = 0.0, sumSq = 0.0;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double sos = 0.0, sosNormalized = 0.0;

        for (var wavelength : fieldResult.wavelengths()) {
            for (var sample : wavelength.samples()) {
                if (!sample.valid() || sample.shear() == null) continue;
                double realized = sagittal
                        ? sample.shear().sagittalFrequency()
                        : sample.shear().tangentialFrequency();
                if (!Double.isFinite(realized) || !(realized > 0.0)) continue;
                double ratio = realized / requested;
                n += 1.0;
                sum += ratio;
                sumSq += ratio * ratio;
                min = Math.min(min, ratio);
                max = Math.max(max, ratio);

                double difference = sagittal
                        ? sample.sagittalDifference() : sample.tangentialDifference();
                double residual = Math.sqrt(sample.weight()) * difference;
                sos += residual * residual;
                double normalized = residual / ratio;
                sosNormalized += normalized * normalized;
            }
        }
        if (n < 2.0) {
            System.out.printf("%4d %5.2f %4s | %8s%n", freq, field, direction, "no data");
            return;
        }
        double mean = sum / n;
        double variance = Math.max(0.0, sumSq / n - mean * mean);
        double dSos = sos > 0.0 ? 100.0 * (sosNormalized - sos) / sos : 0.0;

        System.out.printf("%4d %5.2f %4s | %+7.2f%% %7.2f%% | %6.4f .. %-8.4f | %+7.2f%%%n",
                freq, field, direction,
                100.0 * (mean - 1.0), 100.0 * Math.sqrt(variance), min, max, dSos);
    }
}
