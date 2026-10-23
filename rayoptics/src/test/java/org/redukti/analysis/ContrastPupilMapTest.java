package org.redukti.analysis;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector2;
import org.redukti.plotter.ContrastPupilMapPlot;
import org.redukti.rayoptics.util.Orientation;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrastPupilMapTest {

    private static ContrastAnalysisResult.WavelengthResult block(double[] differences, double[] weights,
                                                                double[] references) {
        var samples = new ArrayList<ContrastAnalysisResult.Sample>();
        for (int i = 0; i < differences.length; i++)
            samples.add(new ContrastAnalysisResult.Sample(new Vector2(0.1 * i, 0),
                    differences[i], differences[i], weights[i], true, null, references[i]));
        return new ContrastAnalysisResult.WavelengthResult(587.5618, 0.1, samples);
    }

    @Test
    void aPureImageShiftLosesNoContrast() {
        var map = new ContrastPupilMap(block(new double[]{0.3, 0.3, 0.3},
                new double[]{0.2, 0.3, 0.5}, new double[]{0, 0, 0}), Orientation.SAGITTAL);
        assertEquals(1.0, map.phasor, 1e-15);
        for (var p : map.points)
            assertEquals(0.0, p.loss(), 1e-15);
    }

    @Test
    void halfAWaveApartIsFullLoss() {
        // In phase with the mean, and half a wave from it
        var map = new ContrastPupilMap(block(new double[]{0.0, 0.0, 0.5},
                new double[]{0.4, 0.4, 0.2}, new double[]{0, 0, 0}), Orientation.TANGENTIAL);
        assertEquals(0.0, map.points.get(0).loss(), 1e-15);
        assertEquals(1.0, map.points.get(2).loss(), 1e-15);
    }

    @Test
    void weightedLossesAddUpToThePhasor() {
        var model = MtfTest.buildTestModel();
        var result = ContrastAnalysis.eval(model, new ContrastOptions(40.0).num_rings(4).num_spokes(8));
        for (var field : result.fields) {
            for (int orientation : new int[]{Orientation.SAGITTAL, Orientation.TANGENTIAL}) {
                var block = field.wavelengths().get(0);
                var map = new ContrastPupilMap(block, orientation);
                double loss = 0;
                for (var p : map.points)
                    loss += p.weight() * p.loss();
                assertEquals(map.phasor, 1 - 2 * loss, 1e-12);
                assertTrue(map.phasor > 0 && map.phasor <= 1);
                for (var p : map.points)
                    assertTrue(Double.isFinite(p.average_phase()), "contrast samples record the reference OPD");
            }
        }
    }

    @Test
    void averagePhaseIsTheMeanOfTheTwoWavefrontPhases() {
        // W(p) = 0.25, W(p + s) = 0.25 + 0.5: mean 0.5 waves, so pi radians
        var map = new ContrastPupilMap(block(new double[]{0.5, 0.0},
                new double[]{0.5, 0.5}, new double[]{0.25, 0.0}), Orientation.SAGITTAL);
        assertEquals(Math.PI, map.points.get(0).average_phase(), 1e-15);
        assertEquals(0.0, map.points.get(1).average_phase(), 1e-15);
    }

    @Test
    void rejectsABlockWithoutValidSamples() {
        var samples = List.of(new ContrastAnalysisResult.Sample(Vector2.vector2_0, 0, 0, 1, false, null));
        var block = new ContrastAnalysisResult.WavelengthResult(587.5618, 0.1, samples);
        assertThrows(IllegalArgumentException.class, () -> new ContrastPupilMap(block, Orientation.SAGITTAL));
    }

    @Test
    void plotDrawsACirclePerLossySample() {
        var model = MtfTest.buildTestModel();
        var result = ContrastAnalysis.eval(model, new ContrastOptions(40.0).num_rings(4).num_spokes(8));
        var field = result.fields.get(1);
        var block = field.wavelengths().get(0);
        var maps = new ContrastPupilMap[][]{{
                new ContrastPupilMap(block, Orientation.SAGITTAL),
                new ContrastPupilMap(block, Orientation.TANGENTIAL)}};
        String svg = new ContrastPupilMapPlot(field.field(), new int[]{40}, maps).plot();
        long lossy = maps[0][0].points.stream().filter(p -> p.loss() > 0).count()
                + maps[0][1].points.stream().filter(p -> p.loss() > 0).count();
        assertTrue(svg.contains("<svg"));
        // RendererSvg writes circles as ellipses, and defines one more for its point marker
        assertEquals(lossy + 1, svg.split("<ellipse", -1).length - 1);
    }
}
