package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.optim.*;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;

public class Noctilux50Optim2 {

   private static Prescription getPrescription() {
        Prescription prescription = new Prescription(50.2,1.0,47.0,43.2,false)
                .surf(60.93448034183235, 8.071, 54.57, 1.6779, 55.2,	"N-LAK12")
                .surf(1756.423894554349,	0.1,	54.57)
                .surf(30.17091963464339,	8.0,	46.571,1.883,	40.8,	"S-LAH58")
                .surf( 68.8969841585084, 1.7857, 44.644)
                .surf( 121.40569225318634,	4.0714, 45.214,	1.7847,	26.08,	"SF56A")
                .surf(19.554269954219002, 9.35, 31.6)
                .stop(7.1,	30.6)
                .surf(-23.83193616804,	1.357,		31.0,	1.72825,28.41,	"SF10")
                .surf(91.8777392221589,	8.7143,		37.643,	1.883,40.8,	"S-LAH58")
                .surf( -32.0992621547598,	0.1, 37.714)
                .surf(92.56034743956,	4.0,		35.286,1.788,	47.49,	"N-LAF21")
                .surf(549.3168895825511,	0.1, 35.286)
                .surf(83.0795202171,	4,		33.429,1.788,	47.49,	"N-LAF21")
                .surf(-197.873443,	27.365, 	33.429)
                .build();
        return prescription;
    }

    public static void main(String[] args) {
        var prescription = getPrescription();
        var f = new MeritFunction(prescription,
                new In[] {
                     new VarRadius(prescription, 0),
                     new VarRadius(prescription,1),
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
                     new VarThickness(prescription,13)
                },
                new Out[] {
                      new SpotRMS(prescription, 1, 13.0, 5.0),
                      new SpotRMS(prescription, 2, 20.0, 2.0),
                      new SpotMaxRadius(prescription, 1, 25.0, 5.0),
                      new SpotMaxRadius(prescription, 2, 50.0, 2.0),
                      new Parax(prescription, ParaxialFirstOrderInfo.Effective_focal_length,52.4, 1.0),
                      new Parax(prescription, ParaxialFirstOrderInfo.Enp_dist, 42.9, 1.0)
                });
        var lm = f.getSolver();
        int istatus = 0;
        while (istatus!= LMLSolver.BADITER &&
                istatus!= LMLSolver.LEVELITER &&
                istatus!= LMLSolver.MAXITER) {
            istatus = lm.iLMiter();
        }
        System.out.println(f.toString());
    }

}
