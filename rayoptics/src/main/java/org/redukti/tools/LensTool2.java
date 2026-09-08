package org.redukti.tools;

import org.redukti.exporters.ZemaxExporter;
import org.redukti.importers.obench.ObenchFetcher;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.mathlib.M;
import org.redukti.plotter.GeoMTFByFieldPlot;
import org.redukti.plotter.GeoMTFPlot;
import org.redukti.plotter.RayAberrationPlot;
import org.redukti.plotter.SpotDiagram;
import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.layout.Layout2D;
import org.redukti.rayoptics.layout.LayoutOptions;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;
import org.redukti.util.Args;
import org.redukti.util.Helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LensTool2 {

    public static OpticalBenchDataImporter.LensSpecifications getSpecsFromFile(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return specs;
    }

    /**
     * When --patent is used, downloads the prescription from the Optical Bench and
     * saves it next to the other output, so that everything downstream - including
     * the output file names - works exactly as it does for a local specfile, and
     * the source the report was built from is kept alongside the report.
     *
     * @return false if the lens could not be fetched, having already reported why
     */
    private static boolean resolveSpecfile(Args arguments) {
        if (arguments.patent == null)
            return true;
        String url = ObenchFetcher.urlFor(arguments.patent, arguments.example);
        try {
            String content = ObenchFetcher.fetch(arguments.patent, arguments.example);
            Path target = Path.of(arguments.outdir,
                    ObenchFetcher.fileNameFor(arguments.patent, arguments.example));
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.writeString(target, content);
            arguments.specfile = target.toString();
            System.out.println("Fetched " + url);
            System.out.println("Saved " + target);
            return true;
        }
        catch (ObenchFetcher.NotFoundException e) {
            System.err.println(e.getMessage());
            return false;
        }
        catch (Exception e) {
            System.err.println("Could not fetch " + url + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads the prescription, optionally running the glass type matcher over it
     * first. The matched prescription is used for this run only; it replaces the
     * input file just when --update-specfile asks for that.
     */
    private static OpticalBenchDataImporter.LensSpecifications loadSpecs(Args arguments) throws Exception {
        if (!arguments.assign_glass_types)
            return getSpecsFromFile(arguments.specfile);
        Path specpath = Path.of(arguments.specfile);
        var result = GlassFinder.enrich(Files.readString(specpath), arguments.force,
                arguments.index_line_value());
        System.out.printf("Assigned %d glass types; %d ambiguous; %d unmatched%n",
                result.selected(), result.ambiguous(), result.unmatched());
        if (result.ambiguous() > 0)
            System.out.println("Ambiguous surfaces carry candidate= fields; pick one by hand for a better fit");
        if (arguments.update_specfile) {
            Files.writeString(specpath, result.text());
            System.out.println("Updated " + specpath);
        }
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(result.text());
        return specs;
    }
    public static Prescription createPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, boolean d_line) {
        return Prescription.build_prescription(specs, use_glass_types, false, d_line);
    }
    public static Prescription createPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, boolean weighted, boolean d_line) {
        return Prescription.build_prescription(specs, use_glass_types, weighted, d_line);
    }
    public static Prescription createPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, double[] wvls, double[] wts) {
        return Prescription.build_prescription(specs,use_glass_types,wvls,wts);
    }
    public static OpticalModel createSystem(Prescription prescription, boolean fov_angle, VigType vig_type, boolean use_wideangle_aiming, double[] fields, int config) {
        return new RayOpticsModelBuilder(prescription).build_optical_model(fov_angle,fields,false,vig_type,use_wideangle_aiming,config);
    }

    public static void outputSpotAnalysis(SpotAnalysisResult.SpotResultsForField result, Path output_file, Double radius) throws Exception {
        if (output_file != null) {
            Helper.createOutputFile(output_file, new SpotDiagram(result).plot(radius));
        } else {
            System.out.println(new SpotDiagram(result).plot(radius));
        }
    }

    private static DecimalFormat decimalFormat = M.decimal_format();
    public static StringBuilder fodToMarkdown(FirstOrderData fod, StringBuilder sb) {
        sb.append("| parameter | value |\n");
        sb.append("| ---       | ---   |\n");
        return   sb.append("| effective_focal_length |" + decimalFormat.format(fod.efl) +
                "\n| back_focal_length | " + decimalFormat.format(fod.bfl) +
                "\n| optical_invariant | " + decimalFormat.format(fod.opt_inv) +
                "\n| object_distance | " + fod.obj_dist +
                "\n| image_distance | " + decimalFormat.format(fod.img_dist) +
                "\n| power | " + decimalFormat.format(fod.power) +
                "\n| pp1_H | " + decimalFormat.format(fod.pp1) +
                "\n| ppk_H' | " + decimalFormat.format(fod.ppk) +
                "\n| ffl_F | " + decimalFormat.format(fod.ffl) +
                "\n| fno | " + decimalFormat.format(fod.fno) +
                "\n| enp_dist_P | " + decimalFormat.format(fod.enp_dist) +
                "\n| enp_radius | " + decimalFormat.format(fod.enp_radius) +
                "\n| exp_dist_P' | " + decimalFormat.format(fod.exp_dist) +
                "\n| exp_radius | " + decimalFormat.format(fod.exp_radius) +
                "\n| m | " + decimalFormat.format(fod.m) +
                "\n| red | " + fod.red +
                "\n| n_obj | " + decimalFormat.format(fod.n_obj) +
                "\n| n_img | " + decimalFormat.format(fod.n_img) +
                "\n| img_ht | " + decimalFormat.format(fod.img_ht) +
                "\n| obj_ang | " + decimalFormat.format(fod.obj_ang) +
                "\n| obj_na | " + decimalFormat.format(fod.obj_na) +
                "\n| img_na | " + decimalFormat.format(fod.img_na) +
                "|\n");
    }

    public static StringBuilder spotResultsMarkdownTable(SpotAnalysisResult spotAnalysisResult, StringBuilder sb) {
        sb.append("| Field | Spot Mean Radius | Spot Max Radius |\n");
        sb.append("| ---   | ---              | ---             |\n");
        for (var result: spotAnalysisResult.spot_results) {
            sb.append(" | ").append(result.fld)
                    .append(" | ").append(decimalFormat.format(result.get_mean_radius()))
                    .append(" | ").append(decimalFormat.format(result.get_max_radius()))
                    .append("|\n");
        }
        return sb;
    }

    public static StringBuilder startREADME(OpticalBenchDataImporter.LensSpecifications specs) {
        Prescription prescription = Prescription.build_prescription(specs,true);
        StringBuilder sb = prescription.to_markdown_str(new StringBuilder());
        return sb;
    }

    public static StringBuilder addConfigLabelToREADME(StringBuilder sb, String label) {
        if (label != null)
            sb.append("# ").append(label).append("\n");
        return sb;
    }

    public static StringBuilder addLayoutsToREADME(StringBuilder sb, String scenario_filesuffix) {
        sb.append("## Layouts\n");
        sb.append(String.format("![Layout Elements](./layoutonly%s.svg)\n",scenario_filesuffix));
        sb.append(String.format("![Layout](./layout%s.svg)\n",scenario_filesuffix));
        return sb;
    }
    public static StringBuilder addSpotDiagramsToREADME(StringBuilder sb, String scenario_filesuffix) {
        sb.append("## Spot Diagrams\n");
        sb.append(String.format("![Spot Diagram Field 0.0](./spot%s.svg)\n", scenario_filesuffix));
        sb.append(String.format("![Spot Diagram Field 0.7](./spot-semi-skew%s.svg)\n", scenario_filesuffix));
        sb.append(String.format("![Spot Diagram Field 1.0](./spot-skew%s.svg)\n", scenario_filesuffix));
        return sb;
    }

    public static void addFodToREADME(StringBuilder sb, FirstOrderData fod) {
        sb.append("## Paraxial Parameters\n");
        fodToMarkdown(fod,sb);
    }

    public static void addSpotReportToREADME(StringBuilder sb, SpotAnalysisResult spotAnalysisResult) {
        sb.append("## Spot Analysis\n");
        spotResultsMarkdownTable(spotAnalysisResult,sb);
    }

    public static void addMTFsToREADME(StringBuilder sb,  String scenario_filesuffix, int[] mtf_freqs) {
        // describe the frequencies actually plotted, not the default ones
        String freq_legend = GeoMTFByFieldPlot.freq_legend(mtf_freqs);
        sb.append("## Polychromatic Geometric MTF\n");
        sb.append(String.format("![Polychromatic Geometrical MTF](./mtf%s.svg)\n", scenario_filesuffix));
        sb.append(String.format("* %s cycles/mm\n", freq_legend));
        sb.append("* Solid lines represent sagittal, dashed lines tangential\n");
        sb.append("* To generate above, MTFs for wavelengths 587.5618(d), 486.1327(F), 656.2725(C) were calculated across 10 fields, and then averaged\n");
        sb.append("## Polychromatic Geometric MTF (Weighted)\n");
        sb.append(String.format("![Polychromatic Geometrical MTF Weighted](./mtf-w%s.svg)\n", scenario_filesuffix));
        sb.append(String.format("* %s cycles/mm\n", freq_legend));
        sb.append("* Solid lines represent sagittal, dashed lines tangential\n");
        sb.append("* To generate above, MTFs for wavelengths 587.5618(d) wt(1.0), 656.2725(C) wt(0.475), 546.074(e) wt(0.98), 486.1327(F) wt(0.49), 435.8343(g) wt(0.15) were calculated across 10 fields, and then combined using weighted average\n");
    }

    public static void createREADME(StringBuilder sb, String specFile, Path output_file) throws Exception {
        String filename = Helper.getFilename(specFile);
        String zmxFilename = Helper.replaceExtension(filename, ".zmx");
        sb.append("## Resources\n");
        sb.append("* [OpticalBench Compatible Data File, tab delimited](./prescription.txt)\n");
        sb.append("* [Zemax file](./" + zmxFilename + ")\n\n");
        sb.append("Report / Zemax file generated using [Beam42](https://github.com/BeamFour/Beam42) on " + LocalDate.now() + "\n");
        Helper.createOutputFile(output_file,sb.toString());
    }


    private static SpotAnalysisResult generateSpotDiagrams(OpticalModel opm,Args arguments,boolean standardSize, String filename_suffix) throws Exception {
        var spotAnalysis = SpotAnalysis.eval(opm, spotOptions(arguments));
        Helper.createOutputFile(Helper.getOutputFileWithPath(arguments.specfile,suffixed_name("spot-report",filename_suffix,".txt"),arguments.outdir), spotAnalysis.toString());
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            String filename = null;
            if (spotFld.fld.y == 0.0)
                filename = suffixed_name("spot", filename_suffix, ".svg");
            else if (spotFld.fld.y == 0.7)
                filename = suffixed_name("spot-semi-skew", filename_suffix, ".svg");
            else if (spotFld.fld.y == 1.0)
                filename = suffixed_name("spot-skew", filename_suffix, ".svg");
            if (filename == null)
                continue;
            var outfile = Helper.getOutputFileWithPath(arguments.specfile, filename, arguments.outdir);
            outputSpotAnalysis(spotFld, outfile,standardSize ? 600. : null);
        }
        return spotAnalysis;
    }

    private static void generateMTFs(OpticalModel opm, Args arguments, double[] fields, Map<Double,Double> wv_wts, String outname, String filename_suffix) throws Exception {
        var spotAnalysis = SpotAnalysis.eval(opm, spotOptions(arguments));
        var mtfs = new ArrayList<PolyMTF>();
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            var cfg = spotFld.mtfHistogramConfig();
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                String filename = suffixed_name("mtf-fld" + i + "-" + (int)intercepts.wvl, filename_suffix,  ".svg");
                var output_file = Helper.getOutputFileWithPath(arguments.specfile,filename,arguments.outdir);
                var mtf = new MonochromaticGeometricMTF(intercepts, cfg);
                if (polyMtfForField == null)
                    polyMtfForField = new PolyMTF(mtf.mtf.fft_size,mtf.h2d.pixel_size);
                var wt = wv_wts.getOrDefault(intercepts.wvl,0.0);
                if (wt != 0.0)
                    polyMtfForField.add(mtf.mtf, wt);
                if (arguments.do_mono_chrome_mtfs)
                    Helper.createOutputFile(output_file,new GeoMTFPlot(spotFld.fld,mtf).plot());
            }
            if (polyMtfForField != null) {
                polyMtfForField.compute();
                mtfs.add(polyMtfForField);
            }
        }
        int[] freqs = arguments.mtf_freqs;
        var mtfResults = new ArrayList<MTFResultByFreq>();
        for (var freq: freqs)
            mtfResults.add(new MTFResultByFreq(mtfs,freq));
        var mtffile = Helper.getOutputFileWithPath(arguments.specfile,suffixed_name(outname, filename_suffix, ".svg"),arguments.outdir);
        var plot = new GeoMTFByFieldPlot(mtfResults,fields);
        Helper.createOutputFile(mtffile,plot.plot());
        var mtfdata = Helper.getOutputFileWithPath(arguments.specfile,suffixed_name(outname, filename_suffix, ".csv"),arguments.outdir);
        Helper.createOutputFile(mtfdata,plot.toString());
    }

    private static void generateRayAberrationPlots(OpticalModel opm, Args arguments, String filname_suffix) throws Exception {
        var rayAber = TransverseRayAberrationAnalysis.eval(opm, 21, false, new TraceOptions());
        for (var fan_results: rayAber.results) {
            String filename = suffixed_name("rayabbr-fld" + fan_results.fi + "-" + (fan_results.xy == 1? "tan" : "sag"), filname_suffix, ".svg");
            var output_file = Helper.getOutputFileWithPath(arguments.specfile,filename,arguments.outdir);
            Helper.createOutputFile(output_file, new RayAberrationPlot(rayAber).plot(fan_results, 0));
        }
        var opdAber = WavefrontAberrationAnalysis.eval(opm, 21, false, new TraceOptions());
        for (var fan_results: opdAber.results) {
            String filename = suffixed_name("opdabbr-fld" + fan_results.fi + "-" + (fan_results.xy == 1? "tan" : "sag"), filname_suffix, ".svg");
            var output_file = Helper.getOutputFileWithPath(arguments.specfile,filename,arguments.outdir);
            Helper.createOutputFile(output_file, new RayAberrationPlot(opdAber).plot(fan_results, 0));
        }
    }

    private static SpotOptions spotOptions(Args arguments) {
        SpotOptions options = new SpotOptions();
        if (arguments.spot_pattern == SpotOptions.PATTERN_GAUSS_QUADRATURE)
            return options.use_gaussian_quadrature();
        if (arguments.spot_pattern == SpotOptions.PATTERN_GRID)
            return options.use_grid().num_rays(arguments.spot_grid_size);
        return options.use_hexapolar();
    }

    private static OpticalModel createLayoutSystem(
            Prescription prescription,
            int config,
            VigType vigType,
            boolean useWideAngleAiming) {

        double[] layoutFields = {0.0, 1.0};

        return createSystem(
                prescription,
                true,
                vigType,
                useWideAngleAiming,
                layoutFields,
                config);
    }

    public static void doLayoutDiagrams(Prescription prescription,Args arguments, int config, String filename_suffix) throws Exception {
        // First we use rayoptics to get ray starts
        // For very wide angle lenses, blindly spraying rays doesn't work very well
        var opm = createLayoutSystem(prescription,config,VigType.SetPupil,true);
        Layout2D layout = new Layout2D();
        Path output = Helper.getOutputFileWithPath(arguments.specfile,suffixed_name("layout-fan",filename_suffix,".svg"),arguments.outdir);
        String fan = layout.renderSvg(opm, 1000, 500,
                new LayoutOptions().drawReferenceRays(false).fanRayCount(9).clipRays(true).useTraceFan(true));
        Files.writeString(output, fan);
        output = Helper.getOutputFileWithPath(arguments.specfile,suffixed_name("layoutonly",filename_suffix,".svg"),arguments.outdir);
        String elements = layout.renderSvg(opm, 1000, 500,
                new LayoutOptions().drawReferenceRays(false));
        Files.writeString(output, elements);
        String reference = layout.renderSvg(opm, 1000, 500,
                new LayoutOptions());
        output = Helper.getOutputFileWithPath(arguments.specfile,suffixed_name("layout",filename_suffix,".svg"),arguments.outdir);
        Files.writeString(output, reference);
    }

    /**
     * Runs the routine airspace optimization. A prime gets its back focus
     * varied; a zoom gets the other variable airspaces varied, one configuration
     * at a time, because a variable cannot yet be shared across configurations.
     */
    private static void runDefaultOptimizations(Prescription prescription, Args arguments,
                                                VigType vigType) throws Exception {
        int backFocus = DefaultOptimizations.findBackFocusSurface(prescription);
        int configurations = Math.max(prescription.get_num_configurations(), 1);
        boolean zoom = prescription.get_num_configurations() > 1;
        for (int config = 0; config < configurations; config++) {
            int[] surfaces;
            String what;
            if (zoom) {
                surfaces = DefaultOptimizations.findVariableThicknesses(prescription, backFocus);
                what = "variable airspaces";
            }
            else if (backFocus >= 0) {
                surfaces = new int[]{backFocus};
                what = "back focus at surface " + (backFocus + 1);
            }
            else {
                System.out.println("Could not identify a back focus airspace to optimize; skipping");
                return;
            }
            if (surfaces.length == 0) {
                System.out.println("No variable airspaces to optimize; skipping");
                return;
            }
            var objective = arguments.optimize_goal.equals("mtf")
                    ? DefaultOptimizations.Objective.MTF
                    : DefaultOptimizations.Objective.CONTRAST;
            var result = DefaultOptimizations.optimizeThicknesses(prescription, surfaces,
                    arguments.mtf_freqs, config, vigType, arguments.only_d_line, objective);
            System.out.printf("Optimized %s for configuration %d on %s: status %d, merit %.6g -> %.6g%s%n",
                    what, config, arguments.optimize_goal, result.status(), result.before(), result.after(),
                    result.improved() ? "" : " (no improvement)");
        }
    }

    private static String suffixed_name(String baseName, String suffix, String ext) {
        return baseName + suffix + ext;
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.patent != null && arguments.specfile != null) {
            System.err.println("Use either --specfile or --patent, not both");
            System.exit(1);
        }
        if (arguments.patent != null && arguments.example == null) {
            System.err.println("--patent also needs --example, e.g. --patent JP1993-034592 --example 2");
            System.exit(1);
        }
        if (arguments.patent != null && arguments.outdir == null) {
            // A fetched lens has no local file to take its location from, so the
            // caller has to say where the download and the report should land
            // rather than have them appear in whatever the working directory is.
            System.err.println("--patent also needs --outdir, naming the directory to put the lens and its report in");
            System.exit(1);
        }
        if (arguments.specfile == null && arguments.patent == null) {
            System.err.println("Usage: (--specfile inputfile [--outdir dir] | --patent number --example n --outdir dir) \\");
            System.err.println("       [--only-d-line] [--dont-use-glass-types] \\");
            System.err.println("       [--output-ray-aberration-plots] [--output-wavelength-mtfs] [--auto-size-spot-diagrams] \\");
            System.err.println("       [--use-spot-pattern " + Args.spot_pattern_names() + "] [--spot-grid-size count] [--vig-type " + Args.vig_type_names() + "] \\");
            System.err.println("       [--real-ray-aiming|--paraxial-ray-aiming] [--mtf freq,freq,...] \\");
            System.err.println("       [--assign-glass-types [--index-line d|e] [--force] [--update-specfile]] [--optimize [--optimize-goal contrast|mtf]]");
            System.err.println("       --assign-glass-types matches each surface's nd/vd to a catalog glass for this run;");
            System.err.println("         --force re-matches surfaces that already name a glass, --update-specfile writes the result back to the specfile");
            System.err.println("         --index-line e when the prescription quotes the refractive index at the e line rather than the d line");
            System.err.println("       --optimize varies the back focus on a prime, or the other variable airspaces on a zoom, at the central field");
            System.err.println("       --optimize-goal defaults to contrast; mtf uses the geometric MTF directly, which stalls more easily");
            System.err.println("       --mtf takes spatial frequencies in cycles/mm and defaults to 10,30,50, which is what the reports under Examples/ use");
            System.err.println("       --real-ray-aiming aims the chief ray by tracing a real ray at the entrance pupil, --paraxial-ray-aiming uses paraxial aiming; real is the default");
            System.err.println("       --patent fetches the prescription from the PhotonsToPhotos Optical Bench, e.g. --patent JP1993-034592 --example 2 --outdir ef14mm");
            System.err.println("         --outdir is required with --patent: a fetched lens has no local file to take its location from");
            System.err.println("       Output files are created alongside the specfile unless --outdir is given");
            System.exit(1);
        }
        if (!resolveSpecfile(arguments))
            System.exit(1);
        if (arguments.update_specfile && !arguments.assign_glass_types) {
            System.err.println("--update-specfile only applies with --assign-glass-types");
            System.exit(1);
        }
        try {
            long startTime = System.nanoTime();
            final double[] fields = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
            VigType vigType = arguments.vig_type;
            // Real ray aiming is what makes very wide angle lenses trace correctly,
            // so it stays on unless the caller asks for paraxial aiming.
            boolean realRayAiming = arguments.real_ray_aiming == null || arguments.real_ray_aiming;
            OpticalBenchDataImporter.LensSpecifications specs = loadSpecs(arguments);
            var prescription = createPrescription(specs,arguments.use_glass_types,arguments.only_d_line);
            if (arguments.optimize)
                runDefaultOptimizations(prescription, arguments, vigType);
            String prescription_output = prescription.to_opt_bench_str(new StringBuilder()).toString();
            Helper.createOutputFile(Helper.getOutputFileWithPath(arguments.specfile, "prescription.txt", arguments.outdir), prescription_output);
            ZemaxExporter zemaxExporter = new ZemaxExporter();
            Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), zemaxExporter.generate(prescription, arguments.only_d_line));
            StringBuilder SB = startREADME(specs);
            for (int config = 0; config < Math.max(prescription.get_num_configurations(),1); config++) {
                if (prescription.get_num_configurations() > 0)
                    addConfigLabelToREADME(SB,prescription._configuration_names[config]);
                var scenario_filesuffix = prescription.get_num_configurations() > 0 ? ("-"+config) : "";
                var opm = createSystem(prescription, true, vigType, realRayAiming, fields, config);
                var sm = opm.seq_model;
                var osp = opm.optical_spec;
                var fod = opm.optical_spec.parax_data.fod;
                System.out.println(sm.list_surfaces(new StringBuilder()).toString());
                System.out.println(sm.list_gaps(new StringBuilder()).toString());
                System.out.println(osp.list_str(new StringBuilder()).toString());
                Helper.createOutputFile(Helper.getOutputFileWithPath(arguments.specfile, suffixed_name("vig", scenario_filesuffix, ".txt"), arguments.outdir), osp.list_str(new StringBuilder()).toString());
                Helper.createOutputFile(Helper.getOutputFileWithPath(arguments.specfile, suffixed_name("paraxial", scenario_filesuffix, ".txt"), arguments.outdir), fod.toString());
                doLayoutDiagrams(prescription, arguments, config, scenario_filesuffix);

//            StringBuilder buf = new StringBuilder();
//            for (int i = 0; i < fields.length; i++) {
//                Trace.list_ray(buf,osp.fov.fields[i].chief_ray.chief_ray,null,null);
//            }
//            System.out.println(buf.toString());
//            buf = new StringBuilder();
                //System.out.println(Trace.list_ray(buf,Trace.trace_ray(opm, Vector2.vector2_0,osp.fov.fields[4],sm.central_wavelength(),new TraceOptions()).pkg,null,null).toString());

                var spotAnalysis = generateSpotDiagrams(opm, arguments, !arguments.auto_size_spots, scenario_filesuffix);
                addLayoutsToREADME(SB,scenario_filesuffix);
                addSpotDiagramsToREADME(SB,scenario_filesuffix);
                addFodToREADME(SB,fod);
                addSpotReportToREADME(SB,spotAnalysis);
                addMTFsToREADME(SB,scenario_filesuffix,arguments.mtf_freqs);
                generateMTFs(opm, arguments, fields, prescription.get_wvl_wts(), "mtf", scenario_filesuffix);
                if (arguments.do_ray_aberrations)
                    generateRayAberrationPlots(opm, arguments, scenario_filesuffix);
                // Generate MTF with weighted average across wavelengths
                var prescriptionForWeightedMTF = createPrescription(specs, arguments.use_glass_types, true, arguments.only_d_line);
                opm = createSystem(prescriptionForWeightedMTF, true, vigType, realRayAiming, fields, config);
                generateMTFs(opm, arguments, fields, prescriptionForWeightedMTF.get_wvl_wts(), "mtf-w", scenario_filesuffix);
            }
            createREADME(SB,
                    arguments.specfile,
                    Helper.getOutputFileWithPath(arguments.specfile, "README.md", arguments.outdir));
            long finishTime = System.nanoTime();
            System.out.println("Finished in " + TimeUnit.NANOSECONDS.toSeconds(finishTime-startTime) + " secs");
        }
        catch (Exception e) {
            System.err.println("Failed due to: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
