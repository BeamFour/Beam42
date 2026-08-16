package org.redukti.optim;

import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.util.Orientation;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the repetitive variables and goals used by an optical optimization.
 * Surface numbers are zero based, consistently with {@link VarRadius} and the
 * other optimization variable classes.
 */
public final class OptimizationBuilder {
    /**
     * Default strength for {@link #applyThicknessConstraints()} and {@link #applyCurvatureConstraints()}.
     *
     * <p>A single number suffices because {@link Constraint} residuals are fractions of
     * each parameter's starting value, so the per-parameter scaling is already handled: a
     * 0.1mm air gap and a 39mm back focus resist the same <em>proportional</em> change
     * equally. What this weight sets is only the global trade between optical performance
     * and preserving the layout.
     *
     * <p>Raising it does tighten the design - on a 15-element f/2 with every space free,
     * the worst thickness excursion fell from 39% to 11% to 3% at weights of 1, 10 and
     * 100. But the useful range is narrow: past the nominal value the optical cost
     * outruns the benefit and the constraints start to dominate the Jacobian, stalling
     * the solver. Both a badly aberrated starting design and a well corrected one
     * behaved best here.
     *
     * <p>For a one-off override on a particular surface, construct the constraint
     * directly through {@link #additionalGoals(GoalFactory...)} rather than shifting the
     * global weight.
     */
    public static final double NOMINAL_CONSTRAINT_WEIGHT = 1.0;

    /**
     * Default strength for {@link #contrastBalanceGoals(boolean[])}.
     *
     * <p>Much smaller than {@link #NOMINAL_CONSTRAINT_WEIGHT}, and for a concrete reason:
     * a balance residual is a difference of sums of squares, so it is large where a
     * per-sample contrast residual is small. Measured on the Leica 75/2 starting design at
     * 10/30/50 cyc/mm over 11 fields, the balance block at weight 1.0 came to 43.6 against
     * the contrast block's 52.6 - 83% of the optical merit from 33 residuals against
     * 14256. It would have run the solve.
     *
     * <p>0.1 puts it near 8% there, which is visible without dominating. It is a starting
     * point, not a normalization: unlike the constraints, nothing here adapts to the
     * design. Check the actual share on your own case before trusting it.
     */
    public static final double NOMINAL_BALANCE_WEIGHT = 0.1;

    private static final int RAY_FAN_SAMPLES = 10;

    private final Prescription prescription;
    private double[] fields;
    private int[] mtfFrequencies;
    private int[] curvatureSurfaces = new int[0];
    private boolean allCurvatureSurfaces;
    private int[] thicknessSurfaces = new int[0];
    private boolean allThicknessSurfaces;
    private boolean includeExistingAspherics;
    private boolean weighted = true;
    private boolean dLineOnly;
    private boolean addRayAberrationGoals;
    private boolean useHexapolarSpotPattern;
    private int hexapolarSpotRays = 64;
    private int gaussianQuadratureRings = 14;
    private int gaussianQuadratureSpokes = 20;
    private boolean checkSpotApertures = true;
    private double[] spotDeviationXWeights;
    private double[] spotDeviationYWeights;
    private boolean addSpotDeviationGoals;
    // 3x6 is enough to measure a fixed design but not to optimize against: the
    // solver drives the 18 sampled points further than the wavefront between
    // them, so the merit reads better than the lens is. 6x12 is converged - 8x16
    // reproduces it - and 12 spokes samples the x and y axes alike, so sagittal
    // and tangential residuals stay comparable.
    private int contrastRings = 6;
    private int contrastSpokes = 12;
    private boolean calibrateContrastFrequency = false;
    private boolean centerContrastResiduals = false;
    private boolean[] contrastBalanceFields;
    private double contrastBalanceWeight = NOMINAL_BALANCE_WEIGHT;
    private VigType vigType = VigType.SetPupil;
    private boolean freezeVignetting = false;
    private Double thicknessConstraintWeight;
    private Double edgeThicknessConstraintWeight;
    private Double curvatureConstraintWeight;
    private final List<MtfGoals> mtfGoals = new ArrayList<>();
    private final List<ContrastGoals> contrastGoals = new ArrayList<>();
    private final List<Var> additionalVariables = new ArrayList<>();
    private final List<GoalFactory> additionalGoalFactories = new ArrayList<>();
    private SpotGoals spotRmsGoals;
    private SpotGoals spotMaxRadiusGoals;

    private OptimizationBuilder(Prescription prescription) {
        if (prescription == null)
            throw new IllegalArgumentException("prescription must not be null");
        if (prescription._surfaces == null)
            throw new IllegalArgumentException("prescription must be built before optimization");
        this.prescription = prescription;
    }

    public static OptimizationBuilder builder(Prescription prescription) {
        return new OptimizationBuilder(prescription);
    }

    // ------------------------------------------------------------------
    // Configuration - what gets evaluated, and how finely
    // ------------------------------------------------------------------

    public OptimizationBuilder fields(double... fields) {
        this.fields = copy(fields);
        return this;
    }

    public OptimizationBuilder mtfFrequencies(int... frequencies) {
        this.mtfFrequencies = copy(frequencies);
        return this;
    }

    /** Use the prescription's wavelength weights; false assigns every wavelength weight 1.0. */
    public OptimizationBuilder weighted(boolean weighted) {
        this.weighted = weighted;
        return this;
    }

    /** Restrict enabled ray-aberration goals to the Fraunhofer d-line. */
    public OptimizationBuilder dLineOnly(boolean dLineOnly) {
        this.dLineOnly = dLineOnly;
        return this;
    }

    /**
     * How each rebuilt model establishes vignetting. See {@link Analysis#vignetting(VigType)}
     * for the trade-offs; {@link VigType#Paraxial} in particular leaves the sagittal pupil
     * unvignetted and breaks on-axis rotational symmetry.
     */
    public OptimizationBuilder vignetting(VigType vigType) {
        this.vigType = vigType == null ? VigType.None : vigType;
        return this;
    }

    /**
     * Measure vignetting once at the start and hold it fixed for the run, so every
     * iteration is compared on the same pupil. See
     * {@link Analysis#freezing_vignetting(boolean)} for the trade-off.
     */
    public OptimizationBuilder freezeVignetting() {
        return freezeVignetting(true);
    }

    public OptimizationBuilder freezeVignetting(boolean freeze) {
        this.freezeVignetting = freeze;
        return this;
    }

    /**
     * Configure the ordinary Gaussian-quadrature spot pattern shared by spot and
     * geometric-MTF analyses. Contrast uses its separate sheared-pupil pattern.
     */
    public OptimizationBuilder gaussianQuadratureSampling(int rings, int spokes) {
        if (rings < 1 || spokes < 1)
            throw new IllegalArgumentException(
                    "Gaussian-quadrature rings and spokes must be at least 1");
        this.gaussianQuadratureRings = rings;
        this.gaussianQuadratureSpokes = spokes;
        return this;
    }

    /**
     * Whether Gaussian-quadrature spot rays are rejected when they cross a physical
     * surface aperture. Disable this with frozen vignetting to optimize a fixed
     * factor-defined pupil, matching the usual Zemax GQ merit-function behaviour.
     * Grid and hexapolar sampling always retain physical aperture checking.
     */
    public OptimizationBuilder checkSpotApertures(boolean check) {
        this.checkSpotApertures = check;
        return this;
    }

    /**
     * Use hexapolar sampling for spot analysis even when no maximum-radius
     * goal requires it. The default is Gaussian quadrature.
     */
    public OptimizationBuilder hexapolarSampling() {
        return hexapolarSampling(64);
    }

    /**
     * Use hexapolar sampling with the requested number of pupil rings for
     * spot analysis even when no maximum-radius goal requires it.
     */
    public OptimizationBuilder hexapolarSampling(int numRays) {
        if (numRays < 1)
            throw new IllegalArgumentException("hexapolar spot rays must be at least 1");
        this.useHexapolarSpotPattern = true;
        this.hexapolarSpotRays = numRays;
        return this;
    }

    public OptimizationBuilder contrastSampling(int rings, int spokes) {
        if (rings < 1 || spokes < 1)
            throw new IllegalArgumentException("contrast rings and spokes must be at least 1");
        contrastRings = rings;
        contrastSpokes = spokes;
        return this;
    }

    /**
     * Correct the contrast pupil shift so each sample realises the requested spatial
     * frequency in image space.
     *
     * <p>The shift is applied in entrance-pupil coordinates but derives from an
     * exit-pupil relation, so pupil aberration makes the realised frequency fall short,
     * increasingly with field - measured 8.5% low at full field tangential on an f/2
     * lens. Enabling this measures the shortfall per field, wavelength and direction and
     * scales the shift to compensate, which brought that case to within 0.1%.
     *
     * <p>Off by default because it changes every contrast residual.
     */
    public OptimizationBuilder calibrateContrastFrequency(boolean value) {
        calibrateContrastFrequency = value;
        return this;
    }

    /**
     * Subtract the constant part of each contrast block, so the residuals carry the
     * variance the OTF modulus depends on rather than the un-centred second moment.
     *
     * <p>A constant wavefront difference across the pupil is tilt, which displaces the
     * image and costs no MTF - but it is reducible, so leaving it in offers the solver
     * merit reduction that buys nothing. It is identically zero in the sagittal direction
     * by symmetry and reaches 57% of an outer-field tangential block on the Leica 75/2,
     * which biases the astigmatic focus split toward tangential.
     *
     * <p>Off by default because it changes every contrast residual. See
     * {@link ContrastAnalysis#center_residuals(ContrastAnalysisResult, int)}.
     */
    public OptimizationBuilder centerContrastResiduals(boolean value) {
        centerContrastResiduals = value;
        return this;
    }

    // ------------------------------------------------------------------
    // Variables - what the solver is allowed to change
    // ------------------------------------------------------------------

    public OptimizationBuilder varyCurvatures(int... surfaces) {
        this.curvatureSurfaces = copy(surfaces);
        this.allCurvatureSurfaces = false;
        return this;
    }

    /**
     * Vary every curved optical surface. Aperture/field stops and surfaces
     * whose radius is zero (and therefore intentionally flat) are excluded.
     */
    public OptimizationBuilder varyAllCurvatures() {
        this.allCurvatureSurfaces = true;
        return this;
    }

    public OptimizationBuilder varyThicknesses(int... surfaces) {
        this.thicknessSurfaces = copy(surfaces);
        this.allThicknessSurfaces = false;
        return this;
    }

    /**
     * Vary every thickness, air spaces and element thicknesses alike. Surfaces with zero
     * thickness are excluded, being coincident rather than a space to open up.
     *
     * <p>The counterpart to {@link #varyAllCurvatures()}, and best paired with
     * {@link #applyThicknessConstraints(double)} - with every space free and nothing holding the
     * layout, the solver will collapse gaps and drive elements through one another.
     */
    public OptimizationBuilder varyAllThicknesses() {
        this.allThicknessSurfaces = true;
        return this;
    }

    /** Thickness of a surface for the scenario this builder targets. */
    private double thicknessOf(int surface) {
        var definition = prescription._surfaces[surface];
        return definition._thickness_by_scenario != null
                ? definition._thickness_by_scenario[0]
                : definition._thickness;
    }

    /**
     * Vary the conic constants and polynomial coefficients already present in the
     * prescription. Only nonzero terms become variables, so a spherical surface stays
     * spherical and an asphere does not gain orders it did not have.
     */
    public OptimizationBuilder varyExistingAspherics() {
        return varyExistingAspherics(true);
    }

    public OptimizationBuilder varyExistingAspherics(boolean include) {
        this.includeExistingAspherics = include;
        return this;
    }

    /** Adds caller-defined variables after the automatically generated variables. */
    public OptimizationBuilder additionalVariables(Var... variables) {
        if (variables == null)
            throw new IllegalArgumentException("additional variables must not be null");
        for (Var variable : variables) {
            if (variable == null)
                throw new IllegalArgumentException("additional variables must not contain null");
            if (variable._prescription != prescription)
                throw new IllegalArgumentException("additional variables must use this builder's prescription");
            additionalVariables.add(variable);
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Goals - what the solver optimizes towards
    // ------------------------------------------------------------------

    public static MtfGoals mtf(int frequency, double[] sagittal, double[] tangential) {
        return new MtfGoals(frequency, sagittal, tangential, null, null);
    }

    /** Applies one weight per field to both sagittal and tangential goals. */
    public static MtfGoals mtf(int frequency, double[] sagittal, double[] tangential, double[] weights) {
        return new MtfGoals(frequency, sagittal, tangential, weights, weights);
    }

    public static MtfGoals mtf(int frequency, double[] sagittal, double[] tangential,
                               double[] sagittalWeights, double[] tangentialWeights) {
        return new MtfGoals(frequency, sagittal, tangential, sagittalWeights, tangentialWeights);
    }

    /** Contrast optimization at one frequency, with one directional weight per field. */
    public static ContrastGoals contrast(int frequency, double[] sagittalWeights,
                                         double[] tangentialWeights) {
        return new ContrastGoals(frequency, sagittalWeights, tangentialWeights);
    }

    /** Applies one weight per field to both contrast directions. */
    public static ContrastGoals contrast(int frequency, double[] weights) {
        return new ContrastGoals(frequency, weights, weights);
    }

    public OptimizationBuilder mtfGoals(MtfGoals... goals) {
        if (goals == null)
            throw new IllegalArgumentException("MTF goals must not be null");
        this.mtfGoals.addAll(Arrays.asList(goals));
        return this;
    }

    public OptimizationBuilder contrastGoals(ContrastGoals... goals) {
        if (goals == null)
            throw new IllegalArgumentException("contrast goals must not be null");
        this.contrastGoals.addAll(Arrays.asList(goals));
        return this;
    }

    /**
     * Hold sagittal and tangential contrast in balance at the selected fields, at the
     * nominal balance weight. One flag per configured field, in field order; a false
     * leaves that field's astigmatism entirely unconstrained, which is usually what the
     * outermost field wants.
     *
     * <p>Applies to every configured contrast frequency, so this adds one residual per
     * enabled field per frequency. See {@link GoalContrastBalance} for what it measures
     * and why the contrast merit does not already care.
     */
    public OptimizationBuilder contrastBalanceGoals(boolean[] fields) {
        return contrastBalanceGoals(fields, NOMINAL_BALANCE_WEIGHT);
    }

    /**
     * Hold sagittal and tangential contrast in balance at the selected fields, at a chosen
     * weight.
     *
     * <p>The weight needs setting deliberately, because the scale is nothing like the
     * per-sample contrast residuals: a balance residual is a difference of sums of squares,
     * of order 0.1 to 3 waves squared on the test lenses, so a handful of them can outweigh
     * thousands of contrast residuals. See {@link #NOMINAL_BALANCE_WEIGHT} for the measured
     * example. Compare the two blocks' sum-of-squares contributions on your own case rather
     * than assuming the default is proportionate.
     *
     * @param fields one flag per configured field, in field order
     * @param weight relative strength
     */
    public OptimizationBuilder contrastBalanceGoals(boolean[] fields, double weight) {
        if (fields == null)
            throw new IllegalArgumentException("contrast balance field flags must not be null");
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException(
                    "contrast balance weight must be finite and non-negative");
        this.contrastBalanceFields = fields.clone();
        this.contrastBalanceWeight = weight;
        return this;
    }

    /**
     * One aggregate {@link GoalSpotRMS} per field, each aiming at a target RMS spot
     * radius in microns. To minimize spot size rather than hit a number, prefer
     * {@link #spotDeviationGoals(double...)}, which takes weights instead.
     */
    public OptimizationBuilder spotRmsGoals(double[] targets) {
        return spotRmsGoals(targets, null);
    }

    public OptimizationBuilder spotRmsGoals(double[] targets, double[] weights) {
        this.spotRmsGoals = new SpotGoals(targets, weights);
        return this;
    }

    /**
     * Minimize RMS spot radius through the individual signed X/Y ray deviations that
     * make it up, one {@link GoalSpotDeviation} per orientation per sampled ray.
     * Differentiating those exposes far more to the solver than one square-rooted
     * aggregate does.
     *
     * <p>These take <em>weights</em>, not targets - every residual aims at zero. One
     * weight per field is applied to every wavelength, sample and orientation.
     */
    public OptimizationBuilder spotDeviationGoals(double... fieldWeights) {
        this.addSpotDeviationGoals = true;
        this.spotDeviationXWeights = copy(fieldWeights);
        this.spotDeviationYWeights = copy(fieldWeights);
        return this;
    }

    /** Assign separate per-field weights to the signed X and Y spot deviations. */
    public OptimizationBuilder spotDeviationGoals(double[] xWeights, double[] yWeights) {
        this.addSpotDeviationGoals = true;
        this.spotDeviationXWeights = copy(xWeights);
        this.spotDeviationYWeights = copy(yWeights);
        return this;
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets) {
        return spotMaxRadiusGoals(targets, null);
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets, double[] weights) {
        this.spotMaxRadiusGoals = new SpotGoals(targets, weights);
        return this;
    }

    /**
     * Add the legacy ten-sample sagittal and tangential ray-aberration fans to
     * the merit function. They are disabled by default: dense contrast or spot
     * goals already provide enough residuals, and a failed fan edge ray should
     * not invalidate an otherwise usable merit function.
     */
    public OptimizationBuilder rayAberrationGoals() {
        return rayAberrationGoals(true);
    }

    public OptimizationBuilder rayAberrationGoals(boolean enabled) {
        this.addRayAberrationGoals = enabled;
        return this;
    }

    /**
     * Adds caller-defined goals after the automatically generated goals.
     * Factories receive the exact Analysis owned by the resulting setup.
     */
    public OptimizationBuilder additionalGoals(GoalFactory... factories) {
        if (factories == null)
            throw new IllegalArgumentException("additional goal factories must not be null");
        for (GoalFactory factory : factories) {
            if (factory == null)
                throw new IllegalArgumentException("additional goal factories must not contain null");
            additionalGoalFactories.add(factory);
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Constraints - what holds the starting design together
    // ------------------------------------------------------------------

    /**
     * Hold the varied thicknesses near their starting values, at the nominal weight.
     *
     * <p>An optical merit function has no opinion about mechanical layout, so left alone
     * the solver will collapse air spaces and push elements through each other and
     * through the stop. This anchors each varied thickness to where it began: it is still
     * free to move, it just costs merit to do so.
     */
    public OptimizationBuilder applyThicknessConstraints() {
        return applyThicknessConstraints(NOMINAL_CONSTRAINT_WEIGHT);
    }

    /**
     * Hold the varied thicknesses near their starting values, at a chosen weight.
     *
     * @param weight relative strength; see {@link #NOMINAL_CONSTRAINT_WEIGHT} for why the
     *               nominal value is usually the right one
     */
    public OptimizationBuilder applyThicknessConstraints(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("thickness constraint weight must be finite and non-negative");
        this.thicknessConstraintWeight = weight;
        return this;
    }

    /**
     * Hold the <em>edge</em> separation of each varied gap near its starting value, at the
     * nominal weight.
     *
     * <p>Complements {@link #applyThicknessConstraints()} rather than replacing it. That
     * one holds axial centre thickness, which two surfaces can honour while still crossing
     * away from the axis once curvature moves - the failure mode that produced overlapping
     * first and second surfaces on the Leica 75/2 with thickness constraints already in
     * place. Use both when curvatures and thicknesses are varied together.
     */
    public OptimizationBuilder applyEdgeThicknessConstraints() {
        return applyEdgeThicknessConstraints(NOMINAL_CONSTRAINT_WEIGHT);
    }

    /**
     * Hold the varied gaps near their starting edge separations, at a chosen weight.
     *
     * <p>Gaps whose starting edge separation is not positive and finite are skipped: a
     * fractional constraint cannot be formed around zero, and a design that already starts
     * with coincident or crossed surfaces has nothing useful to anchor to. See
     * {@link ConstraintEdgeThickness#is_constrainable(Analysis, int)}.
     *
     * @param weight relative strength; see {@link #NOMINAL_CONSTRAINT_WEIGHT}
     */
    public OptimizationBuilder applyEdgeThicknessConstraints(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("edge thickness constraint weight must be finite and non-negative");
        this.edgeThicknessConstraintWeight = weight;
        return this;
    }

    /**
     * Hold the varied surfaces near their starting <em>curvatures</em>, at the nominal
     * weight.
     *
     * <p>Curvature, not radius: radius runs away towards infinity on a near-flat surface
     * for a negligible optical change, so a fractional radius constraint would barely
     * restrain it there while over-restraining a strongly curved one. See
     * {@link ConstraintCurvature}.
     */
    public OptimizationBuilder applyCurvatureConstraints() {
        return applyCurvatureConstraints(NOMINAL_CONSTRAINT_WEIGHT);
    }

    /**
     * Hold the varied surfaces near their starting curvatures, at a chosen weight.
     *
     * @param weight relative strength; see {@link #NOMINAL_CONSTRAINT_WEIGHT}
     */
    public OptimizationBuilder applyCurvatureConstraints(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("curvature constraint weight must be finite and non-negative");
        this.curvatureConstraintWeight = weight;
        return this;
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    public OptimizationSetup build() {
        validate();
        Analysis analysis = new Analysis(prescription, copy(fields), copy(mtfFrequencies));
        List<Var> variables = buildVariables();
        List<Goal> goals = buildGoals(analysis, variables);
        if (goals.size() < variables.size())
            throw new IllegalArgumentException(
                    "optimization requires at least as many goals as variables: "
                            + goals.size() + " goals for " + variables.size() + " variables"
                            + "; add optical goals or enable rayAberrationGoals()");
        analysis.vignetting(vigType)
                .freezing_vignetting(freezeVignetting)
                .checking_spot_apertures(checkSpotApertures);
        configureSpotPattern(analysis, goals);
        configureContrastAnalysis(analysis, goals);
        configureRequiredAnalyses(analysis, goals);
        return new OptimizationSetup(analysis, variables.toArray(new Var[0]), goals.toArray(new Goal[0]));
    }

    private void configureContrastAnalysis(Analysis analysis, List<Goal> goals) {
        if (contrastGoals.isEmpty()) return;
        int[] frequencies = contrastGoals.stream().mapToInt(goal -> goal.frequency).toArray();
        analysis.using_contrast_analysis(frequencies, contrastRings, contrastSpokes);
        analysis.calibrating_contrast_frequency(calibrateContrastFrequency);
        analysis.centering_contrast_residuals(centerContrastResiduals);
    }

    private void configureRequiredAnalyses(Analysis analysis, List<Goal> goals) {
        // Additional goal factories are conservatively assumed to require all analyses.
        if (additionalGoalFactories.isEmpty()) {
            boolean spots = goals.stream().anyMatch(goal ->
                    goal instanceof GoalSpotRMS || goal instanceof GoalSpotDeviation
                            || goal instanceof GoalSpotMaxRadius || goal instanceof GoalGeoMTF);
            boolean mtf = goals.stream().anyMatch(GoalGeoMTF.class::isInstance);
            boolean rayAberrations = goals.stream().anyMatch(goal ->
                    goal instanceof GoalRayAberration || goal instanceof GoalMTFProxy);
            analysis.required_analyses(spots, rayAberrations, mtf);
        }
    }

    private void configureSpotPattern(Analysis analysis, List<Goal> goals) {
        boolean hasSpotMaxRadiusGoal = goals.stream().anyMatch(GoalSpotMaxRadius.class::isInstance);
        if (addSpotDeviationGoals) {
            analysis.using_gauss_quadrature_pattern(
                            gaussianQuadratureRings, gaussianQuadratureSpokes)
                    .retaining_failed_spot_rays(true);
        }
        else if (useHexapolarSpotPattern || hasSpotMaxRadiusGoal)
            analysis.using_hexapolar_pattern(hexapolarSpotRays);
        else
            analysis.using_gauss_quadrature_pattern(
                    gaussianQuadratureRings, gaussianQuadratureSpokes);
    }

    private List<Var> buildVariables() {
        List<Var> result = new ArrayList<>();
        if (allCurvatureSurfaces) {
            for (int surface = 0; surface < prescription._surfaces.length; surface++) {
                var definition = prescription._surfaces[surface];
                if (!definition.is_aperture_stop() && !definition.is_field_stop()
                        && definition._radius != 0.0)
                    result.add(new VarRadius(prescription, surface));
            }
        } else {
            for (int surface : curvatureSurfaces)
                result.add(new VarRadius(prescription, surface));
        }
        if (allThicknessSurfaces) {
            for (int surface = 0; surface < prescription._surfaces.length; surface++) {
                // A zero thickness is a coincident surface, not a space to open up, and
                // it gives the fractional ConstraintThickness no base to work from.
                if (thicknessOf(surface) != 0.0)
                    result.add(new VarThickness(prescription, surface));
            }
        } else {
            for (int surface : thicknessSurfaces)
                result.add(new VarThickness(prescription, surface));
        }
        if (includeExistingAspherics) {
            for (int surfaceId = 0; surfaceId < prescription._surfaces.length; surfaceId++) {
                var surface = prescription._surfaces[surfaceId];
                if (surface._k != 0.0)
                    result.add(new VarAsphK(prescription, surfaceId));
                if (surface._coeffs == null)
                    continue;
                for (int coefficient = 0; coefficient < surface._coeffs.length; coefficient++) {
                    double value = surface._coeffs[coefficient];
                    if (value != 0.0)
                        result.add(new VarAsphCoeff(prescription, surfaceId, coefficient,
                                scalingFor(value)));
                }
            }
        }
        result.addAll(additionalVariables);
        return result;
    }

    private List<Goal> buildGoals(Analysis analysis, List<Var> variables) {
        List<Goal> result = new ArrayList<>();
        // Anchor the varied parameters to where they started. Built from the variable
        // list so the goals attach to exactly what is free to move, and built here while
        // the prescription still holds its original values.
        if (thicknessConstraintWeight != null) {
            for (Var variable : variables)
                if (variable instanceof VarThickness thickness)
                    result.add(new ConstraintThickness(analysis, thickness._surface_id,
                            thicknessConstraintWeight));
        }
        if (edgeThicknessConstraintWeight != null) {
            for (Var variable : variables)
                if (variable instanceof VarThickness thickness
                        && ConstraintEdgeThickness.is_constrainable(analysis, thickness._surface_id))
                    result.add(new ConstraintEdgeThickness(analysis, thickness._surface_id,
                            edgeThicknessConstraintWeight));
        }
        if (curvatureConstraintWeight != null) {
            for (Var variable : variables)
                if (variable instanceof VarRadius radius)
                    result.add(new ConstraintCurvature(analysis, radius._surface_id,
                            curvatureConstraintWeight));
        }
        for (MtfGoals curve : mtfGoals) {
            for (int field = 0; field < fields.length; field++) {
                result.add(new GoalGeoMTF(analysis, field + 1, Orientation.SAGITTAL, curve.frequency,
                        curve.sagittal[field] / 100.0, curve.sagittalWeights[field]));
                result.add(new GoalGeoMTF(analysis, field + 1, Orientation.TANGENTIAL, curve.frequency,
                        curve.tangential[field] / 100.0, curve.tangentialWeights[field]));
            }
        }

        int contrastSamples = contrastRings * contrastSpokes;
        for (int contrast_index = 0; contrast_index < contrastGoals.size(); contrast_index++) {
            ContrastGoals curve = contrastGoals.get(contrast_index);
            for (int field = 0; field < fields.length; field++) {
                for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                    double wavelengthWeight = weighted ? prescription._wts[wavelength] : 1.0;
                    for (int sample = 0; sample < contrastSamples; sample++) {
                        result.add(new GoalContrast(analysis, contrast_index, curve.frequency, field + 1,
                                wavelength, sample, Orientation.SAGITTAL,
                                wavelengthWeight * curve.sagittalWeights[field]));
                        result.add(new GoalContrast(analysis, contrast_index, curve.frequency, field + 1,
                                wavelength, sample, Orientation.TANGENTIAL,
                                wavelengthWeight * curve.tangentialWeights[field]));
                    }
                }
            }
        }

        if (contrastBalanceFields != null) {
            double[] wavelengthWeights = new double[prescription._wvls.length];
            for (int w = 0; w < wavelengthWeights.length; w++)
                wavelengthWeights[w] = weighted ? prescription._wts[w] : 1.0;
            for (int contrast_index = 0; contrast_index < contrastGoals.size(); contrast_index++) {
                ContrastGoals curve = contrastGoals.get(contrast_index);
                for (int field = 0; field < fields.length; field++) {
                    if (!contrastBalanceFields[field]) continue;
                    result.add(new GoalContrastBalance(analysis, contrast_index, curve.frequency,
                            field + 1, wavelengthWeights,
                            curve.sagittalWeights[field], curve.tangentialWeights[field],
                            contrastBalanceWeight));
                }
            }
        }

        if (spotRmsGoals != null) {
            for (int field = 0; field < fields.length; field++)
                result.add(new GoalSpotRMS(analysis, field + 1,
                        spotRmsGoals.targets[field], spotRmsGoals.weights[field]));
        }
        if (addSpotDeviationGoals) {
            int samples = gaussianQuadratureRings * gaussianQuadratureSpokes;
            for (int field = 0; field < fields.length; field++) {
                for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                    double wavelengthWeight = weighted ? prescription._wts[wavelength] : 1.0;
                    for (int sample = 0; sample < samples; sample++) {
                        result.add(new GoalSpotDeviation(analysis, field + 1, wavelength,
                                sample, Orientation.X,
                                wavelengthWeight * spotDeviationXWeights[field]));
                        result.add(new GoalSpotDeviation(analysis, field + 1, wavelength,
                                sample, Orientation.Y,
                                wavelengthWeight * spotDeviationYWeights[field]));
                    }
                }
            }
        }
        if (spotMaxRadiusGoals != null) {
            for (int field = 0; field < fields.length; field++)
                result.add(new GoalSpotMaxRadius(analysis, field + 1,
                        spotMaxRadiusGoals.targets[field], spotMaxRadiusGoals.weights[field]));
        }

        // Anchor first-order properties to the requested prescription values.
        result.add(new GoalParax(analysis, ParaxHelper.Effective_focal_length,
                prescription._focal_length, 1.0));
        result.add(new GoalParax(analysis, ParaxHelper.Fno, prescription._fno, 1.0));

        if (addRayAberrationGoals) {
            for (int field = 1; field <= fields.length; field++) {
                for (int orientation = Orientation.SAGITTAL; orientation <= Orientation.TANGENTIAL; orientation++) {
                    for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                        if (dLineOnly && !sameWavelength(prescription._wvls[wavelength], Glass.d))
                            continue;
                        double weight = weighted ? prescription._wts[wavelength] : 1.0;
                        for (int sample = 0; sample < RAY_FAN_SAMPLES; sample++)
                            result.add(new GoalRayAberration(analysis, field, orientation, sample,
                                    prescription._wvls[wavelength], 0.0, weight));
                    }
                }
            }
        }
        for (GoalFactory factory : additionalGoalFactories) {
            Goal goal = factory.create(analysis);
            if (goal == null)
                throw new IllegalArgumentException("an additional goal factory returned null");
            if (goal._analysis != analysis)
                throw new IllegalArgumentException("additional goals must use the Analysis supplied to their factory");
            result.add(goal);
        }
        return result;
    }

    private void validate() {
        if (fields == null || fields.length == 0)
            throw new IllegalArgumentException("at least one field is required");
        if (contrastBalanceFields != null) {
            if (contrastBalanceFields.length != fields.length)
                throw new IllegalArgumentException(
                        "contrast balance needs one flag per field: " + fields.length
                                + " fields but " + contrastBalanceFields.length + " flags");
            if (contrastGoals.isEmpty())
                throw new IllegalArgumentException(
                        "contrast balance goals require contrast goals to balance");
        }
        for (double field : fields)
            if (!Double.isFinite(field) || field < 0.0 || field > 1.0)
                throw new IllegalArgumentException("fields must be finite values between 0 and 1");
        if (fields[0] != 0.0)
            throw new IllegalArgumentException("the first field must be 0.0");

        if (mtfFrequencies == null || mtfFrequencies.length == 0)
            throw new IllegalArgumentException("at least one MTF frequency is required");
        Set<Integer> frequencies = new HashSet<>();
        for (int frequency : mtfFrequencies) {
            if (frequency <= 0 || !frequencies.add(frequency))
                throw new IllegalArgumentException("MTF frequencies must be positive and unique");
        }
        Set<Integer> goalFrequencies = new HashSet<>();
        for (MtfGoals curve : mtfGoals) {
            if (!frequencies.contains(curve.frequency))
                throw new IllegalArgumentException("MTF goal frequency was not requested for measurement: " + curve.frequency);
            if (!goalFrequencies.add(curve.frequency))
                throw new IllegalArgumentException("duplicate MTF goal frequency: " + curve.frequency);
            curve.validate(fields.length);
        }
        Set<Integer> contrastFrequencies = new HashSet<>();
        for (ContrastGoals curve : contrastGoals) {
            if (curve == null)
                throw new IllegalArgumentException("contrast goals must not contain null");
            if (curve.frequency <= 0 || !contrastFrequencies.add(curve.frequency))
                throw new IllegalArgumentException("contrast frequencies must be positive and unique");
            curve.validate(fields.length);
        }
        if (spotRmsGoals != null)
            spotRmsGoals.validate(fields.length, "spot RMS");
        if (addSpotDeviationGoals) {
            MtfGoals.validateWeights(spotDeviationXWeights, fields.length,
                    "spot deviation X weights");
            MtfGoals.validateWeights(spotDeviationYWeights, fields.length,
                    "spot deviation Y weights");
            if (spotRmsGoals != null)
                throw new IllegalArgumentException(
                        "aggregate spot RMS goals and per-ray spot deviation goals cannot both be enabled");
            if (spotMaxRadiusGoals != null || useHexapolarSpotPattern)
                throw new IllegalArgumentException(
                        "spot deviation goals require Gaussian-quadrature spot sampling");
        }
        if (spotMaxRadiusGoals != null)
            spotMaxRadiusGoals.validate(fields.length, "spot maximum radius");
        validateSurfaces(curvatureSurfaces, "curvature");
        validateSurfaces(thicknessSurfaces, "thickness");
        if (addRayAberrationGoals && dLineOnly
                && Arrays.stream(prescription._wvls).noneMatch(w -> sameWavelength(w, Glass.d)))
            throw new IllegalArgumentException("d-line optimization requires the prescription to contain the d-line wavelength");
    }

    private void validateSurfaces(int[] surfaces, String kind) {
        Set<Integer> seen = new HashSet<>();
        for (int surface : surfaces) {
            if (surface < 0 || surface >= prescription._surfaces.length)
                throw new IllegalArgumentException(kind + " surface is out of range: " + surface);
            if (!seen.add(surface))
                throw new IllegalArgumentException("duplicate " + kind + " surface: " + surface);
        }
    }

    /** Normalizes an existing coefficient to a scaled value in [1, 10). */
    static double scalingFor(double value) {
        double magnitude = Math.abs(value);
        return Math.pow(10.0, -Math.floor(Math.log10(magnitude)));
    }

    private static boolean sameWavelength(double a, double b) {
        return Math.abs(a - b) < 1.0e-3;
    }

    private static double[] copy(double[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    private static int[] copy(int[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    public static final class MtfGoals {
        private final int frequency;
        private final double[] sagittal;
        private final double[] tangential;
        private final double[] sagittalWeights;
        private final double[] tangentialWeights;

        private MtfGoals(int frequency, double[] sagittal, double[] tangential,
                         double[] sagittalWeights, double[] tangentialWeights) {
            this.frequency = frequency;
            this.sagittal = copy(sagittal);
            this.tangential = copy(tangential);
            this.sagittalWeights = sagittalWeights == null ? unitWeights(sagittal) : copy(sagittalWeights);
            this.tangentialWeights = tangentialWeights == null ? unitWeights(tangential) : copy(tangentialWeights);
        }

        private void validate(int fieldCount) {
            validateTargets(sagittal, fieldCount, "sagittal targets");
            validateTargets(tangential, fieldCount, "tangential targets");
            validateWeights(sagittalWeights, fieldCount, "sagittal weights");
            validateWeights(tangentialWeights, fieldCount, "tangential weights");
        }

        private static double[] unitWeights(double[] targets) {
            if (targets == null)
                return null;
            double[] weights = new double[targets.length];
            Arrays.fill(weights, 1.0);
            return weights;
        }

        private static void validateTargets(double[] values, int count, String name) {
            if (values == null || values.length != count)
                throw new IllegalArgumentException(name + " must contain one value per field");
            for (double value : values)
                if (!Double.isFinite(value) || value < 0.0 || value > 100.0)
                    throw new IllegalArgumentException(name + " must be percentages between 0 and 100");
        }

        private static void validateWeights(double[] values, int count, String name) {
            if (values == null || values.length != count)
                throw new IllegalArgumentException(name + " must contain one value per field");
            for (double value : values)
                if (!Double.isFinite(value) || value < 0.0)
                    throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    public static final class ContrastGoals {
        private final int frequency;
        private final double[] sagittalWeights;
        private final double[] tangentialWeights;

        private ContrastGoals(int frequency, double[] sagittalWeights, double[] tangentialWeights) {
            this.frequency = frequency;
            this.sagittalWeights = copy(sagittalWeights);
            this.tangentialWeights = copy(tangentialWeights);
        }

        private void validate(int fieldCount) {
            MtfGoals.validateWeights(sagittalWeights, fieldCount, "sagittal contrast weights");
            MtfGoals.validateWeights(tangentialWeights, fieldCount, "tangential contrast weights");
        }
    }

    private static final class SpotGoals {
        private final double[] targets;
        private final double[] weights;

        private SpotGoals(double[] targets, double[] weights) {
            this.targets = copy(targets);
            this.weights = weights == null ? unitWeights(targets) : copy(weights);
        }

        private void validate(int fieldCount, String name) {
            if (targets == null || targets.length != fieldCount)
                throw new IllegalArgumentException(name + " targets must contain one value per field");
            if (weights == null || weights.length != fieldCount)
                throw new IllegalArgumentException(name + " weights must contain one value per field");
            for (double target : targets)
                if (!Double.isFinite(target) || target < 0.0)
                    throw new IllegalArgumentException(name + " targets must be finite and non-negative");
            for (double weight : weights)
                if (!Double.isFinite(weight) || weight < 0.0)
                    throw new IllegalArgumentException(name + " weights must be finite and non-negative");
        }

        private static double[] unitWeights(double[] targets) {
            if (targets == null)
                return null;
            double[] weights = new double[targets.length];
            Arrays.fill(weights, 1.0);
            return weights;
        }
    }

    @FunctionalInterface
    public interface GoalFactory {
        Goal create(Analysis analysis);
    }

    public static final class OptimizationSetup {
        private final Analysis analysis;
        private final Var[] variables;
        private final Goal[] goals;

        private OptimizationSetup(Analysis analysis, Var[] variables, Goal[] goals) {
            this.analysis = analysis;
            this.variables = variables;
            this.goals = goals;
        }

        public Analysis analysis() { return analysis; }
        public Var[] variables() { return Arrays.copyOf(variables, variables.length); }
        public Goal[] goals() { return Arrays.copyOf(goals, goals.length); }

        public LMDerMeritFunction meritFunction(boolean useNative) {
            return new LMDerMeritFunction(analysis, variables, goals, useNative);
        }
    }
}
