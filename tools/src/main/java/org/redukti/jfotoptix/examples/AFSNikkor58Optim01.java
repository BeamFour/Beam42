package org.redukti.jfotoptix.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.LMLSolver;
import org.redukti.optim.*;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.spec.Prescription;

public class AFSNikkor58Optim01 {

   public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs,true);
   }
    public static void main(String[] args) throws Exception {
        var prescription = getPrescription(args[0]);
        var analysis = new Analysis(prescription, new double[]{0.1,0.3,0.5,0.7,1.0}).spotDensity(50);
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription,0),
                     new VarRadius(prescription,1),
                     new VarRadius(prescription,2),
                     new VarRadius(prescription,3),
                     new VarRadius(prescription,4),
                     new VarAsphK(prescription,4),
                     new VarAsphCoeff(prescription,4,0,1E6),
                     new VarAsphCoeff(prescription,4,1,1E9),
                     new VarAsphCoeff(prescription,4,2,1E11),
                     new VarAsphCoeff(prescription,4,3,1E14),
                     new VarAsphCoeff(prescription,4,4,1E17),
                     new VarAsphCoeff(prescription,4,5,1E20),
                     new VarRadius(prescription,5),
                     new VarRadius(prescription,6),
                     new VarRadius(prescription,8),
                     new VarRadius(prescription,9),
                     new VarRadius(prescription,10),
                     new VarRadius(prescription,11),
                     new VarRadius(prescription,12),
                     new VarRadius(prescription,13),
                     new VarRadius(prescription,14),
                     new VarAsphK(prescription,14),
                     new VarAsphCoeff(prescription,14,0,1E6),
                     new VarAsphCoeff(prescription,14,1,1E9),
                     new VarAsphCoeff(prescription,14,2,1E11),
                     new VarAsphCoeff(prescription,14,3,1E14),
                     new VarAsphCoeff(prescription,14,4,1E17),
                     new VarAsphCoeff(prescription,14,5,1E20),
                     //new VarThickness(prescription,16)
                },
                new Goal[] {
                      new GoalSpotRMS(analysis, 1, 10.0, 5.0),
                      new GoalSpotRMS(analysis, 2, 10.0, 2.0),
                      new GoalSpotRMS(analysis, 3, 20.0, 2.0),
                      new GoalSpotRMS(analysis, 4, 20.0, 2.0),
                      new GoalSpotRMS(analysis, 5, 30.0, 2.0),
                      new GoalSpotRMS(analysis, 6, 45.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 1, 30.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 2, 30.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 3, 60.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 4, 60.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 5, 90.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 6, 120.0, 2.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,58.29, 2.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Enp_dist, 28.745, 1.0),
                      //new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 38.7, 1.0)
                });
        var lm = f.getSolver();
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = lm.iLMiter();
        }
        System.out.println("Status = " + istatus);
        System.out.println(f.toString());
        System.out.println(prescription.toString());
    }
}
