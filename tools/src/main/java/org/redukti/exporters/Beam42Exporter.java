package org.redukti.exporters;


import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.M;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.seq.SequentialModel;
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
            return "1.0";
        return Double.toString(surface_type._nd);
    }
    String generate(Prescription prescription, int scenario) {
        StringBuilder sb = new StringBuilder();
        double wvln = SpectralLine.d;
        sb.append(prescription._surfaces.length).append(" surfaces").append(System.lineSeparator());
        //sb.append("Type   Index   Z    C    Dia   S    A2   A4   A6    A8    A10   A12   A14  ").append(System.lineSeparator());
        //sb.append("-----:-------:----:----:-----:----:----:-----:-----:-----:-----:-----:-----").append(System.lineSeparator());
        generate_heading(sb, opt_columns);
        generate_heading_line(sb, opt_columns);
        double z = 0;
        // Refractive index is shown on next surface
        // value of 1.0 is for air
        String index = "";
        for (var e: prescription._surfaces) {
            if (e._is_aperture_stop || e._is_field_stop) {
                sb.append(opt_columns[Type_col].pad("iris")).append(":")
                        .append(opt_columns[Index_col].pad(index)).append(":")
                        .append(opt_columns[Z_col].pad(z)).append(":")
                        .append(opt_columns[C_col].pad(get_curvature(e))).append(":")
                        .append(opt_columns[Dia_col].pad(get_diameter(e, scenario))).append(":")
                        .append(opt_columns[S_col].pad("")).append(":")
                        .append(opt_columns[A2_col].pad("")).append(":")
                        .append(opt_columns[A4_col].pad("")).append(":")
                        .append(opt_columns[A6_col].pad("")).append(":")
                        .append(opt_columns[A8_col].pad("")).append(":")
                        .append(opt_columns[A10_col].pad("")).append(":")
                        .append(opt_columns[A12_col].pad("")).append(":")
                        .append(opt_columns[A14_col].pad("")).append(": ");
            }
            else {
                sb.append(opt_columns[Type_col].pad("lens")).append(":")
                        .append(opt_columns[Index_col].pad(index)).append(":")
                        .append(opt_columns[Z_col].pad(z)).append(":")
                        .append(opt_columns[C_col].pad(get_curvature(e))).append(":")
                        .append(opt_columns[Dia_col].pad(get_diameter(e, scenario))).append(":");
                if (e.is_even_asphere()) {
                    sb.append(opt_columns[S_col].pad(e.get_cc()+1.0)).append(":")
                            .append(opt_columns[A2_col].pad(get_coeff(e,0))).append(":")
                            .append(opt_columns[A4_col].pad(get_coeff(e,1))).append(":")
                            .append(opt_columns[A6_col].pad(get_coeff(e,2))).append(":")
                            .append(opt_columns[A8_col].pad(get_coeff(e,3))).append(":")
                            .append(opt_columns[A10_col].pad(get_coeff(e,4))).append(":")
                            .append(opt_columns[A12_col].pad(get_coeff(e,5))).append(":")
                            .append(opt_columns[A14_col].pad(get_coeff(e,6))).append(": ");
                } else {
                    sb.append(opt_columns[S_col].pad("")).append(":")
                            .append(opt_columns[A2_col].pad("")).append(":")
                            .append(opt_columns[A4_col].pad("")).append(":")
                            .append(opt_columns[A6_col].pad("")).append(":")
                            .append(opt_columns[A8_col].pad("")).append(":")
                            .append(opt_columns[A10_col].pad("")).append(":")
                            .append(opt_columns[A12_col].pad("")).append(":")
                            .append(opt_columns[A14_col].pad("")).append(": ");
                }
            }
            z += e.get_thickness_by_scenario(scenario);
            index = get_refractive_index_or_glass(e);
            sb.append(System.lineSeparator());
        }
        sb.append(opt_columns[Type_col].pad("film")).append(":")
                        .append(opt_columns[Index_col].pad("")).append(":")
                        .append(opt_columns[Z_col].pad(z)).append(":")
                        .append(opt_columns[C_col].pad(0)).append(":")
                        .append(opt_columns[Dia_col].pad(prescription._diameter_image_circle)).append(":")
                        .append(opt_columns[S_col].pad("")).append(":")
                        .append(opt_columns[A2_col].pad("")).append(":")
                        .append(opt_columns[A4_col].pad("")).append(":")
                        .append(opt_columns[A6_col].pad("")).append(":")
                        .append(opt_columns[A8_col].pad("")).append(":")
                        .append(opt_columns[A10_col].pad("")).append(":")
                        .append(opt_columns[A12_col].pad("")).append(":")
                        .append(opt_columns[A14_col].pad("")).append(": ");
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
//            new ColumnDef("@", 20, MathUtils.decimal_format(10)),
    };

    static final int Ray_wave = 0;
    static final int X0_col = 1;
    static final int Y0_col = 2;
    static final int Z0_col = 3;
    static final int U0_col = 4;
    static final int V0_col = 5;
    static final int W0_col = 6;
    static final int xfinal_col = 7;
    static final int notes_col = 8;

    record RayStart(double wvl, Vector3 position, Vector3 direction) {}

    static String generate_rays_table(List<RayStart> rayStarts) {
        StringBuilder sb = new StringBuilder();
        sb.append(rayStarts.size()).append(" rays").append(System.lineSeparator());
        generate_heading(sb, ray_columns);
        generate_heading_line(sb, ray_columns);
        for (var rayStart : rayStarts) {
            String indexName = "nd ";
            String color = "y";
            if (rayStart.wvl == SpectralLine.F) {
                indexName = "nF ";
                color = "b";
            }
            else if (rayStart.wvl == SpectralLine.C) {
                indexName = "nC ";
                color = "r";
            }
            sb.append(ray_columns[Ray_wave].pad(indexName)).append(color)
                    .append(ray_columns[X0_col].pad(rayStart.position.x())).append(":")
                    .append(ray_columns[Y0_col].pad(rayStart.position.y())).append(":")
                    .append(ray_columns[Z0_col].pad(rayStart.position.z())).append(":")
                    .append(ray_columns[U0_col].pad(rayStart.direction.x())).append(":")
                    .append(ray_columns[V0_col].pad(rayStart.direction.y())).append(":")
                    .append(ray_columns[W0_col].pad(rayStart.direction.z())).append(":")
                    .append(ray_columns[xfinal_col].pad("")).append(":")
                    .append(ray_columns[notes_col].pad("")).append(":")
                    .append(" ").append(System.lineSeparator());

        }
        return sb.toString();
    }

    public static final double REFERENCE_RAY_PLANE_CLEARANCE = 10.0;

    /** Returns the configured chief and pupil-boundary rays on a common plane before surface 1. */
//    public static List<ReferenceRay> reference_rays(OpticalModel model, Field field) {
//        return reference_rays(model, field, model.seq_model.central_wavelength(), REFERENCE_RAY_PLANE_CLEARANCE);
//    }

    /** Positions reference rays on global z = min(surface 1 z) - clearance. */
//    public static List<ReferenceRay> reference_rays(OpticalModel model, Field field,
//                                                                                double wavelength, double clearance) {
//        Objects.requireNonNull(model, "model");
//        Objects.requireNonNull(field, "field");
//        if (!Double.isFinite(clearance) || clearance < 0.0)
//            throw new IllegalArgumentException("clearance must be finite and >= 0");
//        SequentialModel sm = model.seq_model;
//        if (sm.ifcs.size() < 2) throw new IllegalArgumentException("optical model has no first surface");
//        double planeZ = object_side_surface_z(sm) - clearance;
//        TraceOptions options = new TraceOptions();
//        options.rayerr_filter = "full";
//        List<RayPkg> packages = Trace.trace_boundary_rays_at_field(model, field, wavelength, options);
//        List<ReferenceRay> result = new ArrayList<>();
//        for (int i = 0; i < packages.size(); i++) {
//            RayPkg pkg = packages.get(i);
//            if (pkg == null || pkg.ray == null || pkg.ray.size() < 2) continue;
//            String[] labels = model.optical_spec.pupil.ray_labels;
//            String label = i < labels.length ? labels[i] : Integer.toString(i);
//            Vector3 hit = sm.gbl_tfrms.get(1).rt.multiply(pkg.ray.get(1).p).add(sm.gbl_tfrms.get(1).t);
//            Vector3 direction = sm.gbl_tfrms.get(0).rt.multiply(pkg.ray.get(0).d).normalize();
//            if (Math.abs(direction.z) < 1.0e-14)
//                throw new IllegalArgumentException("reference ray is parallel to the start plane: " + label);
//            Vector3 position = hit.add(direction.times((planeZ - hit.z) / direction.z));
//            result.add(new ReferenceRay(label, position, direction));
//        }
//        return List.copyOf(result);
//    }

    public static RayStart chief_ray(OpticalModel model, Field field,
                                     double wavelength, double clearance) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(field, "field");
        if (!Double.isFinite(clearance) || clearance < 0.0)
            throw new IllegalArgumentException("clearance must be finite and >= 0");
        SequentialModel sm = model.seq_model;
        if (sm.ifcs.size() < 2) throw new IllegalArgumentException("optical model has no first surface");
        double planeZ = object_side_surface_z(sm) - clearance;
        TraceOptions options = new TraceOptions();
        options.rayerr_filter = "full";
        ChiefRayPkg crpkg = Trace.trace_chief_ray(model, field, wavelength, 0);
        RayPkg pkg = crpkg.chief_ray;
//        Vector3 hit = sm.gbl_tfrms.get(1).rt.multiply(pkg.ray.get(1).p).add(sm.gbl_tfrms.get(1).t);
//        Vector3 direction = sm.gbl_tfrms.get(0).rt.multiply(pkg.ray.get(0).d).normalize();
//        if (Math.abs(direction.z) < 1.0e-14)
//            throw new IllegalArgumentException("chief ray is parallel to the start plane");
//        Vector3 position = hit.add(direction.times((planeZ - hit.z) / direction.z));
        Vector3 position = pkg.ray.get(1).p;
        Vector3 direction = pkg.ray.get(0).d;
        position = position.minus(direction.times(10));
        return new RayStart(wavelength, position, direction);
    }

    static void create_rings(OpticalModel opt_model, Field fld, double wvl, double clearance, List<RayStart> rayStarts) {
        SequentialModel sm = opt_model.seq_model;
        var grid_def = new TraceRingsDef();
        grid_def.num_rings = 5;
        grid_def.max_radius = 1;
        double planeZ = object_side_surface_z(sm) - clearance;
        var gridList = Trace.trace_rings(opt_model,grid_def,fld,wvl,0.0,
                    null,false,new TraceOptions());
        for (var gridItem: gridList) {
            RayPkg pkg = gridItem.ray_pkg;
//            Vector3 hit = sm.gbl_tfrms.get(1).rt.multiply(pkg.ray.get(1).p).add(sm.gbl_tfrms.get(1).t);
//            Vector3 direction = sm.gbl_tfrms.get(0).rt.multiply(pkg.ray.get(0).d).normalize();
//            if (Math.abs(direction.z) < 1.0e-14)
//                throw new IllegalArgumentException("chief ray is parallel to the start plane");
//            Vector3 position = hit.add(direction.times((planeZ - hit.z) / direction.z));
            Vector3 position = pkg.ray.get(1).p;
            Vector3 direction = pkg.ray.get(0).d;
            position = position.minus(direction.times(10));
            rayStarts.add(new RayStart(wvl, position, direction));
        }
    }

    private static double object_side_surface_z(SequentialModel sm) {
        var surface = sm.ifcs.get(1);
        Tfm3d tfm = sm.gbl_tfrms.get(1);
        double radius = Math.max(0.0, surface.max_aperture);
        double minZ = Double.POSITIVE_INFINITY;
        for (int ri = 0; ri <= 128; ri++) {
            double r = radius * ri / 128.0;
            int count = ri == 0 ? 1 : 256;
            for (int ai = 0; ai < count; ai++) {
                double angle = 2.0 * Math.PI * ai / count;
                double x = r * Math.cos(angle), y = r * Math.sin(angle);
                try {
                    double z = tfm.rt.multiply(new Vector3(x, y, surface.profile.sag(x, y))).add(tfm.t).z;
                    if (Double.isFinite(z)) minZ = Math.min(minZ, z);
                } catch (RuntimeException ignored) { }
            }
        }
        if (!Double.isFinite(minZ))
            throw new IllegalArgumentException("unable to determine the object-side extent of surface 1");
        return minZ;
    }

    static void generate_rays_table(Prescription prescription, int scenario, double[] fields, String[] labels, Args arguments) throws IOException {
        var model = new RayOpticsModelBuilder(prescription).build_optical_model(true,fields,false,VigType.SetPupil,true,scenario);
        var fov = model.optical_spec.fov;
        double[] wavelengths = {SpectralLine.d /*, SpectralLine.F, SpectralLine.C*/};
        for (int i = 0; i < fov.fields.length; i++) {
            var fld = fov.fields[i];
            List<RayStart> rayStarts = new ArrayList<>();
            for (var wvl: wavelengths) {
                var rayStart = chief_ray(model, fld, wvl, REFERENCE_RAY_PLANE_CLEARANCE);
                rayStarts.add(rayStart);
                create_rings(model,fld,wvl,REFERENCE_RAY_PLANE_CLEARANCE,rayStarts);
            }
            //System.out.println(generate_rays_table(rayStarts));
            Helper.createOutputFile(Helper.getOutputPath(arguments, labels[i] + ".RAY"), generate_rays_table(rayStarts));
        }
    }

    static final ColumnDef[] med_columns = {
            new ColumnDef("Glass", 15, null),
            new ColumnDef("nd", 10, M.decimal_format(6)),
            new ColumnDef("nC", 10, M.decimal_format(6)),
            new ColumnDef("nF", 10, M.decimal_format(6)),
    };

    static final int Glass_col = 0;
    static final int nd_col = 1;
    static final int nC_col = 2;
    static final int nF_col = 3;

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
            sb.append(med_columns[Glass_col].pad(g.name())).append(":")
                    .append(med_columns[nd_col].pad(g.nd)).append(":")
                    .append(med_columns[nC_col].pad(g.nC)).append(":")
                    .append(med_columns[nF_col].pad(g.nF)).append(": ")
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

        //System.out.println(exporter.generate(prescription, arguments.scenario));
        Helper.createOutputFile(Helper.getOutputPath(arguments, ".OPT"), exporter.generate(prescription, arguments.scenario));

        generate_rays_table(prescription,arguments.scenario,new double[] {0.0,1.0},new String[] {"","-SKEW"},arguments);

        //System.out.println(exporter.generate(specs, system, arguments.scenario));
        generate_med_table(prescription,arguments);
    }
}
