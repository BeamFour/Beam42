package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

import static org.redukti.optim.OptimizationBuilder.contrast;
import static org.redukti.optim.OptimizationBuilder.mtf;

public class NoctNikkor58mm {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationBuilder.OptimizationSetup createSpotDeviationSetup(Prescription prescription,
                                                             boolean weighted,
                                                             boolean dLineOnly,
                                                             double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphK(prescription, 0),
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .gaussianQuadratureSampling(3, 6)
                .spotRmsRayGoals(fieldWeights)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createSpotSizeSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphK(prescription, 0),
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .spotRmsGoals(new double[]{15, 30, 50, 70},
                              fieldWeights)
                .gaussianQuadratureSampling(6, 12)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createMtfSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphK(prescription, 0),
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .mtfGoals(
                        mtf(10,
                                new double[]{60, 50, 40, 30},
                                new double[]{60, 50, 35, 25},
                                fieldWeights),
                        mtf(30,
                                new double[]{40, 35, 30, 20},
                                new double[]{40, 35, 20, 10},
                                fieldWeights),
                        mtf(50,
                                new double[]{30, 25, 20, 20},
                                new double[]{30, 25, 10, 10},
                                fieldWeights))
                .gaussianQuadratureSampling(6, 12)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }


    static OptimizationBuilder.OptimizationSetup createContrastSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphK(prescription, 0),
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .contrastSampling(6, 12)
                .contrastGoals(
                        contrast(10, fieldWeights),
                        contrast(30, fieldWeights),
                        contrast(50, fieldWeights))
                .calibrateContrastFrequency(true)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createRayAberrationSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 30, 50)
                .allCurvatureSurfaces()
                .additionalVariables(
                        new VarAsphK(prescription, 0),
                        new VarAsphCoeff(prescription,0,1,1E6),
                        new VarAsphCoeff(prescription,0,2,1E9),
                        new VarAsphCoeff(prescription,0,3,1E11),
                        new VarAsphCoeff(prescription,0,4,1E14))
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .rayAberrationGoals()
                .applyCurvatureConstraints()
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 37.78, 1.0))
                .build();
    }


    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0};
        String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/nikkor-58mm-f1.2/version5/Noct-Nikkor-58mmf1.2.txt");
        //String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/nikkor-58mm-f1.2/version19/specs.txt");
        var prescription = getPrescription(specfile, weighted, dLineOnly);
        var setup = createSpotDeviationSetup(prescription, weighted, dLineOnly, fieldWeights);
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
