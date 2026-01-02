package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.mathlib.Vector3Pair;
import org.redukti.rayoptics.seq.Glass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class LeicaSummilux50mmSelect {

    static final class GlassType {
        final String name;
        final double nd;
        final double vd;

        public GlassType(String name, double nd, double vd) {
            this.name = name;
            this.nd = nd;
            this.vd = vd;
        }

        @Override
        public String toString() {
            return "(" + name + "," + nd + "," + vd + ')';
        }
    }

    static final class SurfaceType {
        double radius;
        double thickness;
        double apertureRadius;
        boolean isStop;
        double nd;
        double vd;

        public SurfaceType(boolean isStop, double radius, double thickness, double nd, double apertureRadius, double vd) {
            this.radius = radius;
            this.thickness = thickness;
            this.apertureRadius = apertureRadius;
            this.isStop = isStop;
            this.nd = nd;
            this.vd = vd;
        }
    }

    private static double add_surface(Lens.Builder lens, double radius, double thickness, double diameter, double nd, double vd, String glassName, boolean stop) {
        double apertureRadius = diameter / 2.0;
        if (stop) {
            lens.add_stop(apertureRadius, thickness, true);
            return thickness;
        }
        if (nd != 0.0) {
            var glass = GlassMap.glassByName(glassName);
            if (glass == null) {
                lens.add_surface(radius, apertureRadius, thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, nd, vd, 0.0));
            }
            else {
                lens.add_surface(radius, apertureRadius, thickness, glass);
            }
        } else {
            lens.add_surface(radius, apertureRadius, thickness);
        }
        return thickness;
    }

    // Measured by DM - off patent
    private static List<SurfaceType> getSurfaces() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 40.376, 5.725, 1.73430, 37.93, 28.19));
        list.add(new SurfaceType(false, 90.57, 0.0, 0, 35.49, 0));
        list.add(new SurfaceType(false, 26.437, 8.7, 1.67133, 33.05, 41.64));
        list.add(new SurfaceType(false, -532.104, 2.748, 1.7919, 31.217, 25.55));
        list.add(new SurfaceType(false, 22.866, 5.88, 0, 26.4, 0));
        list.add(new SurfaceType(true, 0, 2.06, 0, 25.798, 0));
        list.add(new SurfaceType(false, -68.33, 2.5158, 1.78831, 25.722, 47.7));
        list.add(new SurfaceType(false, -155.056, 3.05, 0, 25.722, 0));
        list.add(new SurfaceType(false, -50.09, 3.282, 1.64769, 25.722, 33.78));
        list.add(new SurfaceType(false, 70.145, 6.869, 1.816, 27.63, 46.62));
        list.add(new SurfaceType(false, -33.546, 0.534, 0, 27.63, 0));
        list.add(new SurfaceType(false, 47.987, 6.716, 1.816, 25.722, 46.62));
        list.add(new SurfaceType(false, -42.666, 5.037, 1.62004, 25.722, 36.26));
        list.add(new SurfaceType(false, 30.645, 26.256, 0, 26.026, 0));
        return list;
    }

    // Measured by DM - off website
    private static List<SurfaceType> getSurfaces2() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 45.527, 5.419, 1.73430, 37.86, 28.19));
        list.add(new SurfaceType(false, 94.469, 0.229, 0, 36.25, 0));
        list.add(new SurfaceType(false, 25.619, 7.86, 1.67133, 33.2, 41.64));
        list.add(new SurfaceType(false, -451.4844, 3.13, 1.7919, 33.2, 25.55));
        list.add(new SurfaceType(false, 22.769, 5.724, 0, 26.56, 0));
        list.add(new SurfaceType(true, 0, 2.06, 0, 26.1, 0));
        list.add(new SurfaceType(false, -83.844, 2.67, 1.78831, 25.722, 47.7));
        list.add(new SurfaceType(false, -180.816, 3.05, 0, 25.722, 0));
        list.add(new SurfaceType(false, -46.063, 3.13, 1.64769, 25.722, 33.78));
        list.add(new SurfaceType(false, 79.183, 6.793, 1.816, 27.7, 46.62));
        list.add(new SurfaceType(false, -37.255, 0.382, 0, 27.7, 0));
        list.add(new SurfaceType(false, 45.795, 6.564, 1.816, 25.645, 46.62));
        list.add(new SurfaceType(false, -44.038, 4.961, 1.62004, 25.645, 36.26));
        list.add(new SurfaceType(false, 30.437, 25.722, 0, 26.256, 0));
        return list;
    }


    private static OpticalSystem.Builder buildSystem(GlassType[] glassTypes, boolean addPointSource, boolean skew) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 45.0 / 2.0;
        double fNum = 1.4;
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (skew) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);

                Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
                direction = r.multiply(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d)
                    .add_spectral_line(SpectralLine.C)
                    .add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<SurfaceType> surfaces = getSurfaces2();
        int index = 0;
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double nd = s.nd;
            double vd = s.vd;
            String glassName = null;
            if (nd != 0) {
                nd = glassTypes[index].nd;
                vd = glassTypes[index].vd;
                glassName = glassTypes[index].name;
                index++;
            }
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, nd, vd, glassName, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.half_angle_of_view_in_degrees(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    static GlassType[] getGlassTypes() {
        return new GlassType[]{
                //new GlassType("J-BASF6", 1.67133, 41.64),
                //new GlassType("S-NPH2", 1.92286, 18.9),
                new GlassType("S-LAH99", 2.001, 29.14),
                new GlassType("S-LAH97",1.755,52.32),
                new GlassType("S-LAH64",1.788,47.37),
                new GlassType("S-LAH66", 1.7725, 49.6),
                new GlassType("S-LAH59", 1.816, 46.62),
                new GlassType("L-LAH85", 1.854, 40.39),
                new GlassType("S-LAH58", 1.883, 40.77),
                new GlassType("S-TIM22", 1.64769, 33.78),
                new GlassType("S-TIM2", 1.62004, 29.57),
                new GlassType("S-TIM1", 1.62588, 35.7),
                new GlassType("J-SF10", 1.72825, 36.26),
                new GlassType("N-SF11", 1.7919, 25.55),
                new GlassType("N-SF10", 1.7343, 28.19),
                new GlassType("S-FPL51", 1.497, 81.55),
                new GlassType("N-FK51", 1.48656, 84.47),
                new GlassType("J-LAK9",1.691,54.93),
                //new GlassType("S-NBH5", 1.65412, 39.68),
                //new GlassType("S-BAL42", 1.58313, 59.37),
                //new GlassType("S-TIH53", 1.84666, 23.78),
                new GlassType("S-TIH6", 1.80518, 25.43),
                //new GlassType("S-PHM53", 1.603, 65.44),
                //new GlassType("S-NBH8", 1.72047, 34.71),
                new GlassType("M-TAF401",1.77377,47.17),
                new GlassType("M-PCD51",1.59201,67.02),
                new GlassType("N-KZFS2",1.55836,54.01),

                //new GlassType("L-BSL7", 1.51633, 64.07),
                };
    }

    static final class ProcessSystems implements Runnable {

        int start;
        int end;
        GlassType[] glassTypes;
        AtomicLong count;

        public ProcessSystems(int start, int end, GlassType[] glassTypes, AtomicLong count) {
            this.start = start;
            this.end = end;
            this.glassTypes = glassTypes;
            this.count = count;
        }

        public void run() {
            var glasses = new GlassType[8];
            double bestRMS = 999.00;
            String bestData = null;
            for (int a = start; a < end; a++) {
                glasses[0] = glassTypes[a];
                for (int b = 0; b < glassTypes.length; b++) {
                    glasses[1] = glassTypes[b];
                    for (int c = 0; c < glassTypes.length; c++) {
                        glasses[2] = glassTypes[c];
                        for (int d = 0; d < glassTypes.length; d++) {
                            glasses[3] = glassTypes[d];
                            for (int e = 0; e < glassTypes.length; e++) {
                                glasses[4] = glassTypes[e];
                                for (int f = 0; f < glassTypes.length; f++) {
                                    glasses[5] = glassTypes[f];
                                    for (int g = 0; g < glassTypes.length; g++) {
                                        glasses[6] = glassTypes[g];
                                        for (int h = 0; h < glassTypes.length; h++) {
                                            glasses[7] = glassTypes[h];
                                            var system = buildSystem(glasses, false, false).build();
                                            //System.out.println(system);
                                            count.incrementAndGet();
                                            try {
                                                var parax = ParaxialFirstOrderInfo.compute(system);
                                                // Expected H' ppk = 37.5 from front-surface, 20.2 from last surface
                                                // Expected H pp1 = 51.8
                                                // expected H - H1 = 14.3

                                                if (parax.effective_focal_length > 51.0 && parax.effective_focal_length < 52.5
                                                        && parax.back_focal_length > 24.0 && parax.back_focal_length < 27.0) {

//                                                    var sb1 = new StringBuilder();
//                                                    sb1.append(parax.effective_focal_length).append("\t")
//                                                            .append(parax.back_focal_length).append("\t")
//                                                            .append(parax.fno).append("\t");
                                                    //System.out.println(sb1);
                                                    var system2 = buildSystem(glasses, true, false).build();
                                                    var spotAnalysis = new AnalysisSpot(system2, 10);
                                                    spotAnalysis.process_analysis();
                                                    if (spotAnalysis.get_rms_radius() < 2000.0) {
                                                        StringBuilder sb = new StringBuilder();
                                                        sb.append(spotAnalysis.get_rms_radius()).append("\t");
                                                        sb.append(parax.effective_focal_length).append("\t")
                                                                .append(parax.back_focal_length).append("\t")
                                                                .append(parax.fno).append("\t");
                                                        for (int i = 0; i < glasses.length; i++)
                                                            sb.append(glasses[i]).append("\t");
                                                        System.out.println(sb.toString());
                                                        if (spotAnalysis.get_rms_radius() < bestRMS) {
                                                            bestRMS = spotAnalysis.get_rms_radius();
                                                            bestData = sb.toString();
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Best Data");
            System.out.println(bestData);
        }
    }

    public static void main(String[] args) throws Exception {

        var glassTypes = getGlassTypes();
        System.out.println("Trying " + glassTypes.length + " glass types");

        AtomicLong count = new AtomicLong();
        int numThreads = glassTypes.length;
        Thread[] threads = new Thread[numThreads];
        int perThreadGlassCount = 1; //  (int) Math.round((double) glassTypes.length / (double) numThreads);
        int start = 0;
        for (int g = 0; g < numThreads; g++) {
            int end = start + perThreadGlassCount;
            if (start >= glassTypes.length) {
                break;
            }
            if (end >= glassTypes.length) {
                end = glassTypes.length;
            }
            System.out.println("Allocating " + start + " to " + end);
            threads[g] = new Thread(new ProcessSystems(start, end, glassTypes, count));
            start += perThreadGlassCount;
            threads[g].start();
        }

        for (Thread thread : threads) {
            if (thread != null)
                thread.join();
        }
        System.out.println("Processed " + count + " systems");
    }
}
