package org.redukti.tools;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.spec.Prescription;

public class ChiefRayFinder {

    public static class Results {
        public final double aov;
        public final Vector2 xy;
        public final Vector3 origin;
        public final Vector3 intercept;

        public Results(double aov, Vector2 xy, Vector3 origin, Vector3 intercept) {
            this.aov = aov;
            this.xy = xy;
            this.origin = origin;
            this.intercept = intercept;
        }
        @Override
        public String toString() {
            return "Results{" +
                    "aov=" + aov +
                    ", xy=" + xy +
                    ", origin=" + origin +
                    ", intercept=" + intercept +
                    '}';
        }
    }

    public static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.buildPrescription(specs,true);
    }

    public static Results findChiefRayAngle(String specFile, double y_intercept) throws Exception {
        var prescription = getPrescription(specFile);
        return findChiefRayAngle(prescription, y_intercept);
    }

    public static Results findChiefRayAngle(Prescription prescription, double y_intercept) {
//        var analysis = new Analysis(prescription);
//        var f = new MeritFunction(analysis,
//                new Var[] {
//                     new VarRayDist(prescription,0,0,0.01),
//                     new VarRayDist(prescription,1,0,0.01),
//                     new VarAoV(prescription,0,0.1)
//                },
//                new Goal[] {
//                     new GoalRayInterceptApertureStop(analysis, new Vector2(0,0), 1.0),
//                     new GoalRayInterceptImage(analysis, new Vector2(0.0, y_intercept), 1.0),
//                });
//        var lm = f.getSolver();
//        int istatus = 0;
//        while (istatus!= LMLSolver.BADITER &&
//                istatus!= LMLSolver.LEVELITER &&
//                istatus!= LMLSolver.MAXITER) {
//            istatus = lm.iLMiter();
//        }
//        System.out.println("Status = " + istatus);
//        System.out.println(f.toString());
//        if (istatus == LMLSolver.LEVELITER) {
//            // Get the ray from the point source, this is the first element
//            var seq = analysis.systems[0].get_sequence();
//            var tracedRay = analysis.singleRayTraceResults.get_generated(seq.get(0)).get(0);
//            // Origin is the start of the ray
//            var origin = tracedRay.get_position();
//            // Intercept is where it met the first optical surface
//            var intercept = tracedRay.get_intercept_point();
//            // Angle of view
//            var aov = prescription._var_angle_of_view;
//            var xy = prescription._distribution.get_user_defined_points().get(0);
//            return new Results(aov,xy,origin,intercept);
//        }
        throw new RuntimeException("Failed to find chief ray angle");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: ChiefRayFinder <specfile> <y_intercept>");
            System.exit(1);
        }
        String specFile = args[0];
        double y_intercept = Double.parseDouble(args[1]);
        System.out.println(findChiefRayAngle(specFile,y_intercept));
    }
}