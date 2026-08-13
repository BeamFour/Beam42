package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.optim.OptimizationBuilder.OptimizationSetup;
import org.redukti.spec.Prescription;

import static org.redukti.optim.OptimizationBuilder.mtf;
import static org.redukti.optim.OptimizationBuilder.contrast;

public class ZeissOtusML50mm {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationSetup createSetup(Prescription prescription, boolean weighted,
                                         boolean dLineOnly) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 20, 40)
                .curvatureSurfaces(
                        0, 1, 2, 3, 4, 5, 6, 8, 9,
                        11, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23)
                .thicknessSurfaces(25)
                .includeExistingAspherics(true)
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .rayAberrationGoals()
                .mtfGoals(
                        mtf(10,
                                new double[]{93, 93, 94, 93},
                                new double[]{93, 93, 90, 82}),
                        mtf(20,
                                new double[]{85, 85, 85, 80},
                                new double[]{85, 85, 78, 62}),
                        mtf(40,
                                new double[]{65, 65, 64, 58},
                                new double[]{65, 62, 45, 38}))
                .build();
    }

    static OptimizationSetup createContrastSetup(Prescription prescription, boolean weighted,
                                                  boolean dLineOnly) {
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0};
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .curvatureSurfaces(
                        0, 1, 2, 3, 4, 5, 6, 8, 9,
                        11, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23)
                .thicknessSurfaces(25)
                .includeExistingAspherics(true)
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                // Retain the historical Otus merit explicitly. Ray fans are
                // optional in OptimizationBuilder because dense contrast goals
                // do not generally need them.
                .rayAberrationGoals()
                .contrastSampling(6, 12)
                .calibrateContrastFrequency(true)
                .contrastGoals(
                        contrast(10, fieldWeights),
                        contrast(30, fieldWeights),
                        contrast(50, fieldWeights))
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt");
        var prescription = getPrescription(specfile, weighted, dLineOnly);
        //var setup = createSetup(prescription, weighted, dLineOnly);
        var setup = createContrastSetup(prescription, weighted, dLineOnly);
        var analysis = setup.analysis();
        var meritFunction = setup.meritFunction(false);
        analysis.compute();
        var solver = meritFunction.getSolver();

        System.out.println("Aberrations:\n");
        System.out.println(analysis._ray_aberrations.list_ray_fans());
        System.out.println("Before:\n");
        System.out.println(meritFunction);
        long started = System.nanoTime();
        var status = solver.solve();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        System.out.println("Status = " + status);
        System.out.println("After:\n");
        System.out.println(meritFunction);
        System.out.println(prescription);
        System.out.println("Time taken " + elapsedMillis + " ms");
    }
}
