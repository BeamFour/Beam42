package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.analysis.ContrastAnalysisResult;
import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.util.Orientation;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoalContrastTest {
    private static Analysis analysis() {
        var analysis = new Analysis(null, new double[]{0}, new int[0]);
        analysis._contrast_num_rings = 2;
        analysis._contrast_num_spokes = 6;
        var samples = new ArrayList<ContrastAnalysisResult.Sample>();
        for (int i = 0; i < 12; i++)
            samples.add(new ContrastAnalysisResult.Sample(new Vector2(i * 0.01, 0),
                    i + 0.25, -i - 0.5, 1, i != 4, null));
        var result = new ContrastAnalysisResult(30);
        result.fields.add(new ContrastAnalysisResult.FieldResult(null, List.of(
                new ContrastAnalysisResult.WavelengthResult(587.5618, 0.1, samples))));
        analysis._contrasts = new ContrastAnalysisResult[]{result};
        return analysis;
    }

    @Test
    void explicitNodeReadsItsSlotAfterAFailedRay() {
        var analysis = analysis();
        var failed = new GoalContrast(analysis, 0, 30, 1, 0, 0, 2, Orientation.SAGITTAL, 1);
        var next = new GoalContrast(analysis, 0, 30, 1, 0, 1, 2, Orientation.TANGENTIAL, 1);
        assertEquals(LMDerMeritFunction.BIGVAL, failed.value());
        assertEquals(1, next._ring_index);
        assertEquals(2, next._spoke_index);
        assertEquals(5, next._sample_index);
        assertEquals(-5.5, next.value());
        var legacy = new GoalContrast(analysis, 0, 30, 1, 0, 5, Orientation.TANGENTIAL, 1);
        assertEquals(next._ring_index, legacy._ring_index);
        assertEquals(next._spoke_index, legacy._spoke_index);
        assertEquals(next.value(), legacy.value());
    }

    @Test
    void rejectsAChangedPatternEvenWithTheSameSampleCount() {
        var analysis = analysis();
        var goal = new GoalContrast(analysis, 0, 30, 1, 0, 1, 2, Orientation.SAGITTAL, 1);
        assertEquals(5.25, goal.value());
        analysis._contrast_num_rings = 3;
        analysis._contrast_num_spokes = 4;
        assertEquals(LMDerMeritFunction.BIGVAL, goal.value());
        assertEquals(1, goal._ring_index);
        assertEquals(2, goal._spoke_index);
    }

    @Test
    void rejectsNodesOutsideThePattern() {
        var analysis = analysis();
        assertThrows(IllegalArgumentException.class,
                () -> new GoalContrast(analysis, 0, 30, 1, 0, 2, 0, Orientation.SAGITTAL, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalContrast(analysis, 0, 30, 1, 0, 0, 6, Orientation.SAGITTAL, 1));
    }
}
