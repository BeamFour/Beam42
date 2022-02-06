package org.redukti.rayoptics.util;

import java.util.HashMap;
import java.util.Map;

public class KWArgs {

    private Map<String, Object> args = new HashMap<>();

    public KWArgs() {
    }

    public KWArgs(String key, Object v) {
        this.args.put(key, v);
    }

    public KWArgs add(String key, Object v) {
        this.args.put(key, v);
        return this;
    }

    public <T> T get(String key) {
        return (T) args.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        T t = (T) args.get(key);
        if (t == null)
            return defaultValue;
        return t;
    }

}
