package org.redukti.jfotopix.sys;

import org.redukti.jfotopix.math.Vector3Pair;

import java.util.ArrayList;
import java.util.List;

public class Group extends Element implements Container {
    private final ArrayList<Element> elements = new ArrayList<>();

    public Group(Vector3Pair p) {
        super(p);
    }

    @Override
    public List<Element> elements() {
        return elements;
    }
}
