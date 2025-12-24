package org.redukti.tools;

import org.redukti.exporters.ZemaxExporter;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.layout.SystemLayout2D;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.RayTracer;
import org.redukti.mathlib.M;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.plotter.*;
import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.spec.Prescription;
import org.redukti.util.Args;
import org.redukti.util.Helper;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class LensTool2 {

    public static OpticalBenchDataImporter.LensSpecifications getSpecsFromFile(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return specs;
    }

    public static Prescription createPrescription(OpticalBenchDataImporter.LensSpecifications specs, int scenario, boolean use_glass_types, boolean d_line) {
        var p = Prescription.buildPrescription(specs,use_glass_types);
        return p;
    }

    public static OpticalModel createSystem(Prescription prescription,boolean fov_angle,boolean apply_vignetting,boolean use_wideangle_aiming,double[] fields) {
        return prescription.build_ray_optics_model(fov_angle,fields,apply_vignetting,use_wideangle_aiming);
    }

    public static void outputSpotAnalysis(SpotAnalysisResult.SpotResultsByField result, Path output_file, Double radius) throws Exception {
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

    public static void createREADME(String specFile, OpticalBenchDataImporter.LensSpecifications specs, FirstOrderData fod, SpotAnalysisResult spotAnalysisResult, Path output_file) throws Exception {
        Prescription prescription = Prescription.buildPrescription(specs,true);
        StringBuilder sb = prescription.toMarkdownStr(new StringBuilder());
        sb.append("## Layouts\n");
        sb.append("![Layout Only](./layoutonly.svg)\n");
        sb.append("![Layout Field 0.0](./layout.svg)\n");
        sb.append("![Layout Field 0.7](./layout-semi-skew.svg)\n");
        sb.append("![Layout Field 1.0](./layout-skew.svg)\n");
        sb.append("## Spot Diagrams\n");
        sb.append("![Spot Diagram Field 0.0](./spot.svg)\n");
        sb.append("![Spot Diagram Field 0.7](./spot-semi-skew.svg)\n");
        sb.append("![Spot Diagram Field 1.0](./spot-skew.svg)\n");
        sb.append("## Paraxial Parameters\n");
        fodToMarkdown(fod,sb);
        sb.append("## Spot Analysis\n");
        spotResultsMarkdownTable(spotAnalysisResult,sb);
        String filename = Helper.getFilename(specFile);
        String zmxFilename = Helper.replaceExtension(filename, ".zmx");
        sb.append("## Geometric MTF\n");
        sb.append("![Geometrical MTF](./mtf.svg)\n");
        sb.append("* 10,30,50 cycles/mm\n");
        sb.append("* Black lines represent sagittal, blue tangential\n");
        sb.append("## Resources\n");
        sb.append("* [OpticalBench Compatible Data File, tab delimited](./" + filename + ")\n");
        sb.append("* [Zemax file](./" + zmxFilename + ")\n");
        Helper.createOutputFile(output_file,sb.toString());
    }

    private static SpotAnalysisResult generateSpotDiagrams(OpticalModel opm,Args arguments,boolean standardSize) throws Exception {
        var spotAnalysis = SpotAnalysis.eval(opm,new SpotOptions());
        Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"spot-report.txt",arguments.outdir), spotAnalysis.toString());
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            String filename = null;
            if (spotFld.fld.y == 0.0)
                filename = "spot.svg";
            else if (spotFld.fld.y == 0.7)
                filename = "spot-semi-skew.svg";
            else if (spotFld.fld.y == 1.0)
                filename = "spot-skew.svg";
            if (filename == null)
                continue;
            var outfile = Helper.getOutputPath(arguments.specfile, filename, arguments.outdir);
            outputSpotAnalysis(spotFld, outfile,standardSize ? 600. : null);
        }
        return spotAnalysis;
    }

    private static void generateMTFs(OpticalModel opm, Args arguments, double[] fields) throws Exception {
        var spotAnalysis = SpotAnalysis.eval(opm,new SpotOptions().num_rays(64).use_grid(false));
        var mtfs = new ArrayList<PolyMTF>();
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                String filename = "mtf-fld" + i + "-" + (int)intercepts.wvl + ".svg";
                var output_file = Helper.getOutputPath(arguments.specfile,filename,arguments.outdir);
                var mtf = new MonochromaticGeometricMTF(intercepts);
                if (polyMtfForField == null)
                    polyMtfForField = new PolyMTF(mtf.mtf.fft_size,mtf.h2d.pixel_size);
                polyMtfForField.add(mtf.mtf, intercepts.wvl == 587.5618 ? 1.0: 0.5);
                if (arguments.do_mono_chrome_mtfs)
                    Helper.createOutputFile(output_file,new GeoMTFPlot(spotFld.fld,mtf).plot());
            }
            if (polyMtfForField != null) {
                polyMtfForField.compute();
                mtfs.add(polyMtfForField);
            }
        }
        int[] freqs = {10,30,50};
        var mtfResults = new ArrayList<MTFResultByFreq>();
        for (var freq: freqs)
            mtfResults.add(new MTFResultByFreq(mtfs,freq));
        var mtffile = Helper.getOutputPath(arguments.specfile,"mtf.svg",arguments.outdir);
        Helper.createOutputFile(mtffile,new GeoMTFByFieldPlot(mtfResults).plot(fields));
    }

    private static void generateRayAberrationPlots(OpticalModel opm, Args arguments) throws Exception {
        var rayAber = TransverseRayAberrationAnalysis.eval(opm, 21, new TraceOptions());
        for (var fan_results: rayAber.results) {
            String filename = "rayabbr-fld" + fan_results.fi + "-" + (fan_results.xy == 1? "tan" : "sag") + ".svg";
            var output_file = Helper.getOutputPath(arguments.specfile,filename,arguments.outdir);
            Helper.createOutputFile(output_file, new RayAberrationPlot(rayAber).plot(fan_results, 0));
        }
        var opdAber = WavefrontAberrationAnalysis.eval(opm, 21, new TraceOptions());
        for (var fan_results: opdAber.results) {
            String filename = "opdabbr-fld" + fan_results.fi + "-" + (fan_results.xy == 1? "tan" : "sag") + ".svg";
            var output_file = Helper.getOutputPath(arguments.specfile,filename,arguments.outdir);
            Helper.createOutputFile(output_file, new RayAberrationPlot(opdAber).plot(fan_results, 0));
        }
    }

    static class Layout {

        private OpticalSystem createSystem(OpticalBenchDataImporter.LensSpecifications specs, int scenario, boolean use_glass_types, boolean skew_rays, double percent_skew, boolean d_line) {
            OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, scenario, use_glass_types);
            double half_angle_of_view_in_radians = specs.get_half_angle_of_view_in_radians(scenario);
            Vector3 direction = Vector3.vector3_001;
            if (skew_rays) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                half_angle_of_view_in_radians *= percent_skew;
                Matrix3 r = Matrix3.get_rotation_matrix(0, half_angle_of_view_in_radians);
                direction = r.multiply(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d);
            if (!d_line) {
                ps.add_spectral_line(SpectralLine.C)
                        .add_spectral_line(SpectralLine.F);
            }
            systemBuilder.add(ps);
            return systemBuilder.build();
        }

        private void outputLayout(OpticalSystem system, Path output_file) throws Exception {
            // draw 2d system layout
            RendererSvg renderer = new RendererSvg(2400, 1400);
            SystemLayout2D systemLayout2D = new SystemLayout2D();
            systemLayout2D.layout2d(renderer, system);
            if (output_file != null) {
                Helper.createOutputFile(output_file, renderer.write(new StringBuilder()).toString());
            } else {
                System.out.println(renderer.write(new StringBuilder()).toString());
            }
        }

        private void outputLayoutWithRays(OpticalSystem system, Path output_file, int trace_density, boolean dump_system, boolean include_lost_rays) throws Exception {
            // draw 2d system layout
            RendererSvg renderer = new RendererSvg(800, 400);
            SystemLayout2D systemLayout2D = new SystemLayout2D();
            systemLayout2D.layout2d(renderer, system);
            RayTraceParameters parameters = new RayTraceParameters(system);
            RayTracer rayTracer = new RayTracer();
            parameters.set_default_distribution(
                    new Distribution(Pattern.MeridionalDist, trace_density, 0.999));
            if (dump_system) {
                System.out.println(parameters.sequenceToString(new StringBuilder()).toString());
            }
            RayTraceResults result = rayTracer.trace(system, parameters);
            RayTraceRenderer.draw_2d(renderer, result, !include_lost_rays, null);
            if (output_file != null) {
                Helper.createOutputFile(output_file, renderer.write(new StringBuilder()).toString());
            } else {
                System.out.println(renderer.write(new StringBuilder()).toString());
            }
            //result.report();
        }

        public void doLayoutDiagrams(OpticalBenchDataImporter.LensSpecifications specs,Args arguments) throws Exception {
            arguments.include_lost_rays = false;
            OpticalSystem system = createSystem(specs,arguments.scenario,arguments.use_glass_types,false,0,arguments.only_d_line);
            if (arguments.dumpSystem) {
                System.out.println(system);
            }
            OpticalSystem skewedSystem = createSystem(specs,arguments.scenario,arguments.use_glass_types,true,1.0,arguments.only_d_line);
            if (arguments.dumpSystem) {
                System.out.println(skewedSystem);
            }
            OpticalSystem semiSkewedSystem = createSystem(specs,arguments.scenario,arguments.use_glass_types,true,0.7,arguments.only_d_line);
            outputLayout(system,Helper.getOutputPath(arguments.specfile,"layoutonly.svg",arguments.outdir));
            outputLayoutWithRays(system,Helper.getOutputPath(arguments.specfile,"layout.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
            outputLayoutWithRays(semiSkewedSystem,Helper.getOutputPath(arguments.specfile,"layout-semi-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
            outputLayoutWithRays(skewedSystem,Helper.getOutputPath(arguments.specfile,"layout-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
        }
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num] [--dump-system] [--only-d-line] [-o outfilename] [--dont-use-glass-types]");
            System.err.println("       --scenario defaults to 0");
            System.err.println("       Output file will be created in the same location as the specfile");
            System.exit(1);
        }
        try {
            final double[] fields = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

            OpticalBenchDataImporter.LensSpecifications specs = getSpecsFromFile(arguments.specfile);
            var prescription = createPrescription(specs,arguments.scenario,arguments.use_glass_types,arguments.only_d_line);
            System.out.println(prescription.toOptBenchStr(new StringBuilder()).toString());
            var opm = createSystem(prescription,true,true,true,fields);
            var sm = opm.seq_model;
            var osp = opm.optical_spec;
            var fod = opm.optical_spec.parax_data.fod;
            System.out.println(sm.list_surfaces(new StringBuilder()).toString());
            System.out.println(sm.list_gaps(new StringBuilder()).toString());
            System.out.println(osp.list_str(new StringBuilder()).toString());
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"vig.txt",arguments.outdir), osp.list_str(new StringBuilder()).toString());
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"paraxial.txt",arguments.outdir), fod.toString());

            // For layouts we use the old method which isn't very good with
            // wide angle lenses.
            // TODO we need to us rayoptics info re marginal and chief rays and
            // use them in the layout diagrams
            new Layout().doLayoutDiagrams(specs,arguments);

//            StringBuilder buf = new StringBuilder();
//            for (int i = 0; i < fields.length; i++) {
//                Trace.list_ray(buf,osp.fov.fields[i].chief_ray.chief_ray,null,null);
//            }
//            System.out.println(buf.toString());
//            buf = new StringBuilder();
            //System.out.println(Trace.list_ray(buf,Trace.trace_ray(opm, Vector2.vector2_0,osp.fov.fields[4],sm.central_wavelength(),new TraceOptions()).pkg,null,null).toString());

            var spotAnalysis = generateSpotDiagrams(opm,arguments,true);
            generateMTFs(opm,arguments,fields);
            if (arguments.do_ray_aberrations)
                generateRayAberrationPlots(opm,arguments);
            ZemaxExporter zemaxExporter = new ZemaxExporter();
            Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), zemaxExporter.generate(specs, arguments.scenario, arguments.only_d_line));
            createREADME(arguments.specfile,
                    specs,
                    fod,
                    spotAnalysis,
                    Helper.getOutputPath(arguments.specfile,"README.md",arguments.outdir));
        }
        catch (Exception e) {
            System.err.println("Failed due to: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
