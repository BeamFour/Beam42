package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.optim.*;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.spec.Prescription;

public class Noctilux50ChiefRay {

   private static Prescription getPrescription() {
        Prescription prescription = new Prescription(52.4,1.0,45.0,43.2,true)
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
        var analysis = new Analysis(prescription);
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRayDist(prescription,0,0,0.01),
                     new VarRayDist(prescription,1,0,0.01),
                     new VarAoV(prescription,0,0.1)
                },
                new Goal[] {
                     new GoalRayInterceptApertureStop(analysis, new Vector2(0,0), 1.0),
                     new GoalRayInterceptImage(analysis, new Vector2(0.0,15.14), 1.0),
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
        System.out.println("Field 0.7 AOV = "  + prescription.fullAngleOfViewDegrees(0.7));
    }

}
