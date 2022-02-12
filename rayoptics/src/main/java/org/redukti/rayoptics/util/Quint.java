package org.redukti.rayoptics.util;

import java.util.Objects;

/**
 * Tuple with 5 elements
 */
public class Quint<T1, T2, T3, T4, T5> {
    public final T1 first;
    public final T2 second;
    public final T3 third;
    public final T4 fourth;
    public final T5 fifth;

    public Quint(T1 first, T2 second, T3 third, T4 fourth, T5 fifth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    @Override
    public String toString() {
        return "(" +
                first +
                "," + second +
                "," + third +
                "," + fourth +
                "," + fifth +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quint<?, ?, ?, ?, ?> quint = (Quint<?, ?, ?, ?, ?>) o;
        return Objects.equals(first, quint.first) && Objects.equals(second, quint.second) && Objects.equals(third, quint.third) && Objects.equals(fourth, quint.fourth) && Objects.equals(fifth, quint.fifth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, fourth, fifth);
    }
}
