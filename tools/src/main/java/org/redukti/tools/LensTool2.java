package org.redukti.tools;

import org.redukti.exporters.ZemaxExporter;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.mathlib.M;
import org.redukti.plotter.RayAberrationPlot;
import org.redukti.plotter.SpotDiagram;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotAnalysisResult;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.parax.FirstOrderData;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.util.Args;
import org.redukti.util.Helper;

import java.nio.file.Path;
import java.text.DecimalFormat;

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

    public static OpticalModel createSystem(Prescription prescription,boolean fov_angle) {
        return prescription.build_rayoptic_model(fov_angle,null,true);
    }

    public static void outputSpotAnalysis(SpotAnalysisResult.SpotResultsByField result, Path output_file) throws Exception {
        if (output_file != null) {
            Helper.createOutputFile(output_file, new SpotDiagram(result).plot());
        } else {
            System.out.println(new SpotDiagram(result).plot());
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


    public static void createREADME(String specFile, OpticalBenchDataImporter.LensSpecifications specs, FirstOrderData fod, double[] fields, SpotAnalysisResult spotAnalysisResult, Path output_file) throws Exception {
        Prescription prescription = Prescription.buildPrescription(specs,true);
        StringBuilder sb = prescription.toMarkdownStr(new StringBuilder());
        sb.append("## Layouts\n");
        sb.append("![Layout Only](./layout.svg)\n");
        sb.append("## Spot Diagrams\n");
        sb.append("![Spot Diagram Field 0.0](./spot.svg)\n");
        sb.append("![Spot Diagram Field 0.7](./spot-semi-skew.svg)\n");
        sb.append("![Spot Diagram Field 1.0](./spot-skew.svg)\n");
        sb.append("## Paraxial Parameters\n");
        fodToMarkdown(fod,sb);
        sb.append("## Spot Analysis\n");
        spotAnalysisResult.toMarkdownTable(sb);
        String filename = Helper.getFilename(specFile);
        String zmxFilename = Helper.replaceExtension(filename, ".zmx");
        sb.append("## Ray Aberrations\n");
        sb.append("### Field 0.0\n");
        sb.append("![Tangential Ray Aberrations 0.0](./rayabbr-fld0-tan.svg)\n");
        sb.append("![Sagittal Ray Aberrations 0.0](./rayabbr-fld0-sag.svg)\n");
        sb.append("### Field 0.7\n");
        sb.append("![Tangential Ray Aberrations 0.0](./rayabbr-fld1-tan.svg)\n");
        sb.append("![Sagittal Ray Aberrations 0.0](./rayabbr-fld1-sag.svg)\n");
        sb.append("### Field 1.0\n");
        sb.append("![Tangential Ray Aberrations 0.0](./rayabbr-fld2-tan.svg)\n");
        sb.append("![Sagittal Ray Aberrations 0.0](./rayabbr-fld2-sag.svg)\n");
        sb.append("## Resources\n");
        sb.append("* [OpticalBench Compatible Data File, tab delimited](./" + filename + ")\n");
        sb.append("* [Zemax file](./" + zmxFilename + ")\n");
        Helper.createOutputFile(output_file,sb.toString());
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
            OpticalBenchDataImporter.LensSpecifications specs = getSpecsFromFile(arguments.specfile);
            var prescription = createPrescription(specs,arguments.scenario,arguments.use_glass_types,arguments.only_d_line);
            var opm = createSystem(prescription,true);
            var sm = opm.seq_model;
            var osp = opm.optical_spec;
            var fod = opm.optical_spec.parax_data.fod;
            System.out.println(sm.list_surfaces(new StringBuilder()).toString());
            System.out.println(sm.list_gaps(new StringBuilder()).toString());
            System.out.println(osp.list_str(new StringBuilder()).toString());
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"paraxial.txt",arguments.outdir), fod.toString());
            // can't do layout diagrams yet
//            outputLayout(system,Helper.getOutputPath(arguments.specfile,"layoutonly.svg",arguments.outdir));
//            outputLayoutWithRays(system,Helper.getOutputPath(arguments.specfile,"layout.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
//            outputLayoutWithRays(semiSkewedSystem,Helper.getOutputPath(arguments.specfile,"layout-semi-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
//            outputLayoutWithRays(skewedSystem,Helper.getOutputPath(arguments.specfile,"layout-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);

            var spotAnalysis = SpotAnalysis.eval(opm,21, new TraceOptions());
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"spot-report.txt",arguments.outdir), spotAnalysis.toString());
            String[] filenames = {"spot.svg", "spot-semi-skew.svg", "spot-skew.svg"};
            for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
                var spotFld = spotAnalysis.spot_results.get(i);
                var outfile = Helper.getOutputPath(arguments.specfile,filenames[i],arguments.outdir);
                outputSpotAnalysis(spotFld,outfile);
            }

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

            ZemaxExporter zemaxExporter = new ZemaxExporter();
            Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), zemaxExporter.generate(specs, arguments.scenario, arguments.only_d_line));
            createREADME(arguments.specfile,
                    specs,
                    fod,
                    new double[] {0.0, 0.7, 1.0},
                    spotAnalysis,
                    Helper.getOutputPath(arguments.specfile,"README.md",arguments.outdir));
        }
        catch (Exception e) {
            System.err.println("Failed due to: " + e.getMessage());
        }
    }
}
