package org.redukti.mathlib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FFTUtilTest {

    @Test
    void fft2UsesTheNumpySignConvention() {
        // numpy.fft.fft([0, 1, 0, 0]) = [1, -1j, -1, 1j]; in 2d, the first row carries it
        var x = new double[4][8];
        x[0][2] = 1.0;
        var y = FFTUtil.fft2(x);
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, y[i][0], 1e-15);
            assertEquals(0.0, y[i][1], 1e-15);
            assertEquals(0.0, y[i][2], 1e-15);
            assertEquals(-1.0, y[i][3], 1e-15);
            assertEquals(-1.0, y[i][4], 1e-15);
            assertEquals(1.0, y[i][7], 1e-15);
        }
    }

    @Test
    void fftshiftMovesTheOriginToTheCentre() {
        var x = new double[5][10];
        x[0][0] = 1.0;
        var y = FFTUtil.fftshift(x);
        assertEquals(1.0, y[2][4]);
    }
}
