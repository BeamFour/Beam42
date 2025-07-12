package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.optim.*;
import org.redukti.jfotoptix.spec.Prescription;

public class ChiefRayFinder {
    public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs,0,true);
    }

    public static double findChiefRayAngle(String specFile, double y_intercept) throws Exception {
        var prescription = getPrescription(specFile);
        return findChiefRayAngle(prescription, y_intercept);
    }

    public static double findChiefRayAngle(Prescription prescription, double y_intercept) {
        var analysis = new Analysis(prescription);
        var f = new MeritFunction(analysis,
                new Var[] {
                     new VarRayDist(prescription,0,0,0.01),
                     new VarRayDist(prescription,1,0,0.01),
                     new VarAoV(prescription,0,0.1)
                },
                new Goal[] {
                     new GoalRayInterceptApertureStop(analysis, new Vector2(0,0), 1.0),
                     new GoalRayInterceptImage(analysis, new Vector2(0.0, y_intercept), 1.0),
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
        return prescription.varAoV;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: ChiefRayFinder <specfile> <y_intercept>");
            System.exit(1);
        }
        String specFile = args[0];
        double y_intercept = Double.parseDouble(args[1]);
        findChiefRayAngle(specFile,y_intercept);
    }
}