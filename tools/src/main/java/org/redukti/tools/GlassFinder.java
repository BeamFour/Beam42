package org.redukti.tools;

import org.redukti.rayoptics.seq.Glass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Adds catalog glass suggestions to OpticalBench lens-data rows. */
public final class GlassFinder {
    private static final String LENS_DATA_SECTION = "[lens data]";
    private static final String CANDIDATE_PREFIX = "candidate=";

    private GlassFinder() {}

    public record EnrichmentResult(String text, int selected, int ambiguous, int unmatched) {}

    public static EnrichmentResult enrich(String input) {
        String newline = input.contains("\r\n") ? "\r\n" : "\n";
        boolean endsWithNewline = input.endsWith("\n");
        String[] lines = input.split("\\r?\\n", -1);
        List<String> output = new ArrayList<>(lines.length);
        boolean inLensData = false;
        int selected = 0;
        int ambiguous = 0;
        int unmatched = 0;

        int lineCount = endsWithNewline ? lines.length - 1 : lines.length;
        for (int i = 0; i < lineCount; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]"))
                inLensData = trimmed.equalsIgnoreCase(LENS_DATA_SECTION);

            if (!inLensData || trimmed.isEmpty()) {
                output.add(line);
                continue;
            }

            String[] fields = line.split("\\t", -1);
            if (fields.length < 6 || fields[1].equals("AS") || fields[1].equals("FS") ||
                    fields[1].equals("CG") || !isEmpty(fields, 6) || !isEmpty(fields, 7)) {
                output.add(line);
                continue;
            }

            Double nd = parseDouble(fields[3]);
            Double vd = parseDouble(fields[5]);
            if (nd == null || vd == null || nd == 0.0 || vd == 0.0) {
                output.add(line);
                continue;
            }

            List<Glass.GlassMatch> matches = Glass.find_glasses(nd, vd);
            if (matches.isEmpty()) {
                unmatched++;
                output.add(line);
                continue;
            }

            List<Glass.GlassMatch> exactMatches = matches.stream()
                    .filter(Glass.GlassMatch::exact).toList();
            fields = removeCandidateFields(fields);
            if (!exactMatches.isEmpty() || matches.size() == 1) {
                Glass glass = (!exactMatches.isEmpty() ? exactMatches : matches).get(0).glass();
                fields = ensureLength(fields, 8);
                fields[6] = glass.label;
                fields[7] = glass.catalog_name;
                selected++;
                if (matches.size() > 1)
                    fields = appendCandidates(fields, matches);
            } else {
                fields = ensureLength(fields, 8);
                fields = appendCandidates(fields, matches);
                ambiguous++;
            }
            output.add(String.join("\t", fields));
        }

        String text = String.join(newline, output) + (endsWithNewline ? newline : "");
        return new EnrichmentResult(text, selected, ambiguous, unmatched);
    }

    private static String formatCandidate(Glass.GlassMatch match) {
        Glass glass = match.glass();
        return String.format(Locale.ROOT,
                "candidate=%s/%s,nd=%.5f,vd=%.2f,dnd=%.5f,dvd=%.2f",
                glass.catalog_name, glass.label, glass.nd, glass.vd,
                match.nd_difference(), match.vd_difference());
    }

    private static String[] appendCandidates(String[] fields, List<Glass.GlassMatch> matches) {
        List<String> enriched = new ArrayList<>(Arrays.asList(fields));
        for (Glass.GlassMatch match: matches)
            enriched.add(formatCandidate(match));
        return enriched.toArray(String[]::new);
    }

    private static String[] removeCandidateFields(String[] fields) {
        List<String> retained = new ArrayList<>(fields.length);
        for (String field: fields) {
            if (!field.startsWith(CANDIDATE_PREFIX))
                retained.add(field);
        }
        return retained.toArray(String[]::new);
    }

    private static boolean isEmpty(String[] fields, int index) {
        return index >= fields.length || fields[index].isBlank();
    }

    private static String[] ensureLength(String[] fields, int length) {
        if (fields.length >= length)
            return fields;
        String[] expanded = Arrays.copyOf(fields, length);
        Arrays.fill(expanded, fields.length, length, "");
        return expanded;
    }

    private static Double parseDouble(String value) {
        try {
            return value.isBlank() ? null : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        Path input = null;
        Path output = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--specfile") && i + 1 < args.length)
                input = Path.of(args[++i]);
            else if (args[i].equals("-o") && i + 1 < args.length)
                output = Path.of(args[++i]);
            else {
                usage();
                System.exit(2);
            }
        }
        if (input == null || output == null) {
            usage();
            System.exit(2);
        }

        EnrichmentResult result = enrich(Files.readString(input));
        Files.writeString(output, result.text());
        System.out.printf("Selected %d glass types; %d ambiguous; %d unmatched%n",
                result.selected(), result.ambiguous(), result.unmatched());
    }

    private static void usage() {
        System.err.println("Usage: GlassFinder --specfile input.txt -o output.txt");
    }
}
