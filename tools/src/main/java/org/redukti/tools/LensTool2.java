package org.redukti.tools;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotAnalysisResult;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.analysis.WavefrontAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.util.Args;
import org.redukti.util.Helper;

import java.nio.file.Path;

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
        return prescription.build_rayoptic_model(prescription,fov_angle);
    }

    public static void outputSpotAnalysis(SpotAnalysisResult.SpotResultsByField result, Path output_file) throws Exception {
        if (output_file != null) {
            Helper.createOutputFile(output_file, result.plot());
        } else {
            System.out.println(result.plot());
        }
    }

    public static void createREADME(String specFile, OpticalBenchDataImporter.LensSpecifications specs, ParaxialFirstOrderInfo pfo, double[] fields, AnalysisSpot[] spots, Path output_file) throws Exception {
        Prescription prescription = Prescription.buildPrescription(specs,true);
        StringBuilder sb = prescription.toMarkdownStr(new StringBuilder());
        sb.append("## Layouts\n");
        sb.append("![Layout Only](./layoutonly.svg)\n");
        sb.append("![Layout Only](./layout.svg)\n");
        sb.append("![Layout Only](./layout-semi-skew.svg)\n");
        sb.append("![Layout Only](./layout-skew.svg)\n");
        sb.append("## Spot Diagrams\n");
        sb.append("![Layout Only](./spot.svg)\n");
        sb.append("![Layout Only](./spot-semi-skew.svg)\n");
        sb.append("![Layout Only](./spot-skew.svg)\n");
        sb.append("## Paraxial Parameters\n");
        pfo.toMarkdown(sb);
        sb.append("## Spot Analysis\n");
        AnalysisSpot.toMarkdownTableHeader(sb);
        for (int i = 0; i < fields.length; i++) {
            spots[i].toMarkdownTableRow(sb,fields[i]);
        }
        String filename = Helper.getFilename(specFile);
        String zmxFilename = Helper.replaceExtension(filename, ".zmx");
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
            //ParaxialFirstOrderInfo pfo = ParaxialFirstOrderInfo.compute(system);
            //Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"paraxial.txt",arguments.outdir), pfo.toString());
//            outputLayout(system,Helper.getOutputPath(arguments.specfile,"layoutonly.svg",arguments.outdir));
//            outputLayoutWithRays(system,Helper.getOutputPath(arguments.specfile,"layout.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
//            outputLayoutWithRays(semiSkewedSystem,Helper.getOutputPath(arguments.specfile,"layout-semi-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
//            outputLayoutWithRays(skewedSystem,Helper.getOutputPath(arguments.specfile,"layout-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
            StringBuilder spotReport = new StringBuilder();
            var spotAnalysis = SpotAnalysis.eval(opm,21, new TraceOptions());
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
                Helper.createOutputFile(output_file, rayAber.plot(fan_results, 1.0));
            }

            var opdAber = WavefrontAberrationAnalysis.eval(opm, 21, new TraceOptions());
            for (var fan_results: opdAber.results) {
                String filename = "opdabbr-fld" + fan_results.fi + "-" + (fan_results.xy == 1? "tan" : "sag") + ".svg";
                var output_file = Helper.getOutputPath(arguments.specfile,filename,arguments.outdir);
                Helper.createOutputFile(output_file, opdAber.plot(fan_results, 2e-5));
            }


//            spotReport.append(spot0).append("\n");
//            spotReport.append(spot1).append("\n");
//            spotReport.append(spot2).append("\n");
//            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"spot-report.txt",arguments.outdir), spotReport.toString());
//            ZemaxExporter zemaxExporter = new ZemaxExporter();
//            Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), zemaxExporter.generate(specs, arguments.scenario, arguments.only_d_line));
//            createREADME(arguments.specfile,
//                    specs,
//                    pfo,
//                    new double[] {0.0, 0.7, 1.0},
//                    new AnalysisSpot[] { spot0, spot1, spot2 },
//                    Helper.getOutputPath(arguments.specfile,"README.md",arguments.outdir));
        }
        catch (Exception e) {
            System.err.println("Failed due to: " + e.getMessage());
        }
    }
}
