package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.GoalParax;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.optim.ParaxHelper;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import static org.redukti.optim.OptimizationBuilder.contrast;

public class Nikkor28mmf14D {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationBuilder.OptimizationSetup createContrastSetup(Prescription prescription, boolean weighted,
                                                                     boolean dLineOnly) {
        //double[] fieldWeights = {8.0, 4.0, 2.0, 1.0};
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
                .mtfFrequencies(10, 30, 50)
                .varyExistingAspherics()
                .varyAllCurvatures()
                .varyAllThicknesses()
                .applyCurvatureConstraints()
                .applyThicknessConstraints()
                .applyEdgeThicknessConstraints()
                .weighted(true)
                .dLineOnly(dLineOnly)
                .contrastSampling(6, 12)
                .calibrateContrastFrequency(true)
                .vignetting(VigType.SetVig)
                .freezeVignetting()
                .checkSpotApertures(false)
                .contrastGoals(
                        contrast(10, fieldWeights),
                        contrast(30, fieldWeights),
                        contrast(50, fieldWeights))
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 38.1031, 1.0))
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/nikkor-28mm-f1.4d/US005315441_Example01_a.txt");
        var prescription = getPrescription(specfile, weighted, dLineOnly);
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
