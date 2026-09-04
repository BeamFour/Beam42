// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fixed-size ring of paths computed by {@link SequentialModel#path}.
 *
 * <p>The model owns invalidation: {@code PathSeg} captures transforms,
 * refractive indices and propagation directions, so the cache must be cleared
 * after any operation that can change those values.</p>
 */
final class PathCache {
    static final int CAPACITY = 16;

    static final class Key {
        final Double wavelength;
        final Integer start;
        final Integer stop;
        final Integer step;

        Key(Double wavelength, Integer start, Integer stop, Integer step) {
            this.wavelength = wavelength;
            this.start = start;
            this.stop = stop;
            this.step = step;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof Key))
                return false;
            Key key = (Key) other;
            return Objects.equals(wavelength, key.wavelength)
                    && Objects.equals(start, key.start)
                    && Objects.equals(stop, key.stop)
                    && Objects.equals(step, key.step);
        }

        @Override
        public int hashCode() {
            return Objects.hash(wavelength, start, stop, step);
        }
    }

    private static final class Entry {
        Key key;
        List<PathSeg> path;

        Entry(Key key, List<PathSeg> path) {
            this.key = key;
            this.path = path;
        }
    }

    private final ArrayList<Entry> entries = new ArrayList<>(CAPACITY);
    private int next;

    List<PathSeg> find(Key key) {
        for (Entry entry : entries) {
            if (entry.key.equals(key))
                return entry.path;
        }
        return null;
    }

    List<PathSeg> store(Key key, List<PathSeg> path) {
        // A cached path is shared by all later callers. Copy and freeze the
        // outer list so one caller cannot corrupt every subsequent trace.
        List<PathSeg> cached = Collections.unmodifiableList(new ArrayList<>(path));
        if (entries.size() < CAPACITY) {
            entries.add(new Entry(key, cached));
            return cached;
        }
        Entry slot = entries.get(next);
        slot.key = key;
        slot.path = cached;
        next = (next + 1) % CAPACITY;
        return cached;
    }

    void clear() {
        entries.clear();
        next = 0;
    }
}
