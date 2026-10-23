// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.util.Orientation;

import java.util.ArrayList;
import java.util.List;

/**
 * Where in the pupil one contrast block loses contrast, sample by sample.
 *
 * <p>Each valid sample gets a loss {@code m = (1 - cos(dphi - phi_mean)) / 2}, where
 * {@code dphi = 2 pi dW} is its phase difference across the shear and {@code phi_mean} the
 * phase of the block's weighted mean phasor. {@code m} is 0 where the pair is in phase with
 * the block, 1/2 at a quarter wave and 1 at half a wave. The weighted losses add up to the
 * phasor modulus: {@code |sum w exp(i dphi)| / sum w = 1 - 2 sum w m / sum w}, the MTF
 * relative to an aberration-free pupil over the sampled region.
 *
 * <p>Zemax's contrast pupil map uses {@code dphi} itself, not {@code dphi - phi_mean}. The
 * two agree when the mean phase is zero, as it is sagittally on a rotationally symmetric
 * lens. Referencing the mean phase keeps a pure image shift, which costs no contrast, from
 * showing as loss.
 *
 * <p>Each sample also carries the average phase of its pair, {@code pi (W(p) + W(p+s))}, so a
 * plot can show the wavefront shape. It needs the reference OPD, which
 * {@link ContrastAnalysisResult.Sample#referenceOpd()} records; it is NaN otherwise.
 *
 * <p>The samples are those of the contrast analysis, so the map covers the region it
 * samples, not the full overlap of the pupil with its shifted copy.
 */
public class ContrastPupilMap {

    /**
     * @param loss          {@code (1 - cos(dphi - phi_mean)) / 2}, between 0 and 1
     * @param average_phase {@code pi (W(p) + W(p+s))} in radians, or NaN
     * @param weight        the sample's quadrature weight
     */
    public record Point(Vector2 pupil, double loss, double average_phase, double weight) {
    }

    public final double wavelength;
    public final int orientation;
    public final List<Point> points;
    /** phase of the weighted mean phasor, radians */
    public final double mean_phase;
    /** {@code |sum w exp(i dphi)| / sum w}, equal to {@code 1 - 2 * weighted mean loss} */
    public final double phasor;
    /** weighted RMS of the OPD difference about the block's offset, waves */
    public final double rms;

    public ContrastPupilMap(ContrastAnalysisResult.WavelengthResult block, int orientation) {
        Orientation.checked(orientation);
        this.wavelength = block.wavelength();
        this.orientation = orientation;
        boolean sagittal = orientation == Orientation.SAGITTAL;

        double re = 0, im = 0, w = 0;
        for (var s : block.samples()) {
            double d = difference(s, sagittal);
            if (!usable(s, d)) continue;
            re += s.weight() * Math.cos(2 * Math.PI * d);
            im += s.weight() * Math.sin(2 * Math.PI * d);
            w += s.weight();
        }
        if (!(w > 0))
            throw new IllegalArgumentException("the contrast block has no valid samples");
        mean_phase = Math.atan2(im, re);
        phasor = Math.hypot(re, im) / w;
        rms = ContrastReport.rms(block, orientation);

        var list = new ArrayList<Point>();
        for (var s : block.samples()) {
            double d = difference(s, sagittal);
            if (!usable(s, d)) continue;
            double loss = 0.5 * (1 - Math.cos(2 * Math.PI * d - mean_phase));
            double average = Math.PI * (2 * s.referenceOpd() + d);
            list.add(new Point(s.pupil(), loss, average, s.weight() / w));
        }
        points = List.copyOf(list);
    }

    private static double difference(ContrastAnalysisResult.Sample s, boolean sagittal) {
        return sagittal ? s.sagittalOpdDifference() : s.tangentialOpdDifference();
    }

    private static boolean usable(ContrastAnalysisResult.Sample s, double d) {
        return s.valid() && Double.isFinite(d) && s.weight() > 0;
    }
}
