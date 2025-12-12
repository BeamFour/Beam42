// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import org.redukti.rayoptics.parax.Etendue;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.Triple;

/**
 * The PupilSpec class maintains the aperture specification.
 * The PupilSpec can be defined in object or image space.
 * The defining parameters can be pupil, f/# or NA,
 * where pupil is the pupil diameter.
 * <p>
 * Attributes:
 * key: 'aperture', 'object'|'image', 'epd'|'NA'|'f/#'
 * value: size of the pupil
 * pupil_rays: list of relative pupil coordinates for pupil limiting rays
 * ray_labels: list of string labels for pupil_rays
 */
public class PupilSpec {
    public OpticalSpecs optical_spec;
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

    public PupilSpec(OpticalSpecs parent, Pair<ImageKey, ValueKey> k, Double value) {
        if (k == null)
            k = new Pair<>(ImageKey.Object, ValueKey.EPD);
        if (value == null)
            value = 1.0;
        this.optical_spec = parent;
        this.key = new SpecKey(SpecType.Aperture, k.first, k.second);
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

    public void apply_scale_factor(double scale_factor) {
        var value_key = key.valueKey;
        if (value_key == ValueKey.EPD || value_key == ValueKey.PUPIL)
            value *= scale_factor;
    }

    /**
     * return pupil spec as paraxial height or slope value
     */
    public Triple<ImageKey,ValueKey,Double> derive_parax_params() {
        var pupil_oi_key = key.imageKey;
        var pupil_value_key = key.valueKey;
        var pupil_value = this.value;
        ValueKey pupil_key = null;

        if (ValueKey.NA == pupil_value_key) {
            var na = pupil_value;
            var slope = Etendue.na2slp(na);
            pupil_key = ValueKey.Slope;
            pupil_value = slope;
        }
        else if (ValueKey.Fnum == pupil_value_key) {
            var fno = pupil_value;
            var slope = -1.0/(2.0*fno);
            pupil_key = ValueKey.Slope;
            pupil_value = slope;
        }
        else if (ValueKey.EPD == pupil_value_key) {
            var height = pupil_value/2.0;
            pupil_key = ValueKey.Height;
            pupil_value = height;
        }
        return new Triple<>(pupil_oi_key, pupil_key, pupil_value);
    }

    @Override
    public String toString() {
        return "PupilSpec(key=" + key + ", value=" +  value + ")";
    }

    public void list_str(StringBuilder sb) {
        sb.append(key.type).append(": ").append(key.imageKey).append(" ").append(key.valueKey).append(";")
                .append(" value = ").append(value).append("\n");
    }
}
