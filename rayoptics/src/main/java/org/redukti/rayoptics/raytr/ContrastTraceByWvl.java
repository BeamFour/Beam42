package org.redukti.rayoptics.raytr;

import java.util.List;

public record ContrastTraceByWvl<T>(double wavelength, List<T> samples) {
}
