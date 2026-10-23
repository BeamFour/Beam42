package org.redukti.rayoptics.raytr;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link Analyses} and {@link RayGrid} port.
 *
 * <p>The pinned values were captured from upstream ray-optics running
 * {@code analyses.RayGrid(opm, f, foc=foc, num_rays=20)} and
 * {@code analyses.calc_psf(grid[2], 20, 64)} on the model of
 * {@code US004123144Example08PUpstreamTest}, built from the same generated script. On
 * that model the Java port reproduced every grid OPD, eval_wavefront value and scaling
 * exactly, and the PSF to 1.1e-15.
 */
class AnalysesTest {

    private static OpticalModel upstreamModel() throws Exception {
        var build = Class.forName("org.redukti.rayoptics.upstream.US004123144Example08PUpstreamTest")
                .getDeclaredMethod("build");
        build.setAccessible(true);
        return (OpticalModel) build.invoke(null);
    }

    private static RayGrid grid(OpticalModel opm, int f, double foc) {
        return new RayGrid(opm, f, null, foc, null, null, null, null, 20, true, Double.NaN, 1.0);
    }

    @Test
    void rayGridReproducesUpstreamOpd() throws Exception {
        var opm = upstreamModel();
        var focused = grid(opm, 2, 0.0);
        assertEquals(-0.8109431712108661, focused.grid[0][0][0], 1e-15);
        assertEquals(-0.477476674867249, focused.grid[1][0][0], 1e-15);
        assertEquals(0.015908313797373393, focused.grid[2][10][10], 1e-13);

        var defocused = grid(opm, 2, 0.05);
        assertEquals(1.1270749042956802, defocused.grid[2][3][15], 1e-12);
        // Clipped by an aperture, as upstream
        assertTrue(Double.isNaN(grid(opm, 1, 0.05).grid[2][0][10]));
    }

    @Test
    void evalWavefrontAgreesWithTheRefocusedGrid() throws Exception {
        var opm = upstreamModel();
        var focused = grid(opm, 2, 0.0);
        var options = new TraceOptions();
        options.check_apertures = true;
        var fld = opm.optical_spec.fov.fields[2];
        var eval = Analyses.eval_wavefront(opm, fld, opm.optical_spec.wvls.central_wvl(), 0.0,
                null, null, 20, Double.NaN, 1.0, options);
        assertEquals(0.015908313797373393, eval[10][10][2], 1e-13);
        for (int i = 0; i < 20; i++)
            for (int j = 0; j < 20; j++)
                assertEquals(focused.grid[2][i][j], eval[i][j][2], 1e-12);
    }

    @Test
    void calcPsfReproducesUpstream() throws Exception {
        var opm = upstreamModel();
        var AP = Analyses.calc_psf(grid(opm, 2, 0.0).grid[2], 20, 64);
        assertEquals(0.44459091011809493, AP[32][32], 1e-13);
        assertEquals(0.3075630882612663, AP[30][35], 1e-13);
        var defocused = Analyses.calc_psf(grid(opm, 0, 0.05).grid[2], 20, 64);
        assertEquals(0.8138388544390374, defocused[39][25], 1e-13);

        var scaling = grid(opm, 2, 0.0).calc_psf_scaling(64);
        assertEquals(1.3004005879190803, scaling[0], 1e-15);
        assertEquals(0.00041164120903762653, scaling[1], 1e-18);
    }

    @Test
    void calcPsfRejectsAnOddGridAsNumpyDoes() {
        assertThrows(IllegalArgumentException.class,
                () -> Analyses.calc_psf(new double[21][21], 21, 64));
    }

    @Test
    void psfSamplingMatchesUpstream() {
        assertArrayEquals(new int[]{57, 20, 7}, Analyses.psf_sampling(null, 20, 7));
        assertArrayEquals(new int[]{64, 31, 5}, Analyses.psf_sampling(64, null, 5));
        assertArrayEquals(new int[]{64, 25, 6}, Analyses.psf_sampling(64, 25, null));
        assertThrows(IllegalArgumentException.class, () -> Analyses.psf_sampling(64, 25, 6));
    }

    @Test
    void fft2UsesTheNumpySignConvention() {
        // numpy.fft.fft([0, 1, 0, 0]) = [1, -1j, -1, 1j]; in 2d, the first row carries it
        var x = new double[4][8];
        x[0][2] = 1.0;
        var y = Analyses.fft2(x);
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
        var y = Analyses.fftshift(x);
        assertEquals(1.0, y[2][4]);
    }
}
