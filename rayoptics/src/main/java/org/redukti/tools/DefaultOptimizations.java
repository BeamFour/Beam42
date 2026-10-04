// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.tools;

import org.redukti.optim.LMDerMeritFunction;
import org.redukti.optim.OptimizationBuilder;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;

/**
 * The two routine optimizations that a prescription imported from a patent
 * almost always needs, wired up so that LensTool2 can run them without the
 * caller having to build a merit function by hand.
 *
 * <p>Both target MTF at the central field, which is what the manual runs this
 * replaces have used and what tends to converge. Effective focal length and
 * f-number are anchored to the prescription by {@link OptimizationBuilder}
 * itself, so optimizing an airspace cannot quietly turn the lens into a
 * different one.
 *
 * <p>Only one configuration is optimized per call. There is no support for a
 * variable shared across configurations, so a zoom is optimized one
 * configuration at a time and nothing here tries to hold the back focus common
 * between them - instead the airspaces that have to stay common are left out of
 * the solve, which {@link #findVariableThicknesses} explains.
 */
public final class DefaultOptimizations {

    private DefaultOptimizations() {}

    /** Central field, the single field these optimizations are judged on. */
    private static final double[] CENTRAL_FIELD = {0.0};
    /** Goals aim at perfect contrast, so the solver simply maximizes it. */
    private static final double PERFECT_MTF = 1.0;
    /** Pupil sampling for the contrast objective. */
    private static final int CONTRAST_RINGS = 6;
    private static final int CONTRAST_SPOKES = 12;

    /** Which objective the solve is driven by. */
    public enum Objective { CONTRAST, MTF }

    public record Result(int[] surfaces, int status, double before, double after) {
        public boolean improved() {
            return after < before;
        }
    }

    /**
     * Locates the airspace that acts as the back focus.
     *
     * <p>Normally that is the gap after the last surface. Where the design ends
     * in a cover glass it is the gap in front of the cover glass instead: the
     * short gap between cover glass and image is fixed by the sensor stack and
     * moving it is not what "adjust the back focus" means.
     *
     * @return the surface whose thickness is the back focus, or -1 if the
     *         prescription does not end in an airspace that can be varied
     */
    public static int findBackFocusSurface(Prescription prescription) {
        SurfaceType[] surfaces = prescription.get_surfaces();
        if (surfaces.length == 0)
            return -1;
        // Walk back over a trailing cover glass block, if there is one.
        int firstCoverGlass = -1;
        for (int i = surfaces.length - 1; i >= 0; i--) {
            if (surfaces[i].is_cover_glass())
                firstCoverGlass = i;
            else if (firstCoverGlass >= 0)
                break;
        }
        int candidate = firstCoverGlass >= 0 ? firstCoverGlass - 1 : surfaces.length - 1;
        if (candidate < 0)
            return -1;
        // The back focus is an airspace; a glass thickness here means the
        // prescription is not shaped the way this rule assumes.
        if (surfaces[candidate].get_refractive_index() != 0.0)
            return -1;
        return candidate;
    }

    /**
     * The airspaces a zoom varies between configurations. These are the surfaces
     * whose thickness came from a multi valued row of [variable distances] and
     * whose value actually differs between the configurations being reported.
     *
     * <p>That second half of the test is what keeps the back focus out, and it is
     * deliberately a test on the value rather than on the position. A gap holding
     * the same value at every zoom setting is one that has to stay common - the
     * cover glass to sensor spacing, typically - and since a variable cannot yet
     * be shared across configurations, varying it per configuration would hand
     * each zoom setting its own sensor position.
     *
     * <p>Excluding {@link #findBackFocusSurface} instead would be wrong on a design
     * that ends in a cover glass: there the airspace in front of the cover glass is
     * the one that moves with the zoom, so that rule points at a genuine zoom
     * airspace, and dropping it would leave the rear group's spacing out of the
     * solve while admitting the fixed sensor gap behind the cover glass.
     */
    public static int[] findVariableThicknesses(Prescription prescription) {
        SurfaceType[] surfaces = prescription.get_surfaces();
        int count = 0;
        for (int i = 0; i < surfaces.length; i++)
            if (isVariableAirspace(surfaces[i]))
                count++;
        int[] result = new int[count];
        int n = 0;
        for (int i = 0; i < surfaces.length; i++)
            if (isVariableAirspace(surfaces[i]))
                result[n++] = i;
        return result;
    }

    /**
     * True when the thickness row this surface references holds more than one distinct
     * value across the configurations.
     *
     * <p>The array is indexed by configuration, not by the scenario numbering of the
     * input: {@code Prescription} fills it from the scenarios the report's
     * {@code scenarios} list selects, so the scenarios a patent tabulated but this
     * report does not use are already absent and cannot make an airspace look as though
     * it varies. A value those selected scenarios are missing is rejected while the
     * prescription is built, so nothing here has to allow for one.
     */
    private static boolean variesByConfiguration(double[] thickness_by_scenario) {
        if (thickness_by_scenario == null)
            return false;
        for (int i = 1; i < thickness_by_scenario.length; i++)
            if (thickness_by_scenario[i] != thickness_by_scenario[0])
                return true;
        return false;
    }

    private static boolean isVariableAirspace(SurfaceType surface) {
        return variesByConfiguration(surface._thickness_by_scenario)
                && surface.get_refractive_index() == 0.0;
    }

    /**
     * Runs the solver over the given surfaces' thicknesses for one
     * configuration, targeting central field MTF at the requested frequencies.
     * The prescription is updated in place when the solve improves it.
     */
    public static Result optimizeThicknesses(Prescription prescription, int[] surfaces,
                                             int[] mtfFrequencies, int configuration,
                                             VigType vigType, boolean dLineOnly,
                                             Objective objective) throws Exception {
        if (surfaces.length == 0)
            throw new IllegalArgumentException("no surfaces to optimize");
        var builder = OptimizationBuilder.builder(prescription)
                .fields(CENTRAL_FIELD)
                .mtfFrequencies(mtfFrequencies)
                .scenario(configuration)
                .vignetting(vigType)
                .dLineOnly(dLineOnly)
                .varyThicknesses(surfaces)
                .applyThicknessConstraints();
        // Goals at the central field, both meridians, driving contrast up.
        // Without them the only residuals are the automatic focal length and
        // f-number anchors, which do not depend on an airspace at all, and the
        // solve sits still.
        if (objective == Objective.CONTRAST) {
            var goals = new OptimizationBuilder.ContrastGoals[mtfFrequencies.length];
            for (int i = 0; i < mtfFrequencies.length; i++)
                goals[i] = OptimizationBuilder.contrast(mtfFrequencies[i], new double[]{1.0});
            builder = builder
                    .contrastSampling(CONTRAST_RINGS, CONTRAST_SPOKES)
                    .calibrateContrastFrequency(true)
                    .contrastGoals(goals);
        }
        else {
            var goals = new OptimizationBuilder.MtfGoals[mtfFrequencies.length];
            for (int i = 0; i < mtfFrequencies.length; i++)
                goals[i] = OptimizationBuilder.mtf(mtfFrequencies[i],
                        new double[]{PERFECT_MTF}, new double[]{PERFECT_MTF});
            builder = builder.mtfGoals(goals);
        }
        var setup = builder.build();
        var analysis = setup.analysis();
        LMDerMeritFunction merit = setup.meritFunction(false);
        analysis.compute();
        double before = merit.getRMS();
        int status = merit.getSolver().solve();
        double after = merit.getRMS();
        return new Result(surfaces, status, before, after);
    }
}
