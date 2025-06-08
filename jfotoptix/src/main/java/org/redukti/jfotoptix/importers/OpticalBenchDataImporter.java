/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar
*/
package org.redukti.jfotoptix.importers;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.Air;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Rectangle;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

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

    public static final class DescriptiveData {
        public String get_title() {
            return _title;
        }

        void set_title(String title) {
            _title = title;
        }

        String _title;
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

        private String _name;
        private List<String> _values;
    }

    public static final class AsphericalData {
        AsphericalData(int surface_number) {
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
            _diameter = 0;
            _refractive_index = 0;
            _abbe_vd = 0;
            _is_cover_glass = false;
            _glass_name = null;
        }

        public SurfaceType get_surface_type() {
            return _surface_type;
        }

        void set_surface_type(SurfaceType surface_type) {
            _surface_type = surface_type;
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

        public double get_diameter() {
            return _diameter;
        }

        void set_diameter(double value) {
            _diameter = value;
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

        private int _id;
        private SurfaceType _surface_type;
        private double _radius;
        private List<Double> _thickness_by_scenario = new ArrayList<>();
        private double _diameter;
        private double _refractive_index;
        private double _abbe_vd;
        private boolean _is_cover_glass;
        private AsphericalData _aspherical_data;
        private String _glass_name;
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
            List<String> lines = Files.readAllLines(new File(file_name).toPath());
            Section current_section = null;         // Current section
            int surface_id = 1; // We use numeric ids
            // OptBen uses string ids, so we need to map from string id to our id
            Map<String,Integer> surfaceIdMap = new HashMap<>();

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
                        if (words.length >= 2 && words[0].equals("title")) {
                            descriptive_data_.set_title(words[1]);
                        }
                        break;
                    case CONSTANTS: {
                            Variable var = new Variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                            constants_.add(var);
                        }
                        break;
                    case VARIABLE_DISTANCES:
                        if (words.length >= 2) {
                            Variable var = new Variable(words[0]);
                            for (int i = 1; i < words.length; i++) {
                                var.add_value(words[i]);
                            }
                            variables_.add(var);
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
                        if (words.length >= 3 && words[2].length() > 0) {
                            parse_thickness(words[2], surface_data);
                        }
                        /* refractive index */
                        if (words.length >= 4 && words[3].length() > 0) {
                            surface_data.set_refractive_index(parseDouble(words[3]));
                        }
                        /* diameter */
                        if (words.length >= 5 && words[4].length() > 0) {
                            surface_data.set_diameter(parseDouble(words[4]));
                        }
                        /* abbe vd */
                        if (words.length >= 6 && words[5].length() > 0) {
                            surface_data.set_abbe_vd(parseDouble(words[5]));
                        }
                        if (words.length >= 7 && words[6].length() > 0) {
                            surface_data.set_glass_name(words[6]);
                        }
                        surfaces_.add(surface_data);
                    }
                    break;
                    case ASPHERICAL_DATA: {
                        String optBenchID = words[0];
                        int id = surfaceIdMap.get(optBenchID);
                        AsphericalData aspherical_data = new AsphericalData(id);
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
            for (int i = 0; i < variables_.size(); i++) {
                if (name.equals(variables_.get(i).name())) {
                    return variables_.get(i);
                }
            }
            return null;
        }

        LensSurface find_surface(int id) {
            for (int i = 0; i < surfaces_.size(); i++) {
                if (surfaces_.get(i).get_id() == id)
                    return surfaces_.get(i);
            }
            return null;
        }

        public boolean has_constant(String c) {
            for (int i = 0; i < constants_.size(); i++) {
                if (c.equals(constants_.get(i).name())) {
                    return true;
                }
            }
            return false;
        }

        double get_image_height() {
            Variable var = find_variable("Image Height");
            if (var != null)
                return var.get_value_as_double(0);
            return 43.2; // Assume 35mm
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
        public DescriptiveData get_descriptive_data() {
            return descriptive_data_;
        }
        List<Variable> get_variables() {
            return variables_;
        }
        public List<LensSurface> get_surfaces() {
            return surfaces_;
        }
        List<AsphericalData> get_aspherical_data() {
            return aspherical_data_;
        }

        private DescriptiveData descriptive_data_ = new DescriptiveData();
        private List<Variable> variables_ = new ArrayList<>();
        private List<LensSurface> surfaces_ = new ArrayList<>();
        private List<AsphericalData> aspherical_data_ = new ArrayList<>();
        private List<Variable> constants_ = new ArrayList<>();
    }

    enum Section {
        DESCRIPTIVE_DATA,
        CONSTANTS,
        VARIABLE_DISTANCES,
        LENS_DATA,
        ASPHERICAL_DATA;
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
            new SectionMapping("[aspherical data]", Section.ASPHERICAL_DATA)
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

    private static double add_surface(Lens.Builder lens, LensSurface surface,
                       int scenario, boolean use_glass_types) {
        double thickness = surface.get_thickness(scenario);
        double radius = surface.get_radius();
        double aperture_radius = surface.get_diameter() / 2.0;
        double refractive_index = surface.get_refractive_index();
        double abbe_vd = surface.get_abbe_vd();
        String glass_name = surface.get_glass_name();
        if (surface.get_surface_type() == SurfaceType.aperture_stop) {
            lens.add_stop(aperture_radius, thickness, true);
            return thickness;
        }
        else if (surface.get_surface_type() == SurfaceType.field_stop) {
            lens.add_stop(aperture_radius, thickness, false);
            return thickness;
        }
        AsphericalData aspherical_data = surface.get_aspherical_data();
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

    public static OpticalSystem.Builder build_system(LensSpecifications specs, int scenario,
        boolean use_glass_types) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<LensSurface> surfaces = specs.get_surfaces();
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
        sys.angle_of_view(get_angle_of_view(specs, scenario));
        sys.f_number(get_f_number(specs, scenario));
        return sys;
    }

    public static double get_angle_of_view_in_radians(LensSpecifications specs_, int scenario) {
        Variable view_angles = specs_.find_variable("Angle of View");
        return Math.toRadians(view_angles.get_value_as_double(scenario)
                / 2.0);
    }

    public static double get_angle_of_view(LensSpecifications specs_, int scenario) {
        Variable view_angles = specs_.find_variable("Angle of View");
        return view_angles.get_value_as_double(scenario)
                / 2.0;
    }

    public static double get_f_number(LensSpecifications specs, int scenario) {
        Variable fnum = specs.find_variable("F-Number");
        return fnum.get_value_as_double(scenario);
    }
}
