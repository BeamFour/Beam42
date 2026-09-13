package org.redukti.optim;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;

/** Domain rules shared by the Java API and text reader; callers supply contextual errors. */
final class OptimizationValidation {
    private OptimizationValidation() {}

    static void fieldCount(double[] values, int count, Supplier<? extends IllegalArgumentException> error) {
        if (values == null || values.length != count)
            throw error.get();
    }

    /** Inclusive non-negative range; an infinite upper limit still rejects non-finite values. */
    static void range(double[] values, double upper, Supplier<? extends IllegalArgumentException> error) {
        for (double value : values)
            if (!Double.isFinite(value) || value < 0.0 || value > upper)
                throw error.get();
    }

    static void frequency(int value, int[] measured, Supplier<? extends IllegalArgumentException> error) {
        if (Arrays.stream(measured).noneMatch(f -> f == value))
            throw error.get();
    }

    static void positiveUnique(int value, Set<Integer> seen,
                               Supplier<? extends IllegalArgumentException> error) {
        if (value <= 0 || !seen.add(value))
            throw error.get();
    }
}
