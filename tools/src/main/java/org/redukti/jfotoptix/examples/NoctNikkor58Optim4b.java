package org.redukti.jfotoptix.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.optim2.*;
import org.redukti.spec.Prescription;

public class NoctNikkor58Optim4b {

   public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs,true);
   }
    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        var analysis = new Analysis(prescription, new double[]{0.0,0.1,0.3,0.5,0.7,1.0});
        var f = new LMDerMeritFunction(analysis,
                //new MeritFunction(analysis,
                new Var[] {
                     new VarAsphK(prescription,0),
                     new VarAsphCoeff(prescription,0,0,1E6),
                     new VarAsphCoeff(prescription,0,1,1E9),
                     new VarAsphCoeff(prescription,0,2,1E11),
                     new VarAsphCoeff(prescription,0,3,1E14),
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
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,58.0, 2.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Enp_dist, 35.7894, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 38.7, 1.0)
                });
        var lm = f.getSolver();
        var istatus = lm.solve();
        System.out.println("Status = " + istatus);
        System.out.println(f.toString());
        System.out.println(prescription.toString());
    }
}
