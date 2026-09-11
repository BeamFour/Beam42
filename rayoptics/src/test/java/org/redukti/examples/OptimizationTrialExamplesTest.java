package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.OptimizationBuilder.OptimizationSetup;
import org.redukti.optim.OptimizationTrial;
import org.redukti.spec.Prescription;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.redukti.optim.SetupAssertions.assertSameSetup;

/**
 * The trials in Documentation/OPTIMIZER_SPEC.md build exactly the setups the example
 * programs build in code - the same variables and goals, in the same order, and the same
 * analysis settings - and still do after being written out by the builder and read back.
 */
class OptimizationTrialExamplesTest {

    private static String lens(String example) throws Exception {
        return Files.readString(Path.of(ExampleFinder.geoPathToExample(example)));
    }

    private static Prescription prescription(String text, boolean weighted, boolean dLineOnly) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    /** The trial builds the example's setup, before and after a round trip through toTrial. */
    private static void assertTrialBuilds(String example, String trial, OptimizationSetup expected) throws Exception {
        String lens = lens(example);
        var builder = OptimizationTrial.read(lens + "\n" + trial, 1, true);
        String written = builder.toTrial(1);
        assertSameSetup(expected, builder.build());

        var reread = OptimizationTrial.read(lens + "\n" + written, 1, true);
        assertEquals(written, reread.toTrial(1));
        assertSameSetup(expected, reread.build());
    }

    @Test
    void nikkorZ85mmContrast() throws Exception {
        String example = "Examples/jfotoptix/nikkor-85mm-z-f1.2/US20260086320_Example05.txt";
        var expected = NikkorZ85mmf12.createContrastSetup(prescription(lens(example), false, false), false, false);
        assertTrialBuilds(example, """
                [trial 1]
                description           Contrast, every parameter free
                fields                0 to 1 step 0.1
                frequencies           10 30 50
                weighted              no
                vignetting            set-vig frozen
                check-spot-apertures  no

                vary curvatures       all
                vary thicknesses      all
                vary aspherics        existing

                constrain curvatures
                constrain thicknesses
                constrain edges

                goal contrast         10 30 50
                goal contrast         sag  3 3 3 3 3 3 3 2 2 2 2
                goal contrast         balance  all except 0.9 1.0   weight 1.0
                goal contrast         sampling 6 12
                """, expected);
    }

    @Test
    void zeissOtusMtf() throws Exception {
        String example = "Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt";
        var expected = ZeissOtusML50mm.createSetup(prescription(lens(example), true, false), true, false);
        assertTrialBuilds(example, """
                [trial 1]
                description       MTF targets, selected curvatures and the back focus
                fields            0 0.3 0.7 1.0
                frequencies       10 20 40

                vary curvatures   all except 7 10 24
                vary thicknesses  25
                vary aspherics    existing

                goal mtf   10 sag   93 93 94 93
                goal mtf   10 tan   93 93 90 82
                goal mtf   20 sag   85 85 85 80
                goal mtf   20 tan   85 85 78 62
                goal mtf   40 sag   65 65 64 58
                goal mtf   40 tan   65 62 45 38
                goal ray-aberrations  yes
                """, expected);
    }

    @Test
    void noctNikkorSpotSize() throws Exception {
        String example = "Examples/jfotoptix/nikkor-58mm-f1.2/version5/Noct-Nikkor-58mmf1.2.txt";
        var expected = NoctNikkor58mm.createSpotSizeSetup(prescription(lens(example), true, false), true, false,
                new double[]{1.0, 1.0, 1.0, 1.0});
        assertTrialBuilds(example, """
                [trial 1]
                description       RMS spot size, aspherising the front surface
                fields            0 0.3 0.7 1.0
                frequencies       10 30 50

                vary curvatures   all
                vary aspherics    0  K  1:1e6  2:1e9  3:1e11  4:1e14

                constrain curvatures

                goal spot-rms        15 30 50 70
                goal spot sampling   gaussian 6 12
                goal paraxial        bfl 37.78
                """, expected);
    }

    @Test
    void pentaxZoomWideEnd() throws Exception {
        String example = "Examples/jfotoptix/pentax-80-200mm-f2.8/US005572276_Example05P.txt";
        var expected = Pentax80200mmf28.createContrastSetup(prescription(lens(example), false, false), false, false);
        assertTrialBuilds(example, """
                [trial 1]
                description           Moving groups at the wide end
                configuration         0
                fields                0 to 1 step 0.1
                frequencies           10 30 50
                weighted              no
                d-line-only           no
                vignetting            set-vig frozen
                check-spot-apertures  no

                vary thicknesses      7 14 19

                goal contrast         10 30 50
                goal contrast         balance  all except 0 0.9 1.0   weight 1.0
                goal contrast         sampling 6 12
                """, expected);
    }
}
