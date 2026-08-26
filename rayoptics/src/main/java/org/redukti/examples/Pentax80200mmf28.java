package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.ConfigurationReport;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import static org.redukti.optim.OptimizationBuilder.contrast;

public class Pentax80200mmf28 {

    static Prescription getPrescription(String specfile, boolean weighted,
                                        boolean dLineOnly) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    static OptimizationBuilder.OptimizationSetup createContrastSetup(Prescription prescription, boolean weighted,
                                                                     boolean dLineOnly) {
        double[] fields = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double[] sagittalWeights   = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        double[] tangentialWeights = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        boolean[] correctAstigmatism = {false, true, true, true, true, true, true, true, true, false, false};

        return OptimizationBuilder.builder(prescription)
                .fields(fields)
                .mtfFrequencies(50)
                .scenario(1)
//                .varyExistingAspherics()
//                .varyAllCurvatures()
//                .varyAllThicknesses()
                .varyThicknesses(7,14,19)
                //.applyCurvatureConstraints()
                //.applyThicknessConstraints()
                //.applyEdgeThicknessConstraints()
                .weighted(false)
                .dLineOnly(false)
                .contrastSampling(6, 12)
                .calibrateContrastFrequency(false)
                .centerContrastResiduals(false)
                .aimContrastAtExitPupil(false)
                .vignetting(VigType.SetVig)
                .freezeVignetting()
                .checkSpotApertures(false)
                .contrastGoals(
//                        contrast(10,
//                                sagittalWeights,
//                                tangentialWeights),
//                        contrast(30,
//                                sagittalWeights,
//                                tangentialWeights),
                        contrast(50,
                                sagittalWeights,
                                tangentialWeights))
                .contrastBalanceGoals(correctAstigmatism, 1.0)
                .build();
    }

    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        var prescription = getPrescription(ExampleFinder.geoPathToExample("Examples/jfotoptix/pentax-80-200mm-f2.8/US005572276_Example05P.txt"), weighted, dLineOnly);
        var setup = createContrastSetup(prescription, weighted, dLineOnly);
        // Only the zoom spaces carry a value per configuration; everything else is shared,
        // so measure every configuration before and after and check nothing regressed.
        var before = ConfigurationReport.capture(prescription,
                new double[]{0.0, 0.3, 0.5, 0.7, 1.0}, new int[]{10, 30, 50});
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

        var after = ConfigurationReport.capture(prescription,
                new double[]{0.0, 0.3, 0.5, 0.7, 1.0}, new int[]{10, 30, 50});
        System.out.println(ConfigurationReport.compare(before, after));
    }
}
