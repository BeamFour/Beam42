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
}