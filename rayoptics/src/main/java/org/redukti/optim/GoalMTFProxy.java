// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

import org.redukti.rayoptics.util.Orientation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A signed sine residual for one transverse ray-fan sample, targeting zero.
 * Follows the geometric real-OTF merit of Heshmaty-Manesh and Kidger/Benham.
 * This is a fan-based proxy, not a pupil-integrated or diffraction MTF value.
 * Frequency is in reciprocal model-length units (cycles/mm for mm aberrations);
 * wavelength selects the fan and is in nanometres. The solver squares the residual.
 */
public class GoalMTFProxy extends Goal {
    public final int _field;
    public final int _orientation;
    public final int _pos;
    public final double _wvl;
    public final double _freq;
    public GoalMTFProxy(Analysis analysis, int field, int orientation, int pos, double wvl, double freq, double weight) {
        this(analysis, field, orientation, pos, wvl, freq, 0.0, weight);
    }

    public GoalMTFProxy(Analysis analysis, int field, int orientation, int pos, double wvl, double freq, double target, double weight) {
        super(analysis,target,weight);
        Orientation.checked(orientation);
        if (target != 0.0)
            throw new IllegalArgumentException("MTF proxy target must be zero");
        if (analysis == null || analysis._fields == null || field < 1 || field > analysis._fields.length)
            throw new IllegalArgumentException("field must select a configured one-based analysis field");
        if (!Double.isFinite(freq) || freq <= 0.0)
            throw new IllegalArgumentException("frequency must be finite and positive");
        if (!Double.isFinite(wvl) || wvl <= 0.0 || analysis._prescription == null
                || analysis._prescription._wvls == null
                || Arrays.stream(analysis._prescription._wvls).noneMatch(w -> w == wvl))
            throw new IllegalArgumentException("wavelength must match a configured prescription wavelength in nm");
        if (!Double.isFinite(weight) || weight < 0.0)
            throw new IllegalArgumentException("weight must be finite and non-negative");
        if (pos < -Analysis.NUM_TRANSVERSE_RAYS || pos >= Analysis.NUM_TRANSVERSE_RAYS)
            throw new IllegalArgumentException("sample index out of range");
        this._field = field-1;
        this._orientation = orientation;
        this._pos = pos;
        this._wvl = wvl;
        this._freq = freq;
    }

    @Override
    public double value() {
        Double argument = sineArgument();
        if (argument == null) return LMDerMeritFunction.BIGVAL;
        double result = Math.sin(argument);
        return Double.isFinite(result) ? result : LMDerMeritFunction.BIGVAL;
    }

    private Double sineArgument() {
        if (_analysis._ray_aberrations == null) return null;
        var fans = _analysis._ray_aberrations.get_fans(_field, _orientation, _wvl);
        if (fans == null || fans.fan_y == null) return null;
        int index = _pos < 0 ? fans.fan_y.size() + _pos : _pos;
        if (index < 0 || index >= fans.fan_y.size()) return null;
        Double aberration = fans.fan_y.get(index);
        if (aberration == null || !Double.isFinite(aberration)) return null;
        // Multiply f*delta first so that a representable argument is not lost to pi*f overflow.
        double argument = Math.PI * (_freq * aberration);
        return Double.isFinite(argument) ? argument : null;
    }

    /** Summary of selected positive-weight samples at one frequency; no rays are traced here. */
    public record FrequencyDiagnostic(double frequency, int validSamples, int invalidSamples,
                                      double maxArgument, GoalMTFProxy worstSample,
                                      GoalMTFProxy firstInvalidSample) {
        public boolean outsideMonotonicInterval() {
            return maxArgument >= Math.PI / 2.0;
        }
    }

    /** Inspect the current analysis results. Call after compute(), with frequency held fixed. */
    public static List<FrequencyDiagnostic> frequencyDiagnostics(Goal[] goals) {
        var groups = new LinkedHashMap<Double, List<GoalMTFProxy>>();
        for (Goal goal : goals)
            if (goal instanceof GoalMTFProxy proxy && proxy._weight > 0.0)
                groups.computeIfAbsent(proxy._freq, key -> new ArrayList<>()).add(proxy);
        var diagnostics = new ArrayList<FrequencyDiagnostic>();
        for (var group : groups.entrySet()) {
            int valid = 0, invalid = 0;
            double max = Double.NaN;
            GoalMTFProxy worst = null, firstInvalid = null;
            for (var proxy : group.getValue()) {
                Double argument = proxy.sineArgument();
                if (argument == null) {
                    invalid++;
                    if (firstInvalid == null) firstInvalid = proxy;
                } else {
                    valid++;
                    if (worst == null || Math.abs(argument) > max) {
                        max = Math.abs(argument);
                        worst = proxy;
                    }
                }
            }
            diagnostics.add(new FrequencyDiagnostic(group.getKey(), valid, invalid, max, worst, firstInvalid));
        }
        return List.copyOf(diagnostics);
    }

    /** Sample identification without evaluating a residual. */
    public String sampleDescription() {
        return "field=" + (_field + 1) + ", orientation=" + Orientation.name(_orientation)
                + ", wavelength=" + _wvl + " nm, sample=" + _pos;
    }

    @Override
    public String toString() {
        return "MTFProxy field=" + _field + ", orientation=" + Orientation.name(_orientation) + ", pos=" + _pos + ", target=" + _target + ", weight=" + _weight + " = " + value();
    }
}
