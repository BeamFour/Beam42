package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.spec.Prescription;

import static org.redukti.optim.OptimizationBuilder.mtf;

public class GenericMtfOpt {

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
                .varyAllCurvatures()
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .rayAberrationGoals()
                .mtfGoals(
                        mtf(10,
                                new double[]{95, 91, 85, 80},
                                new double[]{95, 91, 85, 80}),
                        mtf(30,
                                new double[]{85, 80, 70, 60},
                                new double[]{85, 80, 70, 60}),
                        mtf(50,
                                new double[]{65, 64, 40, 40},
                                new double[]{65, 64, 40, 15}))
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        var prescription = getPrescription(args[0], weighted, dLineOnly);
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
