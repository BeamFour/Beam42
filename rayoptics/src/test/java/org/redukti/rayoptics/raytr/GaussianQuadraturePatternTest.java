package org.redukti.rayoptics.raytr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaussianQuadraturePatternTest {

    @Test
    void generatesRequestedNumberOfRingsAndSpokes() {
        TraceRingsDef definition = new TraceRingsDef();

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_gaussian_quadrature(definition, 3, 8);

        assertEquals(24, points.size());
        assertEquals(1.0, points.stream().mapToDouble(Trace.GaussianQuadraturePoint::weight).sum(), 1.0e-14);
    }

    @Test
    void usesOptilandDefaultSpokeCount() {
        TraceRingsDef definition = new TraceRingsDef();

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_gaussian_quadrature(definition, 3, null);

        assertEquals(3 * 4 * (3 + 1), points.size());
    }

    @Test
    void integratesLowOrderDiskMoments() {
        TraceRingsDef definition = new TraceRingsDef();
        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_gaussian_quadrature(definition, 3, null);

        double meanX = weightedMoment(points, 1, 0);
        double meanY = weightedMoment(points, 0, 1);
        double meanX2 = weightedMoment(points, 2, 0);
        double meanY2 = weightedMoment(points, 0, 2);
        double meanR4 = points.stream().mapToDouble(point -> {
            double r2 = point.pupil().x * point.pupil().x + point.pupil().y * point.pupil().y;
            return point.weight() * r2 * r2;
        }).sum();

        assertEquals(0.0, meanX, 1.0e-14);
        assertEquals(0.0, meanY, 1.0e-14);
        assertEquals(0.25, meanX2, 1.0e-14);
        assertEquals(0.25, meanY2, 1.0e-14);
        assertEquals(1.0 / 3.0, meanR4, 1.0e-14);
    }

    @Test
    void appliesPupilScaleAndOffset() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.cx = 2.0;
        definition.cy = -3.0;
        definition.max_radius = 0.5;

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_gaussian_quadrature(definition, 2, 4);

        for (Trace.GaussianQuadraturePoint point : points) {
            double x = point.pupil().x - definition.cx;
            double y = point.pupil().y - definition.cy;
            double radius = Math.sqrt(x * x + y * y);
            assertTrue(radius < definition.max_radius);
        }
    }

    @Test
    void rejectsInvalidDimensions() {
        TraceRingsDef definition = new TraceRingsDef();

        assertThrows(IllegalArgumentException.class,
                () -> Trace.generate_gaussian_quadrature(definition, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> Trace.generate_gaussian_quadrature(definition, 1, 0));
    }

    private static double weightedMoment(List<Trace.GaussianQuadraturePoint> points, int xPower, int yPower) {
        return points.stream().mapToDouble(point -> point.weight()
                * Math.pow(point.pupil().x, xPower)
                * Math.pow(point.pupil().y, yPower)).sum();
    }
}
