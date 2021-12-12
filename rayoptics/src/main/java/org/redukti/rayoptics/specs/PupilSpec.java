package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.util.Pair;

/**
 * Aperture specification
 *
 *     Attributes:
 *         key: 'aperture', 'object'|'image', 'pupil'|'NA'|'f/#'
 *         value: size of the pupil
 *         pupil_rays: list of relative pupil coordinates for pupil limiting rays
 *         ray_labels: list of string labels for pupil_rays
 */
public class PupilSpec {
    OpticalSpecs parent;
    SpecKey key;
    double value;

    public PupilSpec(OpticalSpecs parent, Pair<String, String> k, double value) {
        this.parent = parent;
        this.key = new SpecKey("aperture", k.first, k.second);
        this.value = value;
    }
}
