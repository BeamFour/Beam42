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
    void integratesLowOrderAnnularMoments() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.min_radius = 0.5;
        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_gaussian_quadrature(definition, 3, 12);

        double meanR2 = points.stream().mapToDouble(point -> {
            double r2 = point.pupil().x * point.pupil().x
                    + point.pupil().y * point.pupil().y;
            return point.weight() * r2;
        }).sum();
        assertEquals((1.0 + 0.25) / 2.0, meanR2, 1.0e-14);
        assertTrue(points.stream().allMatch(point ->
                point.pupil().x * point.pupil().x + point.pupil().y * point.pupil().y > 0.25));
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
        assertThrows(IllegalArgumentException.class,
                () -> Trace.generate_gaussian_quadrature(definition, 1, 2));
        definition.min_radius = 1.0;
        assertThrows(IllegalArgumentException.class,
                () -> Trace.generate_gaussian_quadrature(definition, 1, 3));
    }

    @Test
    void contrastPatternAndBothPartnersStayInsideTheVignettedPupil() {
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
        }
    }

    /**
     * The samples are absolute pupil coordinates, so the tracer must be told not
     * to apply Field vignetting a second time. Re-applying it would rescale the
     * displaced rays and turn the requested shear into a smaller, field- and
     * direction-dependent one - the defect this pattern exists to avoid.
     */
    @Test
    void reapplyingFieldVignettingWouldCorruptTheRequestedShear() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.num_rings = 3;
        Field field = asymmetricVignettedField();
        Vector2 tangentialShift = new Vector2(0.0, 0.06743);

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_contrast_quadrature(
                        definition, 6, Vector2.vector2_0, tangentialShift, field);

        for (Trace.GaussianQuadraturePoint point : points) {
            Vector2 pupil = point.pupil();
            double reapplied = vignettedY(field, pupil.plus(tangentialShift))
                    - vignettedY(field, pupil);
            // 1-vuy=0.364 and 1-vly=0.313, so a second application shrinks the
            // 40 cycle/mm shear to somewhere near a third of its intended size.
            assertTrue(reapplied < 0.5 * tangentialShift.y,
                    () -> "expected re-applied vignetting to shrink the shear, got " + reapplied);
        }
    }

    /**
     * A base sample and its displaced partner can sit on opposite sides of the
     * y axis, where the vignetting factors differ. Re-applying vignetting there
     * would not even be a rigid translation.
     */
    @Test
    void shearStraddlingTheVignettingAxisIsNotARigidTranslationWhenReapplied() {
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
        Trace.GaussianQuadraturePoint below = points.stream()
                .filter(point -> point.pupil().y + tangentialShift.y < 0.0)
                .findFirst()
                .orElseThrow();

        double crossingShear = vignettedY(field, crossing.pupil().plus(tangentialShift))
                - vignettedY(field, crossing.pupil());
        double belowShear = vignettedY(field, below.pupil().plus(tangentialShift))
                - vignettedY(field, below.pupil());
        assertTrue(Math.abs(crossingShear - belowShear) > 1.0e-6,
                "a straddling pair and a wholly-below pair should disagree once"
                        + " vignetting is re-applied");
    }

    @Test
    void rejectsShearThatLeavesTooLittlePupilToSample() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.num_rings = 3;
        Field field = new Field(null);

        // Well inside the pupil: sampled normally.
        Trace.generate_contrast_quadrature(definition, 6,
                new Vector2(0.5, 0.0), new Vector2(0.0, 0.5), field);

        // The overlap centre is still inside the pupil here, but the pattern
        // would have to collapse onto it, reporting zero wavefront difference.
        assertThrows(IllegalArgumentException.class, () ->
                Trace.generate_contrast_quadrature(definition, 6,
                        new Vector2(1.41, 0.0), new Vector2(0.0, 1.41), field));

        // Beyond the point where even the overlap centre is valid.
        assertThrows(IllegalArgumentException.class, () ->
                Trace.generate_contrast_quadrature(definition, 6,
                        new Vector2(1.9, 0.0), new Vector2(0.0, 1.9), field));
    }

    @Test
    void contrastPatternSamplesAreDistinct() {
        TraceRingsDef definition = new TraceRingsDef();
        definition.num_rings = 3;
        Field field = asymmetricVignettedField();
        Vector2 shift = new Vector2(0.06743, 0.0);

        List<Trace.GaussianQuadraturePoint> points =
                Trace.generate_contrast_quadrature(
                        definition, 6, shift, new Vector2(0.0, 0.06743), field);

        double closest = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++)
            for (int j = i + 1; j < points.size(); j++)
                closest = Math.min(closest,
                        points.get(i).pupil().minus(points.get(j).pupil()).len());
        final double smallest = closest;
        assertTrue(smallest > 0.01,
                () -> "samples collapsed towards the overlap centre, closest pair " + smallest);
    }

    private static double vignettedY(Field field, Vector2 pupil) {
        return field.apply_vignetting(pupil.as_array())[1];
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
