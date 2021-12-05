package org.redukti.rayoptics.specs;

public class SpecKey {
    public final String type;
    public final String imageKey;
    public final String valueKey;

    public SpecKey(String type, String imageKey, String valueKey) {
        this.type = type;
        this.imageKey = imageKey;
        this.valueKey = valueKey;
    }
}