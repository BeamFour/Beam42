package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class LeicaSummilux50ASPH {

    // Measured by DM - off 1001 tale 16

    private static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs, true);
    }

    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        var analysis = new Analysis(prescription, new double[]{0.0,0.1,0.3,0.5,0.7,1.0});
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription, 6),
                     new VarAsphK(prescription,6),
                     new VarAsphCoeff(prescription,6,1,1E6),
                     new VarAsphCoeff(prescription,6,2,1E9),
                     new VarAsphCoeff(prescription,6,3,1E11),
                     new VarAsphCoeff(prescription,6,4,1E14),
                        new VarAsphCoeff(prescription,6,5,1E16),
                        new VarAsphCoeff(prescription,6,6,1E19),
                     new VarRadius(prescription, 7),
                     new VarRadius(prescription, 0),
                     new VarRadius(prescription, 1),
                     new VarRadius(prescription, 2),
                    new VarRadius(prescription, 3),
                    new VarRadius(prescription, 4),
                    new VarRadius(prescription, 8),
                    new VarRadius(prescription, 9),
                    new VarRadius(prescription, 10),
                    new VarRadius(prescription, 11),
                    new VarRadius(prescription, 12),
                    new VarRadius(prescription, 13),
//                     new VarThickness(prescription, 13)
                     //new VarThickness(prescription, 13)
                },
                new Goal[] {
                        new GoalSpotRMS(analysis, 1, 40.0, 3.0),
                        new GoalSpotRMS(analysis, 2, 40.0, 1.0),
                        new GoalSpotRMS(analysis, 3, 50.0, 1.0),
                        new GoalSpotRMS(analysis, 4, 60.0, 1.0),
                        new GoalSpotRMS(analysis, 5, 70.0, 1.0),
                        new GoalSpotRMS(analysis, 6, 200.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 1, 60.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 2, 80.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 3, 100.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 4, 120.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 5, 140.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 6, 500.0, 1.0),

                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,52.0, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Fno, 1.4, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 26.256, 1.0),
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
