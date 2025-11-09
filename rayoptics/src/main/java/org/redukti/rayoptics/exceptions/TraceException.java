// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.exceptions;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.seq.Interface;

public class TraceException extends RuntimeException {

    public int surf;
    public Interface ifc;
    public Vector3 int_pt;
    public Tfm3d prev_tfrm;
    public RayPkg ray_pkg;


    public TraceException() {
    }

    public TraceException(String message) {
        super(message);
    }

    public TraceException(String message, Throwable cause) {
        super(message, cause);
    }

    public TraceException(Throwable cause) {
        super(cause);
    }

    public TraceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
