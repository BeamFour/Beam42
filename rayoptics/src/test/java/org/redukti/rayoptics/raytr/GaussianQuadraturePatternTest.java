package org.redukti.rayoptics.raytr;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.specs.Field;

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

    @Test
    void contrastPatternUsesPhysicalVignettedPupilAndPreservesShear() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.num_rings = 3;
        Field field = asymmetricVignettedField();
        Vector2 sagittalShift = new Vector2(0.06743, 0.0);
        Vector2 tangentialShift = new Vector2(0.0, 0.06743);

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_contrast_quadrature(
                        definition, 6, sagittalShift, tangentialShift, field);

        assertEquals(18, points.size());
        assertEquals(1.0,
                points.stream().mapToDouble(Trace.GaussianQuadraturePoint::weight).sum(),
                1.0e-14);
        for (Trace.GaussianQuadraturePoint point : points) {
            Vector2 pupil = point.pupil();
            assertTrue(Trace.inside_vignetted_pupil(pupil, field));
            assertTrue(Trace.inside_vignetted_pupil(pupil.plus(sagittalShift), field));
            assertTrue(Trace.inside_vignetted_pupil(pupil.plus(tangentialShift), field));
            assertEquals(sagittalShift.x, pupil.plus(sagittalShift).x - pupil.x, 1.0e-15);
            assertEquals(tangentialShift.y, pupil.plus(tangentialShift).y - pupil.y, 1.0e-15);
        }
    }

    @Test
    void physicalContrastShearDoesNotChangeWhenItCrossesVignettingAxis() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.num_rings = 3;
        Field field = asymmetricVignettedField();
        Vector2 tangentialShift = new Vector2(0.0, 0.06743);

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_contrast_quadrature(
                        definition, 6, Vector2.vector2_0, tangentialShift, field);

        Trace.GaussianQuadraturePoint crossing = points.stream()
                .filter(point -> point.pupil().y < 0.0
                        && point.pupil().y + tangentialShift.y > 0.0)
                .findFirst()
                .orElseThrow();
        assertEquals(tangentialShift.y,
                crossing.pupil().plus(tangentialShift).y - crossing.pupil().y,
                1.0e-15);
    }

    private static Field asymmetricVignettedField() {
        Field field = new Field(null);
        field.vux = 0.216;
        field.vlx = 0.216;
        field.vuy = 0.636;
        field.vly = 0.687;
        return field;
    }

    private static double weightedMoment(List<Trace.GaussianQuadraturePoint> points, int xPower, int yPower) {
        return points.stream().mapToDouble(point -> point.weight()
                * Math.pow(point.pupil().x, xPower)
                * Math.pow(point.pupil().y, yPower)).sum();
    }
}
