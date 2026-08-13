package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.GoalParax;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.optim.ParaxHelper;
import org.redukti.optim.VarAsphCoeff;
import org.redukti.spec.Prescription;

import static org.redukti.optim.OptimizationBuilder.mtf;

public class NoctNikkor58mm {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationBuilder.OptimizationSetup createSetup(Prescription prescription, boolean weighted,
                                                                     boolean dLineOnly) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .rayAberrationGoals()
                .spotRmsGoals(new double[]{15, 30, 50, 70})
                .mtfGoals(
                        mtf(10,
                                new double[]{60, 50, 40, 30},
                                new double[]{60, 50, 35, 25}),
                        mtf(30,
                                new double[]{40, 35, 30, 20},
                                new double[]{40, 35, 20, 10}),
                        mtf(50,
                                new double[]{30, 25, 20, 20},
                                new double[]{30, 25, 10, 10}))
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean weighted = true;
        boolean dLineOnly = false;
        String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/nikkor-58mm-f1.2/version5/Noct-Nikkor-58mmf1.2.txt");
        var prescription = getPrescription(specfile, weighted, dLineOnly);
        var setup = createSetup(prescription, weighted, dLineOnly);
        var analysis = setup.analysis();
        var meritFunction = setup.meritFunction(false);
        analysis.compute();
        var solver = meritFunction.getSolver();

        if (analysis._ray_aberrations != null) {
            System.out.println("Aberrations:\n");
            System.out.println(analysis._ray_aberrations.list_ray_fans());
        }
        System.out.println("Before:\n");
        System.out.println(meritFunction);
        var status = solver.solve();
        System.out.println("Status = " + status);
        System.out.println("After:\n");
        System.out.println(meritFunction);
        System.out.println(prescription);
    }
}
