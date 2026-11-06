package org.redukti.examples;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.analysis.ContrastAnalysis;
import org.redukti.analysis.ExitPupilAiming;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.util.Lists;

/**
 * REVIEW.md &sect;9 — does a linear x/y shift on the exit-pupil reference sphere target
 * the OTF frequency coordinate?
 *
 * <p>For each field and a few pupil points, aim a partner at the sphere coordinate the
 * implementation asks for, then score the achieved pair two ways:
 *
 * <ul>
 *   <li><b>chord</b> - the direction cosines of the line from each sphere point to the
 *       image point, {@code u = ref_dir - c/R}. This is Hopkins' reduced coordinate and is
 *       what the shift is constructed from, so it tests the construction only.</li>
 *   <li><b>ray</b> - the traced rays' own image-space direction cosines, which carry the
 *       wavefront slope as well.</li>
 * </ul>
 *
 * Both are reported as realised/requested frequency.
 */
public class ContrastProbe19 {

    static Vector3 chordDirection(ReferenceSphere sphere, Vector3 sphereCoord) {
        // u = (O - B')/R = ref_dir - c/R, with c = B' - Ebar' in image-gap axes
        // u = (O - B') / R, with B' = Ebar' + c and O - Ebar' = R * ref_dir
        return sphere.ref_dir.minus(sphereCoord.times(1.0 / sphere.ref_sphere_radius));
    }

    public static void main(String[] args) throws Exception {
        var p = GenericContrastOpt.getPrescription(ContrastProbes.leicaInputPath(), false, false);
        var setup = GenericContrastOpt.createContrastSetup(p, false, false);
        var a = setup.analysis();
        a.required_analyses(false, false, false);
        a.compute();
        var m = a._opt_model;
        double wvl = m.seq_model.central_wavelength();
        double lambda = m.nm_to_sys_units(wvl);
        double nImg = Math.abs(m.optical_spec.parax_data.fod.n_img);
        var fields = m.optical_spec.fov.fields;
        double focus = m.optical_spec.defocus().get_focus();

        var to = new TraceOptions();
        to.check_apertures = false;
        to.pupil_type = PupilType.REL_PUPIL;
        to.apply_vignetting = false;

        int freq = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        double requestedDirectionDifference = lambda * freq / nImg;
        double entranceGuess = ContrastAnalysis.normalized_entry_pupil_shift(m, wvl, freq);

        System.out.printf("Leica 75/2, %d cyc/mm, lambda=%.6g mm, n_img=%.4f%n", freq, lambda, nImg);
        System.out.printf("requested |delta u| = lambda*nu/n = %.6e%n%n", requestedDirectionDifference);
        System.out.println(" fld  axis   pupil      R [mm]   shift [mm]   chord ratio   ray ratio   aim iters   |O-(E+c)|/R ref, partner");

        for (int fi : new int[]{0, 6, 10}) {
            var f = fields[fi];
            var coords = Trace.setup_pupil_coords(m, f, wvl, focus, null, null);
            f.chief_ray = coords.chief_ray_pkg;
            f.ref_sphere = coords.ref_sphere;
            double R = f.ref_sphere.ref_sphere_radius;
            double shift = ExitPupilAiming.reference_sphere_shift(m, f, wvl, freq);

            for (int axis = 0; axis < 2; axis++) {
                for (double r : new double[]{0.0, 0.5, 0.85}) {
                    var pupil = axis == 0 ? new Vector2(r, 0.0) : new Vector2(0.0, r);
                    var ref = Trace.trace_safe(m, pupil, f, wvl, to);
                    if (ref.pkg == null) { System.out.printf("%4d  %s  %5.2f   reference ray failed%n", fi, axis == 0 ? "sag" : "tan", r); continue; }
                    var cRef = ExitPupilAiming.reference_sphere_intersection(ref.pkg, f.chief_ray, f.ref_sphere);
                    if (cRef == null) { System.out.printf("%4d  %s  %5.2f   no sphere coordinate%n", fi, axis == 0 ? "sag" : "tan", r); continue; }

                    var target = axis == 0
                            ? new Vector2(cRef.x + shift, cRef.y)
                            : new Vector2(cRef.x, cRef.y + shift);
                    var guess = axis == 0
                            ? new Vector2(pupil.x + entranceGuess, pupil.y)
                            : new Vector2(pupil.x, pupil.y + entranceGuess);
                    var aimed = ExitPupilAiming.aim(m, guess, target, f, wvl, to);
                    if (aimed.ray().pkg == null || aimed.ref_sphere_intersect_in_exit_pupil_coord() == null) {
                        System.out.printf("%4d  %s  %5.2f   aim failed%n", fi, axis == 0 ? "sag" : "tan", r);
                        continue;
                    }
                    var cPartner = aimed.ref_sphere_intersect_in_exit_pupil_coord();

                    // chord metric: pure geometry of the two sphere points
                    var uRef = chordDirection(f.ref_sphere, cRef);
                    var uPartner = chordDirection(f.ref_sphere, cPartner);
                    double chordDiff = axis == 0 ? Math.abs(uPartner.x - uRef.x) : Math.abs(uPartner.y - uRef.y);

                    // ray metric: the traced rays' own image-space directions
                    var dRef = Lists.get(ref.pkg.ray, -2).d;
                    var dPartner = Lists.get(aimed.ray().pkg.ray, -2).d;
                    double rayDiff = axis == 0 ? Math.abs(dPartner.x - dRef.x) : Math.abs(dPartner.y - dRef.y);

                    // Independent check of the frame: if c is image-frame axes translated to
                    // Ebar', then Ebar' + c must lie on the sphere, |O - (Ebar' + c)| = R.
                    var ebar = f.chief_ray.cr_exp_seg.exp_pt;
                    // R is built from the image point shifted by the final gap, not from
                    // ReferenceSphere.image_pt itself; see WaveAbr.setup_pupil_coords.
                    double imageThi = Lists.get(m.seq_model.gaps, -1).thi;
                    var o = new Vector3(f.ref_sphere.image_pt.x, f.ref_sphere.image_pt.y,
                            f.ref_sphere.image_pt.z + imageThi);
                    double onSphereRef = o.minus(ebar.plus(cRef)).length() / Math.abs(R);
                    double onSpherePartner = o.minus(ebar.plus(cPartner)).length() / Math.abs(R);
                    System.out.printf("%4d  %s   %5.2f  %9.4f   %9.6f   %11.6f   %9.6f   %7d   %.9f %.9f%n",
                            fi, axis == 0 ? "sag" : "tan", r, R, shift,
                            chordDiff / requestedDirectionDifference,
                            rayDiff / requestedDirectionDifference,
                            aimed.iterations(), onSphereRef, onSpherePartner);
                }
            }
        }
    }
}
