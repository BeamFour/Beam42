// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.exceptions;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.seq.Interface;

public class TraceRayBlockedException extends TraceException {

    public TraceRayBlockedException(Interface ifc, Vector3 int_pt) {
        this.ifc = ifc;
        this.int_pt = int_pt;
    }
}
