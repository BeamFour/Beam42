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
    public OpticalSpecs parent;
    public SpecKey key;
    public double value;
    public double[][] pupil_rays;
    public String[] ray_labels;

    static final double [][] default_pupil_rays = {{0., 0.}, {1., 0.}, {-1., 0.}, {0., 1.}, {0., -1.}};
    static final String[] default_ray_labels = {"00", "+X", "-X", "+Y", "-Y"};

    public PupilSpec(OpticalSpecs parent, Pair<String, String> k, double value) {
        this.parent = parent;
        this.key = new SpecKey("aperture", k.first, k.second);
        this.value = value;
        this.pupil_rays = default_pupil_rays;
        this.ray_labels = default_ray_labels;
    }

    public void update_model() {
        if (pupil_rays == null) {
            pupil_rays = default_pupil_rays;
            ray_labels = default_ray_labels;
        }
    }
}
