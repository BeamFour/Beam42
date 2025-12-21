package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class NoctNikkor58OptimG {

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
                     new VarRadius(prescription, 0),
                     new VarAsphK(prescription,0),
                     new VarAsphCoeff(prescription,0,1,1E6),
                     new VarAsphCoeff(prescription,0,2,1E9),
                     new VarAsphCoeff(prescription,0,3,1E11),
                     new VarAsphCoeff(prescription,0,4,1E14),
                     new VarRadius(prescription,2),
                     new VarRadius(prescription,3),
                     new VarRadius(prescription,4),
                     new VarRadius(prescription,5),
                     new VarRadius(prescription,7),
                     new VarRadius(prescription,8),
                     new VarRadius(prescription,9),
                     new VarRadius(prescription,10),
                     new VarRadius(prescription,11),
                     new VarRadius(prescription,12),
                     new VarRadius(prescription,13),
                     //new VarThickness(prescription, 13)
                },
                new Goal[] {
                        new GoalSpotRMS(analysis, 1, 10.0, 7.0),
                        new GoalSpotRMS(analysis, 2, 12.0, 5.0),
                        new GoalSpotRMS(analysis, 3, 20.0, 2.0),
                        new GoalSpotRMS(analysis, 4, 30.0, 2.0),
                        new GoalSpotRMS(analysis, 5, 40.0, 2.0),
                        new GoalSpotRMS(analysis, 6, 50.0, 3.0),
                        new GoalSpotMaxRadius(analysis, 1, 30.0, 2.0),
                        new GoalSpotMaxRadius(analysis, 2, 35.0, 2.0),
                        new GoalSpotMaxRadius(analysis, 3, 40.0, 2.0),
                        new GoalSpotMaxRadius(analysis, 4, 80.0, 2.0),
                        new GoalSpotMaxRadius(analysis, 5, 120.0, 2.0),
                        new GoalSpotMaxRadius(analysis, 6, 200.0, 2.0),
                        new GoalRayAberration(analysis,6,0,0,587.5618, 0,1),
                        new GoalRayAberration(analysis,6,0,-1,587.5618, 0,1),
                        new GoalRayAberration(analysis,6,0,0,486.1327, 0,1),
                        new GoalRayAberration(analysis,6,0,-1,486.1327, 0,1),

                        // Field 1
//                        new GoalMTF(analysis,1,0,0,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,1,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,2,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,3,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,4,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,5,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,6,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,7,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,8,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,9,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,0,0,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,1,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,2,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,3,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,4,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,5,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,6,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,7,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,8,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,9,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,0,0,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,1,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,2,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,3,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,4,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,5,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,6,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,7,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,8,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,0,9,656.2725, 30, 0,1),
//
//                        new GoalMTF(analysis,1,1,0,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,1,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,2,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,3,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,4,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,5,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,6,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,7,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,8,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,9,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,1,1,0,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,1,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,2,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,3,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,4,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,5,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,6,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,7,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,8,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,9,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,1,1,0,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,1,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,2,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,3,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,4,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,5,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,6,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,7,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,8,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,1,1,9,656.2725, 30, 0,1),

                        // Field 2
//                        new GoalMTF(analysis,2,0,0,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,1,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,2,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,3,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,4,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,5,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,6,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,7,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,8,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,9,587.5618, 30, 0,1),
//                        new GoalMTF(analysis,2,0,0,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,1,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,2,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,3,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,4,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,5,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,6,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,7,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,8,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,9,486.1327, 30, 0,1),
//                        new GoalMTF(analysis,2,0,0,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,1,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,2,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,3,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,4,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,5,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,6,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,7,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,8,656.2725, 30, 0,1),
//                        new GoalMTF(analysis,2,0,9,656.2725, 30, 0,1),

                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,58.0, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Fno, 1.2, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 37.78, 1.0),
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
