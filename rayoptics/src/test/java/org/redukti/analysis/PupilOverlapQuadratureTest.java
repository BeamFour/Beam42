package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.raytr.Analyses;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Orientation;

import static org.junit.jupiter.api.Assertions.*;

class PupilOverlapQuadratureTest {
    @Test
    void integratesCircularOverlapAreaAndCentroidAtFixedCount() {
        var field = new Field(null);
        for (double shift : new double[]{0, 0.1, 0.7, 1.8, -0.7}) {
            for (int axis : new int[]{Orientation.SAGITTAL, Orientation.TANGENTIAL}) {
                var rule = PupilOverlapQuadrature.generate(field, axis, shift, 8, 24);
                assertEquals(192, rule.points().size());
                assertEquals(DiffractionMTF.diffraction_limited(Math.abs(shift) / 2, 1),
                        rule.overlapFraction(), 1e-12);
                double weight = 0, along = 0, across = 0;
                for (var point : rule.points()) {
                    weight += point.weight();
                    along += point.weight() * (axis == 0 ? point.pupil().x : point.pupil().y);
                    across += point.weight() * (axis == 0 ? point.pupil().y : point.pupil().x);
                    assertTrue(point.weight() > 0);
                    assertTrue(ContrastAnalysis.inside_vignetted_pupil(point.pupil(), field));
                    assertTrue(ContrastAnalysis.inside_vignetted_pupil(point.pupil().plus(
                            axis == 0 ? new Vector2(shift, 0) : new Vector2(0, shift)), field));
                }
                assertEquals(1, weight, 1e-14);
                assertEquals(-shift / 2, along, 1e-14);
                assertEquals(0, across, 1e-14);
            }
        }
    }

    @Test
    void integratesTheAsymmetricallyVignettedPupilInBothDirections() {
        var field = new Field(null);
        field.vlx = 0.2;
        field.vux = 0.4;
        field.vly = 0.1;
        field.vuy = 0.3;
        for (int axis : new int[]{0, 1}) {
            double width = axis == 0 ? 1.4 : 1.6;
            for (double shift : new double[]{0, 0.2, -0.8}) {
                var rule = PupilOverlapQuadrature.generate(field, axis, shift, 8, 24);
                assertEquals(Math.PI * 1.4 * 1.6 / 4, rule.pupilArea(), 1e-14);
                assertEquals(DiffractionMTF.diffraction_limited(Math.abs(shift) / width, 1),
                        rule.overlapFraction(), 1e-12);
                for (var point : rule.points()) {
                    assertTrue(ContrastAnalysis.inside_vignetted_pupil(point.pupil(), field));
                    assertTrue(ContrastAnalysis.inside_vignetted_pupil(point.pupil().plus(
                            axis == 0 ? new Vector2(shift, 0) : new Vector2(0, shift)), field));
                }
            }
        }
    }

    private static double wavefront(double x, double y) {
        double r2 = x * x + y * y;
        return 0.8 * r2 * r2 - 0.5 * r2 + 0.3 * x * y * y;
    }

    @Test
    void phasorOverTheCompleteOverlapAgreesWithFftForAnAberratedPupil() {
        int n = 256, centre = n;
        var w = new double[n][n];
        var zero = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double x = -1 + 2.0 * i / (n - 1), y = -1 + 2.0 * j / (n - 1);
                boolean inside = x * x + y * y <= 1;
                w[i][j] = inside ? wavefront(x, y) : Double.NaN;
                zero[i][j] = inside ? 0 : Double.NaN;
            }
        var fft = DiffractionMTF.modulus_of_transform(Analyses.calc_psf(w, n, 2 * n));
        var ideal = DiffractionMTF.modulus_of_transform(Analyses.calc_psf(zero, n, 2 * n));
        for (int axis : new int[]{0, 1})
            for (int k : new int[]{15, 40, 70}) {
                double shift = 2.0 * k / (n - 1);
                var rule = PupilOverlapQuadrature.generate(new Field(null), axis, shift, 24, 48);
                double re = 0, im = 0;
                for (var point : rule.points()) {
                    var p = point.pupil();
                    double phase = 2 * Math.PI * (wavefront(p.x + (axis == 0 ? shift : 0),
                            p.y + (axis == 1 ? shift : 0)) - wavefront(p.x, p.y));
                    re += point.weight() * Math.cos(phase);
                    im += point.weight() * Math.sin(phase);
                }
                int i = centre + (axis == 0 ? k : 0), j = centre + (axis == 1 ? k : 0);
                assertEquals(fft[i][j] / ideal[i][j], Math.hypot(re, im), 3e-3);
                assertEquals(fft[i][j], rule.overlapFraction() * Math.hypot(re, im), 3e-3);
            }
    }

    @Test
    void rejectsEmptyOverlapAndInvalidSampling() {
        var field = new Field(null);
        assertThrows(IllegalArgumentException.class, () -> PupilOverlapQuadrature.generate(field, 0, 2, 6, 12));
        assertThrows(IllegalArgumentException.class, () -> PupilOverlapQuadrature.generate(field, 0, 0.1, 0, 12));
        assertThrows(IllegalArgumentException.class, () -> PupilOverlapQuadrature.generate(field, 0, Double.NaN, 6, 12));
        field.vux = 1;
        assertThrows(IllegalArgumentException.class, () -> PupilOverlapQuadrature.generate(field, 0, 0.1, 6, 12));
    }
}
