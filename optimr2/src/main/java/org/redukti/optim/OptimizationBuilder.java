package org.redukti.optim;

import org.redukti.rayoptics.seq.Glass;
import org.redukti.spec.Prescription;

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
    public static final int SAGITTAL = 0;
    public static final int TANGENTIAL = 1;
    private static final int RAY_FAN_SAMPLES = 10;

    private final Prescription prescription;
    private double[] fields;
    private int[] mtfFrequencies;
    private int[] curvatureSurfaces = new int[0];
    private boolean allCurvatureSurfaces;
    private int[] thicknessSurfaces = new int[0];
    private boolean includeExistingAspherics;
    private boolean weighted = true;
    private boolean dLineOnly;
    private final List<MtfGoals> mtfGoals = new ArrayList<>();
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

    public OptimizationBuilder fields(double... fields) {
        this.fields = copy(fields);
        return this;
    }

    public OptimizationBuilder mtfFrequencies(int... frequencies) {
        this.mtfFrequencies = copy(frequencies);
        return this;
    }

    public OptimizationBuilder curvatureSurfaces(int... surfaces) {
        this.curvatureSurfaces = copy(surfaces);
        this.allCurvatureSurfaces = false;
        return this;
    }

    /**
     * Vary every curved optical surface. Aperture/field stops and surfaces
     * whose radius is zero (and therefore intentionally flat) are excluded.
     */
    public OptimizationBuilder allCurvatureSurfaces() {
        this.allCurvatureSurfaces = true;
        return this;
    }

    public OptimizationBuilder thicknessSurfaces(int... surfaces) {
        this.thicknessSurfaces = copy(surfaces);
        return this;
    }

    /** Include nonzero conic constants and nonzero coefficients already present in the prescription. */
    public OptimizationBuilder includeExistingAspherics(boolean include) {
        this.includeExistingAspherics = include;
        return this;
    }

    /** Use the prescription's wavelength weights; false assigns every wavelength weight 1.0. */
    public OptimizationBuilder weighted(boolean weighted) {
        this.weighted = weighted;
        return this;
    }

    /** Restrict ray-aberration goals to the Fraunhofer d-line. */
    public OptimizationBuilder dLineOnly(boolean dLineOnly) {
        this.dLineOnly = dLineOnly;
        return this;
    }

    public OptimizationBuilder mtfGoals(MtfGoals... goals) {
        if (goals == null)
            throw new IllegalArgumentException("MTF goals must not be null");
        this.mtfGoals.addAll(Arrays.asList(goals));
        return this;
    }

    public OptimizationBuilder spotRmsGoals(double[] targets) {
        return spotRmsGoals(targets, null);
    }

    public OptimizationBuilder spotRmsGoals(double[] targets, double[] weights) {
        this.spotRmsGoals = new SpotGoals(targets, weights);
        return this;
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets) {
        return spotMaxRadiusGoals(targets, null);
    }

    public OptimizationBuilder spotMaxRadiusGoals(double[] targets, double[] weights) {
        this.spotMaxRadiusGoals = new SpotGoals(targets, weights);
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

    public OptimizationSetup build() {
        validate();
        Analysis analysis = new Analysis(prescription, copy(fields), copy(mtfFrequencies));
        List<Var> variables = buildVariables();
        List<Goal> goals = buildGoals(analysis);
        return new OptimizationSetup(analysis, variables.toArray(new Var[0]), goals.toArray(new Goal[0]));
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
        for (int surface : thicknessSurfaces)
            result.add(new VarThickness(prescription, surface));
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

    private List<Goal> buildGoals(Analysis analysis) {
        List<Goal> result = new ArrayList<>();
        for (MtfGoals curve : mtfGoals) {
            for (int field = 0; field < fields.length; field++) {
                result.add(new GeoMTF(analysis, field + 1, SAGITTAL, curve.frequency,
                        curve.sagittal[field] / 100.0, curve.sagittalWeights[field]));
                result.add(new GeoMTF(analysis, field + 1, TANGENTIAL, curve.frequency,
                        curve.tangential[field] / 100.0, curve.tangentialWeights[field]));
            }
        }

        if (spotRmsGoals != null) {
            for (int field = 0; field < fields.length; field++)
                result.add(new GoalSpotRMS(analysis, field + 1,
                        spotRmsGoals.targets[field], spotRmsGoals.weights[field]));
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

        for (int field = 1; field <= fields.length; field++) {
            for (int orientation = SAGITTAL; orientation <= TANGENTIAL; orientation++) {
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
        if (spotRmsGoals != null)
            spotRmsGoals.validate(fields.length, "spot RMS");
        if (spotMaxRadiusGoals != null)
            spotMaxRadiusGoals.validate(fields.length, "spot maximum radius");
        validateSurfaces(curvatureSurfaces, "curvature");
        validateSurfaces(thicknessSurfaces, "thickness");
        if (dLineOnly && Arrays.stream(prescription._wvls).noneMatch(w -> sameWavelength(w, Glass.d)))
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
