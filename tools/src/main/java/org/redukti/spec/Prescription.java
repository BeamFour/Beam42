package org.redukti.spec;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A format for prescription that is easier to work with when trying to optimize.
 * Supports both single and multiple configurations - a multi configuration setup is
 * required for zoom lenses where thicknesses, focal lengths, diameters, fnumbers can
 * vary by zoom setting.
 * There is always a default scenario configuration.
 * During optimization the default scenario is used so for multi-config prescriptions
 * the default scenario must be set appropriately (to be tested).
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
     * source patent data.
     * Note that these scenario numbers get lost when we write new
     * prescription because the new prescription ends up only with the
     * configured scenarios.
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
    // There are other values such as thickness and aperture
    // that vary for configurations but these are specified for
    // each surface

    // This is used only for layout diagrams - to specify rays
    public Distribution _distribution;   // FIXME rename, used for ray finding only

    /** This was used to find chief ray angle in the older optimj module but will no longer be used */
    @Deprecated
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
    public String _original_assignee = "";
    public String _current_assignee = "";
    public String _patent_link = "";

    public Prescription(double focal_length, double fno, double angle_of_view_degrees, double diameter_image_circle, boolean d_line_only) {
        this(focal_length,fno,angle_of_view_degrees,diameter_image_circle,
            d_line_only ? new double[] {587.5618} : new double[] {587.5618, 486.1327, 656.2725},
            d_line_only ? new double[] {1.0} : new double[] {1.0, 1.0, 1.0});
    }
    public Prescription(double focal_length, double fno, double angle_of_view_degrees, double diameter_image_circle, double[] wvls, double[] wts) {
        this._focal_length = focal_length;
        this._fno = fno;
        this._angle_of_view_in_degrees = angle_of_view_degrees;
        this._diameter_image_circle = diameter_image_circle;
        this._wvls = wvls;
        this._wts = wts;
        this._distribution = new Distribution(Pattern.UserDefined,10, 0.999);
    }
    public Prescription surf(double radius, double thickness, double diameter, double nd, double vd, String glass_name) {
        _surface_list.add(new SurfaceType(Integer.toString(_surface_list.size()+1), false, radius, thickness, diameter, nd, vd, glass_name));
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
            throw new IllegalArgumentException("EVEN aspheres must have 0 as first coefficient");
        else if (asph_type == SurfaceType.ASPH_ODD && (coeffs[0] != 0.0 || coeffs[1] != 0.0))
            throw new IllegalArgumentException("ODD aspheres must have 0 as first and second coefficients");
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
    public double image_diameter_for_field(double field) {
        assert field >= 0 && field <= 1.0;
        return _diameter_image_circle * field;
    }

    /**
     * Full angle of view in degrees for given field for default configuration
     * @param field fields are relative, 0 for axis to 1 for edge.
     */
    public double full_angle_of_view_degrees(double field) {
        if (field == 0.0) return 0.0;
        if (field > 0 && field <= 1.0) {
            var radius = image_diameter_for_field(field)/2.0;
            var radians = Math.atan(radius/ _focal_length);
            return 2.0*Math.toDegrees(radians);
        }
        else throw new IllegalArgumentException("Field must be between 0 and 1.");
    }
    /** Get angle of view for default configuration */
    public double get_half_angle_in_degrees() {
        return _angle_of_view_in_degrees/2.0;
    }
    /** Get angle of view for default configuration */
    public double get_half_angle_of_view_in_radians() {
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

    public static Prescription build_prescription_d_line(OpticalBenchDataImporter.LensSpecifications specs) {
        return build_prescription(specs,true,new double[] {587.5618},new double[] {1.0},0);
    }
    public static Prescription build_prescription_e_line(OpticalBenchDataImporter.LensSpecifications specs) {
        return build_prescription(specs,true,new double[] {546.074},new double[] {1.0},0);
    }
    public static Prescription build_prescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types) {
        return build_prescription(specs,use_glass_types,new double[] {587.5618, 486.1327, 656.2725},new double[] {1.0, 1.0, 1.0},0);
    }
    public static Prescription build_prescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, double[] wvls, double[] wts) {
        return build_prescription(specs,use_glass_types,wvls,wts,0);
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
    public static Prescription build_prescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, double[] wvls, double[] wts, int default_scenario) {
        // We use default values variables that can change in a multi-configuration setup.
        // The defaults are useful as they are the ones that are manipulated during optimization
        var prescription = new Prescription(
                specs.get_focal_length(),
                specs.get_f_number(default_scenario),
                specs.get_angle_of_view_in_degrees(default_scenario),
                specs.get_image_height(),
                wvls,
                wts);
        prescription._title = specs.get_descriptive_data().get_value("title");
        var patent_info_n = specs.get_patent_info();
        if (patent_info_n.count() > 0) {
            // New style
            prescription._patent_country = patent_info_n.get_value("country");
            prescription._patent_number = patent_info_n.get_value("number");
            prescription._patent_example = patent_info_n.get_value("example");
            prescription._application_year = patent_info_n.get_value("year applied");
            prescription._inventors = patent_info_n.get_value("inventors");
            prescription._original_assignee = patent_info_n.get_value("original assignee");
            prescription._current_assignee = patent_info_n.get_value("current assignee");
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
            prescription._current_assignee = patentInfo.get_value(5);
            prescription._original_assignee = patentInfo.get_value(5);
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
        if (_current_assignee != null && !_current_assignee.isEmpty())
            sb.append("current assignee\t").append(_current_assignee).append("\n");
        if (_original_assignee != null && !_original_assignee.isEmpty())
            sb.append("original assignee\t").append(_original_assignee).append("\n");
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
    public StringBuilder to_opt_bench_str(StringBuilder sb) {
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
            surface.to_opt_bench_str(sb,is_last);
        }
        sb.append("[aspherical data]\n");
        for (SurfaceType surface : _surface_list) {
            surface.aspherics_to_opt_bench_str(sb);
        }
        sb.append("[notes]\n");
        sb.append("Generated by Beam42\n");
        //sb.append("angle of view = ").append(fullAngleOfViewDegrees(1.0)).append("\n");
        add_patent_section(sb);
        add_report_section(sb);
        return sb;
    }
    public String organization() {
        if (_original_assignee != null && !_original_assignee.isEmpty())
            return _original_assignee;
        return _current_assignee;
    }
    public StringBuilder to_markdown_str(StringBuilder sb) {
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
                    .append(organization()).append(" | ")
                    .append("[link](").append(_patent_link).append(") |\n");
        }
        boolean sawAsph = false;
        SurfaceType.to_markdown_table_header(sb);
        for (SurfaceType surface : _surface_list) {
            surface.to_markdown_table_row(sb);
            if (surface.is_aspheric())
                sawAsph = true;
        }
        if (sawAsph) {
            var max_coeffs = 0;
            for (SurfaceType surface : _surface_list) {
                if (surface._coeffs != null && surface._coeffs.length > max_coeffs)
                    max_coeffs = surface._coeffs.length;
            }
            SurfaceType.aspheric_markdown_table_header(sb,max_coeffs);
            for (SurfaceType surface : _surface_list) {
                if (surface.is_aspheric())
                    surface.asherics_to_markdown_table_row(sb,max_coeffs);
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
                surface.variables_to_markdown_table_row(sb);
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
        return to_opt_bench_str(new StringBuilder()).toString();
    }
}
