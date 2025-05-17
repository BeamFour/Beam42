package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private static OpticalSystem.Builder buildSystem(double[] glassTypes) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 40.9 / 2.0;
        double fNum = 1.2;
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

    static List<Double> getGlassTypes() {
        Double[] glasses = new Double[]{1.6727, 1.68893, 1.69895, 1.71300, 1.71736, 1.72825, 1.72, 1.74, 1.74077, 1.74430, 1.74443, 1.75520, 1.76684, 1.77279, 1.78470, 1.78797, 1.79631, 1.79668, 1.80218, 1.80411, 1.84042, 1.87739};
        return List.of(glasses);
    }

    public static void main(String[] args) throws Exception {

        var glassTypes = getGlassTypes();
        System.out.println("Trying " + glassTypes.size() + " glass types");
        var glasses = new double[7];
        long count = 0;
        for (int a = 0; a < glassTypes.size(); a++) {
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
                                    var system = buildSystem(glasses).build();
                                    //System.out.println(system);
                                    count++;
                                    try {
                                        var parax = ParaxialFirstOrderInfo.compute(system);
                                        var h_diff = parax.pp1 - parax.ppk;  // H - H'
                                        if (parax.effective_focal_length > 57.0 && parax.effective_focal_length < 58.1 && parax.ppk > 37.85 && parax.ppk < 39.7 && h_diff > 14.1 && h_diff < 14.5) {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append(parax.effective_focal_length).append("\t").append(parax.ppk).append("\t").append(parax.pp1).append("\t").append(parax.ppk - parax.pp1).append("\t");
                                            for (int i = 0; i < glasses.length; i++)
                                                sb.append(glasses[i]).append("\t");
                                            System.out.println(sb.toString());
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
        System.out.println("Processed " + count + " systems");
    }
}
