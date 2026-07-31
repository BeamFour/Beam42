package org.redukti.rayoptics.layout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

public class SystemLayout2DTest {
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
        SystemLayout2D layout = new SystemLayout2D();
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
}
