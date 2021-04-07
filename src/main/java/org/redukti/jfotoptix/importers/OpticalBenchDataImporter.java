package org.redukti.jfotoptix.importers;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.material.Air;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Disk;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.jfotoptix.sys.Image;
import org.redukti.jfotoptix.sys.Lens;
import org.redukti.jfotoptix.sys.OpticalSystem;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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

    static final class DescriptiveData {
        String get_title() {
            return title_;
        }

        void set_title(String title) {
            title_ = title;
        }

        String title_;
    }

    static final class Variable {
        Variable(String name) {
            this.name_ = name;
            this.values_ = new ArrayList<>();
        }

        String name() {
            return name_;
        }

        void add_value(String value) {
            values_.add(value);
        }

        int num_scenarios() {
            return values_.size();
        }

        String get_value(int scenario) {
            return values_.get(scenario);
        }

        double get_value_as_double(int scenario) {
            String s = get_value(scenario);
            try {
                return parseDouble(s);
            } catch (Exception e) {
                return 0.0;
            }
        }

        String name_;
        List<String> values_;
    }

    static final class AsphericalData {
        AsphericalData(int surface_number) {
            this.surface_number_ = surface_number;
            this.data_ = new ArrayList<>();
        }

        void add_data(double d) {
            data_.add(d);
        }

        int data_points() {
            return data_.size();
        }

        double data(int i) {
            return i >= 0 && i < data_.size() ? data_.get(i) : 0.0;
        }

        //        void
//        dump (FILE *fp)
//        {
//            fprintf (fp, "Aspheric values[%d] = ", surface_number_);
//            for (int i = 0; i < data_points (); i++)
//            {
//                fprintf (fp, "%.12g ", data (i));
//            }
//            fputc ('\n', fp);
//        }
        int get_surface_number() {
            return surface_number_;
        }

        int surface_number_;
        List<Double> data_;
    }

    enum SurfaceType {
        surface,
        aperture_stop,
        field_stop
    }

    static String SurfaceTypeNames[] = {"S", "AS", "FS"};

    static final class LensSurface {
        LensSurface(int id) {
            id_ = id;
            surface_type_ = SurfaceType.surface;
            radius_ = 0;
            diameter_ = 0;
            refractive_index_ = 0;
            abbe_vd_ = 0;
            is_cover_glass_ = false;
        }

        SurfaceType get_surface_type() {
            return surface_type_;
        }

        void set_surface_type(SurfaceType surface_type) {
            surface_type_ = surface_type;
        }

        double get_radius() {
            return radius_;
        }

        void set_radius(double radius) {
            radius_ = radius;
        }

        double get_thickness(int scenario) {
            if (scenario < thickness_by_scenario_.size())
                return thickness_by_scenario_.get(scenario);
            else {
                assert (1 == thickness_by_scenario_.size());
                return thickness_by_scenario_.get(0);
            }
        }

        void add_thickness(double thickness) {
            thickness_by_scenario_.add(thickness);
        }

        double get_diameter() {
            return diameter_;
        }

        void set_diameter(double value) {
            diameter_ = value;
        }

        double get_refractive_index() {
            return refractive_index_;
        }

        void set_refractive_index(double refractive_index) {
            refractive_index_ = refractive_index;
        }

        double get_abbe_vd() {
            return abbe_vd_;
        }

        void set_abbe_vd(double abbe_vd) {
            abbe_vd_ = abbe_vd;
        }

        AsphericalData get_aspherical_data() {
            return aspherical_data_;
        }

        void set_aspherical_data(AsphericalData aspherical_data) {
            aspherical_data_ = aspherical_data;
        }

        int get_id() {
            return id_;
        }

        boolean is_cover_glass() {
            return is_cover_glass_;
        }

        void  set_is_cover_glass(boolean is_cover_glass) {
            is_cover_glass_ = is_cover_glass;
        }
//        void
//        dump (FILE *fp, unsigned scenario = 0)
//        {
//            fprintf (fp,
//                    "Surface[%d] = type=%s radius=%.12g thickness=%.12g diameter "
//                    "= %.12g nd = %.12g vd = %.12g\n",
//                    id_, SurfaceTypeNames[surface_type_], radius_,
//                    get_thickness (scenario), diameter_, refractive_index_, abbe_vd_);
//        }

        int id_;
        SurfaceType surface_type_;
        double radius_;
        List<Double> thickness_by_scenario_ = new ArrayList<>();
        double diameter_;
        double refractive_index_;
        double abbe_vd_;
        boolean is_cover_glass_;
        AsphericalData aspherical_data_;
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
            int surface_id = 1; // We used to read the id from the OptBench data but
            // this doesn't always work

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
                        surfaces_.add(surface_data);
                    }
                    break;
                    case ASPHERICAL_DATA: {
                        int id = Integer.parseInt(words[0]);
                        AsphericalData aspherical_data = new AsphericalData(id);
                        for (int i = 1; i < words.length; i++) {
                            aspherical_data.add_data(parseDouble(words[i]));
                        }
                        aspherical_data_.add(aspherical_data);
                        LensSurface surface_builder = find_surface(id);
                        if (surface_builder == null) {
//                            fprintf (
//                                    stderr,
//                                    "Ignoring aspherical data as no surface numbered %d\n",
//                                    id);
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

        Variable find_variable(String name) {
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

        //        void
//        dump (FILE *fp = stdout, unsigned scenario = 0)
//        {
//            for (int i = 0; i < surfaces_.size (); i++)
//            {
//                surfaces_.at (i)->dump (fp, scenario);
//                if (surfaces_.at (i)->get_aspherical_data ())
//                {
//                    surfaces_.at (i)->get_aspherical_data ()->dump (fp);
//                }
//            }
//        }
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

        DescriptiveData get_descriptive_data() {
            return descriptive_data_;
        }

        List<Variable> get_variables() {
            return variables_;
        }

        List<LensSurface> get_surfaces() {
            return surfaces_;
        }

        List<AsphericalData> get_aspherical_data() {
            return aspherical_data_;
        }

        DescriptiveData descriptive_data_ = new DescriptiveData();
        List<Variable> variables_ = new ArrayList<>();
        List<LensSurface> surfaces_ = new ArrayList<>();
        List<AsphericalData> aspherical_data_ = new ArrayList<>();
    }

    enum Section {
        DESCRIPTIVE_DATA,
        CONSTANTS,
        VARIABLE_DISTANCES,
        LENS_DATA,
        ASPHERICAL_DATA;
    }

    static final class SectionMapping {
        String name;
        Section section;

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
                       int scenario) {
        double thickness = surface.get_thickness(scenario);
        double radius = surface.get_radius();
        double aperture_radius = surface.get_diameter() / 2.0;
        double refractive_index = surface.get_refractive_index();
        double abbe_vd = surface.get_abbe_vd();
        if (surface.get_surface_type() == SurfaceType.aperture_stop) {
            lens.add_stop(aperture_radius, thickness);
            return thickness;
        }
        AsphericalData aspherical_data = surface.get_aspherical_data();
        if (aspherical_data == null) {
            if (refractive_index != 0.0) {
                if (abbe_vd == 0.0) {
                    //fprintf (stderr, "Abbe vd not specified for surface %d\n",
                    //        surface.get_id ());
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

        if (refractive_index > 0.0) {
            lens.add_surface(
                    new Asphere(radius, k, a4, a6, a8, a10, a12,
                            a14),
                    new Disk(aperture_radius), thickness,
                    new Abbe(Abbe.AbbeFormula.AbbeVd, refractive_index, abbe_vd, 0.0));
        } else {
            lens.add_surface(new Asphere(radius, k, a4, a6,
                            a8, a10, a12, a14),
                    new Disk(aperture_radius),
                    thickness, Air.air);
        }
        return thickness;
    }

    public static OpticalSystem.Builder buildSystem(   LensSpecifications specs, int scenario) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);

        double image_pos = 0.0;
        List<LensSurface> surfaces = specs.get_surfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            double thickness = add_surface(lens, surfaces.get(i), scenario);
            image_pos += thickness;
        }
        // printf ("Image position is at %f\n", image_pos);
        sys.add(lens);

        Image.Builder image = new Image.Builder().position(
                new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001))
                .curve(Flat.flat)
                .shape(new Rectangle(specs.get_image_height() * 2.));
        sys.add(image);

        return sys;
    }

    public static double getAngleOfViewInRadians(   LensSpecifications specs_, int scenario) {
        Variable view_angles = specs_.find_variable("Angle of View");
        return Math.toRadians(view_angles.get_value_as_double(scenario)
                / 2.0);
    }

    public static void main(String[] args) throws Exception {

        LensSpecifications specs = new LensSpecifications();
        specs.parse_file("C:\\work\\github\\goptical\\data\\canon-rf-50mmf1.2\\canon-rf-50mmf1.2.txt");

    }

}
