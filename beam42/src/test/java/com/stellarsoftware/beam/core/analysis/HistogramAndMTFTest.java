package com.stellarsoftware.beam.core.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistogramAndMTFTest {
    @Test
    void histogramTracksOnlyValuesInsideBounds() {
        Histogram1D histogram = new Histogram1D(4, 0.0, 4.0);
        assertTrue(histogram.add(0.5));
        assertTrue(histogram.add(3.5));
        assertFalse(histogram.add(4.0));
        assertEquals(3, histogram.count());
        assertEquals(8.0 / 3.0, histogram.average());
        assertArrayEquals(new int[]{1, 0, 0, 1}, histogram.bins());
    }

    @Test
    void emptyLineSpreadProducesFiniteZeroMtf() {
        LineSpreadMTF mtf = new LineSpreadMTF(new int[3], 0.25);
        assertEquals(5, mtf.magnitude().length); // FFT padded to 8
        for (double value : mtf.magnitude()) {
            assertTrue(Double.isFinite(value));
            assertEquals(0.0, value);
        }
    }

    @Test
    void impulseHasUnitMagnitudeAndCompleteFrequencyAxis() {
        LineSpreadMTF mtf = new LineSpreadMTF(new int[]{0, 1, 0}, 0.25);
        assertArrayEquals(new double[]{0, .5, 1, 1.5, 2}, mtf.frequency(), 1e-12);
        for (double value : mtf.magnitude())
            assertEquals(1.0, value, 5e-8);
    }
}
