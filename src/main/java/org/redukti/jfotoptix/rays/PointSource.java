package org.redukti.jfotoptix.rays;

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

public class PointSource extends RaySource {

    /**
     * Specifies point source location mode
     */
    enum SourceInfinityMode {
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

    public PointSource(SourceInfinityMode m, Vector3 pos_dir) {
        super(m == SourceInfinityMode.SourceAtInfinity
                // position of infinity source is only used for trace::Sequence
                // sort See
                // https://lists.gnu.org/archive/html/goptical/2013-06/msg00004.html
                ? new Vector3Pair(pos_dir.times(-1e9), pos_dir)
                : new Vector3Pair(pos_dir, Vector3.vector3_001));
        _mode = m;
    }


}
