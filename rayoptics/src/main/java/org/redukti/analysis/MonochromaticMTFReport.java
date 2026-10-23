// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.analysis;

import org.redukti.mathlib.M;
import org.redukti.rayoptics.util.Orientation;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Tabulates the monochromatic geometric MTF, one row per frequency, wavelength and
 * orientation, one column per field. The layout is that of {@link ContrastReport}, so
 * the two can be read side by side.
 */
public class MonochromaticMTFReport {

    private static final int COLUMN_WIDTH = 9;
    private static final DecimalFormat VALUE_FORMAT = M.decimal_format(3);
    private static final DecimalFormat LABEL_FORMAT = M.decimal_format(4);

    /**
     * @param fields      field labels, one per entry of {@code mtfs}
     * @param mtfs        per field, the monochromatic MTF of each wavelength, in the same
     *                    wavelength order for every field
     * @param frequencies cycles/mm
     */
    public static String report(double[] fields, List<List<MonochromaticGeometricMTF>> mtfs, int[] frequencies) {
        if (fields.length != mtfs.size())
            throw new IllegalArgumentException("one field label is needed per field");
        var sb = new StringBuilder();
        sb.append("Monochromatic geometric MTF\n\n");
        cell(sb, "freq");
        cell(sb, "wvl");
        cell(sb, "dir");
        for (double field : fields)
            cell(sb, LABEL_FORMAT.format(field));
        sb.append('\n');
        if (mtfs.isEmpty())
            return sb.toString();

        // Rows follow the wavelengths of the first field; a field without one shows "-".
        var wavelengths = mtfs.get(0);
        for (int frequency : frequencies) {
            for (var wavelength : wavelengths) {
                for (int orientation : new int[]{Orientation.TANGENTIAL, Orientation.SAGITTAL}) {
                    cell(sb, Integer.toString(frequency));
                    cell(sb, LABEL_FORMAT.format(wavelength.wvl));
                    cell(sb, Orientation.name(orientation));
                    for (var field : mtfs) {
                        var mono = find(field, wavelength.wvl);
                        if (mono == null) {
                            cell(sb, "-");
                            continue;
                        }
                        var mtf = mono.mtf;
                        double value = MTFResultByFreq.interpolate(mtf.freq,
                                orientation == Orientation.SAGITTAL ? mtf.mag_x : mtf.mag_y, frequency);
                        cell(sb, VALUE_FORMAT.format(value));
                    }
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static MonochromaticGeometricMTF find(List<MonochromaticGeometricMTF> field, double wvl) {
        for (var mono : field)
            if (mono.wvl == wvl) return mono;
        return null;
    }

    private static void cell(StringBuilder sb, String value) {
        sb.append(" ".repeat(Math.max(1, COLUMN_WIDTH - value.length()))).append(value);
    }
}
