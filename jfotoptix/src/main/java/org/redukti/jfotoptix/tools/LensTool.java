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

public class LensTool {


    public static void main(String[] args) throws Exception {
        Args arguments = Args.parseArguments(args);
        if (arguments.specfile == null) {
            System.err.println("Usage: --specfile inputfile [--scenario num] [--skew] [--output layout|spot] [--dump-system] [--exclude-lost-rays] [--spot-density n] [--trace-density n] [--only-d-line] [-o outfilename] [--dont-use-glass-types]");
            System.err.println("       --spot-density defaults to 50");
            System.err.println("       --trace-density defaults to 20");
            System.err.println("       --scenario defaults to 0");
            System.err.println("       Output file will be created in the same location as the specfile");
            System.exit(1);
        }
        try {
            OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
            specs.parse_file(arguments.specfile);
            OpticalSystem.Builder systemBuilder = OpticalBenchDataImporter.build_system(specs, arguments.scenario, arguments.use_glass_types);
            double angleOfView = OpticalBenchDataImporter.get_angle_of_view_in_radians(specs, arguments.scenario);
            Vector3 direction = Vector3.vector3_001;
            if (arguments.skewRays) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);
                Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
                direction = r.times(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d);
            if (!arguments.only_d_line) {
                ps.add_spectral_line(SpectralLine.C)
                        .add_spectral_line(SpectralLine.F);
            }
            systemBuilder.add(ps);

            OpticalSystem system = systemBuilder.build();
            if (arguments.dumpSystem) {
                System.out.println(system);
            }
            if (arguments.outputType.equals("layout")) {
                // draw 2d system layout
                RendererSvg renderer = new RendererSvg(800, 400);
                SystemLayout2D systemLayout2D = new SystemLayout2D();
                systemLayout2D.layout2d(renderer, system);
                RayTraceParameters parameters = new RayTraceParameters(system);
                RayTracer rayTracer = new RayTracer();
                parameters.set_default_distribution(
                        new Distribution(Pattern.MeridionalDist, arguments.trace_density, 0.999));
                if (arguments.dumpSystem) {
                    System.out.println(parameters.sequenceToString(new StringBuilder()).toString());
                }
                RayTraceResults result = rayTracer.trace(system, parameters);
                RayTraceRenderer.draw_2d(renderer, result, !arguments.include_lost_rays, null);
                if (arguments.outputFile != null) {
                    Helper.createOutputFile(Helper.getOutputPath(arguments), renderer.write(new StringBuilder()).toString());
                } else {
                    System.out.println(renderer.write(new StringBuilder()).toString());
                }
                result.report();
            } else if (arguments.outputType.equals("spot")) {
                RendererSvg renderer = new RendererSvg(300, 300, Rgb.rgb_black);
                AnalysisSpot spot = new AnalysisSpot(system, arguments.spot_density);
                spot.draw_diagram(renderer, true);
                if (arguments.outputFile != null) {
                    Helper.createOutputFile(Helper.getOutputPath(arguments), renderer.write(new StringBuilder()).toString());
                } else {
                    System.out.println(renderer.write(new StringBuilder()).toString());
                }
            }
            ParaxialFirstOrderInfo pfo = ParaxialFirstOrderInfo.compute(system);
            System.out.println(pfo);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
