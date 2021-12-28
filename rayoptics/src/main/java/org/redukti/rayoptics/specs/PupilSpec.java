package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.util.Pair;

/**
 * The PupilSpec class maintains the aperture specification.
 * The PupilSpec can be defined in object or image space.
 * The defining parameters can be pupil, f/# or NA,
 * where pupil is the pupil diameter.
 * <p>
 * Attributes:
 * key: 'aperture', 'object'|'image', 'pupil'|'NA'|'f/#'
 * value: size of the pupil
 * pupil_rays: list of relative pupil coordinates for pupil limiting rays
 * ray_labels: list of string labels for pupil_rays
 */
public class PupilSpec {
    public OpticalSpecs parent;
    public SpecKey key;
    public double value;

    /**
     * The PupilSpec class allows rays to be specified as fractions of the pupil dimension.
     * A list of pupil_rays and ray_labels define rays to be used to establish clear aperture
     * dimensions on optical elements and rays to be drawn for the lens layout. A default set of
     * pupil rays is provided that is appropriate for circular pupil systems with plane symmetry.
     */
    public double[][] pupil_rays;
    public String[] ray_labels;

    static final double[][] default_pupil_rays = {{0., 0.}, {1., 0.}, {-1., 0.}, {0., 1.}, {0., -1.}};
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
