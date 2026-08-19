package org.redukti.examples;

import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastAnalysisResult;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.analysis.FrequencyMetric;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.importers.obench.OpticalBenchDataImporter;

/**
 * REVIEW.md &sect;9 follow-up - how much of the existing frequency calibration is real?
 *
 * <p>Finding 7 recorded that {@code calibrate_frequency} takes the worst case from 8.5
 * percent low to 0.08 percent low. Finding 9 observes that the probe and the check both
 * used the direction-cosine metric, so that number shows the probe reproducing its own
 * metric rather than the pair achieving the requested separation in Hopkins' coordinates.
 *
 * <p>This scores all three configurations against a single yardstick: the exit-pupil
 * separation, which is the coordinate the OTF is defined over. The question is not whether
 * calibration converges on its own metric - it does - but how much of the error it removes
 * when measured properly.
 *
 * <ul>
 *   <li><b>off</b> - no calibration.</li>
 *   <li><b>by direction</b> - the probe as it ships, scaling on direction cosines.</li>
 *   <li><b>by pupil</b> - the same probe scaling on exit-pupil separation.</li>
 * </ul>
 *
 * <p>Every column reports the mean ratio of realised to requested frequency measured on
 * the exit-pupil metric, so 1.0000 is correct in all three. The direction-metric ratio is
 * shown alongside for the middle configuration, to make visible the gap between what the
 * probe optimises and what matters.
 */
public class ContrastProbe21 {

    private static final double[] FIELDS = {0.0, 0.3, 0.5, 0.7, 0.85, 1.0};
    private static final int[] FREQS = {10, 40};

    public static void main(String[] args) throws Exception {
        report("Leica R APO 75/2", ContrastProbes.leicaInputPath());
        report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
    }

    private static void report(String label, String specfile) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        OpticalModel model = new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);

        System.out.println();
        System.out.println("=========================================================================");
        System.out.println(label + "   6x12, all wavelengths pooled");
        System.out.println("   all ratios measured on the exit-pupil metric; 1.0000 is correct");
        System.out.println("=========================================================================");
        System.out.printf("%4s %5s %4s | %9s %9s %9s | %11s%n",
                "freq", "fld", "dir", "off", "by dir", "by pupil", "(dir metric)");

        for (int freq : FREQS) {
            var off = ContrastAnalysis.eval(model, options(freq).calibrate_frequency(false));
            var byDirection = ContrastAnalysis.eval(model, options(freq)
                    .calibrate_frequency(true).frequency_metric(FrequencyMetric.RAY_DIRECTION));
            var byPupil = ContrastAnalysis.eval(model, options(freq)
                    .calibrate_frequency(true).frequency_metric(FrequencyMetric.EXIT_PUPIL));

            for (int fi = 0; fi < FIELDS.length; fi++) {
                for (int t = 0; t < 2; t++) {
                    boolean tangential = t == 1;
                    double a = ratio(off, fi, tangential, freq, true);
                    double b = ratio(byDirection, fi, tangential, freq, true);
                    double c = ratio(byPupil, fi, tangential, freq, true);
                    double bDir = ratio(byDirection, fi, tangential, freq, false);
                    if (!Double.isFinite(a)) continue;
                    System.out.printf("%4d %5.2f %4s | %9.4f %9.4f %9.4f | %11.4f%n",
                            freq, FIELDS[fi], tangential ? "tan" : "sag", a, b, c, bDir);
                }
            }
        }
    }

    private static ContrastOptions options(int freq) {
        return new ContrastOptions(freq).num_rings(6).num_spokes(12).measure_frequency(true);
    }

    /** Mean realised/requested over a block, on either metric. */
    private static double ratio(ContrastAnalysisResult result, int fieldIndex,
                                boolean tangential, double requested, boolean pupilMetric) {
        double sum = 0.0;
        int n = 0;
        for (var wavelength : result.fields.get(fieldIndex).wavelengths()) {
            for (var sample : wavelength.samples()) {
                if (!sample.valid() || sample.shear() == null) continue;
                var shear = sample.shear();
                double realized = pupilMetric
                        ? (tangential ? shear.tangentialPupilFrequencyOnAxis()
                                      : shear.sagittalPupilFrequencyOnAxis())
                        : (tangential ? shear.tangentialFrequency() : shear.sagittalFrequency());
                if (!Double.isFinite(realized) || !(realized > 0.0)) continue;
                sum += realized / requested;
                n++;
            }
        }
        return n > 0 ? sum / n : Double.NaN;
    }
}
