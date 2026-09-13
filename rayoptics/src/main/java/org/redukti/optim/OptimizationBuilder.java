package org.redukti.optim;

import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.util.Orientation;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;

import java.util.*;

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
    private final OptimizationConfiguration configuration;
    private final List<Var> additionalVariables = new ArrayList<>();
    private final List<GoalFactory> additionalGoalFactories = new ArrayList<>();

    private OptimizationBuilder(Prescription prescription) {
        this(prescription, new OptimizationConfiguration());
    }

    OptimizationBuilder(Prescription prescription, OptimizationConfiguration configuration) {
        if (prescription == null)
            throw new IllegalArgumentException("prescription must not be null");
        if (prescription._surfaces == null)
            throw new IllegalArgumentException("prescription must be built before optimization");
        this.prescription = prescription;
        this.configuration = configuration.copy();
    }

    public static OptimizationBuilder builder(Prescription prescription) {
        return new OptimizationBuilder(prescription);
    }

    /** The prescription this builder optimizes. */
    public Prescription prescription() {
        return prescription;
    }

    /** Free text describing the setup: written into a trial, and shown when it runs. */
    public OptimizationBuilder description(String description) {
        configuration.description = description;
        return this;
    }

    public String description() {
        return configuration.description;
    }

    /**
     * Where LensTool2 puts the output of a run of this setup as a trial, relative to the
     * prescription's file unless absolute. Only a trial uses it.
     */
    public OptimizationBuilder outdir(String outdir) {
        configuration.outdir = outdir;
        return this;
    }

    public String outdir() {
        return configuration.outdir;
    }

    // ------------------------------------------------------------------
    // Configuration - what gets evaluated, and how finely
    // ------------------------------------------------------------------

    public OptimizationBuilder fields(double... fields) {
        configuration.fields = copy(fields);
        return this;
    }

    public OptimizationBuilder mtfFrequencies(int... frequencies) {
        configuration.mtfFrequencies = copy(frequencies);
        return this;
    }

    /** Use the prescription's wavelength weights; false assigns every wavelength weight 1.0. */
    public OptimizationBuilder weighted(boolean weighted) {
        configuration.weighted = weighted;
        return this;
    }

    /** Restrict enabled ray-aberration goals to the Fraunhofer d-line. */
    public OptimizationBuilder dLineOnly(boolean dLineOnly) {
        configuration.dLineOnly = dLineOnly;
        return this;
    }

    /**
     * Which configuration of a multi-configuration prescription to optimize; zero based,
     * and zero for a single-configuration lens.
     *
     * <p>A zoom prescription carries a thickness per configuration on the varying spaces,
     * and its own focal length, f-number and angle of view. This selects all of them
     * together: the analysis builds the model for that configuration, thickness variables
     * read and write that configuration's value, and the paraxial anchors target that
     * configuration's focal length and f-number.
     *
     * <p>Configurations are optimized one at a time. Nothing here couples them, so a space
     * moved for one configuration is not reconciled against the others - that has to be
     * checked separately.
     */
    public OptimizationBuilder scenario(int scenario) {
        if (scenario < 0)
            throw new IllegalArgumentException("scenario must be non-negative, got " + scenario);
        configuration.scenario = scenario;
        return this;
    }

    /**
     * How each rebuilt model establishes vignetting. See {@link Analysis#vignetting(VigType)}
     * for the trade-offs; {@link VigType#Paraxial} in particular leaves the sagittal pupil
     * unvignetted and breaks on-axis rotational symmetry.
     */
    public OptimizationBuilder vignetting(VigType vigType) {
        configuration.vigType = vigType == null ? VigType.None : vigType;
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
        configuration.freezeVignetting = freeze;
        return this;
    }

    /**
     * Configure the ordinary Gaussian-quadrature spot pattern shared by spot and
     * geometric-MTF analyses. Contrast uses its separate sheared-pupil pattern.
     */
    public OptimizationBuilder gaussianQuadratureSampling(int rings, int spokes) {
        return gaussianQuadratureSampling(rings, spokes, 0.0);
    }

    /** Configure Gaussian quadrature for a concentric annular pupil. */
    public OptimizationBuilder gaussianQuadratureSampling(
            int rings, int spokes, double innerPupilRadius) {
        configuration.gaussianSampling(rings, spokes, innerPupilRadius);
        return this;
    }

    /**
     * Whether Gaussian-quadrature spot rays are rejected when they cross a physical
     * surface aperture. Disable this with frozen vignetting to optimize a fixed
     * factor-defined pupil, matching the usual Zemax GQ merit-function behaviour.
     * Grid and hexapolar sampling always retain physical aperture checking.
     */
    public OptimizationBuilder checkSpotApertures(boolean check) {
        configuration.checkSpotApertures = check;
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
        configuration.useHexapolarSpotPattern = true;
        configuration.hexapolarSpotRays = numRays;
        return this;
    }

    public OptimizationBuilder contrastSampling(int rings, int spokes) {
        if (rings < 1 || spokes < 1)
            throw new IllegalArgumentException("contrast rings and spokes must be at least 1");
        configuration.contrastRings = rings;
        configuration.contrastSpokes = spokes;
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
        configuration.calibrateContrastFrequency = value;
        return this;
    }

    /**
     * Iteratively aim each sheared contrast ray so its separation from the reference
     * ray is realised on the exit-pupil reference sphere. This directly accounts for
     * pupil aberration, including cross-axis displacement.
     *
     * <p>This is mutually exclusive with {@link #calibrateContrastFrequency(boolean)}.
     * Off by default because it changes the sampled rays and costs additional traces.
     */
    public OptimizationBuilder aimContrastAtExitPupil() {
        return aimContrastAtExitPupil(true);
    }

    public OptimizationBuilder aimContrastAtExitPupil(boolean value) {
        configuration.aimContrastAtExitPupil = value;
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
        configuration.centerContrastResiduals = value;
        return this;
    }

    // ------------------------------------------------------------------
    // Variables - what the solver is allowed to change
    // ------------------------------------------------------------------

    public OptimizationBuilder varyCurvatures(int... surfaces) {
        configuration.curvatureSurfaces = copy(surfaces);
        configuration.allCurvatureSurfaces = false;
        configuration.curvatureExclusions = new int[0];
        return this;
    }

    /**
     * Vary every curved optical surface. Aperture/field stops and surfaces
     * whose radius is zero (and therefore intentionally flat) are excluded.
     */
    public OptimizationBuilder varyAllCurvatures() {
        return varyAllCurvaturesExcept();
    }

    /**
     * Vary every surface {@link #varyAllCurvatures()} would, apart from the listed ones.
     * Saves spelling out a long list when only a few surfaces are to stay as they are.
     */
    public OptimizationBuilder varyAllCurvaturesExcept(int... surfaces) {
        configuration.curvatureSurfaces = new int[0];
        configuration.allCurvatureSurfaces = true;
        configuration.curvatureExclusions = copy(surfaces);
        return this;
    }

    public OptimizationBuilder varyThicknesses(int... surfaces) {
        configuration.thicknessSurfaces = copy(surfaces);
        configuration.allThicknessSurfaces = false;
        configuration.thicknessExclusions = new int[0];
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
        return varyAllThicknessesExcept();
    }

    /** Vary every thickness {@link #varyAllThicknesses()} would, apart from the listed surfaces'. */
    public OptimizationBuilder varyAllThicknessesExcept(int... surfaces) {
        configuration.thicknessSurfaces = new int[0];
        configuration.allThicknessSurfaces = true;
        configuration.thicknessExclusions = copy(surfaces);
        return this;
    }

    /** Thickness of a surface for the scenario this builder targets. */
    private double thicknessOf(int surface) {
        var definition = prescription._surfaces[surface];
        return definition._thickness_by_scenario != null
                ? definition._thickness_by_scenario[configuration.scenario]
                : definition._thickness;
    }

    /** Effective focal length this scenario is anchored to. */
    private double focalLengthOf() {
        return prescription._focal_length_by_scenario != null
                ? prescription._focal_length_by_scenario[configuration.scenario]
                : prescription._focal_length;
    }

    /** F-number this scenario is anchored to. */
    private double fNumberOf() {
        return prescription._f_number_by_scenario != null
                ? prescription._f_number_by_scenario[configuration.scenario]
                : prescription._fno;
    }

    /**
     * Reject a scenario the prescription does not define, rather than letting it surface
     * as an array index failure from somewhere inside the solve.
     */
    private void validateScenario() {
        if (configuration.scenario == 0) return;
        int available = scenarioCount();
        if (configuration.scenario >= available)
            throw new IllegalArgumentException("scenario " + configuration.scenario
                    + " requested but the prescription defines " + available
                    + (available == 1 ? " (it is not multi-configuration)" : ""));
    }

    /** Number of configurations the prescription defines; 1 when it is not a zoom. */
    private int scenarioCount() {
        int count = 1;
        if (prescription._focal_length_by_scenario != null)
            count = Math.max(count, prescription._focal_length_by_scenario.length);
        for (var surface : prescription._surfaces)
            if (surface._thickness_by_scenario != null)
                count = Math.max(count, surface._thickness_by_scenario.length);
        return count;
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
        configuration.includeExistingAspherics = include;
        return this;
    }

    /**
     * Vary a surface's conic constant, making the surface an asphere if it is not one.
     * A surface given explicit terms, here or through
     * {@link #varyAsphericCoefficient(int, int)}, is left to them rather than to
     * {@link #varyExistingAspherics()}.
     */
    public OptimizationBuilder varyConic(int surface) {
        return addAsphericTerm(surface, -1, null);
    }

    /**
     * Vary one aspheric coefficient, {@code _coeffs[index]} of the surface: on an even
     * asphere the coefficient of r^(2(index+1)), so index 1 is A4; on an odd asphere the
     * coefficient of r^(index+1), so index 2 is A3. A spherical surface becomes an asphere of
     * the type the prescription already uses, even when it has none, and a coefficient the
     * surface does not have starts at zero.
     * <p>The variable is the coefficient times a scale, so the solver works with values of
     * order one. An existing coefficient is scaled by {@link #scalingFor(double)}; one
     * starting at zero by 10^round(log10 h^n), h being half the surface's diameter and n the
     * power of r, so that one unit moves the sag at the rim by about one lens unit.
     */
    public OptimizationBuilder varyAsphericCoefficient(int surface, int index) {
        return addAsphericTerm(surface, index, null);
    }

    /** Vary one aspheric coefficient, as {@link #varyAsphericCoefficient(int, int)}, with the given scale. */
    public OptimizationBuilder varyAsphericCoefficient(int surface, int index, double scale) {
        if (!Double.isFinite(scale) || scale <= 0.0)
            throw new IllegalArgumentException("the scale of an aspheric coefficient must be finite and positive");
        return addAsphericTerm(surface, index, scale);
    }

    private OptimizationBuilder addAsphericTerm(int surface, int index, Double scale) {
        if (surface < 0 || surface >= prescription._surfaces.length)
            throw new IllegalArgumentException("aspheric surface is out of range: " + surface);
        var definition = prescription._surfaces[surface];
        if (definition.is_aperture_stop() || definition.is_field_stop())
            throw new IllegalArgumentException("surface " + surface + " is a stop; it cannot be aspheric");
        for (AsphericTerm term : configuration.asphericTerms)
            if (term.surface() == surface && term.index() == index)
                throw new IllegalArgumentException((index < 0 ? "the conic constant"
                        : "coefficient " + index) + " of surface " + surface + " is varied twice");
        if (index >= 0) {
            powerOf(asphereTypeOf(surface), index);
            if (scale == null && coefficientOf(surface, index) == 0.0 && !(definition._diameter > 0.0))
                throw new IllegalArgumentException("surface " + surface + " has no diameter to derive a scale for coefficient "
                        + index + " from; give the coefficient a scale");
        }
        configuration.asphericTerms.add(new AsphericTerm(surface, index, scale));
        return this;
    }

    /** The surface's asphere type, or the one it will be made: the prescription's own, else even. */
    private int asphereTypeOf(int surface) {
        var definition = prescription._surfaces[surface];
        if (definition.is_aspheric())
            return definition._asph_type;
        if (prescription.has_odd_aspheric())
            return SurfaceType.ASPH_ODD;
        if (prescription.has_even_a2_aspheric())
            return SurfaceType.ASPH_EVEN_A2;
        return SurfaceType.ASPH_EVEN;
    }

    /** The power of r a coefficient multiplies, rejecting an index the asphere type does not have. */
    private static int powerOf(int asphereType, int index) {
        switch (asphereType) {
            case SurfaceType.ASPH_ODD -> {
                if (index >= 2)
                    return index + 1;
                throw new IllegalArgumentException("coefficient " + index
                        + " is not a term of an odd asphere, whose terms start at index 2, the A3 term");
            }
            case SurfaceType.ASPH_EVEN_A2 -> {
                return 2 * (index + 1);
            }
            default -> {
                if (index >= 1)
                    return 2 * (index + 1);
                throw new IllegalArgumentException("coefficient " + index
                        + " is not a term of an even asphere, whose terms start at index 1, the A4 term");
            }
        }
    }

    private double coefficientOf(int surface, int index) {
        double[] coefficients = prescription._surfaces[surface]._coeffs;
        return coefficients != null && index < coefficients.length ? coefficients[index] : 0.0;
    }

    private boolean hasExplicitAsphericTerms(int surface) {
        for (AsphericTerm term : configuration.asphericTerms)
            if (term.surface() == surface)
                return true;
        return false;
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
        configuration.mtfGoals.addAll(Arrays.asList(goals));
        return this;
    }

    public OptimizationBuilder contrastGoals(ContrastGoals... goals) {
        if (goals == null)
            throw new IllegalArgumentException("contrast goals must not be null");
        configuration.contrastGoals.addAll(Arrays.asList(goals));
        return this;
    }

    /**
     * Hold sagittal and tangential contrast in balance at the selected fields, at the
     * nominal balance weight. One flag per configured field, in field order; a false adds
     * no explicit balance constraint there, which is usually what the outermost field
     * wants. The ordinary contrast residuals still constrain both meridians.
     *
     * <p>Applies to every configured contrast frequency, so this adds one residual per
     * enabled field per frequency. See {@link GoalContrastBalance} for what it measures
     * and why the contrast merit does not already care.
     *
     * <p>Two settings that surprise: the sagittal and tangential contrast weights set the
     * <em>ratio</em> this goal targets rather than merely prioritising a meridian, and on
     * axis it cannot balance anything - with unequal weights it quietly becomes an axial
     * contrast goal instead. Both are covered on {@link GoalContrastBalance}.
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
        configuration.contrastBalanceFields = fields.clone();
        configuration.contrastBalanceWeight = weight;
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
        configuration.spotRmsGoals = new SpotGoals(targets, weights);
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
        configuration.addSpotDeviationGoals = true;
        configuration.spotDeviationXWeights = copy(fieldWeights);
        configuration.spotDeviationYWeights = copy(fieldWeights);
        return this;
    }

    /** Assign separate per-field weights to the signed X and Y spot deviations. */
    public OptimizationBuilder spotDeviationGoals(double[] xWeights, double[] yWeights) {
        configuration.addSpotDeviationGoals = true;
        configuration.spotDeviationXWeights = copy(xWeights);
        configuration.spotDeviationYWeights = copy(yWeights);
        return this;
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets) {
        return spotMaxRadiusGoals(targets, null);
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets, double[] weights) {
        configuration.spotMaxRadiusGoals = new SpotGoals(targets, weights);
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
        configuration.addRayAberrationGoals = enabled;
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

    /** Target a first-order quantity at weight 1; see {@link #paraxialGoal(int, double, double)}. */
    public OptimizationBuilder paraxialGoal(int paraxId, double target) {
        return paraxialGoal(paraxId, target, 1.0);
    }

    /**
     * Target a first-order quantity. Every setup already holds the effective focal length
     * and f-number at the prescription's values for the scenario, at weight 1; a goal for
     * either replaces that one rather than adding a second.
     * @param paraxId a {@link ParaxHelper} id, such as {@link ParaxHelper#Back_focal_length}
     */
    public OptimizationBuilder paraxialGoal(int paraxId, double target, double weight) {
        if (paraxId < 0 || paraxId >= ParaxHelper.Names.length)
            throw new IllegalArgumentException("unknown paraxial quantity: " + paraxId);
        if (!Double.isFinite(target))
            throw new IllegalArgumentException("paraxial target must be finite");
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("paraxial weight must be finite and non-negative");
        for (ParaxialGoal goal : configuration.paraxialGoals)
            if (goal.paraxId() == paraxId)
                throw new IllegalArgumentException("there is already a goal for " + ParaxHelper.Names[paraxId]);
        configuration.paraxialGoals.add(new ParaxialGoal(paraxId, target, weight));
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
        configuration.thicknessConstraintWeight = weight;
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
     *
     * <p>Applies to the gap after a varied thickness <em>and</em> to both gaps beside a
     * surface with a varied radius, conic constant or aspheric coefficient; see
     * {@link #edgeAffectedGaps}. A setup that varies no thickness at all is still covered.
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
        configuration.edgeThicknessConstraintWeight = weight;
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
        configuration.curvatureConstraintWeight = weight;
        return this;
    }

    // ------------------------------------------------------------------
    // Build
    // ------------------------------------------------------------------

    public OptimizationSetup build() {
        validate();
        Analysis analysis = new Analysis(prescription, copy(configuration.fields), copy(configuration.mtfFrequencies), configuration.scenario);
        List<Var> variables = buildVariables();
        List<Goal> goals = buildGoals(analysis, variables);
        if (goals.size() < variables.size())
            throw new IllegalArgumentException(
                    "optimization requires at least as many goals as variables: "
                            + goals.size() + " goals for " + variables.size() + " variables"
                            + "; add optical goals or enable rayAberrationGoals()");
        analysis.vignetting(configuration.vigType)
                .freezing_vignetting(configuration.freezeVignetting)
                .checking_spot_apertures(configuration.checkSpotApertures);
        configureSpotPattern(analysis, goals);
        configureContrastAnalysis(analysis, goals);
        configureRequiredAnalyses(analysis, goals);
        return new OptimizationSetup(analysis, variables.toArray(new Var[0]), goals.toArray(new Goal[0]));
    }

    /**
     * The gaps whose edge separation some varied parameter can move, sorted and
     * deduplicated: the gap a varied thickness <em>is</em>, and <em>both</em> gaps beside
     * a surface whose shape is varied.
     *
     * <p>The second half is easy to miss and was missed here originally, in both the
     * penalty and the bound form. The separation is
     * {@code gap(h) = t + sag_next(h) - sag_this(h)}, so moving a radius, conic constant
     * or aspheric coefficient closes the gap on either side of that surface with no
     * thickness variable involved anywhere. A setup that varies curvatures and aspherics
     * but no thicknesses therefore got <em>no</em> edge protection at all, silently -
     * which is precisely the configuration in which curvature is the only freedom, and
     * curvature-driven crossing is the failure the edge constraint exists to catch.
     *
     * <p>Out-of-range gap indices produced at either end of the surface list are left in
     * and rejected by the caller's {@code is_constrainable} / {@code is_boundable} check.
     */
    private static Set<Integer> edgeAffectedGaps(List<Var> variables) {
        Set<Integer> gaps = new TreeSet<>();
        for (Var variable : variables) {
            if (variable instanceof VarThickness thickness) {
                gaps.add(thickness._surface_id);
                continue;
            }
            int surface;
            if (variable instanceof VarRadius radius) surface = radius._surface_id;
            else if (variable instanceof VarAsphK conic) surface = conic._surface_id;
            else if (variable instanceof VarAsphCoeff coefficient) surface = coefficient._surface_id;
            else continue;
            gaps.add(surface - 1);
            gaps.add(surface);
        }
        return gaps;
    }

    private void configureContrastAnalysis(Analysis analysis, List<Goal> goals) {
        if (configuration.contrastGoals.isEmpty()) return;
        if (configuration.calibrateContrastFrequency && configuration.aimContrastAtExitPupil)
            throw new IllegalArgumentException(
                    "Contrast frequency calibration and exit-pupil aiming are mutually exclusive");
        int[] frequencies = configuration.contrastGoals.stream().mapToInt(goal -> goal.frequency).toArray();
        analysis.using_contrast_analysis(frequencies, configuration.contrastRings, configuration.contrastSpokes);
        analysis.calibrating_contrast_frequency(configuration.calibrateContrastFrequency);
        analysis.aiming_contrast_at_exit_pupil(configuration.aimContrastAtExitPupil);
        analysis.centering_contrast_residuals(configuration.centerContrastResiduals);
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
        if (configuration.addSpotDeviationGoals) {
            analysis.using_gauss_quadrature_pattern(
                            configuration.gaussianQuadratureRings, configuration.gaussianQuadratureSpokes,
                            configuration.gaussianQuadratureInnerRadius)
                    .retaining_failed_spot_rays(true);
        }
        else if (configuration.useHexapolarSpotPattern || hasSpotMaxRadiusGoal)
            analysis.using_hexapolar_pattern(configuration.hexapolarSpotRays);
        else
            analysis.using_gauss_quadrature_pattern(
                    configuration.gaussianQuadratureRings, configuration.gaussianQuadratureSpokes,
                    configuration.gaussianQuadratureInnerRadius);
    }

    private List<Var> buildVariables() {
        List<Var> result = new ArrayList<>();
        if (configuration.allCurvatureSurfaces) {
            for (int surface = 0; surface < prescription._surfaces.length; surface++) {
                var definition = prescription._surfaces[surface];
                if (!definition.is_aperture_stop() && !definition.is_field_stop()
                        && definition._radius != 0.0
                        && !contains(configuration.curvatureExclusions, surface))
                    result.add(new VarRadius(prescription, surface));
            }
        } else {
            for (int surface : configuration.curvatureSurfaces)
                result.add(new VarRadius(prescription, surface));
        }
        if (configuration.allThicknessSurfaces) {
            for (int surface = 0; surface < prescription._surfaces.length; surface++) {
                // A zero thickness is a coincident surface, not a space to open up, and
                // it gives the fractional ConstraintThickness no base to work from.
                if (thicknessOf(surface) != 0.0 && !contains(configuration.thicknessExclusions, surface))
                    result.add(new VarThickness(prescription, surface, configuration.scenario));
            }
        } else {
            for (int surface : configuration.thicknessSurfaces)
                result.add(new VarThickness(prescription, surface, configuration.scenario));
        }
        if (configuration.includeExistingAspherics) {
            for (int surfaceId = 0; surfaceId < prescription._surfaces.length; surfaceId++) {
                if (hasExplicitAsphericTerms(surfaceId))
                    continue;
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
        result.addAll(explicitAsphericVariables());
        result.addAll(additionalVariables);
        return result;
    }

    /**
     * The variables for the explicitly varied aspheric terms. A spherical surface is made an
     * asphere, and a coefficient array too short for a term is extended with zeros, so the
     * variables have somewhere to read from and write to.
     */
    private List<Var> explicitAsphericVariables() {
        List<Var> result = new ArrayList<>();
        for (AsphericTerm term : configuration.asphericTerms) {
            var surface = prescription._surfaces[term.surface()];
            if (!surface.is_aspheric())
                surface._asph_type = asphereTypeOf(term.surface());
            if (surface._coeffs == null)
                surface._coeffs = new double[0];
            if (term.index() < 0) {
                result.add(new VarAsphK(prescription, term.surface()));
                continue;
            }
            if (surface._coeffs.length <= term.index())
                surface._coeffs = Arrays.copyOf(surface._coeffs, term.index() + 1);
            double value = surface._coeffs[term.index()];
            double scale;
            if (term.scale() != null)
                scale = term.scale();
            else if (value != 0.0)
                scale = scalingFor(value);
            else
                scale = Math.pow(10.0, Math.round(powerOf(surface._asph_type, term.index())
                        * Math.log10(surface._diameter / 2.0)));
            result.add(new VarAsphCoeff(prescription, term.surface(), term.index(), scale));
        }
        return result;
    }

    private List<Goal> buildGoals(Analysis analysis, List<Var> variables) {
        List<Goal> result = new ArrayList<>();
        // Anchor the varied parameters to where they started. Built from the variable
        // list so the goals attach to exactly what is free to move, and built here while
        // the prescription still holds its original values.
        if (configuration.thicknessConstraintWeight != null) {
            for (Var variable : variables)
                if (variable instanceof VarThickness thickness)
                    result.add(new ConstraintThickness(analysis, thickness._surface_id,
                            configuration.thicknessConstraintWeight));
        }
        if (configuration.edgeThicknessConstraintWeight != null) {
            for (int gap : edgeAffectedGaps(variables))
                if (ConstraintEdgeThickness.is_constrainable(analysis, gap))
                    result.add(new ConstraintEdgeThickness(analysis, gap,
                            configuration.edgeThicknessConstraintWeight));
        }
        if (configuration.curvatureConstraintWeight != null) {
            for (Var variable : variables)
                if (variable instanceof VarRadius radius)
                    result.add(new ConstraintCurvature(analysis, radius._surface_id,
                            configuration.curvatureConstraintWeight));
        }
        for (MtfGoals curve : configuration.mtfGoals) {
            for (int field = 0; field < configuration.fields.length; field++) {
                result.add(new GoalGeoMTF(analysis, field + 1, Orientation.SAGITTAL, curve.frequency,
                        curve.sagittal[field] / 100.0, curve.sagittalWeights[field]));
                result.add(new GoalGeoMTF(analysis, field + 1, Orientation.TANGENTIAL, curve.frequency,
                        curve.tangential[field] / 100.0, curve.tangentialWeights[field]));
            }
        }

        int contrastSamples = configuration.contrastRings * configuration.contrastSpokes;
        for (int contrast_index = 0; contrast_index < configuration.contrastGoals.size(); contrast_index++) {
            ContrastGoals curve = configuration.contrastGoals.get(contrast_index);
            for (int field = 0; field < configuration.fields.length; field++) {
                for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                    double wavelengthWeight = configuration.weighted ? prescription._wts[wavelength] : 1.0;
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

        if (configuration.contrastBalanceFields != null) {
            double[] wavelengthWeights = new double[prescription._wvls.length];
            for (int w = 0; w < wavelengthWeights.length; w++)
                wavelengthWeights[w] = configuration.weighted ? prescription._wts[w] : 1.0;
            for (int contrast_index = 0; contrast_index < configuration.contrastGoals.size(); contrast_index++) {
                ContrastGoals curve = configuration.contrastGoals.get(contrast_index);
                for (int field = 0; field < configuration.fields.length; field++) {
                    if (!configuration.contrastBalanceFields[field]) continue;
                    result.add(new GoalContrastBalance(analysis, contrast_index, curve.frequency,
                            field + 1, wavelengthWeights,
                            curve.sagittalWeights[field], curve.tangentialWeights[field],
                            configuration.contrastBalanceWeight));
                }
            }
        }

        if (configuration.spotRmsGoals != null) {
            for (int field = 0; field < configuration.fields.length; field++)
                result.add(new GoalSpotRMS(analysis, field + 1,
                        configuration.spotRmsGoals.targets[field], configuration.spotRmsGoals.weights[field]));
        }
        if (configuration.addSpotDeviationGoals) {
            int samples = configuration.gaussianQuadratureRings * configuration.gaussianQuadratureSpokes;
            for (int field = 0; field < configuration.fields.length; field++) {
                for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                    double wavelengthWeight = configuration.weighted ? prescription._wts[wavelength] : 1.0;
                    for (int sample = 0; sample < samples; sample++) {
                        result.add(new GoalSpotDeviation(analysis, field + 1, wavelength,
                                sample, Orientation.X,
                                wavelengthWeight * configuration.spotDeviationXWeights[field]));
                        result.add(new GoalSpotDeviation(analysis, field + 1, wavelength,
                                sample, Orientation.Y,
                                wavelengthWeight * configuration.spotDeviationYWeights[field]));
                    }
                }
            }
        }
        if (configuration.spotMaxRadiusGoals != null) {
            for (int field = 0; field < configuration.fields.length; field++)
                result.add(new GoalSpotMaxRadius(analysis, field + 1,
                        configuration.spotMaxRadiusGoals.targets[field], configuration.spotMaxRadiusGoals.weights[field]));
        }

        // Anchor first-order properties to the requested prescription values, unless the
        // caller has set targets of its own.
        result.add(anchor(analysis, ParaxHelper.Effective_focal_length, focalLengthOf()));
        result.add(anchor(analysis, ParaxHelper.Fno, fNumberOf()));

        if (configuration.addRayAberrationGoals) {
            for (int field = 1; field <= configuration.fields.length; field++) {
                for (int orientation = Orientation.SAGITTAL; orientation <= Orientation.TANGENTIAL; orientation++) {
                    for (int wavelength = 0; wavelength < prescription._wvls.length; wavelength++) {
                        if (configuration.dLineOnly && !sameWavelength(prescription._wvls[wavelength], Glass.d))
                            continue;
                        double weight = configuration.weighted ? prescription._wts[wavelength] : 1.0;
                        for (int sample = 0; sample < RAY_FAN_SAMPLES; sample++)
                            result.add(new GoalRayAberration(analysis, field, orientation, sample,
                                    prescription._wvls[wavelength], 0.0, weight));
                    }
                }
            }
        }
        for (ParaxialGoal goal : configuration.paraxialGoals)
            if (goal.paraxId() != ParaxHelper.Effective_focal_length && goal.paraxId() != ParaxHelper.Fno)
                result.add(new GoalParax(analysis, goal.paraxId(), goal.target(), goal.weight()));
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
        if (configuration.fields == null || configuration.fields.length == 0)
            throw new IllegalArgumentException("at least one field is required");
        validateScenario();
        if (configuration.contrastBalanceFields != null) {
            if (configuration.contrastBalanceFields.length != configuration.fields.length)
                throw new IllegalArgumentException(
                        "contrast balance needs one flag per field: " + configuration.fields.length
                                + " fields but " + configuration.contrastBalanceFields.length + " flags");
            if (configuration.contrastGoals.isEmpty())
                throw new IllegalArgumentException(
                        "contrast balance goals require contrast goals to balance");
        }
        for (double field : configuration.fields)
            if (!Double.isFinite(field) || field < 0.0 || field > 1.0)
                throw new IllegalArgumentException("fields must be finite values between 0 and 1");
        if (configuration.fields[0] != 0.0)
            throw new IllegalArgumentException("the first field must be 0.0");

        if (configuration.mtfFrequencies == null || configuration.mtfFrequencies.length == 0)
            throw new IllegalArgumentException("at least one MTF frequency is required");
        Set<Integer> frequencies = new HashSet<>();
        for (int frequency : configuration.mtfFrequencies) {
            if (frequency <= 0 || !frequencies.add(frequency))
                throw new IllegalArgumentException("MTF frequencies must be positive and unique");
        }
        Set<Integer> goalFrequencies = new HashSet<>();
        for (MtfGoals curve : configuration.mtfGoals) {
            if (!frequencies.contains(curve.frequency))
                throw new IllegalArgumentException("MTF goal frequency was not requested for measurement: " + curve.frequency);
            if (!goalFrequencies.add(curve.frequency))
                throw new IllegalArgumentException("duplicate MTF goal frequency: " + curve.frequency);
            curve.validate(configuration.fields.length);
        }
        Set<Integer> contrastFrequencies = new HashSet<>();
        for (ContrastGoals curve : configuration.contrastGoals) {
            if (curve == null)
                throw new IllegalArgumentException("contrast goals must not contain null");
            if (curve.frequency <= 0 || !contrastFrequencies.add(curve.frequency))
                throw new IllegalArgumentException("contrast frequencies must be positive and unique");
            curve.validate(configuration.fields.length);
        }
        if (configuration.spotRmsGoals != null)
            configuration.spotRmsGoals.validate(configuration.fields.length, "spot RMS");
        if (configuration.addSpotDeviationGoals) {
            MtfGoals.validateWeights(configuration.spotDeviationXWeights, configuration.fields.length,
                    "spot deviation X weights");
            MtfGoals.validateWeights(configuration.spotDeviationYWeights, configuration.fields.length,
                    "spot deviation Y weights");
            if (configuration.spotRmsGoals != null)
                throw new IllegalArgumentException(
                        "aggregate spot RMS goals and per-ray spot deviation goals cannot both be enabled");
            if (configuration.spotMaxRadiusGoals != null || configuration.useHexapolarSpotPattern)
                throw new IllegalArgumentException(
                        "spot deviation goals require Gaussian-quadrature spot sampling");
        }
        if (configuration.spotMaxRadiusGoals != null)
            configuration.spotMaxRadiusGoals.validate(configuration.fields.length, "spot maximum radius");
        validateSurfaces(configuration.curvatureSurfaces, "curvature");
        validateSurfaces(configuration.thicknessSurfaces, "thickness");
        validateSurfaces(configuration.curvatureExclusions, "excluded curvature");
        validateSurfaces(configuration.thicknessExclusions, "excluded thickness");
        if (configuration.addRayAberrationGoals && configuration.dLineOnly
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

    /** A first-order goal: the given target and weight, or the prescription's value at weight 1. */
    private GoalParax anchor(Analysis analysis, int paraxId, double prescribed) {
        for (ParaxialGoal goal : configuration.paraxialGoals)
            if (goal.paraxId() == paraxId)
                return new GoalParax(analysis, paraxId, goal.target(), goal.weight());
        return new GoalParax(analysis, paraxId, prescribed, 1.0);
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values)
            if (v == value)
                return true;
        return false;
    }

    private static double[] copy(double[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    private static int[] copy(int[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    public static final class MtfGoals {
        final int frequency;
        final double[] sagittal;
        final double[] tangential;
        final double[] sagittalWeights;
        final double[] tangentialWeights;

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
        final int frequency;
        final double[] sagittalWeights;
        final double[] tangentialWeights;

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

    static final class SpotGoals {
        final double[] targets;
        final double[] weights;

        SpotGoals(double[] targets, double[] weights) {
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

    /** An explicitly varied aspheric term: the conic constant when index is -1, else _coeffs[index]. */
    record AsphericTerm(int surface, int index, Double scale) {}

    /** A first-order goal on a {@link ParaxHelper} quantity. */
    record ParaxialGoal(int paraxId, double target, double weight) {}

    // ------------------------------------------------------------------
    // Writing - the setup as a [trial n] section
    // ------------------------------------------------------------------

    /**
     * This setup as a {@code [trial number]} section of a prescription file, which
     * {@link OptimizationTrial#read} reads back into an equivalent builder. Every setting
     * the trial's goals consult is written, its default included, so that a later change to a
     * default cannot change what a saved trial means; a setting nothing in the trial consults
     * is left out. Weights left out are 1, which is part of the format rather than a default. Variables and goals given as code, through
     * {@link #additionalVariables(Var...)} or {@link #additionalGoals(GoalFactory...)}, have
     * no written form, so a builder that uses them cannot be written.
     */
    public String toTrial(int number) {
        if (!additionalVariables.isEmpty() || !additionalGoalFactories.isEmpty())
            throw new IllegalStateException(
                    "variables and goals added as code have no written form, so this setup cannot be written as a trial");
        return configuration.toTrial(number);
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
