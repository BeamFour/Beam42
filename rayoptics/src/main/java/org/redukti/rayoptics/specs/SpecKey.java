// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

public class SpecKey {
    public final SpecType type;
    public final ImageKey imageKey;
    public final ValueKey valueKey;

    public SpecKey(SpecType type, ImageKey imageKey, ValueKey valueKey) {
        this.type = type;
        this.imageKey = imageKey;
        this.valueKey = valueKey;
    }

    @Override
    public String toString() {
        return "SpecKey(type=" + type + ", imageKey=" + imageKey + ", valueKey=" + valueKey + ')';
    }
}