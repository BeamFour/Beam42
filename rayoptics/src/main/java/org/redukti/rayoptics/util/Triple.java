package org.redukti.rayoptics.util;

public class Triple<T1, T2, T3> {
    public final T1 first;
    public final T2 second;
    public final T3 third;

    public Triple(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}
