package org.redukti.exporters;


import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.SpectralLine;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;
import org.redukti.util.Args;
import org.redukti.util.Helper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

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

    static final ColumnDef[] opt_columns = {
            new ColumnDef("Type", 5, null),
            new ColumnDef("Index", 12, M.decimal_format(8)),
            new ColumnDef("Z", 12, M.decimal_format(6)),
            new ColumnDef("C", 18, M.decimal_format(12)),
            new ColumnDef("Dia", 12, M.decimal_format(4)),
            new ColumnDef("S", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A2", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A4", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A6", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A8", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A10", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A12", 16, M.decimal_format_scientific(10)),
            new ColumnDef("A14", 16, M.decimal_format_scientific(10)),
    };

    final int OPT_Type_col = 0;
    final int OPT_Index_col = 1;
    final int OPT_Z_col = 2;
    final int OPT_C_col = 3;
    final int OPT_Dia_col = 4;
    final int OPT_S_col = 5;
    final int OPT_A2_col = 6;
    final int OPT_A4_col = 7;
    final int OPT_A6_col = 8;
    final int OPT_A8_col = 9;
    final int OPT_A10_col = 10;
    final int OPT_A12_col = 11;
    final int OPT_A14_col = 12;

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

    double get_curvature(SurfaceType surface_type) {
        if (surface_type.get_radius_of_curvature() == 0.0)
            return 0.0;
        else
            return 1.0/surface_type.get_radius_of_curvature();
    }
    double get_diameter(SurfaceType surface_type, int scenario) {
        return surface_type.get_diameter_by_scenario(scenario);
    }
    double get_coeff(SurfaceType surface_type, int i) {
        if (surface_type._coeffs != null && surface_type._coeffs.length > i)
            return surface_type._coeffs[i];
        return 0.0;
    }

    String get_refractive_index_or_glass(SurfaceType surface_type) {
        if (surface_type._glass_name != null)
            return surface_type._glass_name;
        if (surface_type._nd == 0)
            return "1";
        return Double.toString(surface_type._nd);
    }
    String generate(Prescription prescription, int scenario) {
        StringBuilder sb = new StringBuilder();
        double wvln = SpectralLine.d;
        sb.append(prescription._surfaces.length + 1).append(" surfaces").append(System.lineSeparator());
        //sb.append("Type   Index   Z    C    Dia   S    A2   A4   A6    A8    A10   A12   A14  ").append(System.lineSeparator());
        //sb.append("-----:-------:----:----:-----:----:----:-----:-----:-----:-----:-----:-----").append(System.lineSeparator());
        generate_heading(sb, opt_columns);
        generate_heading_line(sb, opt_columns);
        double z = 0;
        // Refractive index is shown on next surface
        // value of 1.0 is for air
        String index = "1";
        for (var e: prescription._surfaces) {
            if (e._is_aperture_stop || e._is_field_stop) {
                sb.append(opt_columns[OPT_Type_col].pad("iris")).append(":")
                        .append(opt_columns[OPT_Index_col].pad(index)).append(":")
                        .append(opt_columns[OPT_Z_col].pad(z)).append(":")
                        .append(opt_columns[OPT_C_col].pad(get_curvature(e))).append(":")
                        .append(opt_columns[OPT_Dia_col].pad(get_diameter(e, scenario))).append(":")
                        .append(opt_columns[OPT_S_col].pad("")).append(":")
                        .append(opt_columns[OPT_A2_col].pad("")).append(":")
                        .append(opt_columns[OPT_A4_col].pad("")).append(":")
                        .append(opt_columns[OPT_A6_col].pad("")).append(":")
                        .append(opt_columns[OPT_A8_col].pad("")).append(":")
                        .append(opt_columns[OPT_A10_col].pad("")).append(":")
                        .append(opt_columns[OPT_A12_col].pad("")).append(":")
                        .append(opt_columns[OPT_A14_col].pad("")).append(": ");
            }
            else {
                sb.append(opt_columns[OPT_Type_col].pad("lens")).append(":")
                        .append(opt_columns[OPT_Index_col].pad(index)).append(":")
                        .append(opt_columns[OPT_Z_col].pad(z)).append(":")
                        .append(opt_columns[OPT_C_col].pad(get_curvature(e))).append(":")
                        .append(opt_columns[OPT_Dia_col].pad(get_diameter(e, scenario))).append(":");
                if (e.is_even_asphere()) {
                    sb.append(opt_columns[OPT_S_col].pad(e.get_cc() + 1.0)).append(":")
                            .append(opt_columns[OPT_A2_col].pad(get_coeff(e, 0))).append(":")
                            .append(opt_columns[OPT_A4_col].pad(get_coeff(e, 1))).append(":")
                            .append(opt_columns[OPT_A6_col].pad(get_coeff(e, 2))).append(":")
                            .append(opt_columns[OPT_A8_col].pad(get_coeff(e, 3))).append(":")
                            .append(opt_columns[OPT_A10_col].pad(get_coeff(e, 4))).append(":")
                            .append(opt_columns[OPT_A12_col].pad(get_coeff(e, 5))).append(":")
                            .append(opt_columns[OPT_A14_col].pad(get_coeff(e, 6))).append(": ");
                } else if (e.is_aspheric()) {
                    throw new UnsupportedOperationException("Only EVEN asphere supported at present");
                } else {
                    sb.append(opt_columns[OPT_S_col].pad("")).append(":")
                            .append(opt_columns[OPT_A2_col].pad("")).append(":")
                            .append(opt_columns[OPT_A4_col].pad("")).append(":")
                            .append(opt_columns[OPT_A6_col].pad("")).append(":")
                            .append(opt_columns[OPT_A8_col].pad("")).append(":")
                            .append(opt_columns[OPT_A10_col].pad("")).append(":")
                            .append(opt_columns[OPT_A12_col].pad("")).append(":")
                            .append(opt_columns[OPT_A14_col].pad("")).append(": ");
                }
            }
            z += e.get_thickness_by_scenario(scenario);
            index = get_refractive_index_or_glass(e);
            sb.append(System.lineSeparator());
        }
        sb.append(opt_columns[OPT_Type_col].pad("film")).append(":")
                        .append(opt_columns[OPT_Index_col].pad("1")).append(":")
                        .append(opt_columns[OPT_Z_col].pad(z)).append(":")
                        .append(opt_columns[OPT_C_col].pad(0)).append(":")
                        .append(opt_columns[OPT_Dia_col].pad(prescription._diameter_image_circle + 8.0)).append(":")
                        .append(opt_columns[OPT_S_col].pad("")).append(":")
                        .append(opt_columns[OPT_A2_col].pad("")).append(":")
                        .append(opt_columns[OPT_A4_col].pad("")).append(":")
                        .append(opt_columns[OPT_A6_col].pad("")).append(":")
                        .append(opt_columns[OPT_A8_col].pad("")).append(":")
                        .append(opt_columns[OPT_A10_col].pad("")).append(":")
                        .append(opt_columns[OPT_A12_col].pad("")).append(":")
                        .append(opt_columns[OPT_A14_col].pad("")).append(": ");
        return sb.toString();
    }

    static final ColumnDef[] ray_columns = {
            new ColumnDef("@wave", 6, null),
            new ColumnDef("X0", 20, M.decimal_format(12)),
            new ColumnDef("Y0", 20, M.decimal_format(12)),
            new ColumnDef("Z0", 20, M.decimal_format(12)),
            new ColumnDef("U0", 20, M.decimal_format(12)),
            new ColumnDef("V0", 20, M.decimal_format(12)),
            new ColumnDef("W0", 20, M.decimal_format(21)),
            new ColumnDef("xfinal", 16, null),
            new ColumnDef("notes", 30, null),
    };

    static final int RAY_wave_col = 0;
    static final int RAY_X0_col = 1;
    static final int RAY_Y0_col = 2;
    static final int RAY_Z0_col = 3;
    static final int RAY_U0_col = 4;
    static final int RAY_V0_col = 5;
    static final int RAY_W0_col = 6;
    static final int RAY_xfinal_col = 7;
    static final int RAY_notes_col = 8;

    record RayStart(double wvl, Vector3 position, Vector3 direction) {}

    static String generate_rays_table(List<RayStart> rayStarts) {
        StringBuilder sb = new StringBuilder();
        sb.append(rayStarts.size()).append(" rays").append(System.lineSeparator());
        generate_heading(sb, ray_columns);
        generate_heading_line(sb, ray_columns);
        for (var rayStart : rayStarts) {
            String indexName = "nd ";
            String color = "g";
            if (rayStart.wvl == SpectralLine.F) {
                indexName = "nF ";
                color = "b";
            }
            else if (rayStart.wvl == SpectralLine.C) {
                indexName = "nC ";
                color = "r";
            }
            sb.append(ray_columns[RAY_wave_col].pad(indexName)).append(color)
                    .append(ray_columns[RAY_X0_col].pad(rayStart.position.x())).append(":")
                    .append(ray_columns[RAY_Y0_col].pad(rayStart.position.y())).append(":")
                    .append(ray_columns[RAY_Z0_col].pad(rayStart.position.z())).append(":")
                    .append(ray_columns[RAY_U0_col].pad(rayStart.direction.x())).append(":")
                    .append(ray_columns[RAY_V0_col].pad(rayStart.direction.y())).append(":")
                    .append(ray_columns[RAY_W0_col].pad(rayStart.direction.z())).append(":")
                    .append(ray_columns[RAY_xfinal_col].pad("")).append(":")
                    .append(ray_columns[RAY_notes_col].pad("")).append(":")
                    .append(" ").append(System.lineSeparator());

        }
        return sb.toString();
    }

    public static final double REFERENCE_RAY_PLANE_CLEARANCE = 10.0;

    static void create_rings(OpticalModel opt_model, Field fld, double wvl,double clearance, List<RayStart> rayStarts) {
        var grid_def = new TraceRingsDef();
        grid_def.num_rings = 5;
        var gridList = Trace.trace_rings(opt_model,grid_def,fld,wvl,0.0,
                    null,false,new TraceOptions());
        for (var gridItem: gridList) {
            RayPkg pkg = gridItem.ray_pkg;
            Vector3 hit = pkg.ray.get(1).p;
            Vector3 direction = pkg.ray.get(0).d.normalize();
            if (direction.z <= 1.0e-14)
                throw new IllegalArgumentException("Ray does not propagate towards positive z");
            Vector3 position = hit.minus(direction.times(clearance / direction.z));
            rayStarts.add(new RayStart(wvl, position, direction));
        }
    }

    static void generate_rays_table(Prescription prescription, int scenario, double[] fields, String[] labels, Args arguments) throws IOException {
        var model = new RayOpticsModelBuilder(prescription).build_optical_model(true,fields,false,VigType.SetPupil,true,scenario);
        var fov = model.optical_spec.fov;
        double[] wavelengths = arguments.only_d_line
                ? new double[]{SpectralLine.d}
                : new double[]{SpectralLine.d, SpectralLine.F, SpectralLine.C};
        for (int i = 0; i < fov.fields.length; i++) {
            var fld = fov.fields[i];
            List<RayStart> rayStarts = new ArrayList<>();
            for (var wvl: wavelengths) {
                create_rings(model,fld,wvl,REFERENCE_RAY_PLANE_CLEARANCE,rayStarts);
            }
            Helper.createOutputFile(Helper.getOutputPath(arguments, labels[i] + ".RAY"), generate_rays_table(rayStarts));
        }
    }

    static final ColumnDef[] med_columns = {
            new ColumnDef("Glass", 15, null),
            new ColumnDef("nd", 10, M.decimal_format(6)),
            new ColumnDef("nC", 10, M.decimal_format(6)),
            new ColumnDef("nF", 10, M.decimal_format(6)),
    };

    static final int MED_Glass_col = 0;
    static final int MED_nd_col = 1;
    static final int MED_nC_col = 2;
    static final int MED_nF_col = 3;

    static Set<Glass> get_glasses(Prescription prescription) {
        Set<Glass> glasses = new HashSet<>();
        for (var e: prescription._surfaces) {
            if (e._glass_name != null) {
                Glass g = Glass.glass_by_name(e._glass_name);
                if (g != null)
                    glasses.add(g);
            }
        }
        return glasses;
    }

    static void generate_med_table(Prescription prescription, Args arguments) throws IOException {
        var glasses = get_glasses(prescription);
        StringBuilder sb = new StringBuilder();
        sb.append(glasses.size()).append(" glasses").append(System.lineSeparator());
        generate_heading(sb, med_columns);
        generate_heading_line(sb, med_columns);
        for (var g: glasses) {
            sb.append(med_columns[MED_Glass_col].pad(g.name())).append(":")
                    .append(med_columns[MED_nd_col].pad(g.nd)).append(":")
                    .append(med_columns[MED_nC_col].pad(g.nC)).append(":")
                    .append(med_columns[MED_nF_col].pad(g.nF)).append(": ")
                    .append(System.lineSeparator());
        }
        Helper.createOutputFile(Helper.getOutputPath(arguments, ".MED"), sb.toString());
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num] --outdir path -o filename");
            System.exit(1);
        }
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(arguments.specfile);
        Prescription prescription = Prescription.build_prescription(specs,true);
        Beam42Exporter exporter = new Beam42Exporter();

        Helper.createOutputFile(Helper.getOutputPath(arguments, ".OPT"), exporter.generate(prescription, arguments.scenario));
        generate_rays_table(prescription,arguments.scenario,new double[] {0.0,1.0},new String[] {"","-SKEW"},arguments);
        generate_med_table(prescription,arguments);
    }
}
