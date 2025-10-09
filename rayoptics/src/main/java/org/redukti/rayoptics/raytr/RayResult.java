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
