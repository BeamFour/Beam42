package org.redukti.rayoptics.seq;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.optical.OpticalModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathCacheTest {

    @Test
    void returnsTheCachedPathForTheSameArguments() {
        SequentialModel model = model();

        List<PathSeg> first = model.path(null, null, null, 1);
        List<PathSeg> second = model.path(null, null, null, 1);

        assertSame(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
    }

    @Test
    void keysTheCacheOnArgumentsBeforeDefaultsAreApplied() {
        SequentialModel model = model();

        assertNotSame(model.path(null, null, null, null),
                model.path(null, null, null, 1));
    }

    @Test
    void updateModelInvalidatesPreviouslyComputedPaths() {
        SequentialModel model = model();
        List<PathSeg> before = model.path();

        model.gaps.get(0).thi += 1.0;
        model.update_model();

        assertNotSame(before, model.path());
    }

    @Test
    void evictsTheOldestEntryWhenTheRingIsFull() {
        PathCache cache = new PathCache();
        PathCache.Key oldest = new PathCache.Key(0.0, null, null, 1);
        cache.store(oldest, List.of());

        for (int i = 1; i <= PathCache.CAPACITY; i++)
            cache.store(new PathCache.Key((double) i, null, null, 1), List.of());

        assertNull(cache.find(oldest));
    }

    private static SequentialModel model() {
        OpticalModel opticalModel = new OpticalModel();
        opticalModel.seq_model.update_model();
        return opticalModel.seq_model;
    }
}
