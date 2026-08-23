package org.redukti.exporters;

import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;

import java.util.ArrayList;
import java.util.List;

/**
 * A thin wrapper over a {@link Prescription} that adds the two things needed to
 * build a model from it, and nothing else.
 * <p>
 * The lens itself is not copied. Surface data is read from the prescription
 * through this class, so {@link Prescription} remains the single description of
 * the optics. What is added here is:
 * <ul>
 * <li>the export decisions the prescription has no notion of - field points,
 *     wide angle aiming, vignetting calculation, object distance, radius mode,
 *     do_apertures - and the resolution of a configuration index;</li>
 * <li>the order the model has to be built in, as {@link #ops()}.</li>
 * </ul>
 * Both of those are made once and shared by every writer. That is the point:
 * when each emitter walked the source itself, the old exporter and
 * {@link org.redukti.spec.RayOpticsModelBuilder} silently diverged on the field
 * list, the spectral region and is_wide_angle, and a difference of that kind is
 * indistinguishable from a ray trace bug when the two outputs are compared.
 * <p>
 * Numbers are rendered by the writers with {@code Double.toString}, whose output
 * is the shortest decimal that round trips; Python's {@code repr} has the same
 * property, so both sides parse back to bit identical values. Do not format them
 * with a fixed number of decimal places.
 * <p>
 * This holds a live reference to the prescription rather than a snapshot, so
 * later edits to the prescription are visible here. Build and render in one pass.
 */
public final class ModelSpec {

    /**
     * One step in building the model, in the order it must happen. Ops carry a
     * {@link SurfaceType} where they refer to a specific surface; everything
     * else is read from the enclosing spec.
     */
    public sealed interface Op {}

    public record SetPupil() implements Op {}
    public record SetFieldSpec() implements Op {}
    public record SetSpectralRegion() implements Op {}
    public record SetTitle() implements Op {}
    public record SetRadiusMode() implements Op {}
    public record SetObjectDistance() implements Op {}
    public record AddSurface(SurfaceType surface) implements Op {}
    public record SetProfile(SurfaceType surface) implements Op {}
    public record SetStop() implements Op {}
    public record SetDoApertures() implements Op {}
    public record UpdateModel() implements Op {}
    public record ApplyVignetting() implements Op {}

    /** The field list used when none is given. */
    public static final double[] DEFAULT_FIELDS = {0.0, 0.707, 1.0};
    /** Object distance standing in for an infinite conjugate. */
    public static final double INFINITE_OBJECT_DISTANCE = 1e10;

    public final Prescription prescription;
    public final int config;
    private final double[] fields;
    private final VigType vig_type;
    private final Boolean wide_angle;

    /**
     * @param prescription the lens
     * @param config       configuration index, 0 for a single config lens
     * @param fields       relative field points, null for {@link #DEFAULT_FIELDS}
     * @param vig_type     vignetting calculation applied after the model is built
     * @param wide_angle   forces wide angle aiming on or off, null to derive it
     *                     from the half angle of view as the model builder does
     */
    public ModelSpec(Prescription prescription, int config, double[] fields,
                     VigType vig_type, Boolean wide_angle) {
        this.prescription = prescription;
        this.config = config;
        this.fields = fields == null ? DEFAULT_FIELDS : fields;
        this.vig_type = vig_type == null ? VigType.None : vig_type;
        this.wide_angle = wide_angle;
    }

    public double fno() {
        return prescription._f_number_by_scenario == null || config == 0
                ? prescription._fno
                : prescription._f_number_by_scenario[config];
    }

    public double half_angle_degrees() {
        double angle_of_view = prescription._angle_of_views_by_scenario == null || config == 0
                ? prescription._angle_of_view_in_degrees
                : prescription._angle_of_views_by_scenario[config];
        return angle_of_view / 2.0;
    }

    public double[] fields() {
        return fields;
    }

    public boolean is_relative() {
        return true;
    }

    public boolean is_wide_angle() {
        return wide_angle != null ? wide_angle : half_angle_degrees() > 45.;
    }

    public double[] wavelengths() {
        return prescription._wvls;
    }

    public double[] weights() {
        return prescription._wts;
    }

    /** See Prescription._wvls: the first entry is the reference wavelength. */
    public int reference_wavelength_index() {
        return 0;
    }

    public String title() {
        return prescription._title;
    }

    public String dimensions() {
        return "mm";
    }

    public boolean radius_mode() {
        return true;
    }

    public double object_distance() {
        return INFINITE_OBJECT_DISTANCE;
    }

    public boolean do_apertures() {
        return false;
    }

    public VigType vig_type() {
        return vig_type;
    }

    /** Thickness of a surface for this spec's configuration. */
    public double thickness(SurfaceType surface) {
        return surface.get_thickness_by_scenario(config);
    }

    /** Semi-diameter of a surface for this spec's configuration. */
    public double semi_diameter(SurfaceType surface) {
        return surface.get_diameter_by_scenario(config) / 2.0;
    }

    /** The build sequence. Mirrors the order used by RayOpticsModelBuilder. */
    public List<Op> ops() {
        List<Op> ops = new ArrayList<>();
        ops.add(new SetPupil());
        ops.add(new SetFieldSpec());
        ops.add(new SetSpectralRegion());
        ops.add(new SetTitle());
        ops.add(new SetRadiusMode());
        ops.add(new SetObjectDistance());
        for (SurfaceType surface : prescription._surfaces) {
            ops.add(new AddSurface(surface));
            if (surface.is_aspheric())
                ops.add(new SetProfile(surface));
            if (surface.is_aperture_stop())
                ops.add(new SetStop());
        }
        ops.add(new SetDoApertures());
        ops.add(new UpdateModel());
        if (vig_type != VigType.None) {
            ops.add(new ApplyVignetting());
            ops.add(new UpdateModel());
        }
        return ops;
    }
}
