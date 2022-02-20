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
