package org.redukti.rayoptics.layout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.FieldSpec;
import org.redukti.rayoptics.specs.ImageKey;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.PupilSpec;
import org.redukti.rayoptics.specs.ValueKey;
import org.redukti.rayoptics.specs.WvlSpec;
import org.redukti.rayoptics.specs.WvlWt;
import org.redukti.rayoptics.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Layout2DTest {
    @Test
    public void createsStaticElementModel() {
        OpticalModel model = leicaSummicron();
        ElementModel elements = new ElementModel(model);
        long lenses = elements.elements().stream().filter(e -> e.type() == ElementType.LENS).count();
        long stops = elements.elements().stream().filter(e -> e.type() == ElementType.STOP).count();
        long dummies = elements.elements().stream().filter(e -> e.type() == ElementType.DUMMY_INTERFACE).count();
        Assertions.assertEquals(6, lenses);
        Assertions.assertEquals(1, stops);
        Assertions.assertEquals(2, dummies);
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> elements.elements().add(new AirGap(0, model.seq_model.gaps.get(0))));
    }

    @Test
    public void rendersVisualCheckSvgs() throws IOException {
        OpticalModel model = leicaSummicron();
        Layout2D layout = new Layout2D();
        Path output = Path.of("target", "layout-examples");
        Files.createDirectories(output);

        String elements = layout.renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false));
        String reference = layout.renderSvg(model, 1000, 500, new LayoutOptions());
        String fan = layout.renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false).fanRayCount(9));

        Files.writeString(output.resolve("leica-summicron-elements.svg"), elements);
        Files.writeString(output.resolve("leica-summicron-reference-rays.svg"), reference);
        Files.writeString(output.resolve("leica-summicron-ray-fan.svg"), fan);
        Assertions.assertTrue(elements.contains("<polyline"));
        Assertions.assertTrue(reference.length() > elements.length());
        Assertions.assertTrue(fan.length() > elements.length());
        Assertions.assertFalse(reference.contains("NaN"));
        Assertions.assertFalse(reference.contains("Infinity"));
        assertOrthogonalBlackSegments(elements);
    }

    @Test
    public void rendersWideAngleAsphericVisualCheckSvgs() throws IOException {
        OpticalModel model = nikkorWideZoom();
        Layout2D layout = new Layout2D();
        Path output = Path.of("target", "layout-examples");
        Files.createDirectories(output);

        String elements = layout.renderSvg(model, 1200, 600,
                new LayoutOptions().drawReferenceRays(false));
        String reference = layout.renderSvg(model, 1200, 600, new LayoutOptions());
        String fan = layout.renderSvg(model, 1200, 600,
                new LayoutOptions().drawReferenceRays(false).fanRayCount(11));

        Files.writeString(output.resolve("nikkor-wide-zoom-elements.svg"), elements);
        Files.writeString(output.resolve("nikkor-wide-zoom-reference-rays.svg"), reference);
        Files.writeString(output.resolve("nikkor-wide-zoom-ray-fan.svg"), fan);

        long aspherics = model.seq_model.ifcs.stream()
                .filter(ifc -> ifc.profile instanceof EvenPolynomial)
                .count();
        Assertions.assertEquals(4, aspherics);
        Assertions.assertTrue(elements.contains("<polyline"));
        Assertions.assertTrue(reference.length() > elements.length());
        Assertions.assertTrue(fan.length() > elements.length());
        Assertions.assertFalse(reference.contains("NaN"));
        Assertions.assertFalse(reference.contains("Infinity"));
        assertOrthogonalBlackSegments(elements);
    }
    private static void assertOrthogonalBlackSegments(String svg) {
        var line = Pattern.compile("<line x1=\"([^\"]+)\" y1=\"([^\"]+)\" x2=\"([^\"]+)\" y2=\"([^\"]+)\"[^>]*stroke=\"#000000\"").matcher(svg);
        int count = 0;
        while (line.find()) {
            double x1 = Double.parseDouble(line.group(1));
            double y1 = Double.parseDouble(line.group(2));
            double x2 = Double.parseDouble(line.group(3));
            double y2 = Double.parseDouble(line.group(4));
            Assertions.assertTrue(Math.abs(x1 - x2) < 1.0e-9 || Math.abs(y1 - y2) < 1.0e-9,
                    "mechanical edge must be horizontal or vertical");
            count++;
        }
        Assertions.assertTrue(count > 0);
    }
    private static OpticalModel leicaSummicron() {
        OpticalModel model = new OpticalModel();
        SequentialModel sm = model.seq_model;
        OpticalSpecs osp = model.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 2.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle),
                22.5, new double[]{0., 1.}, true, true);
        osp.wvls = new WvlSpec(new WvlWt[]{new WvlWt(587.5618, 1.0)}, 0);
        model.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(42.71, 3.99).rindex(1.73430, 28.19).max_aperture(14.47));
        sm.add_surface(new SurfaceData(195.38, 0.2).max_aperture(13.53));
        sm.add_surface(new SurfaceData(20.5, 7.18).rindex(1.67133, 41.64).max_aperture(12.01));
        sm.add_surface(new SurfaceData(0.0, 1.29).rindex(1.79190, 25.55).max_aperture(10.745));
        sm.add_surface(new SurfaceData(14.94, 5.35).max_aperture(9.195));
        sm.add_surface(new SurfaceData(0.0, 7.61).max_aperture(9.0295));
        sm.set_stop();
        sm.add_surface(new SurfaceData(-14.94, 1.0).rindex(1.65222, 33.60).max_aperture(8.75));
        sm.add_surface(new SurfaceData(0.0, 5.22).rindex(1.79227, 47.15).max_aperture(9.635));
        sm.add_surface(new SurfaceData(-20.5, 0.2).max_aperture(10.19));
        sm.add_surface(new SurfaceData(0.0, 3.69).rindex(1.79227, 47.15).max_aperture(11.48));
        sm.add_surface(new SurfaceData(-42.71, 37.32).max_aperture(11.985));
        sm.do_apertures = false;
        model.update_model();
        VigCalc.set_pupil(model);
        model.update_model();
        return model;
    }
    private static OpticalModel nikkorWideZoom() {
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), 4.0);
        osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), new double[]{0., 57.68}, true);
        osp.wvls = new WvlSpec(
                new WvlWt[]{
                        new WvlWt(486.1327, 0.5),
                        new WvlWt(587.5618, 1.0),
                        new WvlWt(656.2725, 0.5)}, 1);
        opm.system_spec.title = "JP2019-008031 Example 1 (Nikon Nikkor Z 14-30mm f/4 S)";
        opm.system_spec.dimensions = "MM";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        sm.add_surface(new SurfaceData(190.7535,3.0)
                .rindex(1.6937,53.32)
                .max_aperture(29.285));
        sm.add_surface(new SurfaceData(18.8098,9.5)
                .max_aperture(22.485));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(18.8098)
                .cc(-1.0)
                .coefs(new double[]{0.0,-1.33157E-5,-3.07345E-8,6.9126E-11,-3.76684E-14,0.0,0.0});
        sm.add_surface(new SurfaceData(51.563,2.9)
                .rindex(1.6937,53.32)
                .max_aperture(19.205));
        sm.add_surface(new SurfaceData(22.702,9.7)
                .max_aperture(14.475));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(22.702)
                .cc(-1.0)
                .coefs(new double[]{0.0,3.67009E-5,1.37031E-7,-5.20756E-10,3.14884E-12,-5.6153E-15,0.0});
        sm.add_surface(new SurfaceData(-71.0651,1.9)
                .rindex(1.49782,82.57)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(44.4835,0.1)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(32.608,4.5)
                .rindex(1.90265,35.73)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(296.5863,28.616)
                .max_aperture(15.05));
        sm.add_surface(new SurfaceData(63.0604,2.0)
                .rindex(1.59349,67.0)
                .max_aperture(9.04));
        sm.add_surface(new SurfaceData(499.8755,0.1)
                .max_aperture(9.04));
        sm.add_surface(new SurfaceData(24.0057,1.2)
                .rindex(1.883,40.66)
                .max_aperture(9.605));
        sm.add_surface(new SurfaceData(13.347,4.5)
                .rindex(1.56883,56.0)
                .max_aperture(8.54));
        sm.add_surface(new SurfaceData(333.9818,2.5)
                .max_aperture(8.54));
        sm.add_surface(new SurfaceData(0.0,7.483)
                .max_aperture(5.6335));
        sm.set_stop();
        sm.add_surface(new SurfaceData(36.3784,1.1)
                .rindex(1.816,46.59)
                .max_aperture(8.59));
        sm.add_surface(new SurfaceData(14.0097,4.71)
                .rindex(1.51612,64.08)
                .max_aperture(8.42));
        sm.add_surface(new SurfaceData(61.0448,0.2)
                .max_aperture(8.42));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(61.0448)
                .cc(0)
                .coefs(new double[]{0.0,1.75905E-5,-6.64635E-8,2.26551E-10,-4.40763E-12,0.0,0.0});
        sm.add_surface(new SurfaceData(27.9719,3.15)
                .rindex(1.49782,82.57)
                .max_aperture(8.55));
        sm.add_surface(new SurfaceData(-75.3921,0.25)
                .max_aperture(8.55));
        sm.add_surface(new SurfaceData(91.9654,3.05)
                .rindex(1.49782,82.57)
                .max_aperture(8.915));
        sm.add_surface(new SurfaceData(-29.3923,1.579)
                .max_aperture(8.915));
        sm.add_surface(new SurfaceData(72.093,1.0)
                .rindex(1.795,45.31)
                .max_aperture(9.065));
        sm.add_surface(new SurfaceData(20.9929,5.766)
                .max_aperture(9.065));
        sm.add_surface(new SurfaceData(-538.2301,4.8)
                .rindex(1.49782,82.57)
                .max_aperture(10.935));
        sm.add_surface(new SurfaceData(-20.1257,0.1)
                .max_aperture(10.935));
        sm.add_surface(new SurfaceData(-38.9341,1.4)
                .rindex(1.76546,46.75)
                .max_aperture(11.06));
        sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial()
                .r(-38.9341)
                .cc(-1.0)
                .coefs(new double[]{0.0,-2.67902E-5,-3.34364E-8,-1.13765E-10,-1.88017E-13,0.0,0.0});
        sm.add_surface(new SurfaceData(154.832,21.36)
                .max_aperture(11.815));
        sm.do_apertures = false;
        osp.fov.is_wide_angle = true;
        opm.update_model();
        VigCalc.set_vig(opm);
        opm.update_model();
        return opm;
    }
}
