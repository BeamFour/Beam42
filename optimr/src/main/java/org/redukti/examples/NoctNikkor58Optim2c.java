package org.redukti.examples;

import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class NoctNikkor58Optim2c {

    // Measured by DM - off 1001 tale 16

       private static Prescription getPrescription() {
        Prescription prescription = new Prescription(58.0,1.2,40.9,43.28,false)
                .surf(79.9975, 6.885, 50.4875, 1.795, 45.31,	"J-LASF017")
                .asph(SurfaceType.ASPH_EVEN, 0,new double[]{0.0,0.0,0.0,0.0,0.0})
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
        System.out.println(analysis.ray_aberrations.generateReport());
        System.out.println("Before:\n");
        System.out.println(f.toString());
        var istatus = lm.solve();
        System.out.println("Status = " + istatus);
        System.out.println("After:\n");
        System.out.println(f.toString());
        System.out.println(prescription.toString());
    }
}
