package org.redukti.rayoptics.util;

import java.util.Objects;

/**
 * Tuple with 3 elements
 */
public class Triple<T1, T2, T3> {
    public final T1 first;
    public final T2 second;
    public final T3 third;

    public Triple(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(first, triple.first) && Objects.equals(second, triple.second) && Objects.equals(third, triple.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }

    @Override
    public String toString() {
        return "(" +
                first +
                "," + second +
                "," + third +
                ')';
    }
}
