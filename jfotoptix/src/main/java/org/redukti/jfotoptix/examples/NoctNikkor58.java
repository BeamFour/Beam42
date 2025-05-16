package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.medium.Abbe;
import org.redukti.jfotoptix.model.Image;
import org.redukti.jfotoptix.model.Lens;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

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

    private static OpticalSystem.Builder buildSystem(GlassType[] glassTypes) {
        OpticalSystem.Builder sys = new OpticalSystem.Builder();
        double imageHeight = 43.28;
        double angleOfView = 40.9 / 2.0;
        double fNum = 1.2;
        /* anchor lens */
        Lens.Builder lens = new Lens.Builder().position(Vector3Pair.position_000_001);
        double image_pos = 0.0;
        List<SurfaceType> surfaces = getSurfaces();
        for (int i = 0; i < surfaces.size(); i++) {
            SurfaceType s = surfaces.get(i);
            double thickness = add_surface(lens, s.radius, s.thickness, s.apertureRadius, s.nd, s.vd, s.isStop);
            image_pos += thickness;
        }
        sys.add(lens);
        Image.Builder image = new Image.Builder().position(new Vector3Pair(new Vector3(0, 0, image_pos), Vector3.vector3_001)).curve(Flat.flat).shape(new Rectangle(imageHeight * 2.));
        sys.add(image);
        sys.angle_of_view(angleOfView);
        sys.f_number(fNum);
        return sys;
    }

    public static void main(String[] args) throws Exception {

        var system = buildSystem(new GlassType[]{}).build();
        System.out.println(system);
        var parax = ParaxialFirstOrderInfo.compute(system);
        System.out.println(parax);
    }
}
