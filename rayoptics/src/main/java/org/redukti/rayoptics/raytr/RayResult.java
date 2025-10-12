// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.exceptions.TraceException;

public class RayResult {
    public RayPkg pkg;
    public TraceException err;

    public RayResult() {}

    public RayResult(RayPkg pkg, TraceException err) {
        this.pkg = pkg;
        this.err = err;
    }
}
