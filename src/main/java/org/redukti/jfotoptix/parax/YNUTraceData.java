package org.redukti.jfotoptix.parax;

public class YNUTraceData {
    public final double height;
    public final double slope;
    public final double angle_of_incidence;

    public YNUTraceData(double height, double slope, double angle_of_incidence) {
        this.height = height;
        this.slope = slope;
        this.angle_of_incidence = angle_of_incidence;
    }

    @Override
    public String toString() {
        return "[height=" + height +
                ", slope=" + slope +
                ", angle_of_incidence=" + angle_of_incidence +
                ']';
    }
}
