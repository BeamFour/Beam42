package org.redukti.examples;

import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.importers.obench.OpticalBenchDataImporter;

/**
 * REVIEW.md &sect;3 - how much does the un-centred mean tilt the sagittal/tangential
 * balance, and where?
 *
 * <p>The mean term is identically zero in the sagittal direction by symmetry and
 * substantial in the tangential, so leaving it in inflates tangential sum-of-squares
 * relative to sagittal. In a merit that sums both with equal weight, that is an implicit
 * tangential up-weighting - the optimizer sees more to gain from the tangential blocks
 * than the physics warrants.
 *
 * <p>Sagittal and tangential trade against each other through the astigmatic focus split:
 * moving the design toward the tangential focus moves it away from the sagittal one. A
 * merit summing both is largely indifferent to which is favoured, so a spurious term on
 * one side decides it.
 *
 * <p>The last column is the factor by which centring would effectively raise sagittal
 * relative to tangential in that block - the derived equivalent of setting a sagittal
 * weight by hand.
 */
public class ContrastProbe18 {

    private static final double[] FIELDS = {0.0, 0.3, 0.5, 0.7, 0.85, 1.0};
    private static final int[] FREQS = {10, 20, 40};

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

        System.out.println("================================================================");
        System.out.println(label + "   (starting design, 6x12, all wavelengths pooled)");
        System.out.println("================================================================");
        System.out.println("freq  fld |   sag SOS    tan SOS  | tan mean  mean^2 share |"
                + " implied sag up-weight");

        for (int freq : FREQS) {
            var result = ContrastAnalysis.eval(model, new ContrastOptions(freq)
                    .num_rings(6).num_spokes(12));
            for (int fi = 0; fi < FIELDS.length; fi++) {
                double sagSos = 0.0, tanSos = 0.0, tanMeanWeighted = 0.0, weightSum = 0.0;
                for (var wavelength : result.fields.get(fi).wavelengths()) {
                    for (var s : wavelength.samples()) {
                        if (!s.valid()) continue;
                        sagSos += s.weight() * s.sagittalDifference() * s.sagittalDifference();
                        tanSos += s.weight() * s.tangentialDifference() * s.tangentialDifference();
                        tanMeanWeighted += s.weight() * s.tangentialDifference();
                        weightSum += s.weight();
                    }
                }
                if (!(weightSum > 0.0)) continue;
                // Weights are normalized per wavelength group, so pool by weight sum.
                double tanMean = tanMeanWeighted / weightSum;
                double meanSquareTerm = weightSum * tanMean * tanMean;
                double tanCentred = tanSos - meanSquareTerm;
                double share = meanSquareTerm / tanSos;
                double upWeight = tanCentred > 0.0 ? tanSos / tanCentred : Double.NaN;
                System.out.printf("%4d  %.2f | %9.5f  %9.5f | %8.4f  %13.1f%% | %10.2fx%n",
                        freq, FIELDS[fi], sagSos, tanSos, tanMean, 100.0 * share, upWeight);
            }
        }
        System.out.println();
    }
}
