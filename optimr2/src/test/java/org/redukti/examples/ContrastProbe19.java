package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.analysis.ContrastAnalysis;
import org.redukti.rayoptics.analysis.ContrastAnalysisResult;
import org.redukti.rayoptics.analysis.ContrastOptions;
import org.redukti.rayoptics.analysis.PupilShear;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.PupilType;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.importers.obench.OpticalBenchDataImporter;

/**
 * REVIEW.md &sect;9 - is the direction-cosine frequency metric contaminated by the
 * aberration being optimized, and by how much?
 *
 * <p>Finding 9 argues that {@code PupilShear.realized_frequency} is not Hopkins' OTF
 * frequency. His (10.26) shears the pupil function in reduced exit-pupil coordinates, so
 * the shear is a separation on the exit-pupil reference sphere. A ray's image-space
 * direction is the wavefront normal, so a direction-cosine difference carries that
 * separation <em>plus</em> the difference in wavefront slope between the two pupil points.
 * The second part is transverse ray aberration, which is exactly what optimization
 * changes.
 *
 * <p>Two measurements settle it.
 *
 * <p><b>Part A</b> compares the two metrics per sample.
 * {@link PupilShear#realized_frequency_vector} measures the separation on the reference
 * sphere; {@link PupilShear#realized_frequency} measures the direction-cosine difference.
 * Their ratio is the contamination. It also reports the cross-axis component of the
 * exit-pupil separation, which is the skew finding 9 raises: a nominal x or y entrance-
 * pupil displacement need not stay on that axis in the exit pupil, in which case the pair
 * samples the OTF at a two-dimensional frequency rather than on the requested one.
 *
 * <p><b>Part B</b> is the discriminator. Adding defocus changes the wavefront, and
 * therefore the ray slopes, while leaving the entrance-to-exit pupil mapping alone. A
 * metric that measures pupil geometry must be unmoved by it; a metric contaminated by
 * wavefront slope must move. This separates the two hypotheses without needing a full
 * optimization run.
 */
public class ContrastProbe19 {

    private static final double[] FIELDS = {0.0, 0.5, 0.7, 1.0};
    private static final int FREQ = 40;
    private static final double[] PERTURBATION = {0.0, 0.01, 0.03};

    public static void main(String[] args) throws Exception {
        report("Leica R APO 75/2", ContrastProbes.leicaInputPath());
        report("Zeiss Otus ML 50/1.4", ContrastProbes.inputPath());
    }

    private static void report(String label, String specfile) throws Exception {
        System.out.println();
        System.out.println("================================================================");
        System.out.println(label + "   " + FREQ + " cyc/mm, 6x12, central wavelength");
        System.out.println("================================================================");

        partA(build(specfile));
        partB(specfile);
    }

    private static OpticalModel build(String specfile) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);
        return new RayOpticsModelBuilder(prescription)
                .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);
    }

    /** Direction-cosine metric against the exit-pupil-sphere metric, per sample. */
    private static void partA(OpticalModel model) {
        System.out.println();
        System.out.println("A. direction-cosine metric vs Hopkins exit-pupil separation");
        System.out.printf("%5s %4s | %9s %9s | %9s | %9s%n",
                "fld", "dir", "nu_dir", "nu_pupil", "contam", "cross/axis");

        var result = ContrastAnalysis.eval(model,
                new ContrastOptions(FREQ).num_rings(6).num_spokes(12)
                        .calibrate_frequency(true).measure_frequency(true));

        for (int fi = 0; fi < FIELDS.length; fi++) {
            for (int t = 0; t < 2; t++) {
                boolean tangential = t == 1;
                var stats = new Stats();
                var cross = new Stats();
                var direct = new Stats();
                var pupil = new Stats();

                var field = model.optical_spec.fov.fields[fi];
                double focus = model.optical_spec.defocus().get_focus();
                double wvl = model.seq_model.central_wavelength();
                var coords = Trace.setup_pupil_coords(model, field, wvl, focus, null, null);
                field.chief_ray = coords.chief_ray_pkg;
                field.ref_sphere = coords.ref_sphere;

                var wavelengths = result.fields.get(fi).wavelengths();
                for (var wavelength : wavelengths) {
                    for (var sample : wavelength.samples()) {
                        if (!sample.valid() || sample.shear() == null) continue;
                        double nuDir = tangential
                                ? sample.shear().tangentialFrequency()
                                : sample.shear().sagittalFrequency();
                        var offset = tangential
                                ? sample.shear().tangentialOffset()
                                : sample.shear().sagittalOffset();
                        if (offset == null || !Double.isFinite(nuDir)) continue;

                        // Rebuild the pupil-sphere frequency from the recorded offset. The
                        // offset is B~' based; convert with the same scale Hopkins uses.
                        double R = field.ref_sphere.ref_sphere_radius;
                        double lam = model.nm_to_sys_units(wavelength.wavelength());
                        double n = Math.abs(model.optical_spec.parax_data.fod.n_img);
                        double scale = n / (lam * R);
                        double onAxis = Math.abs(tangential ? offset.y : offset.x) * scale;
                        double offAxis = Math.abs(tangential ? offset.x : offset.y) * scale;
                        if (!(onAxis > 0.0)) continue;

                        direct.add(nuDir);
                        pupil.add(onAxis);
                        stats.add(nuDir / onAxis - 1.0);
                        cross.add(offAxis / onAxis);
                    }
                }
                if (stats.n < 2) continue;
                System.out.printf("%5.2f %4s | %9.3f %9.3f | %+7.3f%% | %8.4f%n",
                        FIELDS[fi], tangential ? "tan" : "sag",
                        direct.mean(), pupil.mean(), 100.0 * stats.mean(), cross.mean());
            }
        }
    }

    /**
     * Perturb one interior curvature and watch both metrics. This is the coupling finding
     * 9 predicts: a change to the wavefront should not move a frequency, but it moves a
     * metric built from ray slopes.
     *
     * <p>Defocus does not work as the discriminator, despite being the obvious choice. A
     * focus shift moves the image plane only; the traced rays are identical, so
     * {@code nu_dir} is invariant by construction and the test says nothing. A curvature
     * change is a real change to the wavefront. It also perturbs the pupil mapping, so the
     * paraxial exit pupil is reported alongside to show how little of the movement that
     * accounts for.
     */
    private static void partB(String specfile) throws Exception {
        System.out.println();
        System.out.println("B. response to a curvature perturbation (a real wavefront change)");
        System.out.printf("%9s | %11s %11s | %11s %11s | %9s %9s%n",
                "dR/R", "nu_dir sag", "nu_pup sag", "nu_dir tan", "nu_pup tan",
                "exp_dist", "exp_rad");

        var traceOptions = new TraceOptions();
        traceOptions.pupil_type = PupilType.REL_PUPIL;
        traceOptions.apply_vignetting = false;
        traceOptions.check_apertures = false;

        // The model is rebuilt from the prescription, so the perturbation has to be made
        // there - writing to the traced profile is discarded, exactly as VarRadius shows.
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        Prescription prescription = Prescription.build_prescription(specs, true, false, false);

        // The most strongly curved interior surface, so the perturbation actually bites.
        // Picking by index alone can land on a flat, where a relative change is a no-op.
        int surface = -1;
        double strongest = 0.0;
        for (int i = 1; i < prescription._surfaces.length - 1; i++) {
            double r = prescription._surfaces[i]._radius;
            if (r == 0.0 || !Double.isFinite(r)) continue;
            if (Math.abs(1.0 / r) > strongest) {
                strongest = Math.abs(1.0 / r);
                surface = i;
            }
        }
        if (surface < 0) {
            System.out.println("  no curved interior surface to perturb");
            return;
        }
        double radius0 = prescription._surfaces[surface]._radius;
        System.out.printf("  perturbing surface %d, radius %.4f%n", surface, radius0);

        for (double delta : PERTURBATION) {
            prescription._surfaces[surface]._radius = radius0 * (1.0 + delta);
            OpticalModel model = new RayOpticsModelBuilder(prescription)
                    .build_optical_model(true, FIELDS, false, VigType.SetPupil, true, 0);
            var field = model.optical_spec.fov.fields[FIELDS.length - 1];
            double wvl = model.seq_model.central_wavelength();
            double focus = model.optical_spec.defocus().get_focus();
            double shift = ContrastAnalysis.normalized_entry_pupil_shift(model, wvl, FREQ);

            var coords = Trace.setup_pupil_coords(model, field, wvl, focus, null, null);
            field.chief_ray = coords.chief_ray_pkg;
            field.ref_sphere = coords.ref_sphere;

            // A single pair near the pupil edge, where wavefront slope is largest.
            Double dirSag = null, dirTan = null;
            double pupSag = Double.NaN, pupTan = Double.NaN;

            var reference = Trace.trace_safe(model, new Vector2(0.0, 0.3), field, wvl, traceOptions).pkg;
            var sag = Trace.trace_safe(model, new Vector2(shift, 0.3), field, wvl, traceOptions).pkg;
            var tan = Trace.trace_safe(model, new Vector2(0.0, 0.3 + shift), field, wvl, traceOptions).pkg;
            if (reference != null && sag != null && tan != null) {
                dirSag = PupilShear.realized_frequency(model, reference, sag, wvl, 0);
                dirTan = PupilShear.realized_frequency(model, reference, tan, wvl, 1);
                var vs = PupilShear.realized_frequency_vector(
                        model, reference, sag, field.chief_ray, field.ref_sphere, wvl);
                var vt = PupilShear.realized_frequency_vector(
                        model, reference, tan, field.chief_ray, field.ref_sphere, wvl);
                if (vs != null) pupSag = Math.abs(vs.x);
                if (vt != null) pupTan = Math.abs(vt.y);
            }
            var fod = model.optical_spec.parax_data.fod;
            System.out.printf("%+9.4f | %11.4f %11.4f | %11.4f %11.4f | %9.4f %9.4f%n",
                    delta,
                    dirSag == null ? Double.NaN : dirSag, pupSag,
                    dirTan == null ? Double.NaN : dirTan, pupTan,
                    fod.exp_dist, fod.exp_radius);
        }
        prescription._surfaces[surface]._radius = radius0;
    }

    private static final class Stats {
        int n;
        double sum;
        void add(double v) { if (Double.isFinite(v)) { n++; sum += v; } }
        double mean() { return n > 0 ? sum / n : Double.NaN; }
    }
}
