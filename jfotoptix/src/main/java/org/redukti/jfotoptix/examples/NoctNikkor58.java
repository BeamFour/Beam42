package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Matrix3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.PointSource;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

//Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
// Failed to come up with a solution
public class NoctNikkor58 {

    static final class GlassType {
        final double nd;
        final double vd;

        public GlassType(double nd, double vd) {
            this.nd = nd;
            this.vd = vd;
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

    private static double add_surface(Lens.Builder lens, double radius, double thickness, double diameter, double nd, double vd, boolean stop) {
        double apertureRadius = diameter / 2.0;
        if (stop) {
            lens.add_stop(apertureRadius, thickness, true);
            return thickness;
        }
        if (nd != 0.0) {
            lens.add_surface(radius, apertureRadius, thickness, new Abbe(Abbe.AbbeFormula.AbbeVd, nd, vd, 0.0));
        } else {
            lens.add_surface(radius, apertureRadius, thickness);
        }
        return thickness;
    }

    private static List<SurfaceType> getSurfaces() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 74.2272, 7.175639, 1.8485, 49.93, 43.8));
        list.add(new SurfaceType(false, 0, 0.393186, 0, 49.93, 0));
        list.add(new SurfaceType(false, 33.25766, 9.6305, 1.69, 44.528, 54.7));
        list.add(new SurfaceType(false, 60.92781, 1.671039, 0, 44.528, 0));
        list.add(new SurfaceType(false, 137.839, 3.538761, 1.7783, 42.169, 23.9));
        list.add(new SurfaceType(false, 25.38526, 7.568825, 0, 32.54, 0));
        list.add(new SurfaceType(true, 0, 7.863714, 0, 29.98, 0));
        list.add(new SurfaceType(false, -23.9188, 1.867632, 1.58148, 31.45, 40.9));
        list.add(new SurfaceType(false, 293.9713, 7.863714, 1.6934, 40.2, 53.3));
        list.add(new SurfaceType(false, -39.4556, 0.491482, 0, 40.2, 0));
        list.add(new SurfaceType(false, -274.4783, 5.701193, 1.6516, 39.13, 58.4));
        list.add(new SurfaceType(false, -53.08654, 0.000, 0, 39.13, 0));
        list.add(new SurfaceType(false, 124.1071, 4.12845, 1.6217, 36.86, 58.54));
        list.add(new SurfaceType(false, -96.87727, 37.780, 0, 36.86, 0));
        return list;
    }

    private static OpticalSystem.Builder buildSystem(double[] glassTypes, boolean addPointSource, boolean skew) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 40.9 / 2.0;
        double fNum = 1.2;
        if (addPointSource) {
            Vector3 direction = Vector3.vector3_001;
            if (skew) {
                // Construct unit vector at an angle
                //      double z1 = cos (angleOfView);
                //      double y1 = sin (angleOfView);
                //      unit_vector = math::Vector3 (0, y1, z1);

                Matrix3 r = Matrix3.get_rotation_matrix(0, angleOfView);
                direction = r.times(direction);
            }
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction).add_spectral_line(SpectralLine.d).add_spectral_line(SpectralLine.C).add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<SurfaceType> surfaces = getSurfaces();
        int index = 0;
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double nd = s.nd;
            double vd = s.vd;
            if (nd != 0) {
                nd = glassTypes[index++];
            }
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, nd, vd, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.angle_of_view(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    // Facts H' - H = 14.3
    // F' = 58
    static List<Double> getGlassTypes1() {
        return GlassMap.glasses.values().stream().map(e -> e.get_refractive_index(SpectralLine.d)).filter(e -> e > 1.7 && e < 1.91).sorted().distinct().collect(Collectors.toList());
    }

    static GlassType[] nikonGlasses() {
        return new GlassType[]{new GlassType(1.6727, 32.2), new GlassType(1.68893, 31.16), new GlassType(1.69895, 30.1), new GlassType(1.71300, 53.9),   // canon
                new GlassType(1.71736, 29.5), new GlassType(1.72825, 28.46), new GlassType(1.72, 0), new GlassType(1.74, 28.3), new GlassType(1.74077, 27.6), new GlassType(1.74430, 49.5),   // US 4,621,909
                new GlassType(1.74443, 49.53), new GlassType(1.75520, 27.6), // also in canon
                new GlassType(1.76684, 46.81), new GlassType(1.77279, 49.4), new GlassType(1.78470, 0), new GlassType(1.78797, 47.5), new GlassType(1.79631, 40.8), new GlassType(1.79713, 45.62), new GlassType(1.79668, 45.5), new GlassType(1.80218, 44.7), new GlassType(1.80411, 46.4), new GlassType(1.84042, 43.3), new GlassType(1.87739, 38.1), new GlassType(1.90265, 35.8)};
    }

    static List<Double> getGlassTypes() {
        Double[] glasses = new Double[]{1.6727, 1.68893, 1.69895, 1.71300, 1.71736, 1.72825, 1.72, 1.74, 1.74077, 1.74430, 1.74443, 1.75520, 1.76684, 1.77279, 1.78470, 1.78797, 1.79631, 1.79668, 1.80218, 1.80411, 1.84042, 1.87739};
        //Double[] glasses = new Double[]{1.64769, 1.651, 1.65844, 1.66446, 1.67, 1.6779, 1.68893, 1.6935, 1.6968, 1.69895, 1.7, 1.713, 1.717, 1.72, 1.734, 1.738, 1.741, 1.744, 1.755, 1.757, 1.762, 1.7725, 1.7847, 1.788, 1.795, 1.801, 1.804, 1.816, 1.834, 1.8485, 1.85};
        return List.of(glasses);
    }

    static final class ProcessSystems implements Runnable {

        int start;
        int end;
        List<Double> glassTypes;
        AtomicLong count;

        public ProcessSystems(int start, int end, List<Double> glassTypes, AtomicLong count) {
            this.start = start;
            this.end = end;
            this.glassTypes = glassTypes;
            this.count = count;
        }

        public void run() {
            var glasses = new double[7];
            double bestRMS = 999.00;
            String bestData = null;
            for (int a = start; a < end; a++) {
                glasses[0] = glassTypes.get(a);
                for (int b = 0; b < glassTypes.size(); b++) {
                    glasses[1] = glassTypes.get(b);
                    for (int c = 0; c < glassTypes.size(); c++) {
                        glasses[2] = glassTypes.get(c);
                        for (int d = 0; d < glassTypes.size(); d++) {
                            glasses[3] = glassTypes.get(d);
                            for (int e = 0; e < glassTypes.size(); e++) {
                                glasses[4] = glassTypes.get(e);
                                for (int f = 0; f < glassTypes.size(); f++) {
                                    glasses[5] = glassTypes.get(f);
                                    for (int g = 0; g < glassTypes.size(); g++) {
                                        glasses[6] = glassTypes.get(g);
                                        var system = buildSystem(glasses, false, false).build();
                                        //System.out.println(system);
                                        count.incrementAndGet();
                                        try {
                                            var parax = ParaxialFirstOrderInfo.compute(system);
                                            // Expected H' ppk = 37.5 from front-surface, 20.2 from last surface
                                            // Expected H pp1 = 51.8
                                            // expected H - H1 = 14.3

                                            var h_diff = parax.pp1 - parax.ppk;  // H - H'
                                            if (parax.effective_focal_length > 57.99 && parax.effective_focal_length < 58.01 && parax.back_focal_length > 37.77 && parax.back_focal_length < 37.79
                                                    //&& h_diff > 14 && h_diff < 15
                                                    && parax.pp1 > 51.75 && parax.pp1 < 51.85 // 51.8 from first surface
                                                    && parax.ppk > 20.15 && parax.ppk < 20.25) {  // 20.2 from last surface

                                                var system2 = buildSystem(glasses, true, false).build();
                                                var spotAnalysis = new AnalysisSpot(system2, 20);
                                                spotAnalysis.process_analysis();

                                                if (spotAnalysis.get_rms_radius() < bestRMS) {
                                                    bestRMS = spotAnalysis.get_rms_radius();
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append(spotAnalysis.get_rms_radius()).append("\t");
                                                    sb.append(parax.effective_focal_length).append("\t").append(parax.back_focal_length).append("\t").append(parax.fno).append("\t").append(parax.ppk).append("\t").append(parax.pp1).append("\t").append(h_diff).append("\t");
                                                    for (int i = 0; i < glasses.length; i++)
                                                        sb.append(glasses[i]).append("\t");
                                                    bestData = sb.toString();
                                                }
                                                //                                                StringBuilder sb = new StringBuilder();
                                                //                                                sb.append(spotAnalysis.get_rms_radius()).append("\t");
                                                //                                                sb.append(parax.effective_focal_length).append("\t").append(parax.back_focal_length).append("\t").append(parax.fno).append("\t").append(parax.ppk).append("\t").append(parax.pp1).append("\t").append(h_diff).append("\t");
                                                //                                                for (int i = 0; i < glasses.length; i++)
                                                //                                                    sb.append(glasses[i]).append("\t");
                                                //                                                System.out.println(sb.toString());
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
            System.out.println(bestData);
        }
    }

    public static void main(String[] args) throws Exception {

        var glassTypes = getGlassTypes();
        System.out.println("Trying " + glassTypes.size() + " glass types");

        AtomicLong count = new AtomicLong();
        int numThreads = 8;
        Thread[] threads = new Thread[numThreads];
        int perThreadGlassCount = (int) Math.round((double) glassTypes.size() / (double) numThreads);
        int start = 0;
        for (int g = 0; g < numThreads; g++) {
            int end;
            if (g == numThreads - 1) {
                end = glassTypes.size();
            } else {
                end = start + perThreadGlassCount;
            }
            System.out.println("Allocating " + start + " to " + end);
            threads[g] = new Thread(new ProcessSystems(start, end, glassTypes, count));
            start += perThreadGlassCount;
            threads[g].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Processed " + count + " systems");
    }
}
