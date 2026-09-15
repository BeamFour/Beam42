package org.redukti.optim;

import java.util.*;
import org.redukti.spec.VigType;
import org.redukti.optim.OptimizationBuilder.*;
import static org.redukti.optim.OptimizationBuilder.NOMINAL_BALANCE_WEIGHT;

/**
 * Serializable optimization settings, independent of a prescription or solver.
 * Each builder owns its settings; trial definitions keep a private copy and hand
 * out fresh copies so changing one stage cannot change another stage's settings.
 */
final class OptimizationConfiguration {
    static final int DEFAULT_HEXAPOLAR_RAYS = 64;
    static final int DEFAULT_GAUSSIAN_QUADRATURE_RINGS = 14;
    static final int DEFAULT_GAUSSIAN_QUADRATURE_SPOKES = 20;
    static final int DEFAULT_CONTRAST_RINGS = 6;
    static final int DEFAULT_CONTRAST_SPOKES = 12;

    double[] fields;
    int[] mtfFrequencies;
    int[] curvatureSurfaces = new int[0];
    boolean allCurvatureSurfaces;
    int[] thicknessSurfaces = new int[0];
    boolean allThicknessSurfaces;
    boolean includeExistingAspherics;
    boolean weighted = true;
    boolean dLineOnly;
    boolean addRayAberrationGoals;
    boolean useHexapolarSpotPattern;
    int hexapolarSpotRays = DEFAULT_HEXAPOLAR_RAYS;
    int gaussianQuadratureRings = DEFAULT_GAUSSIAN_QUADRATURE_RINGS;
    int gaussianQuadratureSpokes = DEFAULT_GAUSSIAN_QUADRATURE_SPOKES;
    double gaussianQuadratureInnerRadius = 0.0;
    boolean checkSpotApertures = true;
    double[] spotDeviationXWeights;
    double[] spotDeviationYWeights;
    boolean addSpotDeviationGoals;
    // 3x6 is enough to measure a fixed design but not to optimize against: the
    // solver drives the 18 sampled points further than the wavefront between
    // them, so the merit reads better than the lens is. 6x12 is converged - 8x16
    // reproduces it - and 12 spokes samples the x and y axes alike, so sagittal
    // and tangential residuals stay comparable.
    int contrastRings = DEFAULT_CONTRAST_RINGS;
    int contrastSpokes = DEFAULT_CONTRAST_SPOKES;
    boolean calibrateContrastFrequency = false;
    boolean aimContrastAtExitPupil = false;
    boolean centerContrastResiduals = false;
    boolean[] contrastBalanceFields;
    double contrastBalanceWeight = NOMINAL_BALANCE_WEIGHT;
    int scenario = 0;
    VigType vigType = VigType.SetPupil;
    boolean freezeVignetting = false;
    /** Solver tolerances a trial set; null fields keep the LMDerSolver defaults. */
    SolverTolerances solverTolerances = SolverTolerances.DEFAULTS;
    Double thicknessConstraintWeight;
    Double edgeThicknessConstraintWeight;
    Double curvatureConstraintWeight;
    final List<MtfGoals> mtfGoals = new ArrayList<>();
    final List<ContrastGoals> contrastGoals = new ArrayList<>();
    SpotGoals spotRmsGoals;
    SpotGoals spotMaxRadiusGoals;
    int[] curvatureExclusions = new int[0];
    int[] thicknessExclusions = new int[0];
    /** Aspheric terms varied explicitly, in the order given. */
    final List<AsphericTerm> asphericTerms = new ArrayList<>();
    /** First-order goals, in the order given; efl and fno replace the automatic ones. */
    final List<ParaxialGoal> paraxialGoals = new ArrayList<>();
    String description;
    String outdir;

    void gaussianSampling(int rings, int spokes, double innerPupilRadius) {
        if (rings < 1 || spokes < 3)
            throw new IllegalArgumentException(
                    "Gaussian quadrature requires at least 1 ring and 3 spokes");
        if (!Double.isFinite(innerPupilRadius)
                || innerPupilRadius < 0.0 || innerPupilRadius >= 1.0)
            throw new IllegalArgumentException("Inner pupil radius must be finite and in [0, 1)");
        this.gaussianQuadratureRings = rings;
        this.gaussianQuadratureSpokes = spokes;
        this.gaussianQuadratureInnerRadius = innerPupilRadius;
    }

    OptimizationConfiguration copy() {
        var result = new OptimizationConfiguration();
        result.fields = fields == null ? null : fields.clone();
        result.mtfFrequencies = mtfFrequencies == null ? null : mtfFrequencies.clone();
        result.curvatureSurfaces = curvatureSurfaces == null ? null : curvatureSurfaces.clone();
        result.allCurvatureSurfaces = allCurvatureSurfaces;
        result.thicknessSurfaces = thicknessSurfaces == null ? null : thicknessSurfaces.clone();
        result.allThicknessSurfaces = allThicknessSurfaces;
        result.includeExistingAspherics = includeExistingAspherics;
        result.weighted = weighted;
        result.dLineOnly = dLineOnly;
        result.addRayAberrationGoals = addRayAberrationGoals;
        result.useHexapolarSpotPattern = useHexapolarSpotPattern;
        result.hexapolarSpotRays = hexapolarSpotRays;
        result.gaussianQuadratureRings = gaussianQuadratureRings;
        result.gaussianQuadratureSpokes = gaussianQuadratureSpokes;
        result.gaussianQuadratureInnerRadius = gaussianQuadratureInnerRadius;
        result.checkSpotApertures = checkSpotApertures;
        result.spotDeviationXWeights = spotDeviationXWeights == null ? null : spotDeviationXWeights.clone();
        result.spotDeviationYWeights = spotDeviationYWeights == null ? null : spotDeviationYWeights.clone();
        result.addSpotDeviationGoals = addSpotDeviationGoals;
        result.contrastRings = contrastRings;
        result.contrastSpokes = contrastSpokes;
        result.calibrateContrastFrequency = calibrateContrastFrequency;
        result.aimContrastAtExitPupil = aimContrastAtExitPupil;
        result.centerContrastResiduals = centerContrastResiduals;
        result.contrastBalanceFields = contrastBalanceFields == null ? null : contrastBalanceFields.clone();
        result.contrastBalanceWeight = contrastBalanceWeight;
        result.scenario = scenario;
        result.vigType = vigType;
        result.freezeVignetting = freezeVignetting;
        result.solverTolerances = solverTolerances;
        result.thicknessConstraintWeight = thicknessConstraintWeight;
        result.edgeThicknessConstraintWeight = edgeThicknessConstraintWeight;
        result.curvatureConstraintWeight = curvatureConstraintWeight;
        result.mtfGoals.addAll(mtfGoals);
        result.contrastGoals.addAll(contrastGoals);
        result.spotRmsGoals = spotRmsGoals;
        result.spotMaxRadiusGoals = spotMaxRadiusGoals;
        result.curvatureExclusions = curvatureExclusions == null ? null : curvatureExclusions.clone();
        result.thicknessExclusions = thicknessExclusions == null ? null : thicknessExclusions.clone();
        result.asphericTerms.addAll(asphericTerms);
        result.paraxialGoals.addAll(paraxialGoals);
        result.description = description;
        result.outdir = outdir;
        return result;
    }

    String toTrial(int number) {
        var effective = effectiveAnalysis(false);
        // Preserve configured intent, even when the combination is invalid. Using
        // execution's Gaussian override here would silently repair a rejected setup
        // into a different, valid one when its trial is read back.
        boolean configuredHexapolar = useHexapolarSpotPattern || spotMaxRadiusGoals != null;
        var sb = new StringBuilder();
        sb.append("[trial ").append(number).append("]\n");
        if (description != null)
            line(sb, "description", description);
        if (outdir != null)
            line(sb, "outdir", outdir);
        line(sb, "configuration", Integer.toString(scenario));
        if (fields != null)
            line(sb, "fields", OptimizationTrial.format(fields));
        if (mtfFrequencies != null)
            line(sb, "frequencies", OptimizationTrial.format(mtfFrequencies));
        line(sb, "weighted", yesNo(weighted));
        line(sb, "d-line-only", yesNo(dLineOnly));
        line(sb, "vignetting", OptimizationTrial.kebab(vigType.name()) + (freezeVignetting ? " frozen" : ""));
        if (!checkSpotApertures || (effective.spots() && !configuredHexapolar))
            line(sb, "check-spot-apertures", yesNo(checkSpotApertures));
        // Only what this trial changed: the values came from the same constants, so
        // an untouched trial compares equal and writes nothing.
        if (solverTolerances.ftol() != SolverTolerances.DEFAULT_FTOL)
            line(sb, "solver ftol", OptimizationTrial.format(solverTolerances.ftol()));
        if (solverTolerances.xtol() != SolverTolerances.DEFAULT_XTOL)
            line(sb, "solver xtol", OptimizationTrial.format(solverTolerances.xtol()));
        if (solverTolerances.gtol() != SolverTolerances.DEFAULT_GTOL)
            line(sb, "solver gtol", OptimizationTrial.format(solverTolerances.gtol()));
        if (solverTolerances.maxEvaluations() != SolverTolerances.FROM_VARIABLE_COUNT)
            line(sb, "solver max-evaluations", Integer.toString(solverTolerances.maxEvaluations()));

        if (allCurvatureSurfaces)
            line(sb, "vary curvatures", allExcept(curvatureExclusions));
        else if (curvatureSurfaces.length > 0)
            line(sb, "vary curvatures", OptimizationTrial.format(curvatureSurfaces));
        if (allThicknessSurfaces)
            line(sb, "vary thicknesses", allExcept(thicknessExclusions));
        else if (thicknessSurfaces.length > 0)
            line(sb, "vary thicknesses", OptimizationTrial.format(thicknessSurfaces));
        if (includeExistingAspherics)
            line(sb, "vary aspherics", "existing");
        Map<Integer, List<String>> terms = new LinkedHashMap<>();
        for (AsphericTerm term : asphericTerms)
            terms.computeIfAbsent(term.surface(), s -> new ArrayList<>()).add(term.index() < 0 ? "K"
                    : term.index() + (term.scale() != null ? ":" + OptimizationTrial.format(term.scale()) : ""));
        for (var entry : terms.entrySet())
            line(sb, "vary aspherics", entry.getKey() + " " + String.join(" ", entry.getValue()));

        if (curvatureConstraintWeight != null)
            line(sb, "constrain curvatures", OptimizationTrial.format(curvatureConstraintWeight));
        if (thicknessConstraintWeight != null)
            line(sb, "constrain thicknesses", OptimizationTrial.format(thicknessConstraintWeight));
        if (edgeThicknessConstraintWeight != null)
            line(sb, "constrain edges", OptimizationTrial.format(edgeThicknessConstraintWeight));

        if (!contrastGoals.isEmpty()) {
            line(sb, "goal contrast", OptimizationTrial.format(
                    contrastGoals.stream().mapToInt(goal -> goal.frequency).toArray()));
            contrastWeights(sb, true);
            contrastWeights(sb, false);
            if (contrastBalanceFields != null)
                line(sb, "goal contrast", "balance " + balance() + " weight "
                        + OptimizationTrial.format(contrastBalanceWeight));
            line(sb, "goal contrast", "sampling " + contrastRings + " " + contrastSpokes);
            line(sb, "goal contrast", "calibrate " + yesNo(calibrateContrastFrequency));
            line(sb, "goal contrast", "exit-pupil-aiming " + yesNo(aimContrastAtExitPupil));
            line(sb, "goal contrast", "centering " + yesNo(centerContrastResiduals));
        }
        for (MtfGoals goal : mtfGoals) {
            line(sb, "goal mtf", goal.frequency + " sag " + OptimizationTrial.format(goal.sagittal));
            line(sb, "goal mtf", goal.frequency + " tan " + OptimizationTrial.format(goal.tangential));
            if (Arrays.equals(goal.sagittalWeights, goal.tangentialWeights)) {
                if (!allOnes(goal.sagittalWeights))
                    line(sb, "goal mtf", goal.frequency + " weights " + OptimizationTrial.format(goal.sagittalWeights));
            }
            else {
                if (!allOnes(goal.sagittalWeights))
                    line(sb, "goal mtf", goal.frequency + " sag weights " + OptimizationTrial.format(goal.sagittalWeights));
                if (!allOnes(goal.tangentialWeights))
                    line(sb, "goal mtf", goal.frequency + " tan weights " + OptimizationTrial.format(goal.tangentialWeights));
            }
        }
        spotGoals(sb, "goal spot-rms", spotRmsGoals);
        spotGoals(sb, "goal spot-max-radius", spotMaxRadiusGoals);
        if (addSpotDeviationGoals) {
            if (Arrays.equals(spotDeviationXWeights, spotDeviationYWeights))
                line(sb, "goal spot-deviation", OptimizationTrial.format(spotDeviationXWeights));
            else {
                line(sb, "goal spot-deviation", "x " + OptimizationTrial.format(spotDeviationXWeights));
                line(sb, "goal spot-deviation", "y " + OptimizationTrial.format(spotDeviationYWeights));
            }
        }
        if (gaussianQuadratureRings != DEFAULT_GAUSSIAN_QUADRATURE_RINGS
                || gaussianQuadratureSpokes != DEFAULT_GAUSSIAN_QUADRATURE_SPOKES
                || gaussianQuadratureInnerRadius != 0.0
                || (effective.spots() && !configuredHexapolar))
            line(sb, "goal spot sampling", "gaussian " + gaussianQuadratureRings + " " + gaussianQuadratureSpokes
                    + (gaussianQuadratureInnerRadius != 0.0
                    ? " " + OptimizationTrial.format(gaussianQuadratureInnerRadius) : ""));
        if (configuredHexapolar)
            line(sb, "goal spot sampling", "hexapolar " + hexapolarSpotRays);
        line(sb, "goal ray-aberrations", yesNo(addRayAberrationGoals));
        for (ParaxialGoal goal : paraxialGoals)
            line(sb, "goal paraxial", OptimizationTrial.paraxialName(goal.paraxId()) + " "
                    + OptimizationTrial.format(goal.target())
                    + (goal.weight() != 1.0 ? " weight " + OptimizationTrial.format(goal.weight()) : ""));
        return sb.toString();
    }

    private static void line(StringBuilder sb, String key, String values) {
        OptimizationTrial.line(sb, key, values);
    }

    private static String allExcept(int[] exclusions) {
        return exclusions.length == 0 ? "all" : "all except " + OptimizationTrial.format(exclusions);
    }

    /** Contrast weights: one row when every frequency shares them, else a row per frequency. */
    private void contrastWeights(StringBuilder sb, boolean sagittal) {
        String direction = sagittal ? "sag" : "tan";
        double[] first = sagittal ? contrastGoals.get(0).sagittalWeights : contrastGoals.get(0).tangentialWeights;
        boolean shared = contrastGoals.stream().allMatch(goal ->
                Arrays.equals(sagittal ? goal.sagittalWeights : goal.tangentialWeights, first));
        if (shared) {
            if (!allOnes(first))
                line(sb, "goal contrast", direction + " " + OptimizationTrial.format(first));
            return;
        }
        for (ContrastGoals goal : contrastGoals) {
            double[] weights = sagittal ? goal.sagittalWeights : goal.tangentialWeights;
            if (!allOnes(weights))
                line(sb, "goal contrast", goal.frequency + " " + direction + " " + OptimizationTrial.format(weights));
        }
    }

    /** The balanced fields: all, all except the listed field values, or yes/no for each. */
    private String balance() {
        boolean all = true, none = true;
        for (boolean flag : contrastBalanceFields) {
            all &= flag;
            none &= !flag;
        }
        if (all)
            return "all";
        if (none || fields == null || fields.length != contrastBalanceFields.length) {
            List<String> flags = new ArrayList<>();
            for (boolean flag : contrastBalanceFields)
                flags.add(flag ? "yes" : "no");
            return String.join(" ", flags);
        }
        List<String> except = new ArrayList<>();
        for (int i = 0; i < fields.length; i++)
            if (!contrastBalanceFields[i])
                except.add(OptimizationTrial.format(fields[i]));
        return "all except " + String.join(" ", except);
    }

    private static void spotGoals(StringBuilder sb, String key, SpotGoals goals) {
        if (goals == null)
            return;
        line(sb, key, OptimizationTrial.format(goals.targets));
        if (!allOnes(goals.weights))
            line(sb, key, "weights " + OptimizationTrial.format(goals.weights));
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    /**
     * Resolve the decisions shared by setup construction and canonical writing.
     * A custom maximum-radius goal also requests hexapolar sampling, but per-ray
     * deviation goals retain Gaussian sampling so their residual count stays fixed.
     * Custom goals have no written form, so the writer passes false.
     */
    EffectiveAnalysis effectiveAnalysis(boolean customMaximumRadius) {
        boolean mtf = !mtfGoals.isEmpty();
        boolean spots = spotRmsGoals != null || spotMaxRadiusGoals != null || addSpotDeviationGoals || mtf;
        boolean hexapolar = !addSpotDeviationGoals
                && (useHexapolarSpotPattern || spotMaxRadiusGoals != null || customMaximumRadius);
        return new EffectiveAnalysis(spots, addRayAberrationGoals, mtf, hexapolar, addSpotDeviationGoals);
    }

    record EffectiveAnalysis(boolean spots, boolean rayAberrations, boolean mtf,
                             boolean hexapolar, boolean retainFailedRays) {}

    void configureAnalysis(Analysis analysis, EffectiveAnalysis effective, boolean customGoals) {
        analysis.vignetting(vigType)
                .freezing_vignetting(freezeVignetting)
                .checking_spot_apertures(checkSpotApertures);
        if (effective.hexapolar())
            analysis.using_hexapolar_pattern(hexapolarSpotRays);
        else
            analysis.using_gauss_quadrature_pattern(
                    gaussianQuadratureRings, gaussianQuadratureSpokes, gaussianQuadratureInnerRadius);
        if (effective.retainFailedRays())
            analysis.retaining_failed_spot_rays(true);
        if (!contrastGoals.isEmpty()) {
            if (calibrateContrastFrequency && aimContrastAtExitPupil)
                throw new IllegalArgumentException(
                        "Contrast frequency calibration and exit-pupil aiming are mutually exclusive");
            int[] frequencies = contrastGoals.stream().mapToInt(goal -> goal.frequency).toArray();
            analysis.using_contrast_analysis(frequencies, contrastRings, contrastSpokes);
            analysis.calibrating_contrast_frequency(calibrateContrastFrequency);
            analysis.aiming_contrast_at_exit_pupil(aimContrastAtExitPupil);
            analysis.centering_contrast_residuals(centerContrastResiduals);
        }
        // Unknown factories may need any analysis. Keep the Analysis defaults (or
        // the factory's explicit choices) instead of disabling work based on built-in goals.
        if (!customGoals)
            analysis.required_analyses(effective.spots(), effective.rayAberrations(), effective.mtf());
    }

    private static boolean allOnes(double[] values) {
        if (values == null)
            return true;
        for (double value : values)
            if (value != 1.0)
                return false;
        return true;
    }

}
