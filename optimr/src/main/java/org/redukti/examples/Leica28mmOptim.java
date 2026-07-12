package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.LMLSolver;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.spec.Prescription;
import org.redukti.optim.*;

public class Leica28mmOptim {

   public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs,true);
   }
    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        var analysis = new Analysis(prescription, new double[]{0.3,0.7,1.0}, new int[]{20});
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription,0),
                     new VarRadius(prescription,1),
                     new VarRadius(prescription,2),
                     new VarRadius(prescription,3),
                     new VarRadius(prescription,4),
                     new VarRadius(prescription,5),
                     new VarRadius(prescription,6),
                     new VarRadius(prescription,7),
                     new VarRadius(prescription,8),
                     new VarRadius(prescription,10),
                     new VarRadius(prescription,11),
                     new VarRadius(prescription,12),
                     new VarRadius(prescription,13),
                     new VarRadius(prescription,14),
                     new VarRadius(prescription,15),
                     new VarRadius(prescription,16),
                     new VarRadius(prescription,17),
                     //new VarAsphK(prescription,16),
                     new VarAsphCoeff(prescription,16,0,1E5),
                     new VarAsphCoeff(prescription,16,1,1E8),
                     new VarAsphCoeff(prescription,16,2,1E10),
                     new VarAsphCoeff(prescription,16,3,1E12),
                     //new VarThickness(prescription,16)
                },
                new Goal[] {
                      new GoalSpotRMS(analysis, 1, 5.0, 5.0),
                      new GoalSpotRMS(analysis, 2, 9.0, 2.0),
                      new GoalSpotRMS(analysis, 3, 15.0, 2.0),
                      new GoalSpotRMS(analysis, 4, 30.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 1, 12.0, 3.0),
                      new GoalSpotMaxRadius(analysis, 2, 30.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 3, 90.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 4, 120.0, 2.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,28.32, 2.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Enp_dist, 28.745, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 38.1031, 1.0)
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
