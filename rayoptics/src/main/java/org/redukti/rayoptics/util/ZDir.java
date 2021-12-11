package org.redukti.rayoptics.util;

public enum ZDir {

    PROPAGATE_RIGHT(1.0),
    PROPAGATE_LEFT(-1.0);

    public final double value;

    ZDir(double value) {
        this.value = value;
    }
}
