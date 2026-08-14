package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

public class FindBestBackfocus {

    private static Prescription getPrescription(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
//        return Prescription.build_prescription(specs, true,
//                new double[]{587.5618,656.2725,546.074,486.1327,435.8343},
//                new double[]{1.0,0.475,0.98,0.49,0.15});
        return Prescription.build_prescription(specs, true);
    }

    public static void main(String[] args) throws Exception {
        int scenario = 0;
        var prescription = getPrescription(args[0]);
        var surface = Integer.parseInt(args[1]);
        var analysis = new Analysis(prescription, new double[]{0}, new int[]{10, 30, 50}, scenario);
        var f = new LMDerMeritFunction(analysis,
                new Var[]{
                        new VarThickness(prescription, surface, scenario)
                },
                new Goal[]{
                        new GoalGeoMTF(analysis, 1, 0, 10, 1.0, 1.0),
                        new GoalGeoMTF(analysis, 1, 1, 10, 1.0, 1.0),
                        new GoalGeoMTF(analysis, 1, 0, 30, 1.0, 1.0),
                        new GoalGeoMTF(analysis, 1, 1, 30, 1.0, 1.0),
                        new GoalGeoMTF(analysis, 1, 0, 50, 1.0, 1.0),
                        new GoalGeoMTF(analysis, 1, 1, 50, 1.0, 1.0),
                        prescription._focal_length_by_scenario != null
                            ? new GoalParax(analysis, ParaxHelper.Effective_focal_length, prescription._focal_length_by_scenario[scenario], 1.0)
                            : new GoalParax(analysis, ParaxHelper.Effective_focal_length, prescription._focal_length, 1.0),
                        prescription._f_number_by_scenario != null
                            ? new GoalParax(analysis, ParaxHelper.Fno, prescription._f_number_by_scenario[scenario], 1.0)
                            : new GoalParax(analysis, ParaxHelper.Fno, prescription._fno, 1.0),
                        new GoalRayAberration(analysis, 1, 0, 0, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 1, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 2, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 3, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 4, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 5, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 6, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 7, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 8, 587.5618, 0, 1),
                        new GoalRayAberration(analysis, 1, 0, 9, 587.5618, 0, 1),
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
                },
                false);
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
