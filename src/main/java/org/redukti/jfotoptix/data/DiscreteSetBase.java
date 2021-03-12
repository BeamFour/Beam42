package org.redukti.jfotoptix.data;

import java.util.ArrayList;

public abstract class DiscreteSetBase extends Set1d {

    class EntryS {
        double x, y, d;

        public EntryS(double x, double y, double d) {
            this.x = x;
            this.y = y;
            this.d = d;
        }
    }

    ArrayList<EntryS> data = new ArrayList<>();

    /**
     * Insert data pair in data set. If a pair with the same x
     * value exists, it will be replaced by the new
     * value. Derivative value may be provided as well.
     */
    public void add_data(double x, double y, double d) {
        EntryS e = new EntryS(x, y, d);

        _version++;

        int di = get_interval(x);

        if (di > 0 && (data.get(di - 1).x == x))
            data.set(di - 1, e);
        else
            data.add(di, e);
        invalidate();
    }

    protected abstract void invalidate();

    /**
     * Reserve data entries
     */
    public void reserve(int n) {
        data.ensureCapacity(n);
    }

    /**
     * Clear all data
     */
    public void clear() {
        data.clear();
        _version++;
        invalidate();
    }

    /**
     * Get stored derivative value at index x
     */
    public double get_d_value(int n) {
        assert (n < data.size());
        return data.get(n).d;
    }

    // inherited from Set1d
    public int get_count() {
        return data.size();
    }

    public double get_x_value(int n) {
        assert (n < data.size());
        return data.get(n).x;
    }

    public double get_y_value(int n) {
        assert (n < data.size());
        return data.get(n).y;
    }

    public Range get_x_range() {
        if (data.isEmpty())
            throw new IllegalStateException("_data set contains no _data");
        return new Range(data.get(0).x, data.get(data.size() - 1).x);
    }

    /**
     * find lower bound index of interval containing value
     */
    public int get_interval(double x) {
        int min_idx = 0;
        int max_idx = data.size() + 1;

        while (max_idx - min_idx > 1) {
            int p = (max_idx + min_idx) / 2;

            if (x >= data.get(p - 1).x)
                min_idx = p;
            else
                max_idx = p;
        }
        return min_idx;
    }

    /**
     * find nearest value index
     */
    public int get_nearest(double x) {
        int min_idx = 0;
        int max_idx = data.size();

        while (max_idx - min_idx > 1) {
            int p = (max_idx + min_idx) / 2;

            if (x + x >= data.get(p - 1).x + data.get(p).x)
                min_idx = p;
            else
                max_idx = p;
        }
        return min_idx;
    }

    public double get_x_interval(int x) {
        return data.get(x + 1).x - data.get(x).x;
    }

    public double get_x_interval(int x1, int x2) {
        return data.get(x2).x - data.get(x1).x;
    }
}
