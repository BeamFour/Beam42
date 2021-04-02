package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.tracing.TraceParameters;
import org.redukti.jfotoptix.tracing.TracedRay;

import java.util.List;
import java.util.function.Consumer;

public class PointSource extends RaySource {

    /**
     * Specifies point source location mode
     */
    public enum SourceInfinityMode {
        /**
         * In finite distance mode the point source is located at
         * specified position and all rays are traced from this
         * point.
         */
        SourceAtFiniteDistance,
        /**
         * In infinity mode the point source generate parallel rays
         * oriented along source direction vector.
         */
        SourceAtInfinity,
    }

    SourceInfinityMode _mode;

    public PointSource(int id, Vector3Pair p, Transform3 transform, double min_intensity, double max_intensity, List<SpectralLine> spectrum) {
        super(id, p, transform, min_intensity, max_intensity, spectrum);

    }

    public SourceInfinityMode mode() {
        return _mode;
    }

    public static class Builder extends RaySource.Builder {

        SourceInfinityMode _mode;

        Builder(SourceInfinityMode m, Vector3 pos_dir) {
            position(m == SourceInfinityMode.SourceAtInfinity
                    // position of infinity source is only used for trace::Sequence
                    // sort See
                    // https://lists.gnu.org/archive/html/goptical/2013-06/msg00004.html
                    ? new Vector3Pair(pos_dir.times(-1e9), pos_dir)
                    : new Vector3Pair(pos_dir, Vector3.vector3_001));
            _mode = m;
        }


        @Override
        public Element build() {
            return null;
        }
    }

}
