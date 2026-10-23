// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.raytr;

import org.redukti.mathlib.Vector3;

/**
 * The part of the OPD calculation independent of focus, as returned by
 * {@link WaveAbr#wave_abr_pre_calc}. Upstream returns a tuple whose contents
 * depend on whether the reference sphere is finite or infinite.
 */
public sealed interface PreOpdPkg {

    /** pre_opd, p_coord, b4_pt, b4_dir */
    record FinitePup(double pre_opd, Vector3 p_coord, Vector3 b4_pt, Vector3 b4_dir)
            implements PreOpdPkg {
    }

    /** pre_opd, W0, p_b4, d_b4, p_cr_b4, d_cr_b4 */
    record InfRef(double pre_opd, double W0, Vector3 p_b4, Vector3 d_b4,
                  Vector3 p_cr_b4, Vector3 d_cr_b4)
            implements PreOpdPkg {
    }
}
