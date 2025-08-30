package org.redukti.jfotoptix.spec;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

// A format for prescription that is easier to work with when
// trying to optimize
public class Prescription {

    public final double focalLength;
    public final double fno;
    // The quoted angle of view - e.g. 47 degrees for 50mm
    public final double angleOfViewDegrees;
    // For 35mm this is sqrt(36^2 + 24^2) = 43.27
    public final double diameterImageCircle;
    public final boolean d_line;

    // Used to build
    public List<SurfaceType> surfaceList = new ArrayList<SurfaceType>();
    public SurfaceType[] surfaces;

    // Maps our config id to scenario number in the OpticalBench specs
    public int[] _configurations;
    public String[] _configuration_names;
    public double[] _angle_of_views_by_scenario;
    public double[] _focal_length_by_scenario;
    public double[] _f_number_by_scenario;

    public Distribution distribution;   // FIXME rename, used for ray finding only
    public double varAoV = 0.0;

    // Following are optional values for information only
    public String title;
    public String lensName;
    public String patentCountry = "";
    public String patentNumber;
    public String patentExample = "";
    public String applicationYear = "";
    public String inventors = "";
    public String organization = "";
    public String patentLink = "";

    public Prescription(double focalLength, double fno, double angleOfViewDegrees, double diameterImageCircle, boolean d_line) {
        this.focalLength = focalLength;
        this.fno = fno;
        this.angleOfViewDegrees = angleOfViewDegrees;
        this.diameterImageCircle = diameterImageCircle;
        this.d_line = d_line;
        this.distribution = new Distribution(Pattern.UserDefined,10, 0.999);
    }

    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd, String glassName) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1), false, radius, thickness, diameter, nd, vd, glassName));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),false, radius, thickness, diameter, nd, vd, null));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),false, radius, thickness, diameter, 0, 0, null));
        return this;
    }
    public Prescription stop(double thickness, double diameter) {
        surfaceList.add(new SurfaceType(Integer.toString(surfaceList.size()+1),true,0,thickness,diameter,0,0,null));
        return this;
    }
    public Prescription field_stop(double thickness, double diameter) {
        var surface = new SurfaceType(Integer.toString(surfaceList.size()+1),false,0,thickness,diameter,0,0,null);
        surface.isFieldStop = true;
        surfaceList.add(surface);
        return this;
    }
    public Prescription asph(double k, double[] coeffs) {
        var lastSurface = surfaceList.get(surfaceList.size()-1);
        lastSurface.k = k;
        lastSurface.coeffs = coeffs;
        return this;
    }
    /**
     * The diameter for given field
     */
    public double imageDiameterForField(double field) {
        assert field >= 0 && field <= 1.0;
        return diameterImageCircle*field;
    }

    /**
     * Full angle of view in degrees for given field
     */
    public double fullAngleOfViewDegrees(double field) {
        if (field == 0.0) return 0.0;
        if (field > 0 && field <= 1.0) {
            var radius = imageDiameterForField(field)/2.0;
            var radians = Math.atan(radius/focalLength);
            return 2.0*Math.toDegrees(radians);
        }
        else throw new IllegalArgumentException("Field must be between 0 and 1.");
    }
    public double getHalfAngleOfViewInDegrees() {
        return angleOfViewDegrees/2.0;
    }
    public double getHalfAngleOfViewInRadians() {
        return Math.toRadians(angleOfViewDegrees/2.0);
    }
    public Prescription build() {
        this.surfaces = surfaceList.toArray(new SurfaceType[surfaceList.size()]);
        return this;
    }
    private Prescription import_surface(OpticalBenchDataImporter.LensSurface surface,
                       int scenario, boolean use_glass_types) {
        double thickness = surface.get_thickness(scenario);
        double radius = surface.get_radius();
        double refractive_index = surface.get_refractive_index();
        double abbe_vd = surface.get_abbe_vd();
        String glass_name = surface.get_glass_name();
        if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
            stop(thickness,surface.get_diameter(0));
            return this;
        }
        else if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
            field_stop(thickness,surface.get_diameter(0));
            return this;
        }
        if (use_glass_types && glass_name != null && GlassMap.glassByName(glass_name) != null) {
            surf(radius,thickness,surface.get_diameter(0),refractive_index,abbe_vd,glass_name);
        }
        else if (refractive_index != 0.0) {
            surf(radius,thickness,surface.get_diameter(0),refractive_index, abbe_vd);
        } else {
            surf(radius,thickness,surface.get_diameter(0));
        }
        OpticalBenchDataImporter.AsphericalData aspherical_data = surface.get_aspherical_data();
        if (aspherical_data != null) {
            double k = aspherical_data.data(1);
            double[] coeffs = new double[] {
                    aspherical_data.data(2),
                    aspherical_data.data(3),
                    aspherical_data.data(4),
                    aspherical_data.data(5),
                    aspherical_data.data(6),
                    aspherical_data.data(7),
                    aspherical_data.data(8),
                    aspherical_data.data(9),
                    aspherical_data.data(10)};
            asph(k,coeffs);
        }
        return this;
    }

    public static Prescription buildPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types) {
        var prescription = new Prescription(
                specs.get_focal_length(),
                specs.get_f_number(0),  // default is scenario 0
                specs.get_angle_of_view_in_degrees(0),  // default is scenario 0
                specs.get_image_height(),
                false);
        prescription.title = specs.get_descriptive_data().get_title();
        var patentInfo = specs.get_descriptive_data().find_variable("patent");
        if (patentInfo != null) {
            prescription.patentCountry = patentInfo.get_value(0);
            prescription.patentNumber = patentInfo.get_value(1);
            prescription.patentExample = patentInfo.get_value(2);
            prescription.applicationYear = patentInfo.get_value(3);
            prescription.inventors = patentInfo.get_value( 4);
            prescription.organization = patentInfo.get_value(5);
            prescription.patentLink = patentInfo.get_value(6);
        }
        var lensName = specs.get_descriptive_data().find_variable("lens name");
        if (lensName != null) {
            prescription.lensName = lensName.get_value(0);
        }
        prescription.add_configurations(specs);
        List<OpticalBenchDataImporter.LensSurface> surfaces = specs.get_surfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            prescription.import_surface(surfaces.get(i),0,use_glass_types);
        }
        return prescription.build();
    }

    private Prescription add_configurations(OpticalBenchDataImporter.LensSpecifications specs) {
        OpticalBenchDataImporter.Variable configurations = specs.get_descriptive_data().find_variable("configurations");
        if (configurations == null || configurations.num_values() < 2) {
            // by default, we only do scenario 0
            _configurations = new int[] {0};
            _configuration_names = new String[] {"default"};
        }
        else {
            _configurations = new int[configurations.num_values()/2];
            _configuration_names = new String[configurations.num_values()/2];
            for (int i = 0, j = 0; i < configurations.num_values(); i += 2, j++) {
                int scenario = configurations.get_value_as_integer(i,-1);
                if (scenario == -1)
                    break;
                String name = configurations.get_value(i+1);
                _configurations[j] = scenario;
                _configuration_names[j] = name;
            }
        }
        _focal_length_by_scenario = new double[_configurations.length];
        _f_number_by_scenario = new double[_configurations.length];
        _angle_of_views_by_scenario = new double[_configurations.length];
        for (int i = 0; i < _configurations.length; i++) {
            int scenario = _configurations[i];
            _focal_length_by_scenario[i] = specs.get_focal_length(scenario);
            _f_number_by_scenario[i] = specs.get_f_number(scenario);
            _angle_of_views_by_scenario[i] = specs.get_angle_of_view_in_degrees(scenario);
        }
        return this;
    }

    public OpticalSystem.Builder buildSystem(boolean addPointSource, double field) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            // angleOfViewDegrees may be set when we are called
            // by the ray finder
            if (field != 0.0 || varAoV != 0.0) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double effectiveAngle = field != 0? angleOfViewDegrees*field : varAoV;
                double aov = Math.toRadians(effectiveAngle) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.times(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d);
            if (!d_line)
                ps.add_spectral_line(SpectralLine.C)
                    .add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        for (int i = 0; i < surfaces.length; i++) {
            var s = surfaces[i];
            double thickness = add_surface(lens, s);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(diameterImageCircle * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(getHalfAngleOfViewInDegrees());
        sys.f_number(this.fno);
        return sys;
    }
    private static double add_surface(Lens.Builder lens, SurfaceType s) {
        double apertureRadius = s.diameter / 2.0;
        if (s.isStop) {
            lens.add_stop(apertureRadius, s.thickness, true);
            return s.thickness;
        }
        if (s.k != 0 || (s.coeffs != null && s.coeffs.length > 0)) {
            var curve = getAsphere(s);
            var shape = new Disk(apertureRadius);
            if (s.nd != 0.0) {
                var glass = GlassMap.glassByName(s.glassName);
                if (glass != null) {
                    lens.add_surface(curve, shape, s.thickness, glass);
                } else {
                    lens.add_surface(curve, shape, s.thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.nd, s.vd, 0.0));
                }
            } else {
                lens.add_surface(curve, shape, s.thickness, Air.air);
            }
        }
        else {
            // Non aspherical
            if (s.nd != 0.0) {
                var glass = GlassMap.glassByName(s.glassName);
                if (glass == null) {
                    lens.add_surface(s.radius, apertureRadius, s.thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, s.nd, s.vd, 0.0));
                } else {
                    lens.add_surface(s.radius, apertureRadius, s.thickness, glass);
                }
            } else {
                lens.add_surface(s.radius, apertureRadius, s.thickness);
            }
        }
        return s.thickness;
    }

    private static Asphere getAsphere(SurfaceType s) {
        double k = s.k + 1.0;
        double a4 = s.coeffs.length > 0 ? s.coeffs[0] : 0.0;
        double a6 = s.coeffs.length > 1 ? s.coeffs[1] : 0.0;
        double a8 = s.coeffs.length > 2 ? s.coeffs[2] : 0.0;
        double a10 = s.coeffs.length > 3 ? s.coeffs[3] : 0.0;
        double a12 = s.coeffs.length > 4 ? s.coeffs[4] : 0.0;
        double a14 = s.coeffs.length > 5 ? s.coeffs[5] : 0.0;
        double a16 = s.coeffs.length > 6 ? s.coeffs[6] : 0.0;
        double a18 = s.coeffs.length > 7 ? s.coeffs[7] : 0.0;
        double a20 = s.coeffs.length > 8 ? s.coeffs[8] : 0.0;
        return new Asphere(s.radius, k, a4, a6, a8, a10, a12, a14, a16, a18, a20);
    }

    public StringBuilder toOptBenchStr(StringBuilder sb) {
        sb.append("[descriptive data]\n");
        sb.append("[variable distances]\n");
        sb.append("Focal Length\t").append(focalLength).append("\n");
        sb.append("Angle of View\t").append(angleOfViewDegrees).append("\n");
        sb.append("F-Number\t").append(fno).append("\n");
        sb.append("Image Height\t").append(diameterImageCircle).append("\n");
        sb.append("[lens data]\n");
        for (SurfaceType surface : surfaceList) {
            surface.toOptBenchStr(sb);
        }
        sb.append("[aspherical data]\n");
        for (SurfaceType surface : surfaceList) {
            surface.asphericsToOptBenchStr(sb);
        }
        return sb;
    }
    public StringBuilder toMarkdownStr(StringBuilder sb) {
        if (lensName != null)
            sb.append("# ").append(lensName).append("\n");
        if (patentNumber != null) {
            sb.append("## Patent Information\n");
            sb.append("| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |\n");
            sb.append("| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |\n");
            sb.append("|").append(patentCountry).append(" | ")
                    .append(patentNumber).append(" | ")
                    .append(patentExample).append(" | ")
                    .append(applicationYear).append(" | ")
                    .append(inventors).append(" | ")
                    .append(organization).append(" | ")
                    .append("[link](").append(patentLink).append(") |\n");
        }
        boolean sawAsph = false;
        SurfaceType.toMarkdownTableHeader(sb);
        for (SurfaceType surface : surfaceList) {
            surface.toMarkdownTableRow(sb);
            if (surface.isAspheric())
                sawAsph = true;
        }
        if (sawAsph) {
            SurfaceType.asphericMarkdownTableHeader(sb);
            for (SurfaceType surface : surfaceList) {
                if (surface.isAspheric())
                    surface.ashericToMarkdownTableRow(sb);
            }
        }
        return sb;
    }

    @Override
    public String toString() {
        return toOptBenchStr(new StringBuilder()).toString();
    }
}
