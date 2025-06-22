package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.optim.*;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class NoctNikkor58Optim2 {

    // Measured by DM - off 1001 tale 16

       private static Prescription getPrescription() {
        Prescription prescription = new Prescription(58.0,1.2,40.9,43.28,false)
                .surf(79.9975, 6.885, 50.4875, 1.795, 45.31,	"J-LASF017")
                //.asph(0,new double[]{0.0,0.0,0.0,0.0})
                .surf(0,	0.1,	50.4875)
                .surf(33.737,	9.75,	44.832,1.8485,	43.79,	"J-LASFH22")
                .surf( 70.18675, 1.56, 44.832)
                .surf( 134.505,	2.87, 42.169,	1.74,	28.3,	"S-TIH3")
                .surf(22.3687, 8.44, 32.12841)
                .stop(7.95,	31.227)
                .surf(-23.02418,	1.64,		31.445,	1.74077,27.79,	"S-TIH13")
                .surf(306.553,	8.196,		40.2,	1.788,47.37,	"TAF4")
                .surf( -37.555,	0.15, 40.2)
                .surf(-396.94,	6.147,		39.5,1.7725,	46.62,	"J-LASF016")
                .surf(-52.56789,	0.0, 39.5)
                .surf(223.8426,	4.016,		38.275,1.795,	45.31,	"J-LASF017")
                .surf(-94.08052,	37.78, 	38.275)
                .build();
        return prescription;
    }

    public static void main(String[] args) {
        var prescription = getPrescription();
        var analysis = new Analysis(prescription);
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription, 0),
                     //new VarAsphCoeff(prescription,0,0),
                     //new VarAsphCoeff(prescription,0,1),
                     //new VarAsphCoeff(prescription,0,2),
                     //new VarAsphCoeff(prescription,0,3),
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
                     new VarThickness(prescription, 13)
                },
                new Goal[] {
                      new GoalSpotRMS(analysis, 1, 13.0, 5.0),
                      new GoalSpotRMS(analysis, 2, 20.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 1, 25.0, 5.0),
                      new GoalSpotMaxRadius(analysis, 2, 50.0, 2.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,58.0, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Fno, 1.2, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 37.78, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Pp1, 51.8, 1.0),
                      new GoalParax(analysis, ParaxialFirstOrderInfo.Ppk, 20.2, 1.0)
                });
        var lm = f.getSolver();
        int istatus = 0;
        int iterCount = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = lm.iLMiter();
            iterCount++;
        }
        System.out.println(f.toString());
    }
}
