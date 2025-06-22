package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.model.Stop;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.List;

public class GoalRayInterceptApertureStop extends Goal {
    public final Vector2 targetPoint;
    public GoalRayInterceptApertureStop(Analysis analysis, Vector2 targetPoint, double weight) {
        super(analysis, 0, weight);
        this.targetPoint = targetPoint;
    }
    @Override
    public double value() {
        var apertureStop = analysis
                    .sys1.get_sequence()
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
            sos += MathUtils.square(resid);
        }
        return Math.sqrt(sos/p.length);
    }

    @Override
    public String toString() {
        var apertureStop = analysis
                    .sys1.get_sequence()
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
