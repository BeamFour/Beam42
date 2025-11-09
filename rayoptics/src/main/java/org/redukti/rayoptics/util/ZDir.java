// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.util;

public enum ZDir {

    PROPAGATE_RIGHT(1.0),
    PROPAGATE_LEFT(-1.0);

    public final double value;

    ZDir(double value) {
        this.value = value;
    }

    public ZDir opposite() {
        if (this == PROPAGATE_LEFT)
            return PROPAGATE_RIGHT;
        else
            return PROPAGATE_LEFT;
    }

    public static ZDir from(double v) {
        if (v >= 0.0)
            return PROPAGATE_RIGHT;
        return PROPAGATE_LEFT;
    }
}
