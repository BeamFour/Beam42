// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.optim;

import org.redukti.rayoptics.util.Orientation;

/** A single weighted pupil phase-difference residual for contrast optimization. */
public class GoalContrast extends Goal {
    public final int _contrast_index;
    public final int _frequency;
    public final int _field;
    public final int _wavelength_index;
    public final int _sample_index;
    /** Zero-based identities in the nominal GQ pattern, before pupil mapping. */
    public final int _ring_index;
    public final int _spoke_index;
    private final int _num_rings;
    private final int _num_spokes;
    public final int _orientation;

    /** Select a GQ node explicitly; ordering is spoke first, then ring. */
    public GoalContrast(Analysis analysis, int contrast_index, int frequency, int field,
                        int wavelength_index, int ring_index, int spoke_index,
                        int orientation, double weight) {
        this(analysis, contrast_index, frequency, field, wavelength_index,
                sampleIndex(analysis, ring_index, spoke_index), orientation, weight);
    }

    /** Compatibility constructor for callers supplying a flattened sample index. */
    public GoalContrast(Analysis analysis,
                        int contrast_index,
                        int frequency,
                        int field,
                        int wavelength_index,
                        int sample_index,
                        int orientation,
                        double weight) {
        super(analysis, 0.0, weight);
        if (contrast_index < 0) throw new IllegalArgumentException("contrast index must be non-negative");
        if (field < 1) throw new IllegalArgumentException("field is one based and must be positive");
        if (wavelength_index < 0 || sample_index < 0) throw new IllegalArgumentException("indices must be non-negative");
        Orientation.checked(orientation);
        if (analysis == null || analysis._contrast_num_rings < 1 || analysis._contrast_num_spokes < 3)
            throw new IllegalArgumentException("A contrast GQ pattern is required");
        _num_rings = analysis._contrast_num_rings;
        _num_spokes = analysis._contrast_num_spokes;
        if (sample_index >= _num_rings * _num_spokes)
            throw new IllegalArgumentException("sample index is outside the contrast GQ pattern");
        _contrast_index = contrast_index;
        _frequency = frequency;
        _field = field - 1;
        _wavelength_index = wavelength_index;
        _sample_index = sample_index;
        _ring_index = sample_index % _num_rings;
        _spoke_index = sample_index / _num_rings;
        _orientation = orientation;
    }

    @Override
    public double value() {
        // A changed pattern must not silently associate this goal with another node.
        if (_analysis._contrast_num_rings != _num_rings || _analysis._contrast_num_spokes != _num_spokes)
            return LMDerMeritFunction.BIGVAL;
        if (_analysis._contrasts == null || _contrast_index >= _analysis._contrasts.length)
            return LMDerMeritFunction.BIGVAL;
        var contrast = _analysis._contrasts[_contrast_index];
        if (contrast == null) return LMDerMeritFunction.BIGVAL;
        if (_field >= contrast.fields.size()) return LMDerMeritFunction.BIGVAL;
        var wavelengths = contrast.fields.get(_field).wavelengths();
        if (_wavelength_index >= wavelengths.size()) return LMDerMeritFunction.BIGVAL;
        var wavelength = wavelengths.get(_wavelength_index);
        if (_sample_index >= wavelength.samples().size()) return LMDerMeritFunction.BIGVAL;
        if (!wavelength.samples().get(_sample_index).valid()) return LMDerMeritFunction.BIGVAL;
        // Read through the block rather than the sample: the residual carries the block's
        // constant offset, which is zero unless residual centering is enabled.
        return _orientation == Orientation.SAGITTAL
                ? wavelength.sagittalResidual(_sample_index)
                : wavelength.tangentialResidual(_sample_index);
    }

    private static int sampleIndex(Analysis analysis, int ring, int spoke) {
        if (analysis == null || ring < 0 || ring >= analysis._contrast_num_rings
                || spoke < 0 || spoke >= analysis._contrast_num_spokes)
            throw new IllegalArgumentException("ring or spoke is outside the contrast GQ pattern");
        return spoke * analysis._contrast_num_rings + ring;
    }

    @Override
    public String toString() {
        return "Contrast index=" + _contrast_index + ", frequency=" + _frequency + ", field=" + _field
                + ", wavelength=" + _wavelength_index + ", sample=" + _sample_index
                + ", ring=" + _ring_index + ", spoke=" + _spoke_index
                + ", orientation=" + Orientation.name(_orientation) + ", weight=" + _weight
                + " = " + value();
    }
}
