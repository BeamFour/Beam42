package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.*;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.patterns.PatternGenerator;
import org.redukti.jfotoptix.shape.Round;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Beam42Exporter {
    static final class StringPadding {
        private final String _pad;
        public StringPadding(char c, int padding) {
            char[] spaces = new char[padding];
            Arrays.fill(spaces, c);
            this._pad = new String(spaces);
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
                } else {
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

    static String generate_rays_table(List<Vector3> rays, Vector3 direction) {
        StringBuilder sb = new StringBuilder();
        double wvln = SpectralLine.d / 1000.0;
        sb.append(rays.size()).append(" rays").append(System.lineSeparator());
        generate_heading(sb, ray_columns);
        generate_heading_line(sb, ray_columns);
        for (Vector3 pt : rays) {
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

//    static List<Vector3> generate_circular_starts(double R, double d1, double d2, int ncircles) {
//        List<Vector3> starts = new ArrayList<>();
//        //----next install the rings-------
//        for (int icirc=1; icirc<=ncircles; icirc++)
//        {
//            double daz = 60.0 / icirc;
//            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz;
//            double r = icirc * R / ncircles;
//            for (int jaz = 0; jaz<6*icirc; jaz++)
//            {
//                double a = offset + jaz*daz;
//                double x = d1 + r* U.cosd(a);
//                double y = d2 + r*U.sind(a);
//                starts.add(new Vector3(x, y, 0));
//            }
//        }
//        return starts;
//    }
//
//    static List<Vector3> generate_hexapolar_points2(OpticalSurface surface, int density, double obj_angle) {
//        double tan_angle = obj_angle != 0.0 ? Math.tan(obj_angle) : 1.0;
//        Round shape = (Round) surface.get_shape();
//        List<Vector3> points = generate_circular_starts(shape.get_external_xradius(), 0, 0, 17);
//        List<Vector3> shiftedPoints = new ArrayList<>();
//        for (Vector3 v: points) {
//            double y_ht = v.y(); //  -10 * tan_angle - v.y();
//            shiftedPoints.add(new Vector3(v.x(), y_ht, -10));
//        }
////
////        Consumer<Vector2> f = (v) -> {
////            // Move y to 10 units to left - adjusting height due to angle
////            double y_ht = -10 * tan_angle - v.y();
////            points.add(new Vector3(v.x(), y_ht, -10));
////        };
////        PatternGenerator.get_pattern(shape, f, d, false);
//        return shiftedPoints;
//    }


    // We want to generate rays that hit the first surface in a uniform circular pattern
    // But we approximate the target position to be the Z axis value of first surface.
    // Right now we use 0 for this, but this won't work very well for concave surface! FIXME
    static List<Vector3> generate_hexapolar_points(OpticalSurface surface, int density, double obj_angle) {
        final double z_offset = 10.0;
        Round shape = (Round) surface.get_shape();
        Curve curve = surface.get_curve();
        Distribution d = new Distribution(Pattern.HexaPolarDist, density, 0.999);
        ArrayList<Vector3> points = new ArrayList<>();
        Consumer<Vector2> f = (v) -> {
            double z = curve.sagitta(v);
            double distance = z_offset + z;
            // tan(distance) tells us height of the triangle where tan(angle) = ht/distance.
            double height_adjustment = obj_angle != 0 ? Math.tan(obj_angle) * distance : 1.0;
            // Adjust y
            double y_ht = v.y() - height_adjustment;
            // Adjust z
            points.add(new Vector3(v.x(), y_ht, z-distance));
        };
        PatternGenerator.get_pattern(shape, f, d, false);
        return points;
    }

    static String generate_rays_table(OpticalSurface surface, double obj_angle) {
        Vector3 direction = new Vector3(0, Math.sin(obj_angle), Math.cos(obj_angle));
        List<Vector3> list = generate_hexapolar_points(surface, 30, obj_angle);
        //List<Vector3> list = generate_hexapolar_points2(surface, 10, obj_angle);
        return generate_rays_table(list, direction);
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, arguments.scenario, arguments.use_glass_types);
        OpticalSystem system = systemBuilder.build();
        Beam42Exporter exporter = new Beam42Exporter();

        Helper.createOutputFile(Helper.getOutputPath(arguments, ".OPT"),
                exporter.generate(specs, system, arguments.scenario));
        //System.out.println(exporter.generate(specs, system, arguments.scenario));

        Element firstSurface = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).findFirst().orElse(null);
        OpticalSurface s1 = (OpticalSurface) firstSurface;

        double angleOfView = OpticalBenchDataImporter.get_angle_of_view_in_radians(specs, arguments.scenario);
        Helper.createOutputFile(Helper.getOutputPath(arguments, ".RAY"), generate_rays_table(s1, 0.0));
        Helper.createOutputFile(Helper.getOutputPath(arguments, "-SKEW.RAY"), generate_rays_table(s1, angleOfView));
    }
}
