package org.redukti.optim;

import org.redukti.mathlib.LMLSolver;
import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.jfotoptix.model.Stop;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.List;

/**
 * This goal is specialized to test ray intercept on the aperture
 * It is primarily meant to be used for finding chief ray
 */
public class GoalRayInterceptApertureStop extends Goal {
    public final Vector2 targetPoint;
    public GoalRayInterceptApertureStop(Analysis analysis, Vector2 targetPoint, double weight) {
        super(analysis, 0, weight);
        this.targetPoint = targetPoint;
    }
    @Override
    public double value() {
        var apertureStop = analysis
                    .systems[0].get_sequence()
                    .stream()
                    .filter(e -> e instanceof Stop.ApertureStop)
                    .map(e -> (Stop.ApertureStop)e)
                    .findFirst()
                    .orElse(null);
        if (apertureStop == null) {
            throw new IllegalArgumentException("An aperture stop is required");
        }
        List<TracedRay> rays = analysis.singleRayTraceResults.get_intercepted(apertureStop);
        if (rays == null || rays.isEmpty())
            return LMLSolver.BIGVAL;
        TracedRay ray = rays.get(0);
        double[] p = { ray.get_intercept_point().x(), ray.get_intercept_point().y() };
        double sos = 0.0;
        for (int i = 0; i < p.length; i++) {
            double resid = targetPoint.v(i) - p[i];
            sos += M.square(resid);
        }
        return Math.sqrt(sos/p.length);
    }

    @Override
    public String toString() {
        var apertureStop = analysis
                    .systems[0].get_sequence()
                    .stream()
                    .filter(e -> e instanceof Stop.ApertureStop)
                    .map(e -> (Stop.ApertureStop)e)
                    .findFirst()
                    .orElse(null);
        if (apertureStop == null) {
            throw new IllegalArgumentException("An aperture stop is required");
        }
        List<TracedRay> rays = analysis.singleRayTraceResults.get_intercepted(apertureStop);
        if (rays == null || rays.isEmpty())
            return "Aperture stop intercept goal failed";
        TracedRay ray = rays.get(0);
        return "Aperture stop intercept goal: " + targetPoint + " achieved " + ray.get_intercept_point();
    }
}
