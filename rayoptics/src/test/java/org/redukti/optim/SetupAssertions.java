package org.redukti.optim;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Compares optimization setups by what they hold rather than by identity. */
public final class SetupAssertions {

    private SetupAssertions() {}

    /** The same variables and goals, in the same order, and the same analysis settings. */
    public static void assertSameSetup(OptimizationBuilder.OptimizationSetup expected,
                                       OptimizationBuilder.OptimizationSetup actual) throws Exception {
        assertEquals(describe(expected.variables()), describe(actual.variables()));
        assertEquals(describe(expected.goals()), describe(actual.goals()));
        assertEquals(describe(expected.analysis()), describe(actual.analysis()));
    }

    public static List<String> describe(Object[] items) throws Exception {
        List<String> result = new ArrayList<>();
        for (Object item : items)
            result.add(describe(item));
        return result;
    }

    /**
     * Every plain value an object holds - numbers, flags, enums, strings and arrays of
     * numbers - with its class. References to the prescription, the analysis and other
     * objects are left out: they differ between two setups by identity only.
     */
    public static String describe(Object item) throws Exception {
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
}
