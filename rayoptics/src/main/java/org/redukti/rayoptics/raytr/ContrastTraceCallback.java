package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.specs.Field;

@FunctionalInterface
public interface ContrastTraceCallback<T> {
    T apply(ContrastRayTriplet rays, Field field, double wavelength, double focus);
}
