// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.analysis.RayAberrationResult;
import org.redukti.rayoptics.raytr.TraceFanPoints;
import org.redukti.rayoptics.raytr.TraceFanResult;
import org.redukti.spec.Prescription;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

class GoalMTFProxyTest {
    private static final double WAVELENGTH = 587.6;

    private static class FanAnalysis extends Analysis {
        FanAnalysis(Double... values) {
            super(new Prescription(50, 2, 40, 43, new double[]{WAVELENGTH}, new double[]{1}),
                    new double[]{0}, new int[0]);
            samples(values);
        }

        void samples(Double... values) {
            _ray_aberrations = new RayAberrationResult();
            var fan = new TraceFanPoints(WAVELENGTH, Collections.nCopies(values.length, 0.0),
                    Arrays.asList(values), List.of());
            _ray_aberrations.add(new TraceFanResult(null, 0, 0, List.of(fan), 1, 1));
        }

        @Override public void compute() {}
    }

    private static GoalMTFProxy goal(Analysis analysis, int position) {
        return new GoalMTFProxy(analysis, 1, 0, position, WAVELENGTH, 10, 1);
    }

    @Test void returnsSignedUnsquaredSineAndPreservesNegativeIndices() {
        var analysis = new FanAnalysis(0.0, 0.025, -0.025);
        assertEquals(0, goal(analysis, 0).value());
        assertEquals(Math.sqrt(0.5), goal(analysis, 1).value(), 1e-14);
        assertEquals(-Math.sqrt(0.5), goal(analysis, -1).value(), 1e-14);
        assertEquals(0, goal(analysis, -3).value());
    }

    @Test void validatesConfiguration() {
        var analysis = new FanAnalysis(0.0);
        assertThrows(IllegalArgumentException.class, () -> goal(null, 0));
        for (int field : new int[]{0, 2})
            assertThrows(IllegalArgumentException.class,
                    () -> new GoalMTFProxy(analysis, field, 0, 0, WAVELENGTH, 10, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalMTFProxy(analysis, 1, 2, 0, WAVELENGTH, 10, 1));
        for (int pos : new int[]{-11, 10})
            assertThrows(IllegalArgumentException.class, () -> goal(analysis, pos));
        for (double target : new double[]{1, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,
                    () -> new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 10, target, 1));
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, invalid, 1));
            assertThrows(IllegalArgumentException.class,
                    () -> new GoalMTFProxy(analysis, 1, 0, 0, invalid, 10, 1));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new GoalMTFProxy(analysis, 1, 0, 0, 0.5876, 10, 1));
        for (double weight : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,
                    () -> new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 10, weight));
        assertDoesNotThrow(() -> new GoalMTFProxy(analysis, 1, 0, -10, WAVELENGTH, 10, 0));
    }

    @Test void unavailableAndFailedSamplesUseStandardPenalty() {
        var analysis = new FanAnalysis((Double) null, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.MAX_VALUE);
        for (int i = 0; i < 5; i++)
            assertEquals(LMDerMeritFunction.BIGVAL, goal(analysis, i).value());
        assertEquals(LMDerMeritFunction.BIGVAL, goal(analysis, 9).value());
        assertEquals(LMDerMeritFunction.BIGVAL, goal(analysis, -10).value());
        var missingFan = new GoalMTFProxy(analysis, 1, 1, 0, WAVELENGTH, 10, 1);
        assertEquals(LMDerMeritFunction.BIGVAL, missingFan.value());
        analysis._ray_aberrations = null;
        assertEquals(LMDerMeritFunction.BIGVAL, goal(analysis, 0).value());
    }

    @Test void largeFrequencyDoesNotOverflowBeforeMultiplyingAberration() {
        var analysis = new FanAnalysis(0.0, 1e-309);
        assertEquals(0, new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 1e308, 1).value());
        assertEquals(Math.sin(Math.PI * (1e308 * 1e-309)),
                new GoalMTFProxy(analysis, 1, 0, 1, WAVELENGTH, 1e308, 1).value());
    }

    @Test void solverSquaresAndWeightsOnceAndRejectsFailedSamples() {
        var analysis = new FanAnalysis(0.025);
        var proxy = new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 10, 4);
        var merit = new LMDerMeritFunction(analysis, new Var[0], new Goal[]{proxy}, false);
        double[] residual = new double[1];
        merit.apply(1, 0, new double[0], residual, 1);
        assertEquals(2, residual[0] * residual[0], 1e-14);
        analysis.samples((Double) null);
        merit.apply(1, 0, new double[0], residual, 1);
        assertEquals(LMDerMeritFunction.BIGVAL, residual[0]);
        assertFalse(merit.buildJacobian(new double[0], new double[0], 1));
    }

    @Test void jacobianUsesSineDerivativeAndRejectsInvalidProbes() {
        var analysis = new FanAnalysis(0.025);
        var variable = new Var(null) {
            @Override public double read_from_prescription() {
                set_unscaled_value(0.025);
                return get_scaled_value();
            }
            @Override public void write_to_prescription() {
                analysis.samples(get_unscaled_value());
            }
        };
        variable._d_delta = 1e-6;
        var proxy = new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 10, 4);
        var merit = new LMDerMeritFunction(analysis, new Var[]{variable}, new Goal[]{proxy}, false);
        double[] jacobian = new double[1];
        assertTrue(merit.buildJacobian(new double[]{0.025}, jacobian, 1));
        assertEquals(2 * Math.PI * 10 * Math.cos(Math.PI / 4), jacobian[0], 1e-7);
        assertEquals(Math.sqrt(0.5), proxy.value(), 1e-14);

        var isolated = new FanAnalysis(0.025) {
            @Override public void compute() {
                double value = variable.get_unscaled_value();
                samples(value == 0.025 ? value : null);
            }
        };
        var invalidProbeMerit = new LMDerMeritFunction(isolated, new Var[]{variable},
                new Goal[]{goal(isolated, 0)}, false);
        assertFalse(invalidProbeMerit.buildJacobian(new double[]{0.025}, jacobian, 1));
        assertEquals(Math.sqrt(0.5), goal(isolated, 0).value(), 1e-14);
    }

    @Test void diagnosticGroupsFrequenciesAndSeparatesInvalidAndDisabledSamples() {
        var analysis = new FanAnalysis(0.025, 0.1, null);
        var periodic = goal(analysis, 1);
        var invalid = goal(analysis, 2);
        var safe = new GoalMTFProxy(analysis, 1, 0, 0, WAVELENGTH, 5, 1);
        var disabled = new GoalMTFProxy(analysis, 1, 0, 2, WAVELENGTH, 20, 0);
        var reports = GoalMTFProxy.frequencyDiagnostics(new Goal[]{goal(analysis, 0), periodic, invalid, safe, disabled});
        assertEquals(2, reports.size());
        var report = reports.get(0);
        assertEquals(10, report.frequency());
        assertEquals(2, report.validSamples());
        assertEquals(1, report.invalidSamples());
        assertSame(periodic, report.worstSample());
        assertSame(invalid, report.firstInvalidSample());
        assertEquals(Math.PI, report.maxArgument(), 1e-14);
        assertTrue(report.outsideMonotonicInterval());
        assertFalse(reports.get(1).outsideMonotonicInterval());
        assertEquals(0, periodic.value(), 1e-14);
        analysis.samples(0.05);
        assertTrue(GoalMTFProxy.frequencyDiagnostics(new Goal[]{goal(analysis, 0)})
                .get(0).outsideMonotonicInterval());
        analysis._ray_aberrations = null;
        var unavailable = GoalMTFProxy.frequencyDiagnostics(new Goal[]{invalid}).get(0);
        assertEquals(0, unavailable.validSamples());
        assertTrue(Double.isNaN(unavailable.maxArgument()));
        assertFalse(unavailable.outsideMonotonicInterval());
    }

    @Test void solverPreflightReportsUnsafeFrequencyAndInvalidSamplesWithoutChangingResiduals() {
        var analysis = new FanAnalysis(0.1, null);
        var periodic = goal(analysis, 0);
        var merit = new LMDerMeritFunction(analysis, new Var[0],
                new Goal[]{periodic, goal(analysis, 1)}, false);
        var messages = new ArrayList<String>();
        var handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        Logger logger = Logger.getLogger(LMDerMeritFunction.class.getName());
        logger.addHandler(handler);
        try { assertNotNull(merit.getSolver()); }
        finally { logger.removeHandler(handler); }
        assertTrue(messages.stream().anyMatch(s -> s.contains("at or above pi/2")
                && s.contains("field=1") && s.contains("sample=0")));
        assertTrue(messages.stream().anyMatch(s -> s.contains("1 invalid samples") && s.contains("sample=1")));
        assertEquals(0, periodic.value(), 1e-14);
        assertEquals(10, periodic._freq);
    }
}
