package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.*;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.patterns.PatternGenerator;
import org.redukti.jfotoptix.shape.Round;
import org.redukti.jfotoptix.tracing.*;
import com.stellarsoftware.beam.core.U;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Beam42Exporter {
    double get_angle_of_view(OpticalBenchDataImporter.LensSpecifications system, int scenario) {
        OpticalBenchDataImporter.Variable view_angles = system.find_variable("Angle of View");
        return view_angles.get_value_as_double(scenario) / 2.0;
    }

    static final class StringPadding {
        private final String _pad;

        public StringPadding(char c, int padding) {
            char[] spaces = new char[padding];
            for (int i = 0; i < spaces.length; i++)
                spaces[i] = c;
            this._pad = new String(spaces);
        }

        public StringPadding(int padding) {
            this(' ', padding);
        }

        public String pad_left(String value) {
            String padded_value = _pad + value;
            return padded_value.substring(padded_value.length() - _pad.length());
        }
    }

    static final class ColumnDef {
        public final String heading;
        public final int width;
        public final DecimalFormat decimal_format;
        public final StringPadding padding;
        public final StringPadding line;

        public ColumnDef(String heading, int width, DecimalFormat decimal_format) {
            this.heading = heading;
            this.width = width;
            this.decimal_format = decimal_format;
            this.padding = new StringPadding(' ', width);
            this.line = new StringPadding('-', width);
        }
        public String pad(String value) {
            return padding.pad_left(value);
        }
        public String pad(double v) {
            return padding.pad_left(decimal_format.format(v));
        }
        public String line() {
            return line.pad_left("");
        }
    }

    static final ColumnDef[] columns = {
            new ColumnDef("Type", 5, null),
            new ColumnDef("Index", 12, MathUtils.decimal_format(8)),
            new ColumnDef("Z", 12, MathUtils.decimal_format(6)),
            new ColumnDef("C", 18, MathUtils.decimal_format(12)),
            new ColumnDef("Dia", 12, MathUtils.decimal_format(4)),
            new ColumnDef("S", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A2", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A4", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A6", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A8", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A10", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A12", 16, MathUtils.decimal_format_scientific(10)),
            new ColumnDef("A14", 16, MathUtils.decimal_format_scientific(10)),
    };

    final int Type_col = 0;
    final int Index_col = 1;
    final int Z_col = 2;
    final int C_col = 3;
    final int Dia_col = 4;
    final int S_col = 5;
    final int A2_col = 6;
    final int A4_col = 7;
    final int A6_col = 8;
    final int A8_col = 9;
    final int A10_col = 10;
    final int A12_col = 11;
    final int A14_col = 12;

    static void generate_heading(StringBuilder sb, ColumnDef[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0)
                sb.append(":");
            sb.append(columns[i].pad(columns[i].heading));
        }
        sb.append(": ").append(System.lineSeparator());
    }
    static void generate_heading_line(StringBuilder sb, ColumnDef[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0)
                sb.append(":");
            sb.append(columns[i].line());
        }
        sb.append(":-").append(System.lineSeparator());
    }
    String generate(OpticalBenchDataImporter.LensSpecifications specs, OpticalSystem system, int scenario) {
        StringBuilder sb = new StringBuilder();
        List<Element> seq = system.get_sequence().stream()
                .filter(e -> !(e instanceof RaySource))
                .collect(Collectors.toList());
        double wvln = SpectralLine.d;
        sb.append(seq.size()).append(" surfaces").append(System.lineSeparator());
        //sb.append("Type   Index   Z    C    Dia   S    A2   A4   A6    A8    A10   A12   A14  ").append(System.lineSeparator());
        //sb.append("-----:-------:----:----:-----:----:----:-----:-----:-----:-----:-----:-----").append(System.lineSeparator());
        generate_heading(sb, columns);
        generate_heading_line(sb, columns);

        for (Element e : seq) {
            if (e instanceof Stop) {
                Stop stop = (Stop) e;
                sb.append(columns[Type_col].pad("iris")).append(":")
                        .append(columns[Index_col].pad(stop.get_material(0).get_refractive_index(wvln))).append(":")
                        .append(columns[Z_col].pad(stop.get_position().z())).append(":")
                        .append(columns[C_col].pad(stop.get_curve().get_curvature())).append(":")
                        .append(columns[Dia_col].pad(stop.get_shape().max_radius() * 2.0)).append(":")
                        .append(columns[S_col].pad("")).append(":")
                        .append(columns[A2_col].pad("")).append(":")
                        .append(columns[A4_col].pad("")).append(":")
                        .append(columns[A6_col].pad("")).append(":")
                        .append(columns[A8_col].pad("")).append(":")
                        .append(columns[A10_col].pad("")).append(":")
                        .append(columns[A12_col].pad("")).append(":")
                        .append(columns[A14_col].pad("")).append(": ");
            } else if (e instanceof OpticalSurface) {
                OpticalSurface surface = (OpticalSurface) e;
                sb.append(columns[Type_col].pad("lens")).append(":")
                        .append(columns[Index_col].pad(surface.get_material(0).get_refractive_index(wvln))).append(":")
                        .append(columns[Z_col].pad(surface.get_position().z())).append(":")
                        .append(columns[C_col].pad(surface.get_curve().get_curvature())).append(":")
                        .append(columns[Dia_col].pad(surface.get_shape().max_radius() * 2.0)).append(":");
                Curve curve = surface.get_curve();
                if (curve instanceof Asphere) {
                    Asphere asphere = (Asphere) curve;
                    sb.append(columns[S_col].pad(asphere.get_k())).append(":")
                            .append(columns[A2_col].pad(asphere.get_A2())).append(":")
                            .append(columns[A4_col].pad(asphere.get_A4())).append(":")
                            .append(columns[A6_col].pad(asphere.get_A6())).append(":")
                            .append(columns[A8_col].pad(asphere.get_A8())).append(":")
                            .append(columns[A10_col].pad(asphere.get_A10())).append(":")
                            .append(columns[A12_col].pad(asphere.get_A12())).append(":")
                            .append(columns[A14_col].pad(asphere.get_A14())).append(": ");
                }
                else {
                        sb.append(columns[S_col].pad("")).append(":")
                            .append(columns[A2_col].pad("")).append(":")
                            .append(columns[A4_col].pad("")).append(":")
                            .append(columns[A6_col].pad("")).append(":")
                            .append(columns[A8_col].pad("")).append(":")
                            .append(columns[A10_col].pad("")).append(":")
                            .append(columns[A12_col].pad("")).append(":")
                            .append(columns[A14_col].pad("")).append(": ");
                }
            } else if (e instanceof Image) {
                Image image = (Image) e;
                sb.append(columns[Type_col].pad("film")).append(":")
                        .append(columns[Index_col].pad(1.0)).append(":")
                        .append(columns[Z_col].pad(image.get_position().z())).append(":")
                        .append(columns[C_col].pad(image.get_curve().get_curvature())).append(":")
                        .append(columns[Dia_col].pad(image.get_shape().max_radius())).append(":")
                        .append(columns[S_col].pad("")).append(":")
                        .append(columns[A2_col].pad("")).append(":")
                        .append(columns[A4_col].pad("")).append(":")
                        .append(columns[A6_col].pad("")).append(":")
                        .append(columns[A8_col].pad("")).append(":")
                        .append(columns[A10_col].pad("")).append(":")
                        .append(columns[A12_col].pad("")).append(":")
                        .append(columns[A14_col].pad("")).append(": ");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    static final class Args {
        int scenario = 0;
        String filename = null;
    }

    static LensTool.Args parseArguments(String[] args) {
        LensTool.Args arguments = new LensTool.Args();
        for (int i = 0; i < args.length; i++) {
            String arg1 = args[i];
            String arg2 = i + 1 < args.length ? args[i + 1] : null;
            if (arg1.equals("--specfile")) {
                arguments.specfile = arg2;
                i++;
            } else if (arg1.equals("--scenario")) {
                arguments.scenario = Integer.parseInt(arg2);
                i++;
            }
        }
        return arguments;
    }

    static final ColumnDef[] ray_columns = {
            new ColumnDef("X0", 20, MathUtils.decimal_format(12)),
            new ColumnDef("Y0", 20, MathUtils.decimal_format(12)),
            new ColumnDef("Z0", 20, MathUtils.decimal_format(12)),
            new ColumnDef("U0", 20, MathUtils.decimal_format(12)),
            new ColumnDef("V0", 20, MathUtils.decimal_format(12)),
            new ColumnDef("W0", 20, MathUtils.decimal_format(21)),
            new ColumnDef("xfinal", 16, null),
            new ColumnDef("notes", 30, null),
//            new ColumnDef("@", 20, MathUtils.decimal_format(10)),
    };

    static final int X0_col = 0;
    static final int Y0_col = 1;
    static final int Z0_col = 2;
    static final int U0_col = 3;
    static final int V0_col = 4;
    static final int W0_col = 5;
    static final int xfinal_col = 6;
    static final int notes_col = 7;
    static final int At_col = 99;


    static String generate_rays_table2(List<TracedRay> rays, Vector3 direction) {
        StringBuilder sb = new StringBuilder();
        double wvln = SpectralLine.d/1000.0;
        sb.append(rays.size()).append(" rays").append(System.lineSeparator());
        generate_heading(sb, ray_columns);
        generate_heading_line(sb, ray_columns);
        for (TracedRay ray: rays) {
            //Vector3 pt = ray.get_intercept_point();
            Vector3 pt = ray.get_position();
            sb.append(ray_columns[X0_col].pad(pt.x())).append(":")
                    .append(ray_columns[Y0_col].pad(pt.y())).append(":")
                    .append(ray_columns[Z0_col].pad(pt.z())).append(":")
                    .append(ray_columns[U0_col].pad(direction.x())).append(":")
                    .append(ray_columns[V0_col].pad(direction.y())).append(":")
                    .append(ray_columns[W0_col].pad(direction.z())).append(":")
//                    .append(ray_columns[At_col].pad(wvln)).append(":")
                    .append(ray_columns[xfinal_col].pad("")).append(":")
                    .append(ray_columns[notes_col].pad("")).append(":")
                    .append(" ").append(System.lineSeparator());

        }
        return sb.toString();
    }

    static List<Vector3> generate_circular_starts(double R, double d1, double d2, int ncircles) {
        List<Vector3> starts = new ArrayList<>();
        //----next install the rings-------
        for (int icirc=1; icirc<=ncircles; icirc++)
        {
            double daz = 60.0 / icirc;
            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz;
            double r = icirc * R / ncircles;
            for (int jaz = 0; jaz<6*icirc; jaz++)
            {
                double a = offset + jaz*daz;
                double x = d1 + r*U.cosd(a);
                double y = d2 + r*U.sind(a);
                starts.add(new Vector3(x, y, 0));
            }
        }
        return starts;
    }

    static String generate_rays_table(List<Vector3> rays, Vector3 direction) {
        StringBuilder sb = new StringBuilder();
        double wvln = SpectralLine.d/1000.0;
        sb.append(rays.size()).append(" rays").append(System.lineSeparator());
        generate_heading(sb, ray_columns);
        generate_heading_line(sb, ray_columns);
        for (Vector3 pt: rays) {
            sb.append(ray_columns[X0_col].pad(pt.x())).append(":")
                    .append(ray_columns[Y0_col].pad(pt.y())).append(":")
                    .append(ray_columns[Z0_col].pad(pt.z())).append(":")
                    .append(ray_columns[U0_col].pad(direction.x())).append(":")
                    .append(ray_columns[V0_col].pad(direction.y())).append(":")
                    .append(ray_columns[W0_col].pad(direction.z())).append(":")
//                    .append(ray_columns[At_col].pad(wvln)).append(":")
                    .append(ray_columns[xfinal_col].pad("")).append(":")
                    .append(ray_columns[notes_col].pad("")).append(":")
                    .append(" ").append(System.lineSeparator());

        }
        return sb.toString();
    }


    private static void get_pattern_meridional(OpticalSurface surface, double obj_angle, Consumer<Vector3> f) {
        Asphere curve = (Asphere) surface.get_curve();
        Round shape = (Round) surface.get_shape();
        final double hr = shape.get_internal_xradius() * (2.0 - 0.999);
        final double tr = shape.get_external_xradius() * 0.999;
        final double epsilon = 1e-8;
        final double bound = epsilon;
        final int density = 10;
        int rdens = (int) Math.floor((double) density
                - (density * (hr / tr)));
        rdens = Math.max(1, rdens);
        final double step = (tr - hr) / rdens;
        final double xyr = 1.0 / shape.get_xy_ratio();

        for (double r = tr; r > bound; r -= step) {
            //double y = r * xyr;
            double y = r;
            double z = curve.sagitta(new Vector2(0, y));
            double distance = 10.0 + z;
            double y_ht = -distance * Math.tan(obj_angle) - y;
            Vector3 pt = new Vector3(0, y_ht, -distance);
            f.accept(pt);
        }
    }

    static List<Vector3> generate_hexapolar_points(OpticalSurface surface, int density, double obj_angle) {
        double tan_angle = Math.tan(obj_angle);
        Round shape = (Round) surface.get_shape();
        Distribution d = new Distribution(Pattern.HexaPolarDist, density, 0.999);
        ArrayList<Vector3> points = new ArrayList<>();
        Consumer<Vector2> f = (v) -> {
            double y_ht = -10 * tan_angle - v.y();
            points.add(new Vector3(v.x(), y_ht, -10));
        };
        PatternGenerator.get_pattern(shape, f, d, false);
        return points;
    }


    static String generate_rays_table(OpticalSurface surface, double obj_angle) {
        Vector3 direction = new Vector3(0, Math.sin(obj_angle), Math.cos(obj_angle));
//        List<Vector3> list = new ArrayList<>();
//        get_pattern_meridional(surface, obj_angle, (v) -> {
//            list.add(v);
//        });
        double R = surface.get_shape().max_radius()*0.999;
        double d1 = 0;
        double d2 = 0;
        //List<Vector3> list = generate_circular_starts(R, d1, d2, 17);
        List<Vector3> list = generate_hexapolar_points(surface, 10, obj_angle);
        return generate_rays_table(list, direction);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[" + new StringPadding(10).pad_left("abc") + "]");
        System.out.println("length = " + new StringPadding(10).pad_left("abc").length());
        LensTool.Args arguments = parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        arguments.only_d_line = true;
        arguments.skewRays = true;

        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, arguments.scenario, arguments.use_glass_types);
        OpticalSystem system = systemBuilder.build();
        Beam42Exporter exporter = new Beam42Exporter();
        System.out.println(exporter.generate(specs, system, arguments.scenario));

        double angleOfView = OpticalBenchDataImporter.get_angle_of_view_in_radians(specs, arguments.scenario);
        Vector3 direction = Vector3.vector3_001;
        if (arguments.skewRays)
        {
            // Construct unit vector at an angle
            //      double z1 = cos (angleOfView);
            //      double y1 = sin (angleOfView);
            //      unit_vector = math::Vector3 (0, y1, z1);
            Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
            direction = r.times(direction);
        }

        System.out.println("DIRECTION " + direction.toString());

//        PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
//                .add_spectral_line(SpectralLine.d);
//        if (!arguments.only_d_line) {
//            ps.add_spectral_line(SpectralLine.C)
//                    .add_spectral_line(SpectralLine.F);
//        }
//        systemBuilder.add(ps);
//        system = systemBuilder.build();
//        RayTraceParameters parameters = new RayTraceParameters(system);
//        RayTracer rayTracer = new RayTracer();
//        parameters.set_default_distribution(
//                new Distribution(Pattern.MeridionalDist, arguments.trace_density, 0.999));
//        if (arguments.dumpSystem) {
//            System.out.println(parameters.sequenceToString(new StringBuilder()).toString());
//        }
//        RayTraceResults result = rayTracer.trace(system, parameters);
//
//        Element firstSurface = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).findFirst().orElse(null);
//        System.out.println(generate_rays_table((OpticalSurface) firstSurface, angleOfView));
//        List<TracedRay> rays_at_first_surface = result.get_intercepted(firstSurface);

        //Element source = system.get_sequence().stream().filter(e -> e instanceof RaySource).findFirst().orElse(null);


        //List<TracedRay> rays_at_source = result.get_generated(source);
        //System.out.println(generate_rays_table(rays_at_source, direction));
//
//        OpticalSurface s1 = (OpticalSurface) firstSurface;
//        Asphere curve = (Asphere) s1.get_curve();
//        double obj_angle = angleOfView;
//        double distance = 10.0 - curve.sagitta(new Vector2(0, 15.0));
//        //distance = 939.811951160431;
//        double y_ht = -distance * Math.tan(obj_angle) - 15.0;
//        Vector3 pt = new Vector3(0, y_ht, distance);
//        Vector3 cosines = new Vector3(0, Math.sin(obj_angle), Math.cos(obj_angle));
//
//        System.out.println(pt.toString() + " " + cosines.toString());

        Element firstSurface = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).findFirst().orElse(null);
        OpticalSurface s1 = (OpticalSurface) firstSurface;
        System.out.println(generate_rays_table(s1, angleOfView));
    }

}
