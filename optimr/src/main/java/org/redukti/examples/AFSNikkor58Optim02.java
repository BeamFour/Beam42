package org.redukti.examples;

import org.redukti.spec.Prescription;
import org.redukti.optim.*;
import org.redukti.spec.SurfaceType;

public class AFSNikkor58Optim02 {

   private static Prescription getPrescription() {
        Prescription prescription = new Prescription(58.0216,1.45,41.72,43.2,false)
                .surf(52.8577, 6.0, 47.2, 1.74443, 49.53,	"N-LAK28")
                .asph( SurfaceType.ASPH_EVEN,-0.4279,new double[]{0.,1.10084E-07,6.21998E-10,4.25694E-13,0.0})
                .surf(229.3475,	0.1,	47.2)
                .surf(40.3738,	6.0,	39.6,1.755,	52.34,	"J-LASKH2")
                .surf( 354.9744, 1.5, 39.6, 1.48749, 70.31, "J-FK5")
                .surf( 42.4134,	4.1038, 35.1)
                .surf(290.8467, 1.5, 34.0, 1.68893, 31.16, "J-SF8")
                .surf(31.6359,6.0,33.0)
                .stop(6.0,	28.261)
                .surf(-30.7873,	1.7,		30.0,	1.72825,28.46,	"SF10")
                .surf(35.1427,	7.0,		33.5,	1.883,40.77,	"J-LASF08A")
                .surf(-131.1407,	0.1,		33.5)
                .surf(118.7661,	6.0, 33.8, 1.883, 40.77, "J-LASF08A")
                .surf(-44.2318,	1.5,		33.8,1.53172,	48.78,	"J-LLF6")
                .surf(44.2683,	6.0, 	33.8, 1.7443, 49.53, "N-LAK28")
                .surf(-77.2943, 38.7, 33.8)
                .asph(SurfaceType.ASPH_EVEN,13.1597,new double[]{0.,8.65514E-06,4.15940E-09,1.25812E-11,1.22728E-14})
                .build();
        return prescription;
    }

    public static void main(String[] args) {
        var prescription = getPrescription();
        var analysis = new Analysis(prescription, new double[]{0.3,0.7,1.0}, new int[]{20});
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRadius(prescription,0),
                     new VarAsphK(prescription,0),
                     new VarAsphCoeff(prescription,0,0,1E7),
                     new VarAsphCoeff(prescription,0,1,1E9),
                     new VarAsphCoeff(prescription,0,2,1E11),
                     //new VarAsphCoeff(prescription,0,3,1.0e-14),
                     new VarRadius(prescription,1),
                     new VarRadius(prescription,2),
                     new VarRadius(prescription,3),
                     new VarRadius(prescription,4),
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
                     new VarAsphCoeff(prescription,14,0,1E7),
                     new VarAsphCoeff(prescription,14,1,1E9),
                     new VarAsphCoeff(prescription,14,2,1E11),
                     new VarAsphCoeff(prescription,14,3,1E14),
                     //new VarThickness(prescription,14)
                },
                new Goal[] {
                      new GoalSpotRMS(analysis, 1, 10.0, 7.0),
                      new GoalSpotRMS(analysis, 2, 25.0, 2.0),
                      new GoalSpotRMS(analysis, 3, 30.0, 2.0),
                      new GoalSpotRMS(analysis, 4, 40.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 1, 40.0, 5.0),
                      new GoalSpotMaxRadius(analysis, 2, 80.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 3, 100.0, 2.0),
                      new GoalSpotMaxRadius(analysis, 4, 120.0, 2.0),
                      new GoalParax(analysis, ParaxHelper.Effective_focal_length,58.035, 3.0),
                      new GoalParax(analysis, ParaxHelper.Enp_dist, 29.4, 1.0),
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
