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
 * between them.
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
     * The airspaces a zoom varies between configurations, excluding the back
     * focus. These are the surfaces whose thickness came from a multi valued
     * row of [variable distances].
     */
    public static int[] findVariableThicknesses(Prescription prescription, int backFocusSurface) {
        SurfaceType[] surfaces = prescription.get_surfaces();
        int count = 0;
        for (int i = 0; i < surfaces.length; i++)
            if (isVariableAirspace(surfaces[i], i, backFocusSurface))
                count++;
        int[] result = new int[count];
        int n = 0;
        for (int i = 0; i < surfaces.length; i++)
            if (isVariableAirspace(surfaces[i], i, backFocusSurface))
                result[n++] = i;
        return result;
    }

    private static boolean isVariableAirspace(SurfaceType surface, int index, int backFocusSurface) {
        return index != backFocusSurface
                && surface._thickness_by_scenario != null
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
