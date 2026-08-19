package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import static org.redukti.optim.OptimizationBuilder.contrast;
import static org.redukti.optim.OptimizationBuilder.mtf;

/**
 * The input lens design here was reverse engineered by ZhongFu (https://www.zhihu.com/people/hui-shou-xiang-lai-xiao-se-chu-64-74)
 * from the drawings in the following paper:
 *
 * Double Gauss lens design: a review of some classics
 *  Reginald P. Jonas and Michael D. Thorpe
 *  ELCAN Optical Technologies, 450 Leitz Rd, Midland, Ontario, Canada, L4R 5B8
 *
 * The original design was made by Walter Mandler in 1973.
 * This lens was only made for US military and not sold to the general public.
 * It used the famous Leitz ED glass designated 554666. Below excerpt is from
 * the paper.
 *
 * C341 a 75mm f/2.0 apochromatic R objective designed as one of a suite of lenses for
 * use in the US Navy High Resolution Small Format Camera System. This lens has 8 elements, and uses 2 glass
 * types in a unique material combination. Special short flint material KZFS4 is used as the negative element of
 * each doublet and a Leitz Wetzlar glass; 554666 for all other elements, resulting in a focal length shift over the
 * design waveband 400nm to 900nm of only ±0.03mm. Ray aberration plots in Figure 5d clearly show the level of
 * colour correction and the excellent correction in the field, especially in tangential orientation. Figure 6d shows
 * this lens has been substantially corrected for astigmatism, and has a maximum distortion less than 1%. MTF
 * at f/2.0 and f/5.6 is given in Figures 4g and 4h.
 *
 * The parameters for Leitz glass 554666 are not available, However, CORNING
 * B52-67 appears to be replica of this glass type with ne 1.55416 and ve 66.66.
 *
 * This extremely rare Elcan-R 75 mm f/2 (optical formula C-341) was developed by
 * Ernst Leitz Canada (ELCAN) in the early 1970s specifically for the US Navy’s
 * "High Resolution Small Format Camera System." Only approximately 20 units
 * were ever produced. Designed by Professor Dr. Walter Mandler, this lens is part
 * of a family of apochromatic R-mount optics engineered for exceptional colour
 * and infrared correction, delivering sharp, distortion-free images across a
 * broad spectral range.
 *
 * Over the past 20 years, only four examples of the Elcan-R 75mm f/2 have been
 * recorded at auctions, underscoring both its rarity and historical significance.
 * The offered example is in beautiful condition, with very good optics. It is
 * one of the rarest Leica lenses ever made.
 *
 * Source https://www.leitz-auction.com/en/Elcan-R-2-75mm-US-Navy/A02373
 */
public class LeicaApo75mmMandler {

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
                .fields(0.0, 0.3, 0.7, 0.95)
                .mtfFrequencies(10, 20, 40)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .applyThicknessConstraints()
                .gaussianQuadratureSampling(3, 6)
                .spotDeviationGoals(fieldWeights)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 39.38, 1.0))
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createSpotSizeSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .mtfFrequencies(10, 20, 40)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .applyThicknessConstraints()
                .spotRmsGoals(new double[]{0, 0, 0, 0},
                              fieldWeights)
                .gaussianQuadratureSampling(6, 12)
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 39.38, 1.0))
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createMtfSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] weights) {
        double[] fieldWeights = {1.0, 1.0, 2.0, 1.0, 1.0};
        return OptimizationBuilder.builder(prescription)
                //.fields(0.0, 0.2, 0.4, 0.75, 1.0)
                //.fields(0.0, 0.1, 0.4, 0.75, 1.0)
                .fields(0.0, 0.2, 0.4, 0.6, 1.0)
                .mtfFrequencies(10, 20, 40)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(true)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .applyThicknessConstraints()
                .applyEdgeThicknessConstraints()
                .mtfGoals(
                        mtf(10,
                                new double[]{90, 95, 91, 85, 82},
                                new double[]{90, 95, 91, 85, 75},
                                fieldWeights),
                        mtf(20,
                                new double[]{80, 80, 80, 70, 62},
                                new double[]{80, 80, 80, 70, 58},
                                fieldWeights),
                        mtf(40,
                                new double[]{60, 60, 64, 40, 42},
                                new double[]{60, 60, 64, 40, 15},
                                fieldWeights))
                .gaussianQuadratureSampling(6, 12)
                .vignetting(VigType.SetVig)
                .freezeVignetting()
                .checkSpotApertures(false)
                //.additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 39.38, 1.0))
                .build();
    }


    static OptimizationBuilder.OptimizationSetup createContrastSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] wights) {
        double[] fields = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        double[] sagittalWeights   = {8.0, 8.0, 8.0, 8.0, 8.0, 8.0, 4.0, 2.0, 1.0, 0.5, 0.1};
        double[] tangentialWeights = {4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 1.0, 1.0, 0.5, 0.1, 0.1};
        boolean[] correctAstigmatism = {true, true, true, true, true, true, true, true, true, true, false};
        return OptimizationBuilder.builder(prescription)
                .fields(fields)
                .mtfFrequencies(10, 20, 40)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(false)
                .dLineOnly(dLineOnly)
                .applyCurvatureConstraints()
                .applyThicknessConstraints(5.0)
                .contrastSampling(6, 12)
                .contrastGoals(
                        contrast(10,
                                sagittalWeights,
                                tangentialWeights),
                        contrast(20,
                                sagittalWeights,
                                tangentialWeights),
                        contrast(40,
                                sagittalWeights,
                                tangentialWeights))
                .calibrateContrastFrequency(false)
                .centerContrastResiduals(false)
                .aimContrastAtExitPupil(false)
                //.additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 39.38, 1.0))
                .vignetting(VigType.SetVig)
                .freezeVignetting()
                .checkSpotApertures(false)
                .applyEdgeThicknessConstraints()
                .contrastBalanceGoals(correctAstigmatism, 4.0)
                .build();
    }

    static OptimizationBuilder.OptimizationSetup createRayAberrationSetup(Prescription prescription,
                                                                     boolean weighted,
                                                                     boolean dLineOnly,
                                                                     double[] fieldWeights) {
        return OptimizationBuilder.builder(prescription)
                .fields(0.0, 0.3, 0.7, 1.0)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(weighted)
                .dLineOnly(dLineOnly)
                .mtfFrequencies(10, 20, 40)
                .rayAberrationGoals()
                .applyCurvatureConstraints()
                .applyThicknessConstraints()
                .additionalGoals(analysis -> new GoalParax(analysis, ParaxHelper.Back_focal_length, 39.38, 1.0))
                .build();
    }


    public static void main(String[] args) throws Exception {
        boolean weighted = false;
        boolean dLineOnly = false;
        double[] fieldWeights = {1.0, 1.0, 1.0, 1.0};
        String specfile = ExampleFinder.geoPathToExample("Examples/jfotoptix/leica-r-apo-75mm-f2-mandler/specs-original.txt");
        var prescription = getPrescription(specfile, weighted, dLineOnly);
        var setup = createContrastSetup(prescription, weighted, dLineOnly, fieldWeights);
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
