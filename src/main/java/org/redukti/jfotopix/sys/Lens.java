package org.redukti.jfotopix.sys;

import org.redukti.jfotopix.math.Vector3;
import org.redukti.jfotopix.math.Vector3Pair;

import java.util.ArrayList;

public class Lens extends Group {

    private ArrayList<OpticalSurface> surfaces = new ArrayList<>();
    private int lastPos = 0;

    public Lens() {
        super(new Vector3Pair(new Vector3(0,0,0), Vector3.vector3_001));
    }

}
