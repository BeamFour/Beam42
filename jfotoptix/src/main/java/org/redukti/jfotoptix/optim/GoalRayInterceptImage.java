package org.redukti.jfotoptix.optim;

import org.redukti.jfotoptix.math.LMLSolver;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.List;

public class GoalRayInterceptImage extends Goal {
    public final Vector2 targetPoint;
    public GoalRayInterceptImage(Analysis analysis, Vector2 targetPoint, double weight) {
        super(analysis, 0, weight);
        this.targetPoint = targetPoint;
    }
    @Override
    public double value() {
        var image = (Image) analysis
                .sys1
                .get_sequence()
                .stream()
                .filter(e-> e instanceof Image)
                .findFirst()
                .get();
        if (image == null) {
            throw new IllegalArgumentException("An aperture stop is required");
        }
        List<TracedRay> rays = analysis.singleRayTraceResults.get_intercepted(image);
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
        var image = (Image) analysis
                .sys1
                .get_sequence()
                .stream()
                .filter(e-> e instanceof Image)
                .findFirst()
                .get();
        if (image == null) {
            throw new IllegalArgumentException("An aperture stop is required");
        }
        List<TracedRay> rays = analysis.singleRayTraceResults.get_intercepted(image);
        if (rays == null || rays.isEmpty())
            return "Image intercept goal failed";
        TracedRay ray = rays.get(0);
        return "Image intercept goal: " + targetPoint + " achieved " + ray.get_intercept_point();
    }
}
