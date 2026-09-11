package org.redukti.optim;

import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;

import java.util.*;

/**
 * Writes an optimized prescription: the text it was read from, with the values of the
 * varied parameters replaced and nothing else touched, so comments, layout and any
 * {@code [trial n]} sections survive and the result can be reported on or optimized
 * again.
 */
public final class OptimizedPrescriptionWriter {

    private OptimizedPrescriptionWriter() {}

    /**
     * @param text         the prescription text the prescription was built from
     * @param prescription the prescription after optimization
     * @param variables    the variables the optimization varied
     * @return the text with the varied values updated
     */
    public static String write(String text, Prescription prescription, Var[] variables) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        boolean endsWithNewline = text.endsWith("\n");
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\\r?\\n", -1)));
        if (endsWithNewline)
            lines.remove(lines.size() - 1);

        // Find the rows the way the prescription reader does.
        List<Integer> lensRows = new ArrayList<>();
        Map<String, Integer> distanceRows = new HashMap<>();
        Map<String, Integer> asphericRows = new HashMap<>();
        int lensDataEnd = -1;
        int asphericHeader = -1;
        int asphericDataEnd = -1;
        String section = null;
        for (int i = 0; i < lines.size(); i++) {
            String[] words = OptimizationTrial.splitTabs(lines.get(i));
            if (words.length == 0 || words[0].startsWith("#"))
                continue;
            if (words[0].startsWith("[")) {
                section = words[0];
                if (section.equals("[aspherical data]"))
                    asphericHeader = i;
                continue;
            }
            if ("[lens data]".equals(section)) {
                if (words.length >= 2) {
                    lensRows.add(i);
                    lensDataEnd = i + 1;
                }
            }
            else if ("[variable distances]".equals(section)) {
                if (words.length >= 2)
                    distanceRows.putIfAbsent(words[0], i);
            }
            else if ("[aspherical data]".equals(section)) {
                // The reader applies every row, so a repeated surface ends up with the last.
                asphericRows.put(words[0], i);
                asphericDataEnd = i + 1;
            }
        }
        if (lensRows.size() != prescription._surfaces.length)
            throw new IllegalArgumentException("the text has " + lensRows.size()
                    + " [lens data] rows but the prescription has " + prescription._surfaces.length
                    + " surfaces");

        Set<Integer> radii = new TreeSet<>();
        Set<Integer> conics = new TreeSet<>();
        Map<Integer, Set<Integer>> coefficients = new TreeMap<>();
        for (Var variable : variables) {
            if (variable instanceof VarRadius radius) {
                radii.add(radius._surface_id);
                setField(lines, lensRows.get(radius._surface_id), 1,
                        formatRadius(prescription._surfaces[radius._surface_id]._radius));
            }
            else if (variable instanceof VarThickness thickness) {
                SurfaceType surface = prescription._surfaces[thickness._surface_id];
                int row = lensRows.get(thickness._surface_id);
                String token = field(lines, row, 2);
                double value = surface._thickness_by_scenario != null
                        ? surface._thickness_by_scenario[thickness._scenario]
                        : surface._thickness;
                if (OptimizationTrial.isDistanceName(token)) {
                    Integer distanceRow = distanceRows.get(token);
                    if (distanceRow == null)
                        throw new IllegalArgumentException("thickness " + token + " is not in [variable distances]");
                    // A distance that varies by configuration is held per configuration; the
                    // column is that configuration's scenario in the file.
                    int column = 1 + (surface._thickness_by_scenario != null && prescription._configurations != null
                            ? prescription._configurations[thickness._scenario] : 0);
                    setField(lines, distanceRow, column, format(value));
                }
                else
                    setField(lines, row, 2, format(value));
            }
            else if (variable instanceof VarAsphK conic)
                conics.add(conic._surface_id);
            else if (variable instanceof VarAsphCoeff coefficient)
                coefficients.computeIfAbsent(coefficient._surface_id, s -> new TreeSet<>()).add(coefficient._index);
            else
                throw new IllegalArgumentException("cannot write a " + variable.getClass().getSimpleName());
        }

        Set<Integer> aspheric = new TreeSet<>(conics);
        aspheric.addAll(coefficients.keySet());
        List<String> newRows = new ArrayList<>();
        for (int surfaceId : aspheric) {
            SurfaceType surface = prescription._surfaces[surfaceId];
            String label = OptimizationTrial.splitTabs(lines.get(lensRows.get(surfaceId)))[0];
            int offset = coefficientOffset(surface._asph_type);
            Integer row = asphericRows.get(label);
            if (row == null) {
                // A surface the trial made aspheric: every term is written.
                List<String> fields = new ArrayList<>();
                fields.add(label);
                fields.add(formatRadius(surface._radius));
                fields.add(format(surface._k));
                for (int c = offset; c < surface._coeffs.length; c++)
                    fields.add(format(surface._coeffs[c]));
                newRows.add(String.join("\t", fields));
                continue;
            }
            if (conics.contains(surfaceId))
                setField(lines, row, 2, format(surface._k));
            for (int c : coefficients.getOrDefault(surfaceId, Set.of()))
                setField(lines, row, c - offset + 3, format(surface._coeffs[c]));
        }
        // The aspherical data repeats the radius; keep it in step.
        for (int surfaceId : radii) {
            String label = OptimizationTrial.splitTabs(lines.get(lensRows.get(surfaceId)))[0];
            Integer row = asphericRows.get(label);
            if (row != null)
                setField(lines, row, 1, formatRadius(prescription._surfaces[surfaceId]._radius));
        }

        if (!newRows.isEmpty()) {
            if (asphericHeader >= 0)
                lines.addAll(asphericDataEnd >= 0 ? asphericDataEnd : asphericHeader + 1, newRows);
            else {
                List<String> block = new ArrayList<>();
                block.add("[aspherical data]");
                block.addAll(newRows);
                lines.addAll(lensDataEnd, block);
            }
        }
        return String.join(newline, lines) + (endsWithNewline ? newline : "");
    }

    /**
     * Where the first data column's coefficient sits in {@link SurfaceType#_coeffs}: the
     * reader skips the A2 term of an even asphere, and A1 and A2 of an odd one.
     */
    private static int coefficientOffset(int asphereType) {
        return switch (asphereType) {
            case SurfaceType.ASPH_ODD -> 2;
            case SurfaceType.ASPH_EVEN_A2 -> 0;
            default -> 1;
        };
    }

    private static String field(List<String> lines, int row, int column) {
        String[] fields = lines.get(row).split("\t", -1);
        return column < fields.length ? fields[column] : "";
    }

    /** Replaces one tab-separated field, padding with zeros if the row is too short. */
    private static void setField(List<String> lines, int row, int column, String value) {
        List<String> fields = new ArrayList<>(Arrays.asList(lines.get(row).split("\t", -1)));
        while (fields.size() <= column)
            fields.add("0");
        fields.set(column, value);
        lines.set(row, String.join("\t", fields));
    }

    /** The prescription's own spelling of a flat surface. */
    private static String formatRadius(double radius) {
        return radius == 0.0 ? "Infinity" : format(radius);
    }

    /** The shortest decimal that reads back as the same number. */
    static String format(double value) {
        return value == 0.0 ? "0" : Double.toString(value);
    }
}
