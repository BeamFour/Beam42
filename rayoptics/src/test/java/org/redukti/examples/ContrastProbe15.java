package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

/**
 * How much does the vignetting mode move the measured MTF, on a fixed prescription?
 *
 * <p>{@code Analysis} builds every optical model with {@link VigType#Paraxial}, and
 * {@code Trace.apply_paraxial_vignetting} sets only {@code vuy}/{@code vly} - the x
 * factors stay at zero, because a paraxial ray is meridional and can say nothing about
 * the sagittal pupil. Both the contrast merit and the geometric MTF that validates it
 * therefore run on a pupil that is full width in x at every field. LensTool2 reports
 * against {@link VigType#SetPupil}, which resizes the pupil to put the axial marginal ray
 * on the stop edge and then measures all four factors with real rays.
 *
 * <p>Nothing is optimized here. Every difference between the columns is the vignetting
 * mode alone, on identical prescriptions.
 *
 * <p><b>Result: paraxial vignetting breaks on-axis rotational symmetry.</b> At field 0.0
 * sagittal and tangential MTF must be identical. Under the real-ray modes they are, to
 * every printed digit. Under {@code Paraxial} they are not, because a nonzero {@code vuy}
 * with {@code vux = 0} makes the on-axis pupil an ellipse - full width in x, narrowed in
 * y. The gap at 40 cyc/mm is 0.148 on the Leica and 0.010 on the Otus, which is the order
 * of difference between the lens that shows the sagittal pathology and the one that does
 * not.
 */
public class ContrastProbe15 {

    private static final int[] FREQS = {10, 20, 40};
    private static final double[] FIELDS = {0.0, 0.3, 0.7, 1.0};
    private static final VigType[] MODES = {
            VigType.Paraxial, VigType.SetVig, VigType.SetPupil};

    public static void main(String[] args) throws Exception {
        report("Leica R APO 75/2 (double Gauss)", ContrastProbes.leicaInputPath());
        report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
    }

    private static void report(String label, String specfile) throws Exception {
        System.out.println("================================================================");
        System.out.println(label);
        System.out.println("================================================================");

        var measured = new Measured[MODES.length];
        for (int m = 0; m < MODES.length; m++) measured[m] = measure(specfile, MODES[m]);

        System.out.printf("%-10s  working F/#   x half-width by field (0 .3 .7 1.0)%n", "mode");
        for (int m = 0; m < MODES.length; m++) {
            System.out.printf("%-10s  %8.4f    ", MODES[m], measured[m].fno);
            for (double x : measured[m].pupilX) System.out.printf(" %.4f", x);
            System.out.println();
        }

        System.out.println("\non-axis rotational symmetry (sag and tan must be equal):");
        System.out.println("mode        freq |  sagittal  tangential   asymmetry");
        for (int m = 0; m < MODES.length; m++) {
            for (int f = 0; f < FREQS.length; f++) {
                double s = measured[m].sag[f][0];
                double t = measured[m].tan[f][0];
                System.out.printf("%-10s  %3d  |  %.4f    %.4f      %+.4f%n",
                        MODES[m], FREQS[f], s, t, t - s);
            }
        }

        System.out.println("\nsagittal MTF by field:");
        System.out.print("mode        freq |");
        for (double f : FIELDS) System.out.printf("   %.1f  ", f);
        System.out.println();
        for (int m = 0; m < MODES.length; m++) {
            for (int f = 0; f < FREQS.length; f++) {
                System.out.printf("%-10s  %3d  |", MODES[m], FREQS[f]);
                for (int fi = 0; fi < FIELDS.length; fi++)
                    System.out.printf(" %.4f", measured[m].sag[f][fi]);
                System.out.println();
            }
        }
        System.out.println();
    }

    private record Measured(double fno, double[] pupilX, double[][] sag, double[][] tan) {
    }

    private static Measured measure(String specfile, VigType vigType) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        OpticalModel model = new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, vigType, true, 0);

        var fields = model.optical_spec.fov.fields;
        double[] pupilX = new double[fields.length];
        for (int fi = 0; fi < fields.length; fi++) {
            pupilX[fi] = 0.5 * (fields[fi].apply_vignetting(new double[]{1.0, 0.0})[0]
                    - fields[fi].apply_vignetting(new double[]{-1.0, 0.0})[0]);
        }

        var spotAnalysis = SpotAnalysis.eval(model, new SpotOptions()
                .use_gaussian_quadrature().num_rings(6).num_spokes(12));
        var mtfs = spotAnalysis.computeMTFs(FREQS);
        double[][] sag = new double[FREQS.length][];
        double[][] tan = new double[FREQS.length][];
        for (int i = 0; i < FREQS.length; i++) {
            sag[i] = mtfs[i].sag_mtf_by_field;
            tan[i] = mtfs[i].tan_mtf_by_field;
        }
        return new Measured(model.optical_spec.parax_data.fod.fno, pupilX, sag, tan);
    }
}
