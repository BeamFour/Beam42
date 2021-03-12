package org.redukti.jfotoptix.data;

/**
 Base class for numerical data sets.

 This class is a base class for all numerical data sets
 implementations. It defines an interface to access data in a
 way independent from data storage and dimensions count.

 Each data set implementation may define a alternative specific
 interface to access their data.

 Here x is seen as value used to access the container.
 Containers with more than one dimension will require x0, x1,
 ..., xn known parameters to access the stored y value.
 */
public abstract class DataSet {
    int _version;
    Interpolation _interpolation;

    /** Get total number of dimensions */
    public abstract int get_dimensions ();

    /** Get total number of data stored for dimension n in data set */
    public abstract int get_count (int dim);

    /** Get data stored at position n on dimension dim in data set */
    public abstract double get_x_value (int n, int dim);

    /** Get y data stored at position (x0, x1, ...) in data set */
    public abstract double get_y_value (int x[]);

    /** Interpolate y value corresponding to given x value(s) in data set. */
    public abstract double interpolate (double x[]);

    /** Interpolate y value corresponding to given x value in data
     set. data may be differentiated several times along the requested
     dimension.
     @param deriv Differentiation count, 0 means y value, 1 means 1st
     derivative...
     @param dim Differentiation dimension
     */
    public abstract double interpolate (double x[], int deriv,
                                int dim);

    /** Get minimal and maximal x values on dimension n found in data set */
    public abstract Range get_x_range (int dim);

    /** Get minimal and maximal y values found in data set */
    public Range get_y_range ()
    {
        Range r = new Range(Double.MIN_VALUE, Double.MAX_VALUE);

        int d = get_dimensions ();
        // unsigned int x[d];
        int[] x = new int[d];
        // unsigned int c[d];
        int[] c = new int[d];

        for (int i = 0; i < d; i++)
        {
            if (get_count (i) == 0)
                throw new IllegalStateException ("data set contains no data");

            x[i] = 0;
            c[i] = get_count (i) - 1;
        }

        while (true)
        {
            double y = get_y_value (x);

            if (y < r.first)
                r.first = y;

            if (y > r.second)
                r.second = y;

            for (int i = 0;;)
            {
                if (x[i] < c[i])
                {
                    x[i]++;
                    break;
                }
                else
                {
                    x[i++] = 0;

                    if (i == d)
                        return r;
                }
            }
        }
    }

    /** Select interpolation method */
    //virtual void set_interpolation (Interpolation i) = 0;

    /** Get current interpolation method */
    public Interpolation get_interpolation () {
        return _interpolation;
    }

    // FIXME dataset version number
    /** Return version number which is incremented on each data set change/clear
     */
    public int get_version () {
        return _version;
    }
}
