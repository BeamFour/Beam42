// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.exceptions;

public class TraceMissedSurfaceException extends TraceException {
    public TraceMissedSurfaceException() {
    }

    public TraceMissedSurfaceException(String message) {
        super(message);
    }

    public TraceMissedSurfaceException(Throwable cause) {
        super(cause);
    }
}
