// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.spec;

import org.redukti.rayoptics.specs.WvlSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The wavelengths a design is evaluated at, and the weight each one carries.
 *
 * <p>The weights do two jobs, both of them the same decision: the analyses combine
 * per-wavelength results with them, and the optimizer multiplies them into the weight of
 * every per-wavelength residual. A wavelength weighted 0.15 therefore counts for 0.15 of a
 * wavelength weighted 1.0 in a polychromatic MTF and in the merit alike.
 *
 * <p>Three sets cover the usual choices - {@link #dLine()}, {@link #standardTriColor()} and
 * {@link #standardFiveColor()} - with {@link #k9Tester()} for comparing against measured
 * curves. Nothing here is limited to them: {@link #of} takes any wavelengths and weights.
 *
 * <p><b>What the glasses can answer limits the choice.</b> A glass from a catalog holds
 * measured indices at d, C, F, e, C', F' and g only, and
 * {@link org.redukti.rayoptics.seq.Glass#rindex(double)} throws for any other wavelength.
 * A glass given as nd and vd computes its index from a dispersion model instead, so any
 * wavelength works there as an approximation. The other lines {@link #ofLetters} accepts
 * are for that case, and for when more wavelength data can be loaded.
 */
public final class WavelengthSet {

    /**
     * The spectral lines a set can be spelled with, longest wavelength first. The
     * wavelengths themselves come from {@link WvlSpec#get_wavelength(String)}, which is
     * the ported table and the one authority for what a line's wavelength is; this list
     * only fixes the order a set is written in and the names a trial may use. Laser lines
     * {@code Nd} and {@code He-Ne} are resolvable there too but are not Fraunhofer lines,
     * so they are left out of the names a trial writes.
     *
     * <p>Wider than what a catalog glass can answer, deliberately; see the class comment.
     */
    private static final List<String> LINES =
            List.of("t", "s", "r", "C", "C'", "D", "d", "e", "F", "F'", "g", "h", "i");

    /** Within this of a line's wavelength is that line, for writing a set back out. */
    private static final double LINE_TOLERANCE = 1.0e-6;

    /**
     * The weights of {@link #standardFiveColor()}, in its order. They are the relative
     * wavelength weights of the MTF design data in T. Steinich and V. Blahnik, "Optical
     * design of camera optics for mobile phones," Adv. Opt. Technol. <b>1</b>(1-2), 51-58
     * (2012), <a href="https://doi.org/10.1515/aot-2012-0002">doi:10.1515/aot-2012-0002</a>
     * - 151, 318, 312, 157 and 49 for C, d, e, F and g - divided by the d weight. That
     * paper also weights 404.66 nm at 13 (0.041); this set leaves it out.
     */
    public static final double WT_d = 1.0;
    public static final double WT_C = 0.475;
    public static final double WT_e = 0.98;
    public static final double WT_F = 0.49;
    public static final double WT_g = 0.15;

    /**
     * The weights of {@link #k9Tester()}, in its order.
     *
     * <p>Read off the normalized spectral weighting curve in the ZEISS MTF-Tester K9 data
     * sheet and divided by the value at d. The curve is what its halogen lamp at 3200 K,
     * its BG 38 filter and its Hamamatsu R446 photomultiplier come to between 380 and
     * 730 nm; it peaks near 555-560 nm, so e carries slightly more than d.
     *
     * <p><b>Treat these as approximate.</b> They were digitized from a plot in a single
     * product data sheet - one source, no tabulated values - so a weight is good to a
     * point or two at best. The data sheet is for the K9 service tester; whether the K8
     * that measures published MTF curves weights the same way is not established here.
     *
     * Source https://web.archive.org/web/20071118233344/http://www.zeiss.com/C12567A8003B8B6F/EmbedTitelIntern/ProspektK9_e_PDF/$File/K9_e_Version_021121.pdf
     */
    public static final double K9_WT_d = 1.0;
    public static final double K9_WT_C = 0.330;
    public static final double K9_WT_e = 1.046;
    public static final double K9_WT_F = 0.654;
    public static final double K9_WT_g = 0.235;

    private final double[] _wavelengths;
    private final double[] _weights;

    private WavelengthSet(double[] wavelengths, double[] weights) {
        _wavelengths = wavelengths;
        _weights = weights;
    }

    /** The d line alone. */
    public static WavelengthSet dLine() {
        return ofLetters(List.of("d"), null);
    }

    /** The e line alone. */
    public static WavelengthSet eLine() {
        return ofLetters(List.of("e"), null);
    }

    /** d, F and C at equal weight. */
    public static WavelengthSet standardTriColor() {
        return ofLetters(List.of("d", "F", "C"), null);
    }

    /** d, C, e, F and g at the standard weights; see {@link #hasStandardWeights()}. */
    public static WavelengthSet standardFiveColor() {
        return ofLetters(List.of("d", "C", "e", "F", "g"),
                new double[]{WT_d, WT_C, WT_e, WT_F, WT_g});
    }

    /**
     * The same five lines weighted as the ZEISS MTF-Tester K9 weights the spectrum, which
     * is broader than a photopic response - at g it passes 0.235 against photopic's 0.018.
     * Useful for comparing against measured MTF curves rather than for design. See
     * {@link #K9_WT_d} for where the numbers come from and how far to trust them.
     */
    public static WavelengthSet k9Tester() {
        return ofLetters(List.of("d", "C", "e", "F", "g"),
                new double[]{K9_WT_d, K9_WT_C, K9_WT_e, K9_WT_F, K9_WT_g});
    }

    /**
     * @param wavelengths in nanometres, at least one, each positive
     * @param weights     one per wavelength, each finite and non-negative
     */
    public static WavelengthSet of(double[] wavelengths, double[] weights) {
        if (wavelengths == null || wavelengths.length == 0)
            throw new IllegalArgumentException("at least one wavelength is required");
        if (weights == null || weights.length != wavelengths.length)
            throw new IllegalArgumentException("expected one weight per wavelength: "
                    + wavelengths.length + " wavelengths but "
                    + (weights == null ? 0 : weights.length) + " weights");
        for (double wavelength : wavelengths)
            if (!Double.isFinite(wavelength) || wavelength <= 0.0)
                throw new IllegalArgumentException("wavelengths must be positive, in nanometres");
        for (double weight : weights)
            if (!Double.isFinite(weight) || weight < 0.0)
                throw new IllegalArgumentException("wavelength weights must be finite and non-negative");
        return new WavelengthSet(wavelengths.clone(), weights.clone());
    }

    /** The set a prescription was built with. */
    public static WavelengthSet of(Prescription prescription) {
        return of(prescription._wvls, prescription._wts);
    }

    /**
     * @param letters Fraunhofer letters, as {@link #letters()} writes them
     * @param weights one per letter, or null for equal weights of 1.0
     */
    public static WavelengthSet ofLetters(List<String> letters, double[] weights) {
        if (letters == null || letters.isEmpty())
            throw new IllegalArgumentException("at least one wavelength is required");
        double[] wavelengths = new double[letters.size()];
        for (int i = 0; i < wavelengths.length; i++)
            wavelengths[i] = wavelengthOf(letters.get(i));
        double[] resolved = weights;
        if (resolved == null) {
            resolved = new double[wavelengths.length];
            Arrays.fill(resolved, 1.0);
        }
        return of(wavelengths, resolved);
    }

    /** @throws IllegalArgumentException naming the lines that can be used */
    public static double wavelengthOf(String letter) {
        if (!LINES.contains(letter))
            throw new IllegalArgumentException("unknown wavelength '" + letter
                    + "'; expected one of: " + String.join(" ", LINES));
        return WvlSpec.get_wavelength(letter);
    }

    /** The letter for a wavelength, or null if no spectral line is that wavelength. */
    public static String letterOf(double wavelength) {
        for (String line : LINES)
            if (Math.abs(WvlSpec.get_wavelength(line) - wavelength) <= LINE_TOLERANCE)
                return line;
        return null;
    }

    public double[] wavelengths() {
        return _wavelengths.clone();
    }

    public double[] weights() {
        return _weights.clone();
    }

    public int size() {
        return _wavelengths.length;
    }

    /** True when every weight is 1.0, the case a trial writes without a weights clause. */
    public boolean hasEqualWeights() {
        for (double weight : _weights)
            if (weight != 1.0)
                return false;
        return true;
    }

    /** True when this is {@link #standardFiveColor()}, the case a trial writes as {@code standard}. */
    public boolean hasStandardWeights() {
        return equals(standardFiveColor());
    }

    /**
     * The wavelengths as Fraunhofer letters, in order.
     *
     * @throws IllegalStateException if some wavelength is not a Fraunhofer line, which a
     *                               trial has no way to spell
     */
    public String letters() {
        List<String> letters = new ArrayList<>(_wavelengths.length);
        for (double wavelength : _wavelengths) {
            String letter = letterOf(wavelength);
            if (letter == null)
                throw new IllegalStateException("wavelength " + wavelength
                        + " nm is not a Fraunhofer line, so it cannot be written as a trial");
            letters.add(letter);
        }
        return String.join(" ", letters);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WavelengthSet that)) return false;
        return Arrays.equals(_wavelengths, that._wavelengths)
                && Arrays.equals(_weights, that._weights);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(_wavelengths) + Arrays.hashCode(_weights);
    }

    @Override
    public String toString() {
        return "WavelengthSet{" + Arrays.toString(_wavelengths)
                + ", weights=" + Arrays.toString(_weights) + "}";
    }
}
