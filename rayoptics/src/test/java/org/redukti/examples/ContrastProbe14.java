package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

/**
 * Does the contrast sampler cover the sagittal pupil? <b>Yes - this probe refutes the
 * hypothesis it was written to test.</b> Kept as the record of a ruled-out cause, and
 * because its vignetting-factor table is what led to {@link ContrastProbe15}.
 *
 * <p>The hypothesis was that {@code generate_contrast_quadrature}, which contracts the
 * whole pattern by one isotropic scalar until every sample and both of its displaced
 * partners fit, would let the tangential partner drive the contraction on a pupil much
 * shorter in y than x - starving the sagittal sampling in x, where there was room.
 *
 * <p>Measured coverage is 82-95% and <b>identical in x and y</b> at every field on both
 * lenses: the pattern is mapped into the pupil before contraction, so it is already
 * elliptical and an isotropic contraction preserves the fraction covered. The sagittal
 * direction is not starved.
 *
 * <p>What the table does show is that {@link VigType#Paraxial} reports
 * {@code vux = vlx = 0.000} at every field, on both lenses.
 */
public class ContrastProbe14 {

    public static void main(String[] args) throws Exception {
        report("Leica R APO 75/2 (double Gauss)", ContrastProbes.leicaInputPath());
        report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
    }

    private static void report(String label, String specfile) throws Exception {
        System.out.println("================================================================");
        System.out.println(label);
        System.out.println("================================================================");
        for (VigType vigType : new VigType[]{VigType.Paraxial, VigType.SetVig}) {
            var specs = new OpticalBenchDataImporter.LensSpecifications();
            specs.parse_file(specfile);
            Prescription prescription = Prescription.build_prescription(specs, true, false, false);
            OpticalModel model = new RayOpticsModelBuilder(prescription)
                    .build_optical_model(true, new double[]{0.0, 0.3, 0.7, 1.0},
                            false, vigType, true, 0);

            System.out.println("\n--- VigType." + vigType + " ---");
            System.out.println("fld |   vux    vlx    vuy    vly |  pupil x   pupil y "
                    + "| sampled x  sampled y | x cover  y cover");

            var result = ContrastAnalysis.eval(model, new ContrastOptions(40)
                    .num_rings(6).num_spokes(12));
            var fields = model.optical_spec.fov.fields;
            for (int fi = 0; fi < fields.length; fi++) {
                var fld = fields[fi];
                // Half-extent of the vignetted pupil along each axis: apply_vignetting
                // maps the unit disk edge to these.
                double pupilX = 0.5 * (fld.apply_vignetting(new double[]{1.0, 0.0})[0]
                        - fld.apply_vignetting(new double[]{-1.0, 0.0})[0]);
                double pupilY = 0.5 * (fld.apply_vignetting(new double[]{0.0, 1.0})[1]
                        - fld.apply_vignetting(new double[]{0.0, -1.0})[1]);

                var samples = result.fields.get(fi).wavelengths().get(0).samples();
                double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
                double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                for (var s : samples) {
                    minX = Math.min(minX, s.pupil().x);
                    maxX = Math.max(maxX, s.pupil().x);
                    minY = Math.min(minY, s.pupil().y);
                    maxY = Math.max(maxY, s.pupil().y);
                }
                double sampledX = 0.5 * (maxX - minX);
                double sampledY = 0.5 * (maxY - minY);

                System.out.printf(
                        "%.1f | %.3f  %.3f  %.3f  %.3f |  %.4f    %.4f  |  %.4f    %.4f  |"
                                + "  %5.1f%%   %5.1f%%%n",
                        fld.y, fld.vux, fld.vlx, fld.vuy, fld.vly,
                        pupilX, pupilY, sampledX, sampledY,
                        100.0 * sampledX / pupilX, 100.0 * sampledY / pupilY);
            }
        }
        System.out.println();
    }
}
