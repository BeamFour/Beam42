package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

public class TestOptim {

    /**
     * Prescript based off measurements from Nikkor tale
     */
    private static Prescription getPrescription() {
        Prescription prescription = new Prescription(58.0,1.2,40.9,43.28,false)
                .surf(84.12, 6.885, 50.4875, 1.795, 45.31,	"J-LASF017")
                .asph(SurfaceType.ASPH_EVEN, 3.423989,new double[]{0.0,-4.9391634E-7,-1.11125335E-9,1.4162813E-12,-8.428294E-16})
                .surf(0,	0.1,	50.4875)
                .surf(34.2214,	9.75,	44.832,1.8485,	43.79,	"J-LASFH22")
                .surf( 78.0494, 1.56, 44.832)
                .surf( 147.7,	2.87, 42.169,	1.74,	28.3,	"S-TIH3")
                .surf(22.877, 8.44, 32.12841)
                .stop(7.95,	31.227)
                .surf(-22.677,	1.64,		31.445,	1.74077,27.79,	"S-TIH13")
                .surf(302.631,	8.196,		40.2,	1.788,47.37,	"TAF4")
                .surf( -35.901,	0.15, 40.2)
                .surf(-396.965,	6.147,		39.5,1.7725,	46.62,	"J-LASF016")
                .surf(-54.436,	0.0, 39.5)
                .surf(227.228,	4.016,		38.275,1.795,	45.31,	"J-LASF017")
                .surf(-96.781,	37.772, 	38.275)
                .build();
        return prescription;
    }

    @Test
    public void testNikkor58mmNoct() {
        var prescription = getPrescription();
        var analysis = new Analysis(prescription, new double[]{0.0,0.25,0.75,1.0}, new int[] {10,20});
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
                        new GoalSpotRMS(analysis, 1, 15.0, 1.0),
                        new GoalSpotRMS(analysis, 2, 25.0, 1.0),
                        new GoalSpotRMS(analysis, 3, 40.0, 1.0),
                        new GoalSpotRMS(analysis, 4, 50.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 1, 30.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 2, 50.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 3, 80.0, 1.0),
                        new GoalSpotMaxRadius(analysis, 4, 150.0, 1.0),
                        new GeoMTF(analysis,1,0,10,0.72,1.0),
                        new GeoMTF(analysis,1,1,10,0.72,1.0),
                        new GeoMTF(analysis,2,0,10,0.70,1.0),
                        new GeoMTF(analysis,2,1,10,0.70,1.0),
                        new GeoMTF(analysis,3,0,10,0.50,1.0),
                        new GeoMTF(analysis,3,1,10,0.62,1.0),
                        new GeoMTF(analysis,4,0,10,0.42,1.0),
                        new GeoMTF(analysis,4,1,10,0.38,1.0),
                        new GeoMTF(analysis,1,0,20,0.54,1.0),
                        new GeoMTF(analysis,1,1,20,0.54,1.0),
                        new GeoMTF(analysis,2,0,20,0.40,1.0),
                        new GeoMTF(analysis,2,1,20,0.45,1.0),
                        new GeoMTF(analysis,3,0,20,0.16,1.0),
                        new GeoMTF(analysis,3,1,20,0.22,1.0),
                        new GeoMTF(analysis,4,0,20,0.22,1.0),
                        new GeoMTF(analysis,4,1,20,0.15,1.0),
                        new GoalParax(analysis, ParaxialFirstOrderInfo.Effective_focal_length,58.0, 1.0),
                        new GoalParax(analysis, ParaxialFirstOrderInfo.Fno, 1.2, 1.0),
                        new GoalParax(analysis, ParaxialFirstOrderInfo.Back_focal_length, 37.78, 1.0),
                        new GoalRayAberration(analysis,1,0,0,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,1,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,2,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,3,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,4,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,5,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,6,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,7,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,8,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,9,587.5618, 0,1),
                        new GoalRayAberration(analysis,1,0,0,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,1,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,2,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,3,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,4,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,5,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,6,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,7,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,8,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,9,486.1327, 0,1),
                        new GoalRayAberration(analysis,1,0,0,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,1,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,2,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,3,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,4,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,5,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,6,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,7,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,8,656.2725, 0,1),
                        new GoalRayAberration(analysis,1,0,9,656.2725, 0,1),

                        new GoalRayAberration(analysis,2,0,0,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,1,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,2,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,3,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,4,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,5,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,6,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,7,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,8,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,9,587.5618, 0,1),
                        new GoalRayAberration(analysis,2,0,0,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,1,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,2,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,3,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,4,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,5,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,6,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,7,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,8,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,9,486.1327, 0,1),
                        new GoalRayAberration(analysis,2,0,0,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,1,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,2,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,3,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,4,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,5,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,6,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,7,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,8,656.2725, 0,1),
                        new GoalRayAberration(analysis,2,0,9,656.2725, 0,1),

                        new GoalRayAberration(analysis,3,0,0,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,1,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,2,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,3,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,4,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,5,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,6,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,7,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,8,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,9,587.5618, 0,1),
                        new GoalRayAberration(analysis,3,0,0,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,1,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,2,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,3,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,4,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,5,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,6,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,7,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,8,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,9,486.1327, 0,1),
                        new GoalRayAberration(analysis,3,0,0,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,1,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,2,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,3,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,4,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,5,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,6,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,7,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,8,656.2725, 0,1),
                        new GoalRayAberration(analysis,3,0,9,656.2725, 0,1),

                        new GoalRayAberration(analysis,4,0,0,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,1,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,2,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,3,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,4,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,5,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,6,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,7,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,8,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,9,587.5618, 0,1),
                        new GoalRayAberration(analysis,4,0,0,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,1,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,2,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,3,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,4,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,5,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,6,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,7,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,8,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,9,486.1327, 0,1),
                        new GoalRayAberration(analysis,4,0,0,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,1,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,2,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,3,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,4,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,5,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,6,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,7,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,8,656.2725, 0,1),
                        new GoalRayAberration(analysis,4,0,9,656.2725, 0,1),
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
