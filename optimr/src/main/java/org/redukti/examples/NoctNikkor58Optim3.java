package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

public class NoctNikkor58Optim3 {

   public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs,true);
   }
    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        var analysis = new Analysis(prescription, new double[]{0.1,0.3,0.5,0.7,1.0}, new int[]{20});
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription,0),
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
                     new VarAsphK(prescription,0),
                     new VarAsphCoeff(prescription,0,0,1E6),
                     new VarAsphCoeff(prescription,0,1,1E9),
                     new VarAsphCoeff(prescription,0,2,1E11),
                     new VarAsphCoeff(prescription,0,3,1E14),
                     //new VarThickness(prescription,16)
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
                      new GoalParax(analysis, ParaxHelper.Effective_focal_length,58.0, 2.0),
                      new GoalParax(analysis, ParaxHelper.Enp_dist, 35.7894, 1.0),
                      //new GoalParax(analysis, ParaxHelper.Back_focal_length, 38.7, 1.0)
                });
        analysis.compute();
        var lm = f.getSolver();
        System.out.println("Aberrations:\n");
        System.out.println(analysis._ray_aberrations.list_ray_fans());
        System.out.println("Before:\n");
        System.out.println(f.toString());
        var istatus = lm.solve();
        System.out.println("Status = " + istatus);
        System.out.println("After:\n");
        System.out.println(f.toString());
        System.out.println(prescription.toString());
    }

}
