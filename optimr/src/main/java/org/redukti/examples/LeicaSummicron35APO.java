package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class LeicaSummicron35APO {

    // Measured by DM - off 1001 tale 16

    private static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs, true);
    }

    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        prescription._generate_d_line_only = true;
        var analysis = new Analysis(prescription, new double[]{0.0,0.25,0.75,1.0});
        var f = new MeritFunction(analysis,
                new Var[] {
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,11,1,1E6),
                        new VarAsphCoeff(prescription,11,2,1E9),
                        new VarAsphCoeff(prescription,11,3,1E11),
                        new VarAsphCoeff(prescription,14,1,1E6),
                        new VarAsphCoeff(prescription,14,2,1E9),
                        new VarAsphCoeff(prescription,14,3,1E11),
                        new VarAsphCoeff(prescription,14,4,1E15),
                        new VarAsphCoeff(prescription,14,5,1E18),
                        new VarAsphCoeff(prescription,15,1,1E6),
                        new VarAsphCoeff(prescription,15,2,1E9),
                        new VarAsphCoeff(prescription,15,3,1E11),

                        //                     new VarRadius(prescription, 6),
                     //new VarAsphK(prescription,6),
                     //new VarAsphCoeff(prescription,6,1,1E6),
                     //new VarAsphCoeff(prescription,6,2,1E9),
                     //new VarAsphCoeff(prescription,6,3,1E11),
                     //new VarAsphCoeff(prescription,6,4,1E14),
//                        new VarAsphCoeff(prescription,6,5,1E16),
//                        new VarAsphCoeff(prescription,6,6,1E19),
//                     new VarRadius(prescription, 7),
//                     new VarRadius(prescription, 0),
//                     new VarRadius(prescription, 1),
//                     new VarRadius(prescription, 2),
//                    new VarRadius(prescription, 3),
//                    new VarRadius(prescription, 4),
//                    new VarRadius(prescription, 8),
//                    new VarRadius(prescription, 9),
//                    new VarRadius(prescription, 10),
//                    new VarRadius(prescription, 11),
//                    new VarRadius(prescription, 12),
//                    new VarRadius(prescription, 13),
//                     new VarThickness(prescription, 13)
//                     new VarThickness(prescription, 13)
                },
                new Goal[] {
                        new GoalSpotRMS(analysis, 1, 4.0, 3.0),
                        new GoalSpotRMS(analysis, 2, 4.0, 1.0),
                        new GoalSpotRMS(analysis, 3, 5.0, 1.0),
                        new GoalSpotRMS(analysis, 4, 6.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 1, 12.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 2, 13.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 3, 15.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 4, 20.0, 1.0),

                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,34.79, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Fno, 2.0, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 14.42, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Pp1, 51.8, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Ppk, 20.2, 1.0)
                });
        analysis.compute();
        var lm = f.getSolver();
        System.out.println("Aberrations:\n");
        System.out.println(analysis.ray_aberrations.list_ray_fans());
        System.out.println("Before:\n");
        System.out.println(f.toString());
        var istatus = lm.solve();
        System.out.println("Status = " + istatus);
        System.out.println("After:\n");
        System.out.println(f.toString());
        System.out.println(prescription.toString());
    }
}
