package org.redukti.jfotopix.sys;

import java.util.ArrayList;
import java.util.List;

public class OpticalSystem implements Container {
    private final ArrayList<Element> elements = new ArrayList<>();

    @Override
    public List<Element> elements() {
        return elements;
    }

}
