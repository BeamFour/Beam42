package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.importers.OpticalBenchDataImporter;
import org.redukti.jfotoptix.layout.SystemLayout2D;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.patterns.Distribution;
import org.redukti.jfotoptix.patterns.Pattern;
import org.redukti.jfotoptix.rendering.RendererSvg;
import org.redukti.jfotoptix.rendering.Rgb;
import org.redukti.jfotoptix.tracing.RayTraceParameters;
import org.redukti.jfotoptix.tracing.RayTraceRenderer;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.jfotoptix.tracing.RayTracer;

import java.nio.file.Path;

public class LensTool {

    public static OpticalBenchDataImporter.LensSpecifications getSpecsFromFile(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return specs;
    }

    public static OpticalSystem createSystem(OpticalBenchDataImporter.LensSpecifications specs, int scenario, boolean use_glass_types, boolean skew_rays, boolean d_line) {
        OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, scenario, use_glass_types);
        double angleOfView = OpticalBenchDataImporter.get_angle_of_view_in_radians(specs, scenario);
        Vector3 direction = Vector3.vector3_001;
        if (skew_rays) {
            // Construct unit vector at an angle
            //      double z1 = cos (angleOfView);
            //      double y1 = sin (angleOfView);
            //      unit_vector = math::Vector3 (0, y1, z1);
            Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
            direction = r.times(direction);
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

    public static void outputLayout(OpticalSystem system, Path output_file) throws Exception {
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

    public static void outputLayoutWithRays(OpticalSystem system, Path output_file, int trace_density, boolean dump_system, boolean include_lost_rays) throws Exception {
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
        result.report();
    }

    public static AnalysisSpot outputSpotAnalysis(OpticalSystem system, Path output_file, int spot_density) throws Exception {
        RendererSvg renderer = new RendererSvg(300, 300, Rgb.rgb_black);
        AnalysisSpot spot = new AnalysisSpot(system, spot_density);
        spot.draw_diagram(renderer, true);
        if (output_file != null) {
            Helper.createOutputFile(output_file, renderer.write(new StringBuilder()).toString());
        } else {
            System.out.println(renderer.write(new StringBuilder()).toString());
        }
        return spot;
    }

    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num] [--dump-system] [--exclude-lost-rays] [--spot-density n] [--trace-density n] [--only-d-line] [-o outfilename] [--dont-use-glass-types]");
            System.err.println("       --spot-density defaults to 50");
            System.err.println("       --trace-density defaults to 20");
            System.err.println("       --scenario defaults to 0");
            System.err.println("       Output file will be created in the same location as the specfile");
            System.exit(1);
        }
        try {
            OpticalBenchDataImporter.LensSpecifications specs = getSpecsFromFile(arguments.specfile);
            OpticalSystem system = createSystem(specs,arguments.scenario,arguments.use_glass_types,false,arguments.only_d_line);
            if (arguments.dumpSystem) {
                System.out.println(system);
            }
            OpticalSystem skewedSystem = createSystem(specs,arguments.scenario,arguments.use_glass_types,true,arguments.only_d_line);
            if (arguments.dumpSystem) {
                System.out.println(skewedSystem);
            }
            outputLayout(system,Helper.getOutputPath(arguments.specfile,"layoutonly.svg",arguments.outdir));
            outputLayoutWithRays(system,Helper.getOutputPath(arguments.specfile,"layout.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
            outputLayoutWithRays(skewedSystem,Helper.getOutputPath(arguments.specfile,"layout-skew.svg",arguments.outdir),arguments.trace_density,arguments.dumpSystem,arguments.include_lost_rays);
            StringBuilder spotReport = new StringBuilder();
            spotReport.append(outputSpotAnalysis(system,Helper.getOutputPath(arguments.specfile,"spot.svg",arguments.outdir),arguments.spot_density)).append("\n");
            spotReport.append(outputSpotAnalysis(skewedSystem,Helper.getOutputPath(arguments.specfile,"spot-skew.svg",arguments.outdir),arguments.spot_density)).append("\n");
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"spot-report.txt",arguments.outdir), spotReport.toString());
            ParaxialFirstOrderInfo pfo = ParaxialFirstOrderInfo.compute(system);
            Helper.createOutputFile(Helper.getOutputPath(arguments.specfile,"paraxial.txt",arguments.outdir), pfo.toString());
            ZemaxExporter zemaxExporter = new ZemaxExporter();
            Helper.createOutputFile(Helper.getOutputPathChangeExt(arguments.specfile, ".zmx"), zemaxExporter.generate(specs, arguments.scenario, arguments.only_d_line));
        }
        catch (Exception e) {
            System.err.println("Failed due to: " + e.getMessage());
        }
    }
}
