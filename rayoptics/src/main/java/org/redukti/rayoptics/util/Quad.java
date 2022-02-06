package org.redukti.rayoptics.util;

public class Quad<T1, T2, T3, T4> {
    public final T1 first;
    public final T2 second;
    public final T3 third;
    public final T4 fourth;

    public Quad(T1 first, T2 second, T3 third, T4 fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    @Override
    public String toString() {
        return "(" +
                first +
                "," + second +
                "," + third +
                "," + fourth +
                ')';
    }
}
