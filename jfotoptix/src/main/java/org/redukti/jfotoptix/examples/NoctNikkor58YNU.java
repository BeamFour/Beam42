package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.fastparax.YNUTracer;
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

// Takes too long to run for more than about 22 glasses (that takes 2 hrs as well)
public class NoctNikkor58YNU {

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
            return "\t" + name + "\t" + nd + "\t" + vd;
        }
    }

    static final class SurfaceType {
        double radius;
        double thickness;
        double apertureRadius;
        boolean isStop;
        double nd;
        double vd;
        String glassName;

        public SurfaceType(boolean isStop, double radius, double thickness, double nd, double apertureRadius, double vd, String glassName) {
            this.radius = radius;
            this.thickness = thickness;
            this.apertureRadius = apertureRadius;
            this.isStop = isStop;
            this.nd = nd;
            this.vd = vd;
            this.glassName = glassName;
        }
    }

    private static double add_surface(Lens.Builder lens, double radius, double thickness, double diameter, double nd, double vd, String glassName, boolean stop) {
        double apertureRadius = diameter / 2.0;
        if (stop) {
            lens.add_stop(apertureRadius, thickness, true);
            return thickness;
        }
        if (nd != 0.0) {
            lens.add_surface(radius, apertureRadius, thickness, new GlassMap("Any", glassName, nd, 0,0, vd, 0.0));
        } else {
            lens.add_surface(radius, apertureRadius, thickness);
        }
        return thickness;
    }

    // Based on service manual - computed by DM
    private static List<SurfaceType> getSurfacesSM() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 74.2272, 7.175639, 1.8485, 49.93, 43.8,"G1"));
        list.add(new SurfaceType(false, 0, 0.393186, 0, 49.93, 0, null));
        list.add(new SurfaceType(false, 33.25766, 9.6305, 1.69, 44.528, 54.7, "G2"));
        list.add(new SurfaceType(false, 60.92781, 1.671039, 0, 44.528, 0, null));
        list.add(new SurfaceType(false, 137.839, 3.538761, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 25.38526, 7.568825, 0, 32.54, 0,null));
        list.add(new SurfaceType(true, 0, 7.863714, 0, 29.98, 0,null));
        list.add(new SurfaceType(false, -23.9188, 1.867632, 1.58148, 31.45, 40.9,"G4"));
        list.add(new SurfaceType(false, 293.9713, 7.863714, 1.6934, 40.2, 53.3,"G5"));
        list.add(new SurfaceType(false, -39.4556, 0.491482, 0, 40.2, 0,null));
        list.add(new SurfaceType(false, -274.4783, 5.701193, 1.6516, 39.13, 58.4,"G6"));
        list.add(new SurfaceType(false, -53.08654, 0.000, 0, 39.13, 0,null));
        list.add(new SurfaceType(false, 124.1071, 4.12845, 1.6217, 36.86, 58.54,"G7"));
        list.add(new SurfaceType(false, -96.87727, 37.780, 0, 36.86, 0, null));
        return list;
    }

    // Contrib - service manual based
    private static List<SurfaceType> getSurfacesSMContrib() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 73.77, 7.17, 1.8485, 50.4875, 43.8, "G1"));
        list.add(new SurfaceType(false, -20000, 0.34, 0, 50.4875, 0, null));
        list.add(new SurfaceType(false, 33.44, 9.63, 1.69, 44.832, 54.7, "G2"));
        list.add(new SurfaceType(false, 68.854, 1.6, 0, 44.832, 0, null));
        list.add(new SurfaceType(false, 137.71, 3.43, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 25.57, 7.702, 0, 32.128, 0, null));
        list.add(new SurfaceType(true, 0, 7.771, 0, 31.227, 0, null));
        list.add(new SurfaceType(false, -24.59, 1.87, 1.58148, 31.45, 40.9, "G4"));
        list.add(new SurfaceType(false, 304.93, 7.95, 1.6934, 40.2, 53.3, "G5"));
        list.add(new SurfaceType(false, -39.345, 0.4, 0, 40.2, 0, null));
        list.add(new SurfaceType(false, -295.1, 5.63, 1.6516, 39.5, 58.4, "G6"));
        list.add(new SurfaceType(false, -56.6, 0.1, 0, 39.5, 0, null));
        list.add(new SurfaceType(false, 125.91, 4.09, 1.6217, 38.275, 58.54, "G7"));
        list.add(new SurfaceType(false, -98.36, 37.78, 0, 38.275, 0, null));
        return list;
    }


    // Measured by DM - off 1001 tale 16
    private static List<SurfaceType> getSurfacesTale16() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 79.9975, 6.885, 1.8485, 50.4875, 43.8, "G1"));
        list.add(new SurfaceType(false, 0, 0.1, 0, 50.4875, 0, null));
        list.add(new SurfaceType(false, 33.737, 9.75, 1.69, 44.832, 54.7, "G2"));
        list.add(new SurfaceType(false, 70.18675, 1.56, 0, 44.832, 0, null));
        list.add(new SurfaceType(false, 134.505, 2.87, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 22.3687, 8.44, 0, 32.128, 0, null));
        list.add(new SurfaceType(true, 0, 7.95, 0, 31.227, 0, null));
        list.add(new SurfaceType(false, -23.02418, 1.64, 1.58148, 31.45, 40.9, "G4"));
        list.add(new SurfaceType(false, 306.553, 8.196, 1.6934, 40.2, 53.3, "G5"));
        list.add(new SurfaceType(false, -37.555, 0.15, 0, 40.2, 0, null));
        list.add(new SurfaceType(false, -396.94, 6.147, 1.6516, 39.5, 58.4, "G6"));
        list.add(new SurfaceType(false, -52.56789, 0.000, 0, 39.5, 0, null));
        list.add(new SurfaceType(false, 223.8426, 4.016, 1.6217, 38.275, 58.54, "G7"));
        list.add(new SurfaceType(false, -94.08052, 37.780, 0, 38.275, 0, null));
        return list;
    }

    // modified manually based on Zemax sliders
    private static List<SurfaceType> getSurfacesTale16b() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 79.946, 6.885, 1.8485, 50.4875, 43.8, "G1"));
        list.add(new SurfaceType(false, 0, 0.1, 0, 50.4875, 0, null));
        list.add(new SurfaceType(false, 33.792, 9.75, 1.69, 44.832, 54.7, "G2"));
        list.add(new SurfaceType(false, 70.187, 1.56, 0, 44.832, 0, null));
        list.add(new SurfaceType(false, 134.614, 2.87, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 22.369, 8.5, 0, 32.128, 0, null));
        list.add(new SurfaceType(true, 0, 7.9, 0, 31.227, 0, null));
        list.add(new SurfaceType(false, -23.035, 1.64, 1.58148, 31.45, 40.9, "G4"));
        list.add(new SurfaceType(false, 306.553, 8.196, 1.6934, 40.2, 53.3, "G5"));
        list.add(new SurfaceType(false, -37.566, 0.15, 0, 40.2, 0, null));
        list.add(new SurfaceType(false, -396.814, 6.147, 1.6516, 39.5, 58.4,"G6"));
        list.add(new SurfaceType(false, -52.568, 0.000, 0, 39.5, 0, null));
        list.add(new SurfaceType(false, 223.843, 4.016, 1.6217, 38.275, 58.54, "G7"));
        list.add(new SurfaceType(false, -94.081, 37.780, 0, 38.275, 0, null));
        return list;
    }

    // Latest contrib 01 June
    private static List<SurfaceType> getSurfacesContribTale16() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 80.344, 7.042, 1.8485, 50.4875, 43.8, "G1"));
        list.add(new SurfaceType(false, 0, 0.189, 0, 50.4875, 0, null));
        list.add(new SurfaceType(false, 33.61, 9.715, 1.69, 44.832, 54.7, "G2"));
        list.add(new SurfaceType(false, 69.686, 1.476, 0, 44.832, 0, null));
        list.add(new SurfaceType(false, 129.534, 3.02, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 22.55, 8.47, 0, 32.128, 0, null));
        list.add(new SurfaceType(true, 0, 7.9, 0, 31.227, 0, null));
        list.add(new SurfaceType(false, -23.365, 1.71, 1.58148, 31.45, 40.9, "G4"));
        list.add(new SurfaceType(false, 311.537, 8.141, 1.6934, 40.2, 53.3, "G5"));
        list.add(new SurfaceType(false, -38.532, 0.074, 0, 40.2, 0, null));
        list.add(new SurfaceType(false, -483.703, 6.009, 1.6516, 39.5, 58.4, "G6"));
        list.add(new SurfaceType(false, -55.749, 0.1, 0, 39.5, 0, null));
        list.add(new SurfaceType(false, 200.86, 3.894, 1.6217, 38.275, 58.54, "G7"));
        list.add(new SurfaceType(false, -106.579, 37.780, 0, 38.275, 0, null));
        return list;
    }

    // Latest contrib 01 June
    private static List<SurfaceType> getSurfacesMerge() {
        List<SurfaceType> list = new ArrayList<>();

        list.add(new SurfaceType(false, 80.166, 7.042, 1.8485, 50.4875, 43.8, "G1"));
        list.add(new SurfaceType(false, 0, 0.189, 0, 50.4875, 0, null));
        list.add(new SurfaceType(false, 33.674, 9.715, 1.69, 44.832, 54.7, "G2"));
        list.add(new SurfaceType(false, 69.936, 1.476, 0, 44.832, 0, null));
        list.add(new SurfaceType(false, 132.0195, 3.02, 1.7783, 42.169, 23.9, "G3"));
        list.add(new SurfaceType(false, 22.46, 8.47, 0, 32.128, 0, null));
        list.add(new SurfaceType(true, 0, 7.9, 0, 31.227, 0, null));
        list.add(new SurfaceType(false, -23.195, 1.71, 1.58148, 31.45, 40.9, "G4"));
        list.add(new SurfaceType(false, 309.045, 8.141, 1.6934, 40.2, 53.3, "G5"));
        list.add(new SurfaceType(false, -38.0435, 0.074, 0, 40.2, 0, null));
        list.add(new SurfaceType(false, -440.322, 6.009, 1.6516, 39.5, 58.4, "G6"));
        list.add(new SurfaceType(false, -54.1584, 0.1, 0, 39.5, 0, null));
        list.add(new SurfaceType(false, 212.35, 3.894, 1.6217, 38.275, 58.54, "G7"));
        list.add(new SurfaceType(false, -100.33, 37.780, 0, 38.275, 0, null));
        return list;
    }

    private static List<SurfaceType> getSurfaces() {
        return getSurfacesMerge();
    }

    private static OpticalSystem.Builder buildSystem(boolean addPointSource, boolean skew) {
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
            PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, direction)
                    .add_spectral_line(SpectralLine.d)
                    .add_spectral_line(SpectralLine.C)
                    .add_spectral_line(SpectralLine.F);
            sys.add(ps);
        }
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<SurfaceType> surfaces = getSurfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double nd = s.nd;
            double vd = s.vd;
            String glassName = s.glassName;
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, nd, vd, glassName, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.angle_of_view(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    static GlassType[] getGlassTypesFew() {
        return new GlassType[]{
                new GlassType("J-SF5", 1.6727, 32.19),    // wakamiya J-SF5
                new GlassType("S-TIM22", 1.64769, 33.79),   // US 4,234,242 50mm f1.8   S-TIM22
                new GlassType("S-TIM28", 1.68893, 31.07),  // shimuzu - f1.2 US 3,738,736 S-TIM28
                new GlassType("S-TIM35", 1.69895, 30.13),   // wakamiya S-TIM35
                new GlassType("S-LAL8", 1.713, 53.87),     // shimuzu - f1.2 US 3,738,736 S-LAL8
                new GlassType("J-SF1", 1.71736, 29.57),   // wakamiya J-SF1
                new GlassType("J-SF10", 1.72825, 28.38),   // shimuzu - f1.2 US 3,738,736 J-SF10
                new GlassType("LAC10", 1.72, 50.4),      // shimuzu - f1.2 US 3,738,736  LAC10
                new GlassType("S-TIH3", 1.74, 28.3),      // wakamiya S-TIH3
                new GlassType("S-TIH13", 1.74077, 27.79),   // wakamiya S-TIH13
                new GlassType("S-LAM2", 1.744, 44.78),     // shimuzu - f1.2 US 3,738,736 S-LAM2
                new GlassType("J-SF4", 1.75520, 27.57),   // shimuzu - f1.2 US 3,738,736  J-SF4
                new GlassType("J-LASFH2", 1.76684, 46.78),   // shimuzu - f1.2 US 3,738,736, US 4,234,242 J-LASFH2
                new GlassType("J-LASF016", 1.7725, 46.62),   // wakamiya J-LASF016
                new GlassType("J-SFS3", 1.78470, 26.27),   // shimuzu - f1.2 US 3,738,736 J-SFS3
                new GlassType("TAF4", 1.788, 47.37),   // wakamiya  S-LAH64 TAF4
                new GlassType("J-LASF017", 1.795, 45.31),   // shimuzu - f1.2 US 3,738,736 J-LASF017?
                new GlassType("TAF2", 1.7945, 45.4),  // 50mm f1.8s US 4234242 TAF2
                new GlassType("J-LASF015", 1.8042, 46.52),   // wakamiya TAF3D    LASF015
                new GlassType("J-LASFH22", 1.8485, 43.79),   // wakamiya  J-LASFH22
                new GlassType("J-LASF08A", 1.883, 40.69)
                };
    }

    static GlassType[] getGlassTypes() {
        var glasses = GlassMap.glasses.values().stream()
                .filter(e ->e.get_manufacturer().equals("Hikari"))
                .filter(e -> e.get_name().startsWith("E-"))
                .filter(e->e.get_refractive_index(SpectralLine.d) >= 1.647 && e.get_refractive_index(SpectralLine.d) < 1.91)
                .map(e->new GlassType(e.get_name(), e.nd, e.vd))
                .collect(Collectors.toList())
                .toArray(new GlassType[0]);
        return glasses;
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
            var system = buildSystem(false,false).build();
            var glassNames = system.glassNames();
            var tracer = new YNUTracer(system, glassNames);
            var glasses = system.glassIndices();
            for (int a = start; a < end; a++) {
                glasses[1] = glassTypes[a].nd;
                for (int b = 0; b < glassTypes.length; b++) {
                    glasses[2] = glassTypes[b].nd;
                    for (int c = 0; c < glassTypes.length; c++) {
                        glasses[3] = glassTypes[c].nd;
                        for (int d = 0; d < glassTypes.length; d++) {
                            glasses[4] = glassTypes[d].nd;
                            for (int e = 0; e < glassTypes.length; e++) {
                                glasses[5] = glassTypes[e].nd;
                                for (int f = 0; f < glassTypes.length; f++) {
                                    glasses[6] = glassTypes[f].nd;
                                    for (int g = 0; g < glassTypes.length; g++) {
                                        glasses[7] = glassTypes[g].nd;
                                        count.incrementAndGet();
                                        try {

                                            //var parax = ParaxialFirstOrderInfo.compute(system);
                                            // Expected H' ppk = 37.5 from front-surface, 20.2 from last surface
                                            // Expected H pp1 = 51.8
                                            // expected H - H1 = 14.3
                                            tracer.setGlasses(glassNames, glasses);
                                            var parax = tracer.compute();

                                            if (parax.effective_focal_length > 57.99 && parax.effective_focal_length < 58.01
                                                    && parax.back_focal_length > 37.77 && parax.back_focal_length < 37.79
                                                    && parax.pp1 > 51.75 && parax.pp1 < 51.85 // 51.8 from first surface
                                                    && parax.ppk > 20.15 && parax.ppk < 20.25) {  // 20.2 from last surface

                                                var sb = new StringBuilder();
                                                sb.append(parax.effective_focal_length).append("\t")
                                                        .append(parax.back_focal_length).append("\t")
                                                        .append(parax.fno).append("\t")
                                                        .append(parax.ppk).append("\t")
                                                        .append(parax.pp1).append("\t");
                                                for (int i = 0; i < glasses.length; i++)
                                                    sb.append(glasses[i]).append("\t");
                                                System.out.println(sb);
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
    }

    public static void main(String[] args) throws Exception {
        var glassTypes = getGlassTypes();
        System.out.println("Trying " + glassTypes.length + " glass types");

        AtomicLong count = new AtomicLong();
        int numThreads = 12; //  glassTypes.length;
        Thread[] threads = new Thread[numThreads];
        int perThreadGlassCount = (int) Math.round((double) glassTypes.length / (double) numThreads);
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
