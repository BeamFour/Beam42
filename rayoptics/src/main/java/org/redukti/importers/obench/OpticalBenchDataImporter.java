/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar
*/
package org.redukti.importers.obench;

import org.redukti.rayoptics.seq.Glass;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Imports optical prescriptions from a file in optical bench
 * (https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm)
 * format. This is tab delimited text file. Many examples can be seen in
 * the Examples/jfotopix folder.
 */
public class OpticalBenchDataImporter {

    static double parseDouble(String s) {
        if (s == null || s.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    static int parseInteger(String s, int defaultValue) {
        if (s == null || s.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static final class VarSet {
        List<Variable> variables_ = new ArrayList<>();
        public Variable add_variable(String name) {
            var v = new Variable(name);
            variables_.add(v);
            return v;
        }
        public Variable find_variable(String name) {
            for (int i = 0; i < variables_.size(); i++) {
                if (name.equals(variables_.get(i).name())) {
                    return variables_.get(i);
                }
            }
            return null;
        }

        /**
         * Shortcut for finding a variable and extracting 1st value from it
         */
        public String get_value(String name) {
            var variable = find_variable(name);
            if (variable != null)
                return variable.get_value(0);
            return "";
        }
        public int count() {
            return variables_.size();
        }
    }

    public static final class Variable {
        Variable(String name) {
            this._name = name;
            this._values = new ArrayList<>();
        }

        String name() {
            return _name;
        }

        void add_value(String value) {
            _values.add(value);
        }

        public int num_scenarios() {
            return _values.size();
        }
        public int num_values() {
            return _values.size();
        }

        public String get_value(int scenario) {
            return _values.get(scenario);
        }

        public double get_value_as_double(int scenario) {
            String s = get_value(scenario);
            try {
                return parseDouble(s);
            } catch (Exception e) {
                return 0.0;
            }
        }
        public int get_value_as_integer(int scenario, int defaultValue) {
            String s = get_value(scenario);
            return parseInteger(s,defaultValue);
        }

        private String _name;
        private List<String> _values;
    }

    public enum AsphereType {
        Even,
        EvenA2,
        Odd
    }

    public static final class AsphericalData {
        AsphericalData(AsphereType asphere_type, int surface_number) {
            this._asphere_type = asphere_type;
            this._surface_number = surface_number;
            this._data = new ArrayList<>();
        }
        void add_data(double d) {
            _data.add(d);
        }

        int data_points() {
            return _data.size();
        }

        public double data(int i) {
            return i >= 0 && i < _data.size() ? _data.get(i) : 0.0;
        }

        int get_surface_number() {
            return _surface_number;
        }

        public AsphereType get_asphere_type() {
            return _asphere_type;
        }

        public boolean is_odd_asphere() {
            return _asphere_type == AsphereType.Odd;
        }

        public double[] get_coeffs() {
            int a = 0;
            if (get_asphere_type() == OpticalBenchDataImporter.AsphereType.Odd)
                a = 2;
            else if (get_asphere_type() == OpticalBenchDataImporter.AsphereType.Even)
                a = 1;
            double[] coeffs = new double[_data.size()-2+a];
            for (int i = 2; i < _data.size(); i++, a++) {
                coeffs[a] = data(i);
            }
            return coeffs;
        }

        public double get_cc() {
            return data(1);
        }
        public double get_r() {
            return data(0);
        }

        private AsphereType _asphere_type;
        private int _surface_number;
        private List<Double> _data;
    }

    public enum SurfaceType {
        surface,
        aperture_stop,
        field_stop
    }

    static String SurfaceTypeNames[] = {"S", "AS", "FS"};

    public static final class LensSurface {
        LensSurface(int id) {
            _id = id;
            _surface_type = SurfaceType.surface;
            _radius = 0;
            _refractive_index = 0;
            _abbe_vd = 0;
            _is_cover_glass = false;
            _glass_name = null;
            _catalog_name = null;
        }

        public SurfaceType get_surface_type() {
            return _surface_type;
        }

        void set_surface_type(SurfaceType surface_type) {
            _surface_type = surface_type;
        }

        public boolean is_aperture_stop() {
            return _surface_type == SurfaceType.aperture_stop;
        }

        public double get_radius() {
            return _radius;
        }

        void set_radius(double radius) {
            _radius = radius;
        }

        public double get_thickness(int scenario) {
            if (scenario < _thickness_by_scenario.size())
                return _thickness_by_scenario.get(scenario);
            else {
                assert (1 == _thickness_by_scenario.size());
                return _thickness_by_scenario.get(0);
            }
        }

        void add_thickness(double thickness) {
            _thickness_by_scenario.add(thickness);
        }

        public double get_diameter(int scenario) {
            if (scenario < _diameter_by_scenario.size())
                return _diameter_by_scenario.get(scenario);
            else {
                assert (1 == _diameter_by_scenario.size());
                return _diameter_by_scenario.get(0);
            }
        }

        void set_diameter(double value) {
            _diameter_by_scenario.add(value);
        }

        public double get_refractive_index() {
            return _refractive_index;
        }

        void set_refractive_index(double refractive_index) {
            _refractive_index = refractive_index;
        }

        public double get_abbe_vd() {
            return _abbe_vd;
        }

        void set_abbe_vd(double abbe_vd) {
            _abbe_vd = abbe_vd;
        }

        public AsphericalData get_aspherical_data() {
            return _aspherical_data;
        }

        void set_aspherical_data(AsphericalData aspherical_data) {
            _aspherical_data = aspherical_data;
        }

        int get_id() {
            return _id;
        }

        boolean is_cover_glass() {
            return _is_cover_glass;
        }

        void  set_is_cover_glass(boolean is_cover_glass) {
            _is_cover_glass = is_cover_glass;
        }

        void set_glass_name(String name) { _glass_name = name; }

        public String get_glass_name() { return _glass_name; }

        void set_catalog_name(String name) { _catalog_name = name; }

        public String get_catalog_name() { return _catalog_name; }

        public List<Double> get_thickness_by_scenario() {
            return _thickness_by_scenario;
        }

        public List<Double> get_diameter_by_scenario() {
            return _diameter_by_scenario;
        }

        private int _id;
        private SurfaceType _surface_type;
        private double _radius;
        private List<Double> _thickness_by_scenario = new ArrayList<>();
        private List<Double> _diameter_by_scenario = new ArrayList<>();
        private double _refractive_index;
        private double _abbe_vd;
        private boolean _is_cover_glass;
        private AsphericalData _aspherical_data;
        private String _glass_name;
        private String _catalog_name;
    }

    public static final class LensSpecifications {

        String[] splitLine(String line) {
            List<String> words = new ArrayList<>();
            while (line.length() > 0) {
                int pos = line.indexOf('\t');
                if (pos < 0) {
                    words.add(line);
                    break;
                } else if (pos == 0) {
                    words.add("");
                    line = line.substring(1);
                } else {
                    words.add(line.substring(0, pos));
                    line = line.substring(pos + 1);
                }
            }
            return words.toArray(new String[words.size()]);
        }
        public boolean parse_file(String file_name) throws Exception {
            var lines = Files.readAllLines(new File(file_name).toPath()).toArray(new String[0]);
            return parse_lines(lines);
        }
        public boolean parse_buffer(String buffer) throws Exception {
            String[] lines = buffer.split("\\r?\\n");
            return parse_lines(lines);
        }
        public boolean parse_lines(String[] lines) throws Exception {
            Section current_section = null;         // Current section
            int surface_id = 1; // We use numeric ids
            // OptBen uses string ids, so we need to map from string id to our id
            Map<String,Integer> surfaceIdMap = new HashMap<>();
            AsphereType asphere_type = AsphereType.Even;

            for (String line : lines) {
                String[] words = splitLine(line);
                if (words.length == 0) {
                    continue;
                }
                if (words[0].startsWith("#")) {
                    // comment
                    continue;
                }
                if (words[0].startsWith("[")) {
                    // section name
                    current_section = find_section(words[0]);
                    continue;
                }
                if (current_section == null) {
                    continue;
                }

                switch (current_section) {
                    case DESCRIPTIVE_DATA:
                        if (words.length >= 2) {
                            Variable var = descriptive_data_.add_variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                        }
                        break;
                    case CONSTANTS: {
                            Variable var = constants_.add_variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                        }
                        break;
                    case VARIABLE_DISTANCES:
                        if (words.length >= 2) {
                            Variable var = variables_.add_variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                        }
                        break;
                    case PATENT_INFO:
                        if (words.length >= 2) {
                            Variable var = patent_info_.add_variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                        }
                        break;
                    case REPORT_DATA:
                        if (words.length >= 2) {
                            Variable var = report_data_.add_variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                        }
                        break;
                    case LENS_DATA: {
                        if (words.length < 2)
                            break;
                        int id = surface_id++;
                        surfaceIdMap.put(words[0], id); // Map OptBench ID to our ID
                        LensSurface surface_data = new LensSurface(id);
                        SurfaceType type = SurfaceType.surface;
                        /* radius */
                        if (words[1].equals("AS")) {
                            type = SurfaceType.aperture_stop;
                            surface_data.set_radius(0.0);
                        } else if (words[1].equals("FS")) {
                            type = SurfaceType.field_stop;
                            surface_data.set_radius(0.0);
                        } else if (words[1].equals("CG")) {
                            surface_data.set_radius(0.0);
                            surface_data.set_is_cover_glass(true);
                        } else {
                            if (words[1].equals("Infinity"))
                                surface_data.set_radius(0.0);
                            else
                                surface_data.set_radius(parseDouble(words[1]));
                        }
                        surface_data.set_surface_type(type);
                        /* thickness */
                        if (words.length >= 3 && !words[2].isEmpty()) {
                            parse_thickness(words[2], surface_data);
                        }
                        /* refractive index */
                        if (words.length >= 4 && !words[3].isEmpty()) {
                            surface_data.set_refractive_index(parseDouble(words[3]));
                        }
                        /* diameter */
                        if (words.length >= 5 && !words[4].isEmpty()) {
                            parse_diameter(words[4], type == SurfaceType.aperture_stop, surface_data);
                        }
                        /* abbe vd */
                        if (words.length >= 6 && !words[5].isEmpty()) {
                            surface_data.set_abbe_vd(parseDouble(words[5]));
                        }
                        if (words.length >= 7 && !words[6].isEmpty()) {
                            surface_data.set_glass_name(words[6]);
                        }
                        if (words.length >= 8 && !words[7].isEmpty()) {
                            String catalog_name = Glass.get_catalog_name(words[7]);
                            surface_data.set_catalog_name(catalog_name);
                        }
                        surfaces_.add(surface_data);
                    }
                    break;
                    case ASPHERICAL_DATA: {
                        if (has_constant("AsphericalOddCount"))
                            asphere_type = AsphereType.Odd;
                        else if (has_constant("AsphericalA2"))
                            asphere_type = AsphereType.EvenA2;
                        else
                            asphere_type = AsphereType.Even;
                        String optBenchID = words[0];
                        int id = surfaceIdMap.get(optBenchID);
                        AsphericalData aspherical_data = new AsphericalData(asphere_type,id);
                        for (int i = 1; i < words.length; i++) {
                            aspherical_data.add_data(parseDouble(words[i]));
                        }
                        aspherical_data_.add(aspherical_data);
                        LensSurface surface_builder = find_surface(id);
                        if (surface_builder == null) {
                            throw new RuntimeException("Unknown surface " + optBenchID);
                        } else {
                            surface_builder.set_aspherical_data(aspherical_data);
                        }
                    }
                    break;
                    default:
                        break;
                }
            }
            return true;
        }

        public Variable find_variable(String name) {
            return variables_.find_variable(name);
        }

        LensSurface find_surface(int id) {
            for (int i = 0; i < surfaces_.size(); i++) {
                if (surfaces_.get(i).get_id() == id)
                    return surfaces_.get(i);
            }
            return null;
        }

        public boolean has_constant(String c) {
            return constants_.find_variable(c) != null;
        }

        public double get_image_height() {
            Variable var = find_variable("Image Height");
            if (var != null)
                return var.get_value_as_double(0);
            return 43.2; // Assume 35mm
        }

        public double get_focal_length() {
            Variable var = find_variable("Focal Length");
            if (var != null)
                return var.get_value_as_double(0);
            throw new IllegalArgumentException();
        }

        public double get_focal_length(int scenario) {
            Variable var = find_variable("Focal Length");
            return var.get_value_as_double(scenario);
        }

        public double get_stop_diameter(int scenario) {
            Variable view_angles = find_variable("Aperture Diameter");
            return view_angles.get_value_as_double(scenario);
        }

        public double get_angle_of_view_in_degrees(int scenario) {
            Variable view_angles = find_variable("Angle of View");
            return view_angles.get_value_as_double(scenario);
        }

        public double get_f_number(int scenario) {
            Variable fnum = find_variable("F-Number");
            return fnum.get_value_as_double(scenario);
        }

        public double get_half_angle_of_view_in_radians(int scenario) {
            Variable view_angles = find_variable("Angle of View");
            return Math.toRadians(view_angles.get_value_as_double(scenario)
                    / 2.0);
        }

        public double get_half_angle_of_view_in_degrees(int scenario) {
            Variable view_angles = find_variable("Angle of View");
            return view_angles.get_value_as_double(scenario)
                    / 2.0;
        }

        void parse_thickness(String value,
                        LensSurface surface_builder) {
            if (value.length() == 0) {
                surface_builder.add_thickness(0.0);
                return;
            }
            if (Character.isAlphabetic(value.charAt(0))) {
                Variable var = find_variable(value);
                if (var != null) {
                    for (int i = 0; i < var.num_scenarios(); i++) {
                        String s = var.get_value(i);
                        double d = parseDouble(s);
                        surface_builder.add_thickness(d);
                    }
                } else {
                    //fprintf (stderr, "Variable %s was not found\n", value);
                    surface_builder.add_thickness(0.0);
                }
            } else {
                surface_builder.add_thickness(parseDouble(value));
            }
        }
        void parse_diameter(String value,
                        boolean isApertureStop,
                        LensSurface surface_builder) {
            double dValue = parseDouble(value);
            if (!isApertureStop) {
                surface_builder.set_diameter(dValue);
            }
            else {
                Variable var = find_variable("Aperture Diameter");
                if (var != null) {
                    for (int i = 0; i < var.num_scenarios(); i++) {
                        String s = var.get_value(i);
                        double d = parseDouble(s);
                        surface_builder.set_diameter(d);
                    }
                }
                else {
                    surface_builder.set_diameter(dValue);
                }
            }
        }

        public VarSet get_descriptive_data() {
            return descriptive_data_;
        }
        public List<LensSurface> get_surfaces() {
            return surfaces_;
        }
        List<AsphericalData> get_aspherical_data() {
            return aspherical_data_;
        }
        public VarSet get_patent_info() {
            return patent_info_;
        }
        public VarSet get_report_data() {
            return report_data_;
        }

        private final VarSet descriptive_data_ = new VarSet();
        private final VarSet variables_ = new VarSet();
        private final List<LensSurface> surfaces_ = new ArrayList<>();
        private final List<AsphericalData> aspherical_data_ = new ArrayList<>();
        private final VarSet constants_ = new VarSet();
        private final VarSet patent_info_ = new VarSet();
        private final VarSet report_data_ = new VarSet();
    }

    enum Section {
        DESCRIPTIVE_DATA,
        CONSTANTS,
        VARIABLE_DISTANCES,
        LENS_DATA,
        ASPHERICAL_DATA,
        PATENT_INFO,
        REPORT_DATA;
    }

    static final class SectionMapping {
        final String name;
        final Section section;

        public SectionMapping(String name, Section section) {
            this.name = name;
            this.section = section;
        }
    }

    static SectionMapping g_SectionMappings[] = new SectionMapping[]{
            new SectionMapping("[descriptive data]", Section.DESCRIPTIVE_DATA),
            new SectionMapping("[constants]", Section.CONSTANTS),
            new SectionMapping("[variable distances]", Section.VARIABLE_DISTANCES),
            new SectionMapping("[lens data]", Section.LENS_DATA),
            new SectionMapping("[aspherical data]", Section.ASPHERICAL_DATA),
            new SectionMapping("[patent info]", Section.PATENT_INFO),
            new SectionMapping("[report data]", Section.REPORT_DATA),
    };

    static Section find_section(String name) {
        Section section = null;
        for (int i = 0; i < g_SectionMappings.length; i++) {
            if (g_SectionMappings[i].name.equals(name)) {
                section = g_SectionMappings[i].section;
                break;
            }
        }
        return section;
    }
}
