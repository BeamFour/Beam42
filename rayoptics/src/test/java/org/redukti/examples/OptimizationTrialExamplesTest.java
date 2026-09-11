package org.redukti.examples;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.Analysis;
import org.redukti.optim.OptimizationBuilder.OptimizationSetup;
import org.redukti.optim.OptimizationTrial;
import org.redukti.spec.Prescription;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The trials in Documentation/OPTIMIZER_SPEC.md build exactly the setups the example
 * programs build in code: the same variables and goals, in the same order, and the same
 * analysis settings.
 */
class OptimizationTrialExamplesTest {

    private static String withTrial(String example, String trial) throws Exception {
        return Files.readString(Path.of(ExampleFinder.geoPathToExample(example))) + "\n" + trial;
    }

    private static Prescription prescription(String text, boolean weighted, boolean dLineOnly) throws Exception {
        var specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(text);
        return Prescription.build_prescription(specs, true, weighted, dLineOnly);
    }

    private static OptimizationSetup trialSetup(String text) throws Exception {
        var trial = OptimizationTrial.parse(text, 1);
        return trial.builder(prescription(text, trial.weighted(), trial.dLineOnly())).build();
    }

    private static void assertSameSetup(OptimizationSetup expected, OptimizationSetup actual) throws Exception {
        assertEquals(describe(expected.variables()), describe(actual.variables()));
        assertEquals(describe(expected.goals()), describe(actual.goals()));
        assertEquals(describe(expected.analysis()), describe(actual.analysis()));
    }

    private static List<String> describe(Object[] items) throws Exception {
        List<String> result = new ArrayList<>();
        for (Object item : items)
            result.add(describe(item));
        return result;
    }

    /**
     * Every plain value an object holds - numbers, flags, enums, strings and arrays of
     * numbers - with its class. References to the prescription, the analysis and other
     * objects are left out: they differ between the two setups by identity only.
     */
    private static String describe(Object item) throws Exception {
        StringBuilder sb = new StringBuilder(item.getClass().getSimpleName()).append('{');
        for (Class<?> c = item.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()))
                    continue;
                Class<?> type = field.getType();
                boolean plain = type.isPrimitive() || type.isEnum() || type == String.class
                        || type == Boolean.class || type == Integer.class || type == Double.class
                        || (type.isArray() && type.getComponentType().isPrimitive());
                if (!plain)
                    continue;
                field.setAccessible(true);
                Object value = field.get(item);
                String text;
                if (value instanceof double[] d) text = Arrays.toString(d);
                else if (value instanceof int[] i) text = Arrays.toString(i);
                else if (value instanceof boolean[] b) text = Arrays.toString(b);
                else text = String.valueOf(value);
                sb.append(field.getName()).append('=').append(text).append(' ');
            }
        }
        return sb.append('}').toString();
    }

    @Test
    void nikkorZ85mmContrast() throws Exception {
        String text = withTrial("Examples/jfotoptix/nikkor-85mm-z-f1.2/US20260086320_Example05.txt", """
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
                """);
        var expected = NikkorZ85mmf12.createContrastSetup(prescription(text, false, false), false, false);
        assertSameSetup(expected, trialSetup(text));
    }

    @Test
    void zeissOtusMtf() throws Exception {
        String text = withTrial("Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt", """
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
                """);
        var expected = ZeissOtusML50mm.createSetup(prescription(text, true, false), true, false);
        assertSameSetup(expected, trialSetup(text));
    }

    @Test
    void noctNikkorSpotSize() throws Exception {
        String text = withTrial("Examples/jfotoptix/nikkor-58mm-f1.2/version5/Noct-Nikkor-58mmf1.2.txt", """
                [trial 1]
                description       RMS spot size, aspherising the front surface
                fields            0 0.3 0.7 1.0
                frequencies       10 30 50

                vary curvatures   all
                vary aspherics    0  K  A4:1e6  A6:1e9  A8:1e11  A10:1e14

                constrain curvatures

                goal spot-rms        15 30 50 70
                goal spot sampling   gaussian 6 12
                goal paraxial        bfl 37.78
                """);
        var expected = NoctNikkor58mm.createSpotSizeSetup(prescription(text, true, false), true, false,
                new double[]{1.0, 1.0, 1.0, 1.0});
        assertSameSetup(expected, trialSetup(text));
    }

    @Test
    void pentaxZoomWideEnd() throws Exception {
        String text = withTrial("Examples/jfotoptix/pentax-80-200mm-f2.8/US005572276_Example05P.txt", """
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
                """);
        var expected = Pentax80200mmf28.createContrastSetup(prescription(text, false, false), false, false);
        assertSameSetup(expected, trialSetup(text));
    }
}
