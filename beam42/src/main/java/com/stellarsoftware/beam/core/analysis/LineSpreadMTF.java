package com.stellarsoftware.beam.core.analysis;

import com.stellarsoftware.beam.core.U;

/** MTF magnitude and frequency axis calculated from a sampled line-spread function. */
public final class LineSpreadMTF {
    private final double[] frequency;
    private final double[] magnitude;

    public LineSpreadMTF(int[] lineSpread, double pixelSize) {
        if (lineSpread == null || lineSpread.length == 0 || !Double.isFinite(pixelSize) || pixelSize <= 0)
            throw new IllegalArgumentException("Invalid line-spread function");

        int fftSize = nextPowerOfTwo(2 * lineSpread.length);
        double[] complex = new double[2 * fftSize];
        int offset = (fftSize - lineSpread.length) / 2;
        for (int i = 0; i < lineSpread.length; i++)
            complex[2 * (offset + i)] = lineSpread[i];

        int[] dimensions = {fftSize};
        U.fourn(complex, dimensions, 1, 1);
        frequency = new double[fftSize / 2 + 1];
        magnitude = new double[frequency.length];
        double dc = Math.hypot(complex[0], complex[1]);
        for (int i = 0; i < frequency.length; i++) {
            frequency[i] = i / (fftSize * pixelSize);
            magnitude[i] = dc == 0.0 ? 0.0
                    : Math.hypot(complex[2 * i], complex[2 * i + 1]) / dc;
        }
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            if (result > (1 << 29))
                throw new IllegalArgumentException("Line-spread function is too large");
            result <<= 1;
        }
        return result;
    }

    public double[] frequency() { return frequency.clone(); }
    public double[] magnitude() { return magnitude.clone(); }
}
