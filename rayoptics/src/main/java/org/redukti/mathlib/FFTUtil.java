// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.mathlib;

import org.redukti.mathlib.fftpack.ComplexDoubleFFT;

/**
 * Replacements for the numpy.fft functions used by the ray-optics port.
 *
 * <p>Complex arrays are square, with rows interleaved re, im: element [i][j] is
 * {@code x[i][2*j] + 1j*x[i][2*j+1]}.
 */
public class FFTUtil {

    /** numpy.fft.fftshift over both axes of a square complex array, rows interleaved re, im. */
    public static double[][] fftshift(double[][] x) {
        int n = x.length;
        int shift = n / 2;
        var y = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            int si = (i + shift) % n;
            for (int j = 0; j < n; j++) {
                int sj = (j + shift) % n;
                y[si][2 * sj] = x[i][2 * j];
                y[si][2 * sj + 1] = x[i][2 * j + 1];
            }
        }
        return y;
    }

    /** numpy.fft.fft2 of a square complex array, rows interleaved re, im. */
    public static double[][] fft2(double[][] x) {
        int n = x.length;
        var fft = new ComplexDoubleFFT(n);
        var y = new double[n][];
        for (int i = 0; i < n; i++) {
            y[i] = x[i].clone();
            fft.ft(y[i]);
        }
        var column = new double[2 * n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                column[2 * i] = y[i][2 * j];
                column[2 * i + 1] = y[i][2 * j + 1];
            }
            fft.ft(column);
            for (int i = 0; i < n; i++) {
                y[i][2 * j] = column[2 * i];
                y[i][2 * j + 1] = column[2 * i + 1];
            }
        }
        return y;
    }
}
