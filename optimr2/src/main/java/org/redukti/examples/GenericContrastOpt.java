package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.spec.Prescription;

import static org.redukti.optim.OptimizationBuilder.contrast;

public class GenericContrastOpt {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationBuilder.OptimizationSetup createContrastSetup(Prescription prescription, boolean weighted,
                                                                     boolean dLineOnly) {
        //double[] fieldWeights = {8.0, 4.0, 2.0, 1.0};
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0};
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                //.thicknessSurfaces(1,4,6,7,9,12,14)
                //.thicknessSurfaces(3,5,8,10,12,15,16,18)  Nikkor 200mm f2
                //.curvatureSurfaces(0)
                .includeExistingAspherics(true)
                .allCurvatureSurfaces()
                //.allThicknessSurfaces()
                // Everything is free, so hold the layout: without these the solver
                // collapses air spaces and pushes elements through the stop.
                .applyCurvatureConstraints()
                //.applyThicknessConstraints()
                .weighted(weighted)
                .dLineOnly(dLineOnly)
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
        var prescription = getPrescription(args[0], weighted, dLineOnly);
        //var setup = createSetup(prescription, weighted, dLineOnly);
        var setup = createContrastSetup(prescription, weighted, dLineOnly);
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
