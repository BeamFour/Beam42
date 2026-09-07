package org.redukti.examples;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.redukti.optim.OptimizationBuilder.contrast;

/**
 * Opt-in experiment: do hard {@link Bound}s hold the Leica 75/2 layout better than the
 * weighted {@link Constraint} penalties, and at what optical cost?
 *
 * <p>This is the case the layout constraints were written for. Documentation/REVIEW.md
 * records the failure it is meant to prevent - "with every air space free it drove three
 * gaps negative, passing elements through each other and through the stop" - and open
 * question 2 records why the penalty form is awkward: the constraint block does not grow
 * with the optical block, so moving this setup from 4 fields to 11 took the contrast
 * residuals from 5184 to 14256 while the constraint count stayed put, and the weight that
 * was tuned at 4 fields no longer holds the same line. A bound has no weight to detune.
 *
 * <pre>{@code
 * mvn -o test -Dtest=LeicaLayoutBoundsTest -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Doptimization.leicaBounds=true
 * }</pre>
 *
 * <p>Add {@code -Doptimization.leicaBounds.fast=true} for a reduced configuration (3
 * fields, 1 frequency, 3x6 sampling) that exercises the same code in a fraction of the
 * time but is not the configuration the finding was made in. Select configurations with
 * {@code -Doptimization.leicaBounds.cases=lmder-tuned,dls-bounds}.
 *
 * <p>Results go to {@code rayoptics/target/leica-bounds/}. Nothing here asserts an
 * optical regression: the point is to produce the numbers, not to lock them.
 */
class LeicaLayoutBoundsTest {

    /** Floor as a fraction of the starting value, for both thickness and edge bounds. */
    private static final double BOUND_FRACTION = 0.5;

    private record Configuration(String name, boolean dls, String description) {}

    private static final List<Configuration> CONFIGURATIONS = List.of(
            new Configuration("lmder-tuned", false,
                    "LMDER, thickness penalty at 5.0, curvature and edge penalties at 1.0"),
            new Configuration("lmder-nominal", false,
                    "LMDER, every layout penalty at the nominal 1.0"),
            new Configuration("dls-nominal", true,
                    "DLS at Beam42 defaults, every layout penalty at the nominal 1.0"),
            new Configuration("dls-bounds", true,
                    "DLS at Beam42 defaults, layout penalties replaced by hard bounds at "
                            + BOUND_FRACTION + " of start"),
            new Configuration("dls-bounds-damped", true,
                    "as dls-bounds, but starting damping 1e-2 rather than 1e-6, to see"
                            + " whether the boundary is reached less abruptly"),
            new Configuration("dls-bounds-trust", true,
                    "as dls-bounds, with a 1mm trust radius, to see whether capping the"
                            + " first step keeps the solve off the bounds long enough"));

    @Test
    void compareLayoutPenaltiesWithHardBounds() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("optimization.leicaBounds"));
        boolean fast = Boolean.getBoolean("optimization.leicaBounds.fast");
        List<String> selected = List.of(System.getProperty("optimization.leicaBounds.cases",
                "lmder-tuned,lmder-nominal,dls-nominal,dls-bounds,dls-bounds-damped").split(","));
        int dlsIterations = Integer.getInteger("optimization.leicaBounds.iterations", 100);
        Path output = repositoryRoot().resolve("rayoptics/target/leica-bounds");
        Files.createDirectories(output);

        StringBuilder csv = new StringBuilder("configuration,solver,variables,goals,bounds,seconds,"
                + "analysisEvaluations,iterations,jacobians,status,stopReason,"
                + "opticalRmsBefore,opticalRmsAfter,minThicknessFraction,minEdgeFraction,minEdgeMm,"
                + "crossedGaps,efl,fno,spotMean,mtf40Mean\n");
        StringBuilder log = new StringBuilder();

        Layout start = null;
        Double startOptical = null;
        for (Configuration configuration : CONFIGURATIONS) {
            if (!selected.contains(configuration.name)) continue;

            var prescription = LeicaApo75mmMandler.getPrescription(
                    ContrastProbes.leicaInputPath(), false, false);
            var setup = setupFor(configuration, prescription, fast);
            var analysis = setup.analysis();
            var merit = setup.meritFunction(false);
            for (int warmup = 0; warmup < 3; warmup++) analysis.compute();

            // A yardstick independent of the configuration's own constraint block, which
            // differs between rows and makes their merit RMS incomparable. Same optical
            // goals, no constraints, over the same prescription object but its own
            // Analysis, so it must be recomputed each time it is read.
            var yardstick = opticalSetup(prescription, fast);
            Layout before = Layout.of(prescription);
            if (start == null) start = before;
            double opticalBefore = opticalRms(yardstick);
            if (startOptical == null) startOptical = opticalBefore;
            else assertEquals(startOptical, opticalBefore, 1e-12, "starting optical merit must match");

            var options = DampedLeastSquaresSolver.defaultOptions();
            options.maxIterations = dlsIterations;
            if (configuration.name.equals("dls-bounds-damped")) options.damping = new double[]{1e-2};
            if (configuration.name.equals("dls-bounds-trust")) options.trustRadii = new double[]{1.0};
            String tolerance = System.getProperty("optimization.leicaBounds.ctol");
            if (tolerance != null) options.constraintTolerance = Double.parseDouble(tolerance);
            Solver solver = configuration.dls
                    ? setup.dampedLeastSquaresSolver(options)
                    : merit.getSolver();
            System.out.println("LEICA-BOUNDS starting " + configuration.name
                    + " vars=" + setup.variables().length + " goals=" + setup.goals().length
                    + " bounds=" + setup.bounds().length + " opticalRms=" + opticalBefore);

            long evaluationsBefore = analysis.getEvaluationCount();
            long started = System.nanoTime();
            int status = solver.solve();
            double seconds = (System.nanoTime() - started) / 1e9;
            long evaluations = analysis.getEvaluationCount() - evaluationsBefore;

            var dls = solver instanceof DampedLeastSquaresSolver d ? d : null;
            String reason = dls == null ? "MINPACK info=" + status : dls.result().message();
            double opticalAfter = opticalRms(yardstick);
            Layout after = Layout.of(prescription);

            // Optical measurements outside the solve timer, independent of the merit.
            analysis.required_analyses(true, true, true);
            analysis.compute();
            double[] spots = new double[analysis._spots.length];
            for (int i = 0; i < spots.length; i++) spots[i] = analysis._spots[i].get_mean_radius();
            double spotMean = mean(spots);
            int highest = analysis._mtfs.length - 1;
            double mtfMean = 0.5 * (mean(analysis._mtfs[highest].sag_mtf_by_field)
                    + mean(analysis._mtfs[highest].tan_mtf_by_field));

            csv.append(configuration.name).append(',').append(dls == null ? "lmder" : "dls")
                    .append(',').append(setup.variables().length)
                    .append(',').append(setup.goals().length)
                    .append(',').append(setup.bounds().length)
                    .append(',').append(fmt(seconds)).append(',').append(evaluations)
                    .append(',').append(dls == null ? "" : dls.result().iterations())
                    .append(',').append(dls == null ? "" : dls.result().njev())
                    .append(',').append(status).append(',').append(reason)
                    .append(',').append(fmt(opticalBefore)).append(',').append(fmt(opticalAfter))
                    .append(',').append(fmt(after.minThicknessFraction(before)))
                    .append(',').append(fmt(after.minEdgeFraction(before)))
                    .append(',').append(fmt(after.minEdge()))
                    .append(',').append(after.crossedGaps())
                    .append(',').append(fmt(analysis._pfo[ParaxHelper.Effective_focal_length]))
                    .append(',').append(fmt(analysis._pfo[ParaxHelper.Fno]))
                    .append(',').append(fmt(spotMean)).append(',').append(fmt(mtfMean))
                    .append('\n');

            log.append("== ").append(configuration.name).append(" - ")
                    .append(configuration.description).append('\n')
                    .append("stop: ").append(reason).append(" status=").append(status).append('\n')
                    .append(after.report(before)).append('\n');
            if (dls != null) {
                StringBuilder history = new StringBuilder(
                        "iteration,cost,violation,alpha,stepNorm,dampingAttempts,activeCount\n");
                int iteration = 0;
                for (var h : dls.result().history())
                    history.append(++iteration).append(',').append(h.cost())
                            .append(',').append(h.constraintViolation())
                            .append(',').append(h.alpha()).append(',').append(h.stepNorm())
                            .append(',').append(h.dampingAttempts())
                            .append(',').append(h.activeInequalities().length).append('\n');
                Files.writeString(output.resolve((fast ? "fast-" : "") + configuration.name
                        + "-history.csv"), history);
                log.append("active bounds at the accepted point:\n");
                Bound[] bounds = dls.bounds();
                int offset = dls.result().inequalityMultipliers().length - bounds.length;
                for (int i = 0; i < bounds.length; i++) {
                    double multiplier = dls.result().inequalityMultipliers()[offset + i];
                    if (multiplier != 0.0 || bounds[i].value() < 1e-8)
                        log.append("  ").append(bounds[i]).append(" multiplier=")
                                .append(fmt(multiplier)).append('\n');
                }
                log.append('\n');
            }
            Files.writeString(output.resolve(configuration.name + "-prescription.txt"),
                    prescription.toString());
            Files.writeString(output.resolve((fast ? "fast-" : "") + "results.csv"), csv);
            Files.writeString(output.resolve((fast ? "fast-" : "") + "layout.txt"), log);
            System.out.println("LEICA-BOUNDS finished " + configuration.name
                    + " seconds=" + fmt(seconds) + " evaluations=" + evaluations
                    + " opticalRms=" + fmt(opticalAfter)
                    + " minEdge=" + fmt(after.minEdge())
                    + " crossed=" + after.crossedGaps() + " stop=" + reason);

            assertTrue(Double.isFinite(opticalAfter), configuration.name + " left a non-finite merit");
        }
        System.out.println(csv);
    }

    /** Axial thicknesses and edge separations, as they stand. */
    private record Layout(double[] thickness, double[] edge, double[] height) {

        static Layout of(Prescription prescription) {
            int gaps = prescription._surfaces.length - 1;
            double[] thickness = new double[gaps], edge = new double[gaps], height = new double[gaps];
            for (int surface = 0; surface < gaps; surface++) {
                thickness[surface] = prescription._surfaces[surface].get_thickness_by_scenario(0);
                height[surface] = ConstraintEdgeThickness.default_height(prescription, 0, surface);
                edge[surface] = height[surface] > 0.0
                        ? ConstraintEdgeThickness.edge_gap(prescription, 0, surface, height[surface])
                        : Double.NaN;
            }
            return new Layout(thickness, edge, height);
        }

        /** Smallest surviving fraction of any starting axial thickness. */
        double minThicknessFraction(Layout start) { return minFraction(thickness, start.thickness); }

        /** Smallest surviving fraction of any starting edge separation. Negative means
         * the surfaces have crossed. */
        double minEdgeFraction(Layout start) { return minFraction(edge, start.edge); }

        double minEdge() {
            double smallest = Double.POSITIVE_INFINITY;
            for (double gap : edge) if (Double.isFinite(gap)) smallest = Math.min(smallest, gap);
            return smallest;
        }

        int crossedGaps() {
            int crossed = 0;
            for (double gap : edge) if (!Double.isFinite(gap) || gap <= 0.0) crossed++;
            return crossed;
        }

        private static double minFraction(double[] now, double[] start) {
            double smallest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < now.length; i++) {
                if (!(Math.abs(start[i]) > 0.0) || !Double.isFinite(start[i])) continue;
                double fraction = Double.isFinite(now[i]) ? now[i] / start[i] : Double.NEGATIVE_INFINITY;
                smallest = Math.min(smallest, fraction);
            }
            return smallest;
        }

        String report(Layout start) {
            var text = new StringBuilder(String.format(Locale.ROOT,
                    "%-4s %12s %12s %8s %12s %12s %8s%n",
                    "gap", "t", "t0", "t/t0", "edge", "edge0", "e/e0"));
            for (int i = 0; i < thickness.length; i++) {
                if (!(Math.abs(start.thickness[i]) > 0.0)) continue;
                text.append(String.format(Locale.ROOT, "%-4d %12.6f %12.6f %8.4f %12.6f %12.6f %8.4f%n",
                        i, thickness[i], start.thickness[i], thickness[i] / start.thickness[i],
                        edge[i], start.edge[i], edge[i] / start.edge[i]));
            }
            return text.toString();
        }
    }

    /**
     * The Leica contrast configuration, with only the layout-constraint treatment varying.
     * Everything else follows {@link LeicaApo75mmMandler#createContrastSetup}.
     */
    private static OptimizationBuilder.OptimizationSetup setupFor(
            Configuration configuration, Prescription prescription, boolean fast) {
        var builder = baseBuilder(prescription, fast).applyCurvatureConstraints();
        switch (configuration.name) {
            case "lmder-tuned" -> builder.applyThicknessConstraints(5.0)
                    .applyEdgeThicknessConstraints();
            case "lmder-nominal", "dls-nominal" -> builder.applyThicknessConstraints()
                    .applyEdgeThicknessConstraints();
            case "dls-bounds", "dls-bounds-damped", "dls-bounds-trust" -> builder.boundThicknesses(BOUND_FRACTION)
                    .boundEdgeThicknesses(BOUND_FRACTION);
            default -> throw new IllegalArgumentException(configuration.name);
        }
        return builder.build();
    }

    /** The same optical goals with no constraint block at all: the cross-row yardstick. */
    private static OptimizationBuilder.OptimizationSetup opticalSetup(
            Prescription prescription, boolean fast) {
        return baseBuilder(prescription, fast).build();
    }

    private static OptimizationBuilder baseBuilder(Prescription prescription, boolean fast) {
        double[] fields = fast
                ? new double[]{0.0, 0.5, 1.0}
                : new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double[] sagittal = fast
                ? new double[]{8.0, 8.0, 0.1}
                : new double[]{8.0, 8.0, 8.0, 8.0, 8.0, 8.0, 4.0, 2.0, 1.0, 0.5, 0.1};
        double[] tangential = fast
                ? new double[]{4.0, 4.0, 0.1}
                : new double[]{4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 1.0, 1.0, 0.5, 0.1, 0.1};
        boolean[] correctAstigmatism = new boolean[fields.length];
        for (int i = 0; i < fields.length - 1; i++) correctAstigmatism[i] = true;

        var builder = OptimizationBuilder.builder(prescription)
                .fields(fields)
                .mtfFrequencies(10, 20, 40)
                .varyAllCurvatures()
                .varyAllThicknesses()
                .weighted(false)
                .dLineOnly(false)
                .contrastSampling(fast ? 3 : 6, fast ? 6 : 12)
                .calibrateContrastFrequency(false)
                .centerContrastResiduals(false)
                .aimContrastAtExitPupil(false)
                .vignetting(VigType.SetVig)
                .freezeVignetting()
                .checkSpotApertures(false)
                .contrastBalanceGoals(correctAstigmatism, 4.0);
        return fast
                ? builder.contrastGoals(contrast(40, sagittal, tangential))
                : builder.contrastGoals(
                        contrast(10, sagittal, tangential),
                        contrast(20, sagittal, tangential),
                        contrast(40, sagittal, tangential));
    }

    /** The yardstick has its own Analysis over the shared prescription, so it has to be
     * recomputed before it reports anything. Not counted against any solver's budget. */
    private static double opticalRms(OptimizationBuilder.OptimizationSetup setup) {
        setup.analysis().compute();
        return setup.meritFunction(false).getRMS();
    }

    private static double mean(double[] values) {
        double total = 0;
        for (double value : values) total += value;
        return values.length == 0 ? Double.NaN : total / values.length;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.6g", value);
    }

    private static Path repositoryRoot() {
        Path root = Path.of(System.getProperty(
                        "maven.multiModuleProjectDirectory", System.getProperty("user.dir")))
                .toAbsolutePath().normalize();
        if (Files.isDirectory(root.resolve("Examples"))) return root;
        if (Files.isDirectory(root.resolve("../Examples"))) return root.resolve("..").normalize();
        throw new IllegalStateException("Cannot locate repository root from " + root);
    }
}
