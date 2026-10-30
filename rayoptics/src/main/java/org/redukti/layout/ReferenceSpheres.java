// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.layout;

import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.RaySeg;
import org.redukti.rayoptics.raytr.ReferenceSphere;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.raytr.WaveAbr;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.specs.ConjugateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Meridional geometry of a field's reference spheres, in layout coordinates
 * (x is global z, y is global y).
 * <p>
 * With real ray aiming the entrance pupil is the aim's Field.z_enp. Otherwise
 * it is where the chief ray crosses the axis, the construction WaveAbr uses
 * for the exit pupil. An axial chief ray has no crossing, so it takes the
 * paraxial pupil. An object at infinity has a plane rather than a sphere,
 * normal to the chief ray.
 * <p>
 * The geometry assumes the optical axis is the global z axis, and the field
 * lies in the y-z plane.
 */
public final class ReferenceSpheres {
    /**
     * One reference surface.
     *
     * @param pupil         the real pupil centre, on the chief ray
     * @param paraxialPupil the paraxial pupil centre, on the axis
     * @param centre        object or image point; null for a plane
     * @param radius        distance from centre to pupil; infinite for a plane
     */
    public record Section(Vector2 pupil, Vector2 paraxialPupil, Vector2 centre, double radius) {
        public boolean isPlane() { return centre == null; }
    }

    /**
     * A ray, with its object and image space segments and where they meet the
     * reference surfaces. The direction is that of light.
     *
     * @param firstPoint   intersection with the first surface
     * @param incidentDir  unit direction in object space
     * @param lastPoint    intersection with the last surface before the image
     * @param emergentDir  unit direction in image space
     * @param entranceHit  intersection with the entrance surface
     * @param exitHit      intersection with the exit sphere; null if the ray misses it
     */
    public record Ray(RayPkg pkg, Vector2 firstPoint, Vector2 incidentDir,
                      Vector2 lastPoint, Vector2 emergentDir,
                      Vector2 entranceHit, Vector2 exitHit) {}

    public final Section entrance;
    public final Section exit;
    public final Ray chief;
    /** The upper and lower meridional rim rays that traced to the image. */
    public final List<Ray> rims;

    private ReferenceSpheres(Section entrance, Section exit, Ray chief, List<Ray> rims) {
        this.entrance = entrance;
        this.exit = exit;
        this.chief = chief;
        this.rims = rims;
    }

    /** Aims and traces the chief and rim rays of a field and builds both reference surfaces. */
    public static ReferenceSpheres compute(OpticalModel model, Field field, double wavelength,
                                           TraceOptions traceOptions) {
        // Sets field.chief_ray and field.ref_sphere
        List<RayPkg> boundary = Trace.trace_boundary_rays_at_field(model, field, wavelength, traceOptions);
        var named = Trace.boundary_ray_dict(model, boundary);
        var chiefPkg = field.chief_ray.chief_ray;

        Vector3 objectPoint = objectPoint(model, chiefPkg);
        Section entrance = entranceSection(model, field, chiefPkg, objectPoint);
        Section exit = exitSection(model, field);

        Vector3 entrancePupil = new Vector3(0.0, entrance.pupil.y(), entrance.pupil.x());
        Ray chief = ray(model, field, chiefPkg, objectPoint, entrancePupil);
        List<Ray> rims = new ArrayList<>();
        for (String name : new String[]{"+Y", "-Y"}) {
            RayPkg rim = named.get(name);
            // A failed ray stops short of the image and has no image space segment
            if (rim != null && rim.ray.size() == chiefPkg.ray.size())
                rims.add(ray(model, field, rim, objectPoint, entrancePupil));
        }
        return new ReferenceSpheres(entrance, exit, chief, rims);
    }

    /** Returns the global object point, or null when it is at infinity. */
    private static Vector3 objectPoint(OpticalModel model, RayPkg chief) {
        if (model.optical_spec.conjugate_type(ImageKey.Object) == ConjugateType.INFINITE) return null;
        return global(model.seq_model.gbl_tfrms.get(0), chief.ray.get(0).p);
    }

    private static Section entranceSection(OpticalModel model, Field field, RayPkg chief, Vector3 objectPoint) {
        SequentialModel sm = model.seq_model;
        FirstOrderData fod = model.optical_spec.parax_data.fod;
        Vector3 first = global(sm.gbl_tfrms.get(1), chief.ray.get(1).p);
        Vector3 direction = sm.gbl_tfrms.get(0).rt.multiply(chief.ray.get(0).d);
        double firstVertex = sm.gbl_tfrms.get(1).t.z;
        Vector3 pupil;
        if (field.z_enp != null)
            pupil = new Vector3(0.0, 0.0, firstVertex + field.z_enp);
        else if (Math.abs(direction.y) < 1e-14)
            pupil = new Vector3(0.0, 0.0, firstVertex + fod.enp_dist);
        else
            pupil = first.plus(direction.times(-first.y / direction.y));
        Vector2 paraxial = new Vector2(firstVertex + fod.enp_dist, 0.0);
        if (objectPoint == null)
            return new Section(layout(pupil), paraxial, null, Double.POSITIVE_INFINITY);
        return new Section(layout(pupil), paraxial, layout(objectPoint), pupil.minus(objectPoint).length());
    }

    private static Section exitSection(OpticalModel model, Field field) {
        SequentialModel sm = model.seq_model;
        FirstOrderData fod = model.optical_spec.parax_data.fod;
        ReferenceSphere sphere = field.ref_sphere;
        // Exit pupil coordinates are relative to the last surface, the image point to the image
        Tfm3d last = Lists.get(sm.gbl_tfrms, -2);
        Tfm3d image = Lists.get(sm.gbl_tfrms, -1);
        Vector2 pupil = layout(global(last, field.chief_ray.cr_exp_seg.exp_pt));
        Vector2 paraxial = new Vector2(last.t.z + fod.exp_dist, 0.0);
        return new Section(pupil, paraxial, layout(global(image, sphere.image_pt)), sphere.ref_sphere_radius);
    }

    private static Ray ray(OpticalModel model, Field field, RayPkg pkg, Vector3 objectPoint, Vector3 entrancePupil) {
        SequentialModel sm = model.seq_model;
        Tfm3d lastTfm = Lists.get(sm.gbl_tfrms, -2);
        RaySeg lastSeg = Lists.get(pkg.ray, -2);
        Vector3 first = global(sm.gbl_tfrms.get(1), pkg.ray.get(1).p);
        Vector3 incident = sm.gbl_tfrms.get(0).rt.multiply(pkg.ray.get(0).d);
        Vector3 chiefIncident = sm.gbl_tfrms.get(0).rt.multiply(field.chief_ray.chief_ray.ray.get(0).d);

        Vector3 entranceHit;
        if (objectPoint == null) {
            double distance = entrancePupil.minus(first).dot(chiefIncident) / incident.dot(chiefIncident);
            entranceHit = first.plus(incident.times(distance));
        }
        else {
            // Every ray leaves the object point, the centre of this sphere
            double radius = entrancePupil.minus(objectPoint).length();
            double side = chiefIncident.dot(entrancePupil.minus(objectPoint)) < 0.0 ? -1.0 : 1.0;
            entranceHit = objectPoint.plus(incident.times(side * radius));
        }

        // The same point the OPD is measured to, relative to the exit pupil centre
        Vector3 exitCoord = WaveAbr.wave_abr_calc_finite_pupil(pkg, field.chief_ray, field.ref_sphere)
                .ray_exit_pupil_coord();
        Vector2 exitHit = null;
        if (exitCoord != null)
            exitHit = layout(global(lastTfm, field.chief_ray.cr_exp_seg.exp_pt.plus(exitCoord)));

        return new Ray(pkg, layout(first), layoutDirection(incident),
                layout(global(lastTfm, lastSeg.p)), layoutDirection(lastTfm.rt.multiply(lastSeg.d)),
                layout(entranceHit), exitHit);
    }

    private static Vector3 global(Tfm3d tfm, Vector3 local) {
        return tfm.rt.multiply(local).add(tfm.t);
    }

    private static Vector2 layout(Vector3 global) {
        return new Vector2(global.z, global.y);
    }

    private static Vector2 layoutDirection(Vector3 direction) {
        double length = Math.hypot(direction.z, direction.y);
        return M.isZero(length) ? new Vector2(1.0, 0.0) : new Vector2(direction.z / length, direction.y / length);
    }
}
