package org.redukti.jfotoptix.data;

public interface InterpolatableDataSet {
    double get_x_interval(int x);
    double get_x_interval(int x1, int x2);
    int get_interval(double x);
    /** Get x data at index n in data set */
    double get_x_value(int n);
    /** Get y data stored at index n in data set */
    double get_y_value(int n);
    double get_d_value(int n);
    int get_nearest(double x);
    /** Get total number of data stored in data set */
    int get_count();
}
