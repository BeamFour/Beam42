package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.curve.Asphere;
import org.redukti.jfotoptix.curve.Curve;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.MathUtils;
import org.redukti.jfotoptix.model.*;

import java.text.DecimalFormat;
import java.util.List;
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
            new ColumnDef("Index", 12, MathUtils.decimal_format(6)),
            new ColumnDef("Z", 12, MathUtils.decimal_format(4)),
            new ColumnDef("C", 18, MathUtils.decimal_format(9)),
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

    public static void main(String[] args) throws Exception {
        System.out.println("[" + new StringPadding(10).pad_left("abc") + "]");
        System.out.println("length = " + new StringPadding(10).pad_left("abc").length());
        LensTool.Args arguments = parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num]");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        OpticalSystem system = OpticalBenchDataImporter.build_system(specs, arguments.scenario, arguments.use_glass_types).build();
        Beam42Exporter exporter = new Beam42Exporter();
        System.out.println(exporter.generate(specs, system, arguments.scenario));
    }

}
