package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.patterns.PatternGenerator;

import java.util.function.Function;

public abstract class ShapeBase implements Shape {
    @Override
    public void get_pattern(Function<Vector2, Void> f, Distribution d, boolean unobstructed) {
        PatternGenerator.get_pattern(d, unobstructed, this, f);
    }

    @Override
    public double get_hole_radius(Vector2 dir) {
        return 0;
    }

}
