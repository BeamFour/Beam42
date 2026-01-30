package org.redukti.spec;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.mathlib.Vector3Pair;
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
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.elem.profiles.RadialPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

import java.util.*;

/**
 * A format for prescription that is easier to work with when trying to optimize.
 * Supports both single and multiple configurations - a multi configuration setup is
 * required for zoom lenses where thicknesses, focal lengths, diameters, fnumbers can
 * vary by zoom setting.
 * There is always a default scenario configuration.
 */
public class Prescription {

    /** Focal length of default scenario, in multi config this is defined by _focal_length_by_scenario */
    public final double _focal_length;
    /** F-number of default scenario, in multi config this is defined by _f_number_by_scenario */
    public final double _fno;
    /** The quoted angle of view - e.g. 47 degrees for 50mm; this is the full angle of view
     * This is defined by _angle_of_views_by_scenario in multi config
     */
    public final double _angle_of_view_in_degrees;
    /** For 35mm this is sqrt(36^2 + 24^2) = 43.27 */
    public final double _diameter_image_circle;

    /** wavelengths to use,
     * NOTE atm first wvl is made reference wvl
     */
    public final double[] _wvls;
    /** wavelength weights - mainly used when computing spot and MTFs */
    public final double[] _wts;
    /** when building we use a list */
    public List<SurfaceType> _surface_list = new ArrayList<SurfaceType>();
    /** After construction this is the list of surfaces */
    public SurfaceType[] _surfaces;

    /** Maps our config id to scenario number in the OpticalBench specs
     * The scenario in OBench corresponds to how it is defined in
     * source patent data
     */
    public int[] _configurations;
    /** Each config is given a name */
    public String[] _configuration_names;
    /** Each config has its own angle of view */
    public double[] _angle_of_views_by_scenario;
    /** Each config has its own focal length */
    public double[] _focal_length_by_scenario;
    /** Each config has its own fnumber */
    public double[] _f_number_by_scenario;

    public Distribution _distribution;   // FIXME rename, used for ray finding only

    public double _var_angle_of_view = 0.0;

    /** Following are optional values for information only, used to generate
     * lens report.
     */
    public String _title;
    public String _lens_name;
    public String _patent_country = "";
    public String _patent_number;
    public String _patent_example = "";
    public String _application_year = "";
    public String _inventors = "";
    public String _organization = "";
    public String _patent_link = "";

    public Prescription(double focalLength, double fno, double angleOfViewDegrees, double diameterImageCircle, boolean d_line) {
        this(focalLength,fno,angleOfViewDegrees,diameterImageCircle,
            d_line ? new double[] {587.5618} : new double[] {587.5618, 486.1327, 656.2725},
            d_line ? new double[] {1.0} : new double[] {1.0, 1.0, 1.0});
    }
    public Prescription(double focalLength, double fno, double angleOfViewDegrees, double diameterImageCircle, double[] wvls, double[] wts) {
        this._focal_length = focalLength;
        this._fno = fno;
        this._angle_of_view_in_degrees = angleOfViewDegrees;
        this._diameter_image_circle = diameterImageCircle;
        this._wvls = wvls;
        this._wts = wts;
        this._distribution = new Distribution(Pattern.UserDefined,10, 0.999);
    }
    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd, String glassName) {
        _surface_list.add(new SurfaceType(Integer.toString(_surface_list.size()+1), false, radius, thickness, diameter, nd, vd, glassName));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd) {
        _surface_list.add(new SurfaceType(Integer.toString(_surface_list.size()+1),false, radius, thickness, diameter, nd, vd, null));
        return this;
    }
    public Prescription surf(double radius, double thickness, double diameter) {
        _surface_list.add(new SurfaceType(Integer.toString(_surface_list.size()+1),false, radius, thickness, diameter, 0, 0, null));
        return this;
    }
    public Prescription stop(double thickness, double diameter) {
        _surface_list.add(new SurfaceType(Integer.toString(_surface_list.size()+1),true,0,thickness,diameter,0,0,null));
        return this;
    }
    public Prescription field_stop(double thickness, double diameter) {
        var surface = new SurfaceType(Integer.toString(_surface_list.size()+1),false,0,thickness,diameter,0,0,null);
        surface._is_field_stop = true;
        _surface_list.add(surface);
        return this;
    }

    /** Sets up the last added surface as an asphere.
     * @param asph_type 1=EVEN,2=EVEN_A2,3=ODD
     * @param coeffs for EVEN first param is not used and should be 0, for ODD first 2 must be 0
     */
    public Prescription asph(int asph_type, double k, double[] coeffs) {
        if (asph_type == SurfaceType.ASPH_EVEN && coeffs[0] != 0.0)
            throw new IllegalArgumentException("Even aspheres must have 0 as first coefficient");
        else if (asph_type == SurfaceType.ASPH_ODD && (coeffs[0] != 0.0 || coeffs[1] != 0.0))
            throw new IllegalArgumentException("Odd aspheres must have 0 as first and second coefficients");
        var lastSurface = _surface_list.get(_surface_list.size()-1);
        lastSurface._asph_type = asph_type;
        lastSurface._k = k;
        lastSurface._coeffs = coeffs;
        return this;
    }
    /**
     * Derive diameter for given field.
     * @param field fields are relative, 0 for axis to 1 for edge.
     */
    public double imageDiameterForField(double field) {
        assert field >= 0 && field <= 1.0;
        return _diameter_image_circle * field;
    }

    /**
     * Full angle of view in degrees for given field for default configuration
     * @param field fields are relative, 0 for axis to 1 for edge.
     */
    public double fullAngleOfViewDegrees(double field) {
        if (field == 0.0) return 0.0;
        if (field > 0 && field <= 1.0) {
            var radius = imageDiameterForField(field)/2.0;
            var radians = Math.atan(radius/ _focal_length);
            return 2.0*Math.toDegrees(radians);
        }
        else throw new IllegalArgumentException("Field must be between 0 and 1.");
    }
    /** Get angle of view for default configuration */
    public double getHalfAngleOfViewInDegrees() {
        return _angle_of_view_in_degrees/2.0;
    }
    /** Get angle of view for default configuration */
    public double getHalfAngleOfViewInRadians() {
        return Math.toRadians(_angle_of_view_in_degrees/2.0);
    }
    public Prescription build() {
        this._surfaces = _surface_list.toArray(new SurfaceType[_surface_list.size()]);
        return this;
    }
    private Prescription import_surface(OpticalBenchDataImporter.LensSurface surface,
                       int scenario, boolean use_glass_types) {
        double thickness = surface.get_thickness(scenario);
        double radius = surface.get_radius();
        double refractive_index = surface.get_refractive_index();
        double abbe_vd = surface.get_abbe_vd();
        double diameter = surface.get_diameter(scenario);
        String glass_name = surface.get_glass_name();
        if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.aperture_stop) {
            stop(thickness,diameter);
            return this;
        }
        else if (surface.get_surface_type() == OpticalBenchDataImporter.SurfaceType.field_stop) {
            field_stop(thickness,diameter);
            return this;
        }
        if (use_glass_types && glass_name != null && GlassMap.glassByName(glass_name) != null) {
            surf(radius,thickness,diameter,refractive_index,abbe_vd,glass_name);
        }
        else if (refractive_index != 0.0) {
            surf(radius,thickness,diameter,refractive_index, abbe_vd);
        } else {
            surf(radius,thickness,diameter);
        }
        OpticalBenchDataImporter.AsphericalData aspherical_data = surface.get_aspherical_data();
        if (aspherical_data != null) {
            int asph_type = SurfaceType.ASPH_EVEN;
            switch (aspherical_data.get_asphere_type()) {
                case Even -> asph_type = SurfaceType.ASPH_EVEN;
                case EvenA2 -> asph_type = SurfaceType.ASPH_EVEN_A2;
                case Odd -> asph_type = SurfaceType.ASPH_ODD;
            }
            double k = aspherical_data.get_cc();
            double[] coeffs = aspherical_data.get_coeffs();
            asph(asph_type,k,coeffs);
        }
        return this;
    }

    public static Prescription buildPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types) {
        return buildPrescription(specs,use_glass_types,new double[] {587.5618, 486.1327, 656.2725},new double[] {1.0, 1.0, 1.0},0);
    }
    public static Prescription buildPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types,double[] wvls,double[] wts) {
        return buildPrescription(specs,use_glass_types,wvls,wts,0);
    }

    /**
     * Helper to build a Prescription from OpticalBench file.
     *
     * @param specs Specs obtained from OpticalBench format
     * @param use_glass_types If true will use glass types if glass names are provided
     * @param wvls Wavelengths to use
     * @param wts Wavelength weights - mainly used for Spot diagrams and MTFs
     * @param default_scenario Default scenario - use 0 if input has no scenarios
     */
    public static Prescription buildPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types,double[] wvls,double[] wts,int default_scenario) {
        // We use default values variables that can change in a multi-configuration setup.
        // The defaults are useful as they are the ones that are manipulated during optimization
        var prescription = new Prescription(
                specs.get_focal_length(),
                specs.get_f_number(default_scenario),
                specs.get_angle_of_view_in_degrees(default_scenario),
                specs.get_image_height(),
                wvls,
                wts);
        prescription._title = specs.get_descriptive_data().get_title();
        var patent_info_n = specs.get_patent_info();
        if (patent_info_n.count() > 0) {
            // New style
            prescription._patent_country = patent_info_n.get_value("country");
            prescription._patent_number = patent_info_n.get_value("number");
            prescription._patent_example = patent_info_n.get_value("example");
            prescription._application_year = patent_info_n.get_value("year applied");
            prescription._inventors = patent_info_n.get_value("inventors");
            prescription._organization = patent_info_n.get_value("original assignee");
            if (prescription._organization.isEmpty())
                prescription._organization = patent_info_n.get_value("current assignee");
            prescription._patent_link = patent_info_n.get_value("link");
        }
        else if (specs.get_descriptive_data().find_variable("patent") != null) {
            // old style to be deleted
            var patentInfo = specs.get_descriptive_data().find_variable("patent");
            prescription._patent_country = patentInfo.get_value(0);
            prescription._patent_number = patentInfo.get_value(1);
            prescription._patent_example = patentInfo.get_value(2);
            prescription._application_year = patentInfo.get_value(3);
            prescription._inventors = patentInfo.get_value( 4);
            prescription._organization = patentInfo.get_value(5);
            prescription._patent_link = patentInfo.get_value(6);
        }
        var report_data = specs.get_report_data();
        String lensName = null;
        if (report_data.count() > 0) {
            // new style
            lensName = report_data.get_value("lens name");
        }
        if (lensName == null) {
            // old style - to be removed
            var variable = specs.get_descriptive_data().find_variable("lens name");
            if (variable != null)
                lensName = variable.get_value(0);
        }
        if (lensName != null) {
            prescription._lens_name = lensName;
        }
        // If the input file defines a configuration section
        // then we import the configurations
        prescription.add_configurations(specs);
        List<OpticalBenchDataImporter.LensSurface> surfaces = specs.get_surfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            prescription.import_surface(surfaces.get(i),default_scenario,use_glass_types);
            if (prescription._configurations != null) {
                prescription.add_configuration_data(surfaces.get(i));
            }
        }
        return prescription.build();
    }

    private void add_configuration_data(OpticalBenchDataImporter.LensSurface lensSurface) {
        if (_configurations == null || _configurations.length == 0)
            return;
        var lastSurface = _surface_list.get(_surface_list.size()-1);
        var thickness_by_scenario = lensSurface.get_thickness_by_scenario();
        if (thickness_by_scenario.size() > 1) {
            double[] thickness = new double[_configurations.length];
            for (int i = 0; i < _configurations.length; i++) {
                int scenario = _configurations[i];
                thickness[i] = thickness_by_scenario.get(scenario);
            }
            assert thickness[0] == lastSurface._thickness;
            lastSurface.set_thickness_by_scenario(thickness);
        }
        if (lensSurface.is_aperture_stop()) {
            var diameter_by_scenario = lensSurface.get_diameter_by_scenario();
            if (diameter_by_scenario.size() > 1) {
                double[] diameter = new double[_configurations.length];
                for (int i = 0; i < diameter.length; i++) {
                    int scenario = _configurations[i];
                    diameter[i] = diameter_by_scenario.get(scenario);
                }
                assert diameter[0] == lastSurface._diameter;
                lastSurface.set_diameter_by_scenario(diameter);
            }
        }
    }

    private Prescription add_configurations(OpticalBenchDataImporter.LensSpecifications specs) {
        var configurations = specs.get_report_data().find_variable("scenarios");
        var configuration_names = specs.get_report_data().find_variable("names");
        if (configurations != null && configurations.num_values() > 0 &&
            configuration_names != null && configuration_names.num_values() == configurations.num_values()) {
            _configurations = new int[configurations.num_values()];
            _configuration_names = new String[configuration_names.num_values()];
            for (int i = 0; i < configurations.num_values(); i++) {
                int scenario = configurations.get_value_as_integer(i,0);
                String name = configuration_names.get_value(i);
                _configurations[i] = scenario;
                _configuration_names[i] = name;
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
        }
        return this;
    }

    public OpticalSystem.Builder buildSystem(boolean addPointSource, double field) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            // angleOfViewDegrees may be set when we are called
            // by the ray finder
            if (field != 0.0 || _var_angle_of_view != 0.0) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                double effectiveAngle = field != 0? _angle_of_view_in_degrees *field : _var_angle_of_view;
                double aov = Math.toRadians(effectiveAngle) / 2.0;
                Matrix3 r = Matrix3.get_rotation_matrix(0, aov);
                direction = r.multiply(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction);
            for (double wvl: _wvls)
                ps.add_spectral_line(wvl);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        for (int i = 0; i < _surfaces.length; i++) {
            var s = _surfaces[i];
            double thickness = add_surface(lens, s);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(_diameter_image_circle * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(getHalfAngleOfViewInDegrees());
        sys.f_number(this._fno);
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

    public OpticalModel build_ray_optics_model(boolean fov_angle, double[] fields, boolean do_apertures, VigType vig_type, boolean use_wideangle_aiming, int config) {
        if (fields == null)
            fields = new double[]{0., .707, 1.};
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        var angle_of_view_deg = config == 0 ? _angle_of_view_in_degrees : _angle_of_views_by_scenario[config];
        double half_angle_deg =  angle_of_view_deg/2.0;
        var fno = config == 0 ? _fno : _f_number_by_scenario[config];
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), fno);
        if (fov_angle) {
            osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), fields);
            osp.fov.value = half_angle_deg;
        }
        else {
            osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Image, ValueKey.RealHeight), fields);
            osp.fov.value = _diameter_image_circle/2.0;
        }
        osp.fov.is_relative = true; // Fields are specified as 0, 0.7, 1.0 etc - without actual sizes
        osp.fov.is_wide_angle = (half_angle_deg > 45.) || use_wideangle_aiming;
        var wvls = new ArrayList<WvlWt>();
        for (int i = 0; i < _wvls.length; i++)
            wvls.add(new WvlWt(_wvls[i], _wts[i]));
        osp.wvls = new WvlSpec(wvls.toArray(new WvlWt[0]), 0);
        opm.system_spec.title = _title;
        opm.system_spec.dimensions = "mm";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        for (int i = 0; i < _surfaces.length; i++) {
            var s = _surfaces[i];
            add_rayoptic_surface(sm,s,config);
        }
        sm.do_apertures = do_apertures;
        opm.update_model();
        switch (vig_type)  {
            case Paraxial -> {
                Trace.apply_paraxial_vignetting(opm);
                opm.update_model();
            }
            case SetPupil -> {
                VigCalc.set_pupil(opm);
                opm.update_model();
            }
            case SetStopAperture -> {
                VigCalc.set_stop_aperture(opm);
                opm.update_model();
            }
            case SetVig -> {
                VigCalc.set_vig(opm,false);
                opm.update_model();
            }
        }
        return opm;
    }

    private void add_rayoptic_surface(SequentialModel sm, SurfaceType s, int config) {
        var diameter = s.get_diameter_by_scenario(config);
        double ap_radius = diameter / 2.0;
        double thickness = s.get_thickness_by_scenario(config);

        if (s.get_refractive_index() != 0.0) {
            var glass = GlassMap.glassByName(s.get_glass_name());
            if (glass == null) {
                sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                        .max_aperture(ap_radius)
                        .rindex(s.get_refractive_index(), s.get_abbe_vd()));
            }
            else {
                sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                        .max_aperture(ap_radius)
                        .rindex(s.get_refractive_index(), s.get_abbe_vd(), glass.get_name(), glass.get_manufacturer()));
            }
        } else {
            sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                        .max_aperture(ap_radius));
        }
        if (s.is_aspheric()) {
            if (s.is_odd_asphere())
                sm.ifcs.get(sm.cur_surface).profile = new RadialPolynomial().r(s.get_radius_of_curvature()).cc(s.get_cc()).coefs(s.get_aspheric_coeffs());
            else
                sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial().r(s.get_radius_of_curvature()).cc(s.get_cc()).coefs(s.get_aspheric_coeffs());
        }
        if (s.is_aperture_stop()) sm.set_stop();
    }

    public String get_title() {
        return _title;
    }

    public int get_num_configurations() {
        return _configurations != null ? _configurations.length : 0;
    }

    public double get_f_number() {
        return _fno;
    }

    public SurfaceType[] get_surfaces() {
        return _surfaces;
    }

    public boolean has_odd_aspheric() {
        for (var s: _surfaces) {
            if (s.is_odd_asphere())
                return true;
        }
        return false;
    }
    public boolean has_even_a2_aspheric() {
        for (var s: _surfaces) {
            if (s.is_even_a2_asphere())
                return true;
        }
        return false;
    }
    private void add_patent_section(StringBuilder sb) {
        sb.append("[patent info]\n");
        if (_patent_country != null && !_patent_country.isEmpty())
            sb.append("country\t").append(_patent_country).append("\n");
        if (_patent_number != null && !_patent_number.isEmpty())
            sb.append("number\t").append(_patent_number).append("\n");
        if (_patent_example != null && !_patent_example.isEmpty())
            sb.append("example\t").append(_patent_example).append("\n");
        if (_application_year != null && !_application_year.isEmpty())
            sb.append("year applied\t").append(_application_year).append("\n");
        if (_inventors != null && !_inventors.isEmpty())
            sb.append("inventors\t").append(_inventors).append("\n");
        if (_organization != null && !_organization.isEmpty())
            sb.append("current assignee\t").append(_organization).append("\n");
        if (_patent_link != null && !_patent_link.isEmpty())
            sb.append("link\t").append(_patent_link).append("\n");
    }
    private void add_report_section(StringBuilder sb) {
        sb.append("[report data]\n");
        if (_lens_name != null && !_lens_name.isEmpty())
            sb.append("lens name\t").append(_lens_name).append("\n");
        if (_configurations != null) {
            sb.append("scenarios");
            // we change scenarios to be ours
            for (int i = 0; i < _configurations.length; i++) sb.append("\t").append(i);
            sb.append("\n");
            sb.append("names");
            for (var nm: _configuration_names) sb.append("\t").append(nm);
            sb.append("\n");
        }
    }
    public StringBuilder toOptBenchStr(StringBuilder sb) {
        sb.append("[descriptive data]\n");
        sb.append("title\t").append(_title).append("\n");
        sb.append("[constants]\n");
        if (has_odd_aspheric())
            sb.append("AsphericalOddCount\t1\n");
        else if (has_even_a2_aspheric())
            sb.append("AsphericalA2\n");
        sb.append("[variable distances]\n");
        sb.append("Focal Length");
        if (_configurations == null)
            sb.append("\t").append(_focal_length);
        else {
            for (double v : _focal_length_by_scenario) sb.append("\t").append(v);
        }
        sb.append("\n");
        sb.append("Angle of View");
        if (_configurations== null)
            sb.append("\t").append(_angle_of_view_in_degrees);
        else {
            for (double v : _angle_of_views_by_scenario) sb.append("\t").append(v);
        }
        sb.append("\n");
        sb.append("F-Number");
        if (_configurations == null)
            sb.append("\t").append(_fno);
        else {
            for (double v : _f_number_by_scenario) sb.append("\t").append(v);
        }
        sb.append("\n");
        sb.append("Image Height");
        if (_configurations == null)
            sb.append("\t").append(_diameter_image_circle);
        else {
            for (int i = 0; i < _configurations.length; i++)
                sb.append("\t").append(_diameter_image_circle);
        }
        sb.append("\n");
        sb.append("Magnification");
        if (_configurations == null)
            sb.append("\t0");
        else {
            for (int i = 0; i < _configurations.length; i++)
                sb.append("\t0");
        }
        sb.append("\n");
        var last_surf = _surface_list.get(_surface_list.size()-1);
        sb.append("Bf");
        if (_configurations == null)
            sb.append("\t").append(last_surf._thickness);
        else {
            if (last_surf._thickness_by_scenario == null) {
                for (int i = 0; i < _configurations.length; i++)
                    sb.append("\t").append(last_surf._thickness);
            }
            else {
                for (double v: last_surf._thickness_by_scenario)
                    sb.append("\t").append(v);
            }
        }
        sb.append("\n");
        for (int i = 0; i < _surface_list.size(); i++) {
            var surf = _surface_list.get(i);
            if (i < _surface_list.size()-1 && surf._thickness_by_scenario != null) {
                // last surface already dealt with above
                sb.append("d").append(surf._id);
                for (int j = 0; j < surf._thickness_by_scenario.length; j++)
                    sb.append("\t").append(surf._thickness_by_scenario[j]);
                sb.append("\n");
            }
            if (surf._diameter_by_scenario != null && surf._is_aperture_stop) {
                sb.append("Aperture Diameter");
                for (int j = 0; j < surf._diameter_by_scenario.length; j++)
                    sb.append("\t").append(surf._diameter_by_scenario[j]);
                sb.append("\n");
            }
        }
        sb.append("[lens data]\n");
        for (int i = 0; i < _surface_list.size(); i++) {
            var surface = _surface_list.get(i);
            var is_last = i == _surface_list.size()-1;
            surface.toOptBenchStr(sb,is_last);
        }
        sb.append("[aspherical data]\n");
        for (SurfaceType surface : _surface_list) {
            surface.asphericsToOptBenchStr(sb);
        }
        sb.append("[notes]\n");
        sb.append("Generated by Beam42\n");
        //sb.append("angle of view = ").append(fullAngleOfViewDegrees(1.0)).append("\n");
        add_patent_section(sb);
        add_report_section(sb);
        return sb;
    }
    public StringBuilder toMarkdownStr(StringBuilder sb) {
        if (_lens_name != null)
            sb.append("# ").append(_lens_name).append("\n");
        if (_patent_number != null) {
            sb.append("## Patent Information\n");
            sb.append("| Country | Patent Number | Example | Year of Application | Inventors | Organisation | Link |\n");
            sb.append("| ---     | ---           | ---     | ---                 | ---       | ---          | ---  |\n");
            sb.append("|").append(_patent_country).append(" | ")
                    .append(_patent_number).append(" | ")
                    .append(_patent_example).append(" | ")
                    .append(_application_year).append(" | ")
                    .append(_inventors).append(" | ")
                    .append(_organization).append(" | ")
                    .append("[link](").append(_patent_link).append(") |\n");
        }
        boolean sawAsph = false;
        SurfaceType.toMarkdownTableHeader(sb);
        for (SurfaceType surface : _surface_list) {
            surface.toMarkdownTableRow(sb);
            if (surface.is_aspheric())
                sawAsph = true;
        }
        if (sawAsph) {
            var max_coeffs = 0;
            for (SurfaceType surface : _surface_list) {
                if (surface._coeffs != null && surface._coeffs.length > max_coeffs)
                    max_coeffs = surface._coeffs.length;
            }
            SurfaceType.asphericMarkdownTableHeader(sb,max_coeffs);
            for (SurfaceType surface : _surface_list) {
                if (surface.is_aspheric())
                    surface.ashericToMarkdownTableRow(sb,max_coeffs);
            }
        }
        if (get_num_configurations() > 1) {
            sb.append("## Variables\n");
            sb.append("| Variable |");
            for (int i = 0; i < get_num_configurations(); i++) {
                sb.append(" ").append(_configuration_names[i]).append(" |");
            }
            sb.append("\n");
            sb.append("| --- |");
            for (int i = 0; i < get_num_configurations(); i++) {
                sb.append(" --- |");
            }
            sb.append("\n");
            sb.append("| Focal length |");
            for (int i = 0; i < get_num_configurations(); i++) {
                sb.append(_focal_length_by_scenario[i]).append(" |");
            }
            sb.append("\n");
            sb.append("| F-Number |");
            for (int i = 0; i < get_num_configurations(); i++) {
                sb.append(_f_number_by_scenario[i]).append(" |");
            }
            sb.append("\n");
            sb.append("| Angle of View |");
            for (int i = 0; i < get_num_configurations(); i++) {
                sb.append(_angle_of_views_by_scenario[i]).append(" |");
            }
            sb.append("\n");
            for (SurfaceType surface : _surface_list) {
                surface.variablesToMarkdownTableRow(sb);
            }
        }
        return sb;
    }
    public Map<Double,Double> get_wvl_wts() {
        var map = new LinkedHashMap<Double,Double>();
        for (int i = 0; i < _wvls.length; i++)
            map.put(_wvls[i],_wts[i]);
        return map;
    }
    @Override
    public String toString() {
        return toOptBenchStr(new StringBuilder()).toString();
    }
}
