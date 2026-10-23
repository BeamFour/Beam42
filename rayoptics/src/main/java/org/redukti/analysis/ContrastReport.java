// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.M;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.util.Orientation;

import java.text.DecimalFormat;

/**
 * Tabulates the wavefront differences the contrast optimizer works on, one row per
 * frequency, wavelength and orientation, one column per field.
 *
 * <p>Each value is the quadrature-weighted RMS of the OPD difference over the valid
 * samples of that block, in waves: {@code sqrt(sum w dW^2 / sum w)}. Its square is the
 * block's contribution to the contrast merit before field, orientation and wavelength
 * weights, normalized by the weight of the samples that traced.
 */
public class ContrastReport {

    private static final int COLUMN_WIDTH = 9;
    private static final DecimalFormat VALUE_FORMAT = M.decimal_format(4);
    private static final DecimalFormat LABEL_FORMAT = M.decimal_format(4);

    /** Weighted RMS of one orientation's OPD differences, or NaN if no sample is valid. */
    public static double rms(ContrastAnalysisResult.WavelengthResult block, int orientation) {
        double sum = 0.0;
        double weight = 0.0;
        for (int i = 0; i < block.samples().size(); i++) {
            var sample = block.samples().get(i);
            if (!sample.valid()) continue;
            double residual = orientation == Orientation.SAGITTAL
                    ? block.sagittalResidual(i) : block.tangentialResidual(i);
            if (!Double.isFinite(residual)) continue;
            sum += residual * residual;
            weight += sample.weight();
        }
        return weight > 0.0 ? Math.sqrt(sum / weight) : Double.NaN;
    }

    /** True if any sample of the block failed to trace. */
    private static boolean incomplete(ContrastAnalysisResult.WavelengthResult block) {
        for (var sample : block.samples())
            if (!sample.valid()) return true;
        return false;
    }

    /**
     * Evaluates the contrast analysis at each frequency with {@code options} and formats
     * the table. A frequency the analysis cannot sample, for instance one with no common
     * pupil overlap, is reported as a line of its own rather than failing the report.
     *
     * @param options supplies everything but the frequency: sampling, calibration,
     *                exit-pupil aiming and centering
     */
    public static String report(OpticalModel opticalModel, int[] frequencies, ContrastOptions options) {
        var sb = new StringBuilder();
        sb.append("RMS OPD difference (waves), sampling ")
                .append(options.numRings).append(" rings x ").append(options.numSpokes).append(" spokes")
                .append(", calibrate ").append(yesNo(options.calibrateFrequency))
                .append(", exit-pupil-aiming ").append(yesNo(options.aimExitPupil))
                .append(", centering ").append(yesNo(options.centerResiduals))
                .append("\n\n");

        var fields = opticalModel.optical_spec.fov.fields;
        cell(sb, "freq");
        cell(sb, "wvl");
        cell(sb, "dir");
        for (var field : fields)
            cell(sb, LABEL_FORMAT.format(field.y));
        sb.append('\n');

        var notes = new StringBuilder();
        boolean anyIncomplete = false;
        for (int frequency : frequencies) {
            ContrastAnalysisResult result;
            try {
                result = ContrastAnalysis.eval(opticalModel, options.with_frequency(frequency));
            }
            catch (IllegalArgumentException e) {
                notes.append("frequency ").append(frequency).append(": ").append(e.getMessage()).append('\n');
                continue;
            }
            var wavelengths = opticalModel.optical_spec.wvls.wavelengths;
            for (int wi = 0; wi < wavelengths.length; wi++) {
                for (int orientation : new int[]{Orientation.TANGENTIAL, Orientation.SAGITTAL}) {
                    cell(sb, Integer.toString(frequency));
                    cell(sb, LABEL_FORMAT.format(wavelengths[wi]));
                    cell(sb, Orientation.name(orientation));
                    for (var field : result.fields) {
                        var block = field.wavelengths().get(wi);
                        double value = rms(block, orientation);
                        boolean partial = incomplete(block);
                        anyIncomplete |= partial;
                        cell(sb, Double.isNaN(value) ? "-"
                                : VALUE_FORMAT.format(value) + (partial ? "*" : ""));
                    }
                    sb.append('\n');
                }
            }
        }
        if (anyIncomplete)
            sb.append("\n* some samples failed to trace; the value is over the samples that did\n");
        if (!notes.isEmpty())
            sb.append('\n').append(notes);
        return sb.toString();
    }

    private static void cell(StringBuilder sb, String value) {
        sb.append(" ".repeat(Math.max(1, COLUMN_WIDTH - value.length()))).append(value);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
