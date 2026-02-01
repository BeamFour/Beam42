package org.redukti.spec;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.mathlib.Vector3Pair;

import java.util.List;

public class FotoptixSystemBuilder {

    public final Prescription _prescription;

    public FotoptixSystemBuilder(Prescription _prescription) {
        this._prescription = _prescription;
    }

    public OpticalSystem.Builder buildSystem(boolean addPointSource, double field) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            // angleOfViewDegrees may be set when we are called
            // by the ray finder
            if (field != 0.0 || _prescription._var_angle_of_view != 0.0) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double effectiveAngle = field != 0? _prescription._angle_of_view_in_degrees *field : _prescription._var_angle_of_view;
                double aov = Math.toRadians(effectiveAngle) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.multiply(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction);
            for (double wvl: _prescription._wvls)
                ps.add_spectral_line(wvl);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        for (int i = 0; i < _prescription._surfaces.length; i++) {
            var s = _prescription._surfaces[i];
            double thickness = add_surface(lens, s);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder()
                .position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001))
                .curve(Flat.flat)
                .shape(new Rectangle(_prescription._diameter_image_circle * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(_prescription.get_half_angle_in_degrees());
        sys.f_number(_prescription._fno);
        return sys;
    }
    private static double add_surface(Lens.Builder lens, SurfaceType s) {
        double ap_radius = s.get_diameter() / 2.0;
        double thickness = s.get_thickness();
        if (s.is_aperture_stop()) {
            lens.add_stop(ap_radius, thickness, true);
            return thickness;
        }
        if (s.is_aspheric()) {
            var curve = build_asphere(s);
            var shape = new Disk(ap_radius);
            if (s.get_refractive_index() != 0.0) {
                var glass = GlassMap.glassByName(s.get_glass_name());
                if (glass != null) {
                    lens.add_surface(curve, shape, thickness, glass);
                } else {
                    lens.add_surface(curve, shape, thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.get_refractive_index(), s.get_abbe_vd(), 0.0));
                }
            } else {
                lens.add_surface(curve, shape, thickness, Air.air);
            }
        }
        else {
            // Non aspherical
            if (s.get_refractive_index() != 0.0) {
                var glass = GlassMap.glassByName(s.get_glass_name());
                if (glass == null) {
                    lens.add_surface(s.get_radius_of_curvature(), ap_radius, thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.get_refractive_index(), s.get_abbe_vd(), 0.0));
                } else {
                    lens.add_surface(s.get_radius_of_curvature(), ap_radius, thickness, glass);
                }
            } else {
                lens.add_surface(s.get_radius_of_curvature(), ap_radius, thickness);
            }
        }
        return thickness;
    }

    private static Asphere build_asphere(SurfaceType s) {
        double[] coeffs = s.get_aspheric_coeffs();
        double k = s.get_cc() + 1.0;
        double a4 = coeffs.length > 1 ? coeffs[1] : 0.0;
        double a6 = coeffs.length > 2 ? coeffs[2] : 0.0;
        double a8 = coeffs.length > 3 ? coeffs[3] : 0.0;
        double a10 = coeffs.length > 4 ? coeffs[4] : 0.0;
        double a12 = coeffs.length > 5 ? coeffs[5] : 0.0;
        double a14 = coeffs.length > 6 ? coeffs[6] : 0.0;
        double a16 = coeffs.length > 7 ? coeffs[7] : 0.0;
        double a18 = coeffs.length > 8 ? coeffs[8] : 0.0;
        double a20 = coeffs.length > 9 ? coeffs[9] : 0.0;
        return new Asphere(s.get_radius_of_curvature(), k, a4, a6, a8, a10, a12, a14, a16, a18, a20);
    }

    private static double add_surface(Lens.Builder lens, OpticalBenchDataImporter.LensSurface surface,
                                      int scenario, boolean use_glass_types) {
        double thickness = surface.get_thickness(scenario);
        double radius = surface.get_radius();
        double aperture_radius = surface.get_diameter(scenario) / 2.0;
        double refractive_index = surface.get_refractive_index();
        double abbe_vd = surface.get_abbe_vd();
        String glass_name = surface.get_glass_name();
        if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
            lens.add_stop(aperture_radius, thickness, true);
            return thickness;
        }
        else if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
            lens.add_stop(aperture_radius, thickness, false);
            return thickness;
        }
        OpticalBenchDataImporter.AsphericalData aspherical_data = surface.get_aspherical_data();
        if (aspherical_data == null) {
            if (use_glass_types && glass_name != null && GlassMap.glassByName(glass_name) != null) {
                lens.add_surface(
                        radius, aperture_radius, thickness,
                        GlassMap.glassByName(glass_name));
            }
            else if (refractive_index != 0.0) {
                if (abbe_vd == 0.0) {
                    return -1.0;
                }
                lens.add_surface(
                        radius, aperture_radius, thickness,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, refractive_index, abbe_vd, 0.0));
            } else {
                lens.add_surface(radius, aperture_radius, thickness);
            }
            return thickness;
        }
        double k = aspherical_data.data(1) + 1.0;
        double a4 = aspherical_data.data(2);
        double a6 = aspherical_data.data(3);
        double a8 = aspherical_data.data(4);
        double a10 = aspherical_data.data(5);
        double a12 = aspherical_data.data(6);
        double a14 = aspherical_data.data(7);
        double a16 = aspherical_data.data(8);
        double a18 = aspherical_data.data(9);
        double a20 = aspherical_data.data(10);

        if (use_glass_types && glass_name != null && GlassMap.glassByName(glass_name) != null) {
            lens.add_surface(
                    new Asphere(radius, k, a4, a6, a8, a10, a12,
                            a14, a16, a18, a20),
                    new Disk(aperture_radius), thickness,
                    GlassMap.glassByName(glass_name));
        }
        else if (refractive_index > 0.0) {
            lens.add_surface(
                    new Asphere(radius, k, a4, a6, a8, a10, a12,
                            a14, a16, a18, a20),
                    new Disk(aperture_radius), thickness,
                    new Abbe(Abbe.AbbeFormula.AbbeVd, refractive_index, abbe_vd, 0.0));
        } else {
            lens.add_surface(new Asphere(radius, k, a4, a6,
                            a8, a10, a12, a14, a16, a18, a20),
                    new Disk(aperture_radius),
                    thickness, Air.air);
        }
        return thickness;
    }

    public static OpticalSystem.Builder build_system(OpticalBenchDataImporter.LensSpecifications specs, int scenario,
                                                     boolean use_glass_types) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<OpticalBenchDataImporter.LensSurface> surfaces = specs.get_surfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            double thickness = add_surface(lens, surfaces.get(i), scenario, use_glass_types);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(
                        new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001))
                .curve(Flat.flat)
                .shape(new Rectangle(specs.get_image_height() * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(specs.get_half_angle_of_view_in_degrees(scenario));
        sys.f_number(specs.get_f_number(scenario));
        return sys;
    }

}
