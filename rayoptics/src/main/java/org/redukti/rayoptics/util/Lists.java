package org.redukti.rayoptics.util;

import java.util.ArrayList;
import java.util.List;

public class Lists {

    public static <E> List<E> slice(List<E> inputList, Integer start_, Integer stop_, Integer step_) {
        List<E> newList = new ArrayList<>();
        int step = step_ == null ? 1 : step_;
        int length = inputList.size();
        int start, stop;
        if (start_ == null) {
            start = (step < 0) ? length-1 : 0;
        }
        else {
            start = (start_ < 0) ? start_ + length : start_;
        }
        if (stop_ == null) {
            stop = (step < 0) ? -1 : length;
        }
        else {
            stop = (stop_ < 0) ? stop_ + length : stop_;
        }
        if (step < 0) {
            for (int i = Math.min(start,length-1); i >= Math.max(stop,0); i += step) {
                newList.add(inputList.get(i));
            }
        }
        else if (step > 0) {
            for (int i = Math.max(0,start); i < Math.min(stop,length); i += step) {
                newList.add(inputList.get(i));
            }
        }
        else {
            throw new IllegalArgumentException();
        }
        return newList;
    }

    public static <E> List<E> from(List<E> inputList, int start) {
        return slice(inputList, start, null, null);
    }

    public static <E> List<E> upto(List<E> inputList, int stop) {
        return slice(inputList, null, stop, null);
    }

    public static <E> List<E> step(List<E> inputList, int step) {
        return slice(inputList, null, null, step);
    }

    public static <E> E get(List<E> inputList, int i) {
        if (i < 0)
            i += inputList.size();
        return inputList.get(i);
    }

    public static <E> void set(List<E> inputList, int i, E e) {
        if (i < 0)
            i += inputList.size();
        inputList.set(i, e);
    }

    public static <T1, T2> List<Pair<T1, T2>> zip_longest(List<T1> list1, List<T2> list2) {
        List<Pair<T1, T2>> list = new ArrayList<>();
        for (int i = 0; i < Math.max(list1.size(), list2.size()); i++) {
            T1 t1 = i < list1.size() ? list1.get(i) : null;
            T2 t2 = i < list2.size() ? list2.get(i) : null;
            list.add(new Pair<>(t1, t2));
        }
        return list;
    }

}
