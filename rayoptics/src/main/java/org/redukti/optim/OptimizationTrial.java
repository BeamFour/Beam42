package org.redukti.optim;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;
import org.redukti.util.Args;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a {@code [trial n]} section of a prescription file into an
 * {@link OptimizationBuilder}; {@link OptimizationBuilder#toTrial(int)} writes one back.
 * The format is documented in Documentation/OPTIMIZER.md.
 *
 * <p>Values mean what they mean to the builder: surfaces are its zero-based positions in
 * [lens data], aspheric coefficients its indices into the coefficient array, and so on.
 * Everything the text alone can settle - syntax, per-field value counts, surface numbers -
 * is checked before the builder is made, and a problem is reported with its line number.
 */
public final class OptimizationTrial {

    /** A problem with a trial, reported against the line it was found on. */
    public static final class TrialException extends IllegalArgumentException {
        public TrialException(String message) {
            super(message);
        }
    }

    private static final Pattern TRIAL_HEADER =
            Pattern.compile("\\[\\s*trial\\s+(\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    private static final Pattern PIPELINE_HEADER =
            Pattern.compile("\\[\\s*pipeline\\s+(\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    /** First-order quantities a paraxial goal can name, in {@link ParaxHelper} ids. */
    private static final Map<String, Integer> PARAXIAL_QUANTITIES = new LinkedHashMap<>();
    static {
        PARAXIAL_QUANTITIES.put("efl", ParaxHelper.Effective_focal_length);
        PARAXIAL_QUANTITIES.put("bfl", ParaxHelper.Back_focal_length);
        PARAXIAL_QUANTITIES.put("ffl", ParaxHelper.Ffl);
        PARAXIAL_QUANTITIES.put("fno", ParaxHelper.Fno);
        PARAXIAL_QUANTITIES.put("img-dist", ParaxHelper.Image_distance);
        PARAXIAL_QUANTITIES.put("obj-dist", ParaxHelper.Object_distance);
        PARAXIAL_QUANTITIES.put("pp1", ParaxHelper.Pp1);
        PARAXIAL_QUANTITIES.put("ppk", ParaxHelper.Ppk);
        PARAXIAL_QUANTITIES.put("enp-dist", ParaxHelper.Enp_dist);
        PARAXIAL_QUANTITIES.put("enp-radius", ParaxHelper.Enp_radius);
        PARAXIAL_QUANTITIES.put("exp-dist", ParaxHelper.Exp_dist);
        PARAXIAL_QUANTITIES.put("exp-radius", ParaxHelper.Exp_radius);
        PARAXIAL_QUANTITIES.put("img-ht", ParaxHelper.Img_ht);
        PARAXIAL_QUANTITIES.put("obj-ang", ParaxHelper.Obj_ang);
        PARAXIAL_QUANTITIES.put("obj-na", ParaxHelper.Obj_na);
        PARAXIAL_QUANTITIES.put("img-na", ParaxHelper.Img_na);
        PARAXIAL_QUANTITIES.put("red", ParaxHelper.Red);
        PARAXIAL_QUANTITIES.put("power", ParaxHelper.Power);
        PARAXIAL_QUANTITIES.put("opt-inv", ParaxHelper.Optical_invariant);
    }

    private OptimizationTrial() {}

    /**
     * Reads trial {@code number} from the text of a prescription file into a builder for
     * that prescription, built from the same text with the trial's wavelength settings.
     *
     * @throws TrialException if the file has no such trial, or the trial has a problem
     */
    public static OptimizationBuilder read(String text, int number, boolean useGlassTypes) throws Exception {
        return parse(text, number).createBuilder(text, useGlassTypes);
    }

    /** Parse a reusable definition without importing glass or constructing a prescription. */
    public static TrialDefinition parse(String text, int number) {
        return new TrialDefinition(Reader.parse(text, number));
    }

    /**
     * Parsed trial settings, reusable across pipeline stages. Canonical configuration is
     * resolved once; the private reader retains source locations for surface-dependent
     * diagnostics. Each application creates a fresh builder and stage state.
     */
    public static final class TrialDefinition {
        private final Reader settings;
        private final OptimizationConfiguration configuration;

        private TrialDefinition(Reader settings) {
            this.settings = settings;
            this.configuration = settings.configuration();
        }

        public int number() {
            return settings.number;
        }

        /** Build the current design using this trial's wavelength settings. */
        public OptimizationBuilder createBuilder(String prescriptionText, boolean useGlassTypes) throws Exception {
            var specs = new OpticalBenchDataImporter.LensSpecifications();
            specs.parse_buffer(prescriptionText);
            var prescription = Prescription.build_prescription(
                    specs, useGlassTypes, configuration.weighted, configuration.dLineOnly);
            return settings.apply(prescription, configuration);
        }

        /**
         * Write canonical trial text, including effective defaults, without constructing
         * a prescription, builder or solver. Surface-dependent checks run when creating a stage.
         */
        public String toTrial() {
            return configuration.toTrial(number());
        }

        /** Compatibility overload that also performs prescription-dependent validation. */
        public String toTrial(Prescription prescription) {
            return settings.apply(prescription, configuration).toTrial(number());
        }
    }

    /**
     * Reads {@code [pipeline number]}, or returns null when the number names a trial rather
     * than a pipeline: the two share one numbering.
     *
     * @throws TrialException if the file defines neither, if both claim the number, or if
     *                        the pipeline has a problem
     */
    public static OptimizationPipeline readPipeline(String text, int number) {
        var index = new SectionIndex(text);
        var trials = index.trials;
        var pipelines = index.pipelines;
        Integer start = pipelines.get(number);
        if (start == null) {
            if (trials.containsKey(number))
                return null;
            throw new TrialException("there is no [trial " + number + "] or [pipeline " + number
                    + "] in this prescription; it defines " + defined("trial", trials) + " and "
                    + defined("pipeline", pipelines));
        }
        return readPipeline(index.lines, start - 1, number, trials.keySet(), pipelines.keySet());
    }

    /** Shared header validation. Map values are one-based source lines for diagnostics. */
    private static final class SectionIndex {
        final String[] lines;
        final Map<Integer, Integer> trials = new TreeMap<>();
        final Map<Integer, Integer> pipelines = new TreeMap<>();

        SectionIndex(String text) {
            lines = text.split("\\r?\\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String header = lines[i].trim();
                if (!header.startsWith("["))
                    continue;
                index(header, i + 1, "trial", TRIAL_HEADER, trials);
                index(header, i + 1, "pipeline", PIPELINE_HEADER, pipelines);
            }
            for (int both : pipelines.keySet())
                if (trials.containsKey(both))
                    throw new TrialException("the number " + both + " is used by both [trial " + both
                            + "] at line " + trials.get(both) + " and [pipeline " + both + "] at line "
                            + pipelines.get(both) + "; trials and pipelines share one numbering");
        }

        private void index(String header, int line, String kind, Pattern pattern,
                           Map<Integer, Integer> sections) {
            Matcher matcher = pattern.matcher(header);
            if (!matcher.matches()) {
                if (lower(header.substring(1).stripLeading()).startsWith(kind))
                    throw new TrialException("line " + line + ": expected [" + kind
                            + " <number>], found " + header);
                return;
            }
            int number;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new TrialException("line " + line + ": " + kind + " number is out of range: "
                        + matcher.group(1));
            }
            Integer earlier = sections.put(number, line);
            if (earlier != null)
                throw new TrialException("[" + kind + " " + number + "] is defined twice, at lines "
                        + earlier + " and " + line);
        }
    }

    private static String defined(String what, Map<Integer, Integer> numbers) {
        if (numbers.isEmpty())
            return "no " + what + "s";
        return (numbers.size() == 1 ? what + " " : what + "s ")
                + String.join(", ", numbers.keySet().stream().map(String::valueOf).toList());
    }

    private static OptimizationPipeline readPipeline(String[] lines, int start, int number,
                                                     Set<Integer> trials, Set<Integer> pipelines) {
        String description = null;
        String outdir = null;
        int[] stages = null;
        int stagesLine = 0;
        Set<String> seen = new HashSet<>();
        for (int i = start + 1; i < lines.length && !lines[i].trim().startsWith("["); i++) {
            int hash = lines[i].indexOf('#');
            String text = (hash >= 0 ? lines[i].substring(0, hash) : lines[i]).trim();
            if (text.isEmpty())
                continue;
            int line = i + 1;
            String[] w = text.split("\\s+");
            String keyword = lower(w[0]);
            if (!seen.add(keyword))
                throw pipelineError(number, line, "'" + keyword + "' is given more than once");
            switch (keyword) {
                case "description" -> description = text.substring(w[0].length()).trim();
                case "outdir" -> {
                    if (w.length < 2)
                        throw pipelineError(number, line, "expected 'outdir <directory>'");
                    outdir = text.substring(w[0].length()).trim();
                }
                case "trials" -> {
                    if (w.length < 2)
                        throw pipelineError(number, line, "expected 'trials <number> <number> ...'");
                    stages = new int[w.length - 1];
                    stagesLine = line;
                    for (int t = 1; t < w.length; t++) {
                        int stage;
                        try {
                            stage = Integer.parseInt(w[t]);
                        }
                        catch (NumberFormatException e) {
                            throw pipelineError(number, line, "expected a trial number, found '" + w[t] + "'");
                        }
                        if (pipelines.contains(stage))
                            throw pipelineError(number, line, "stage " + stage
                                    + " is a pipeline; a pipeline runs trials, not other pipelines");
                        if (!trials.contains(stage))
                            throw pipelineError(number, line, "there is no [trial " + stage + "] in this prescription");
                        stages[t - 1] = stage;
                    }
                }
                default -> throw pipelineError(number, line, "unknown keyword '" + w[0]
                        + "'; a pipeline takes description, outdir and trials");
            }
        }
        if (stages == null)
            throw new TrialException("pipeline " + number + ": 'trials' is required, naming the trials to run in order");
        if (stages.length == 0)
            throw pipelineError(number, stagesLine, "a pipeline needs at least one trial");
        return new OptimizationPipeline(number, description, outdir, stages);
    }

    private static TrialException pipelineError(int number, int line, String message) {
        return new TrialException("pipeline " + number + ", line " + line + ": " + message);
    }

    /** How a variable is named when a trial runs: by surface position, as in the trial. */
    public static String describe(Var variable) {
        if (variable instanceof VarRadius radius)
            return "surface " + radius._surface_id + " radius";
        if (variable instanceof VarThickness thickness)
            return "surface " + thickness._surface_id + " thickness";
        if (variable instanceof VarAsphK conic)
            return "surface " + conic._surface_id + " K";
        if (variable instanceof VarAsphCoeff coefficient)
            return "surface " + coefficient._surface_id + " coefficient " + coefficient._index;
        return variable.toString();
    }

    // ------------------------------------------------------------------
    // Shared with OptimizationBuilder.toTrial
    // ------------------------------------------------------------------

    static String paraxialName(int paraxId) {
        for (var entry : PARAXIAL_QUANTITIES.entrySet())
            if (entry.getValue() == paraxId)
                return entry.getKey();
        throw new IllegalArgumentException("paraxial quantity " + ParaxHelper.Names[paraxId]
                + " has no name in a trial");
    }

    /** A number as a trial writes it: whole numbers without a fraction, others as Java does. */
    static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e7)
            return Long.toString((long) value);
        return Double.toString(value);
    }

    static String format(double[] values) {
        List<String> result = new ArrayList<>();
        for (double value : values)
            result.add(format(value));
        return String.join(" ", result);
    }

    static String format(int[] values) {
        List<String> result = new ArrayList<>();
        for (int value : values)
            result.add(Integer.toString(value));
        return String.join(" ", result);
    }

    /** One line of a trial or pipeline: the keyword padded to a column, then its values. */
    static void line(StringBuilder sb, String key, String values) {
        sb.append(key);
        int padding = Math.max(1, 22 - key.length());
        for (int i = 0; i < padding; i++)
            sb.append(' ');
        sb.append(values).append('\n');
    }

    /** SetPupil as set-pupil: the spelling --vig-type and a trial use. */
    static String kebab(String name) {
        var sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0)
                sb.append('-');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /** Splits a line on tabs exactly as the prescription reader does. */
    static String[] splitTabs(String line) {
        List<String> words = new ArrayList<>();
        while (line.length() > 0) {
            int pos = line.indexOf('\t');
            if (pos < 0) {
                words.add(line);
                break;
            }
            words.add(line.substring(0, pos));
            line = line.substring(pos + 1);
        }
        return words.toArray(new String[0]);
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /** Values given one per field, kept with their line until the field count is known. */
    private record PerField(double[] values, int line) {}

    private enum SelectionKind { ALL, ALL_EXCEPT, LIST }

    private record Selection(SelectionKind kind, int[] surfaces) {}

    /** One aspheric term: the conic constant when index is -1, else a coefficient index. */
    private record Term(int index, Double scale) {}

    private record AsphericRow(int surface, List<Term> terms, int line) {}

    private record ParaxialGoal(int id, double target, double weight) {}

    private static final class MtfRows {
        final int line;
        PerField sagittal, tangential, weights, sagittalWeights, tangentialWeights;

        MtfRows(int line) {
            this.line = line;
        }
    }

    /** One trial's settings as read, before they are applied to a builder. */
    private static final class Reader {
        private final int number;
        /** Radius column of each [lens data] row: surface numbers are positions in it. */
        private final List<String> radii;
        private final Set<String> seen = new HashSet<>();

        private String description;
        private String outdir;
        private int configuration = 0;
        private double[] fields;
        private int[] frequencies;
        private boolean weighted = true;
        private boolean dLineOnly = false;
        private VigType vignetting;
        private boolean freezeVignetting;
        private Boolean checkSpotApertures;

        private Selection curvatures;
        private Selection thicknesses;
        private boolean existingAspherics;
        private final List<AsphericRow> asphericRows = new ArrayList<>();

        private Double curvatureConstraint;
        private int curvatureConstraintLine;
        private Double thicknessConstraint;
        private Double edgeConstraint;

        private int[] contrastFrequencies;
        private PerField contrastSagittal;
        private PerField contrastTangential;
        private final Map<Integer, PerField> contrastSagittalFor = new LinkedHashMap<>();
        private final Map<Integer, PerField> contrastTangentialFor = new LinkedHashMap<>();
        private List<String> balanceFields;
        private int balanceLine;
        private double balanceWeight = OptimizationBuilder.NOMINAL_BALANCE_WEIGHT;
        private int[] contrastSampling;
        private Boolean calibrateContrast;
        private Boolean exitPupilAiming;
        private Boolean centerContrast;
        private int contrastSettingsLine;

        private final Map<Integer, MtfRows> mtf = new LinkedHashMap<>();

        private PerField spotRms;
        private PerField spotRmsWeights;
        private PerField spotMaxRadius;
        private PerField spotMaxRadiusWeights;
        private PerField spotDeviation;
        private PerField spotDeviationX;
        private PerField spotDeviationY;
        private int[] gaussianSampling;
        private double gaussianInnerRadius;
        private Integer hexapolarRays;

        private boolean rayAberrations;
        private final List<ParaxialGoal> paraxialGoals = new ArrayList<>();

        private Reader(int number, List<String> radii) {
            this.number = number;
            this.radii = radii;
        }

        static Reader parse(String text, int number) {
            var index = new SectionIndex(text);
            String[] lines = index.lines;

            // The lens data as the prescription reader sees it.
            List<String> radii = new ArrayList<>();
            String section = null;
            for (String line : lines) {
                String[] words = splitTabs(line);
                if (words.length == 0 || words[0].startsWith("#"))
                    continue;
                if (words[0].startsWith("[")) {
                    section = words[0];
                    continue;
                }
                if ("[lens data]".equals(section) && words.length >= 2)
                    radii.add(words[1]);
            }

            var headers = index.trials;
            if (!headers.containsKey(number))
                throw new TrialException("there is no [trial " + number + "] in this prescription; "
                        + (headers.isEmpty() ? "it defines no trials"
                        : "it defines " + (headers.size() == 1 ? "trial " : "trials ")
                        + String.join(", ", headers.keySet().stream().map(String::valueOf).toList())));

            var reader = new Reader(number, radii);
            // The stored one-based header line is the zero-based first body line.
            for (int i = headers.get(number); i < lines.length && !lines[i].trim().startsWith("["); i++)
                reader.read(i + 1, lines[i]);
            reader.finish();
            return reader;
        }

        private void read(int line, String raw) {
            int hash = raw.indexOf('#');
            String text = (hash >= 0 ? raw.substring(0, hash) : raw).trim();
            if (text.isEmpty())
                return;
            String[] w = text.split("\\s+");
            switch (lower(w[0])) {
                case "description" -> {
                    once(line, "description");
                    description = text.substring(w[0].length()).trim();
                }
                case "outdir" -> {
                    once(line, "outdir");
                    if (w.length < 2)
                        throw error(line, "expected 'outdir <directory>'");
                    outdir = text.substring(w[0].length()).trim();
                }
                case "configuration" -> {
                    once(line, "configuration");
                    count(line, w, 2, "configuration <number>");
                    configuration = nonNegativeInt(line, w[1], "configuration");
                }
                case "fields" -> {
                    once(line, "fields");
                    fields = fieldList(line, w);
                }
                case "frequencies" -> {
                    once(line, "frequencies");
                    frequencies = positiveInts(line, w, 1, "frequencies");
                }
                case "weighted" -> {
                    once(line, "weighted");
                    weighted = yesNo(line, w, 1);
                }
                case "d-line-only" -> {
                    once(line, "d-line-only");
                    dLineOnly = yesNo(line, w, 1);
                }
                case "check-spot-apertures" -> {
                    once(line, "check-spot-apertures");
                    checkSpotApertures = yesNo(line, w, 1);
                }
                case "vignetting" -> {
                    once(line, "vignetting");
                    if (w.length < 2 || w.length > 3)
                        throw error(line, "expected 'vignetting <type> [frozen]'");
                    try {
                        vignetting = Args.parse_vig_type(w[1]);
                    }
                    catch (IllegalArgumentException e) {
                        throw error(line, "unknown vignetting type '" + w[1] + "', expected one of: "
                                + Args.vig_type_names());
                    }
                    if (w.length == 3) {
                        if (!lower(w[2]).equals("frozen"))
                            throw error(line, "expected 'frozen' after the vignetting type, found '" + w[2] + "'");
                        freezeVignetting = true;
                    }
                }
                case "vary" -> vary(line, w);
                case "constrain" -> constrain(line, w);
                case "goal" -> goal(line, w);
                default -> throw error(line, "unknown keyword '" + w[0] + "'");
            }
        }

        private void vary(int line, String[] w) {
            if (w.length < 3)
                throw error(line, "expected 'vary curvatures|thicknesses|aspherics ...'");
            switch (lower(w[1])) {
                case "curvatures" -> {
                    once(line, "vary curvatures");
                    curvatures = selection(line, w, true);
                }
                case "thicknesses" -> {
                    once(line, "vary thicknesses");
                    thicknesses = selection(line, w, false);
                }
                case "aspherics" -> aspherics(line, w);
                default -> throw error(line, "cannot vary '" + w[1] + "'; expected curvatures, thicknesses or aspherics");
            }
        }

        private Selection selection(int line, String[] w, boolean curvature) {
            if (lower(w[2]).equals("all")) {
                if (w.length == 3)
                    return new Selection(SelectionKind.ALL, new int[0]);
                if (w.length > 4 && lower(w[3]).equals("except"))
                    return new Selection(SelectionKind.ALL_EXCEPT, surfaces(line, w, 4, false));
                throw error(line, "expected 'all' or 'all except <surfaces>'");
            }
            return new Selection(SelectionKind.LIST, surfaces(line, w, 2, curvature));
        }

        private int[] surfaces(int line, String[] w, int from, boolean curvature) {
            int[] result = new int[w.length - from];
            Set<Integer> unique = new HashSet<>();
            for (int i = from; i < w.length; i++) {
                int surface = surface(line, w[i]);
                if (!unique.add(surface))
                    throw error(line, "surface " + surface + " is listed twice");
                if (curvature && isStop(surface))
                    throw error(line, "surface " + surface + " is a stop; it has no curvature to vary");
                result[i - from] = surface;
            }
            return result;
        }

        /**
         * A surface number: the row's position in [lens data], counting from 0 as
         * {@link OptimizationBuilder} does. The ids in the file's first column are not used.
         */
        private int surface(int line, String value) {
            int surface;
            try {
                surface = Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
                throw error(line, "expected a surface number, found '" + value
                        + "'; surfaces are numbered by their position in [lens data], from 0");
            }
            if (surface < 0 || surface >= radii.size())
                throw error(line, "there is no surface " + value + "; [lens data] has surfaces 0 to "
                        + (radii.size() - 1));
            return surface;
        }

        private boolean isStop(int surface) {
            String radius = radii.get(surface);
            return radius.equals("AS") || radius.equals("FS");
        }

        private void aspherics(int line, String[] w) {
            if (w.length == 3 && lower(w[2]).equals("existing")) {
                once(line, "vary aspherics existing");
                existingAspherics = true;
                return;
            }
            if (w.length < 4)
                throw error(line, "expected 'vary aspherics existing' or 'vary aspherics <surface> <terms>'");
            int surface = surface(line, w[2]);
            once(line, "vary aspherics " + surface);
            List<Term> terms = new ArrayList<>();
            Set<Integer> indices = new HashSet<>();
            for (int i = 3; i < w.length; i++) {
                String token = w[i];
                int colon = token.indexOf(':');
                String name = colon >= 0 ? token.substring(0, colon) : token;
                Double scale = colon >= 0 ? positive(line, token.substring(colon + 1), "scale of " + name) : null;
                int index;
                if (lower(name).equals("k")) {
                    if (scale != null)
                        throw error(line, "K takes no scale");
                    index = -1;
                }
                else {
                    try {
                        index = Integer.parseInt(name);
                    }
                    catch (NumberFormatException e) {
                        throw error(line, "expected K or a coefficient index, found '" + name + "'");
                    }
                    if (index < 0)
                        throw error(line, "coefficient indices start at 0, found " + name);
                }
                if (!indices.add(index))
                    throw error(line, (index < 0 ? "K" : "coefficient " + index) + " is listed twice");
                terms.add(new Term(index, scale));
            }
            asphericRows.add(new AsphericRow(surface, terms, line));
        }

        private void constrain(int line, String[] w) {
            if (w.length < 2 || w.length > 3)
                throw error(line, "expected 'constrain curvatures|thicknesses|edges [weight]'");
            double weight = w.length == 3 ? nonNegative(line, w[2], "constraint weight") : 1.0;
            switch (lower(w[1])) {
                case "curvatures" -> {
                    once(line, "constrain curvatures");
                    curvatureConstraint = weight;
                    curvatureConstraintLine = line;
                }
                case "thicknesses" -> {
                    once(line, "constrain thicknesses");
                    thicknessConstraint = weight;
                }
                case "edges" -> {
                    once(line, "constrain edges");
                    edgeConstraint = weight;
                }
                default -> throw error(line, "cannot constrain '" + w[1] + "'; expected curvatures, thicknesses or edges");
            }
        }

        private void goal(int line, String[] w) {
            if (w.length < 3)
                throw error(line, "expected 'goal <type> ...'");
            switch (lower(w[1])) {
                case "contrast" -> contrastGoal(line, w);
                case "mtf" -> mtfGoal(line, w);
                case "spot-rms" -> {
                    if (lower(w[2]).equals("weights")) {
                        once(line, "goal spot-rms weights");
                        spotRmsWeights = new PerField(numbers(line, w, 3), line);
                    }
                    else {
                        once(line, "goal spot-rms");
                        spotRms = new PerField(numbers(line, w, 2), line);
                    }
                }
                case "spot-max-radius" -> {
                    if (lower(w[2]).equals("weights")) {
                        once(line, "goal spot-max-radius weights");
                        spotMaxRadiusWeights = new PerField(numbers(line, w, 3), line);
                    }
                    else {
                        once(line, "goal spot-max-radius");
                        spotMaxRadius = new PerField(numbers(line, w, 2), line);
                    }
                }
                case "spot-deviation" -> {
                    switch (lower(w[2])) {
                        case "x" -> {
                            once(line, "goal spot-deviation x");
                            spotDeviationX = new PerField(numbers(line, w, 3), line);
                        }
                        case "y" -> {
                            once(line, "goal spot-deviation y");
                            spotDeviationY = new PerField(numbers(line, w, 3), line);
                        }
                        default -> {
                            once(line, "goal spot-deviation");
                            spotDeviation = new PerField(numbers(line, w, 2), line);
                        }
                    }
                }
                case "spot" -> spotSampling(line, w);
                case "ray-aberrations" -> {
                    once(line, "goal ray-aberrations");
                    rayAberrations = yesNo(line, w, 2);
                }
                case "paraxial" -> paraxialGoal(line, w);
                default -> throw error(line, "unknown goal '" + w[1] + "'; expected contrast, mtf, spot-rms, "
                        + "spot-max-radius, spot-deviation, spot, ray-aberrations or paraxial");
            }
        }

        private void contrastGoal(int line, String[] w) {
            switch (lower(w[2])) {
                case "sag" -> {
                    once(line, "goal contrast sag");
                    contrastSagittal = new PerField(numbers(line, w, 3), line);
                }
                case "tan" -> {
                    once(line, "goal contrast tan");
                    contrastTangential = new PerField(numbers(line, w, 3), line);
                }
                case "balance" -> {
                    once(line, "goal contrast balance");
                    List<String> tokens = new ArrayList<>(Arrays.asList(w).subList(3, w.length));
                    int n = tokens.size();
                    if (n >= 2 && lower(tokens.get(n - 2)).equals("weight")) {
                        balanceWeight = nonNegative(line, tokens.get(n - 1), "balance weight");
                        tokens = tokens.subList(0, n - 2);
                    }
                    if (tokens.isEmpty())
                        throw error(line, "expected the fields to balance: all, all except <fields>, or yes/no per field");
                    balanceFields = tokens;
                    balanceLine = line;
                }
                case "sampling" -> {
                    once(line, "goal contrast sampling");
                    count(line, w, 5, "goal contrast sampling <rings> <spokes>");
                    contrastSampling = new int[]{positiveInt(line, w[3], "rings"), positiveInt(line, w[4], "spokes")};
                    contrastSettingsLine = line;
                }
                case "calibrate" -> {
                    once(line, "goal contrast calibrate");
                    calibrateContrast = yesNo(line, w, 3);
                    contrastSettingsLine = line;
                }
                case "exit-pupil-aiming" -> {
                    once(line, "goal contrast exit-pupil-aiming");
                    exitPupilAiming = yesNo(line, w, 3);
                    contrastSettingsLine = line;
                }
                case "centering" -> {
                    once(line, "goal contrast centering");
                    centerContrast = yesNo(line, w, 3);
                    contrastSettingsLine = line;
                }
                default -> {
                    if (w.length >= 4 && (lower(w[3]).equals("sag") || lower(w[3]).equals("tan"))) {
                        int frequency = positiveInt(line, w[2], "contrast frequency");
                        boolean sagittal = lower(w[3]).equals("sag");
                        once(line, "goal contrast " + frequency + " " + lower(w[3]));
                        var values = new PerField(numbers(line, w, 4), line);
                        (sagittal ? contrastSagittalFor : contrastTangentialFor).put(frequency, values);
                    }
                    else {
                        once(line, "goal contrast");
                        contrastFrequencies = positiveInts(line, w, 2, "contrast frequencies");
                    }
                }
            }
        }

        private void mtfGoal(int line, String[] w) {
            if (w.length < 5)
                throw error(line, "expected 'goal mtf <frequency> sag|tan [weights] <values>' or 'goal mtf <frequency> weights <values>'");
            int frequency = positiveInt(line, w[2], "MTF frequency");
            MtfRows rows = mtf.computeIfAbsent(frequency, f -> new MtfRows(line));
            String what = lower(w[3]);
            if (what.equals("weights")) {
                once(line, "goal mtf " + frequency + " weights");
                rows.weights = new PerField(numbers(line, w, 4), line);
            }
            else if (what.equals("sag") || what.equals("tan")) {
                boolean sagittal = what.equals("sag");
                if (lower(w[4]).equals("weights")) {
                    once(line, "goal mtf " + frequency + " " + what + " weights");
                    var values = new PerField(numbers(line, w, 5), line);
                    if (sagittal) rows.sagittalWeights = values;
                    else rows.tangentialWeights = values;
                }
                else {
                    once(line, "goal mtf " + frequency + " " + what);
                    var values = new PerField(numbers(line, w, 4), line);
                    if (sagittal) rows.sagittal = values;
                    else rows.tangential = values;
                }
            }
            else
                throw error(line, "expected sag, tan or weights after the MTF frequency, found '" + w[3] + "'");
        }

        private void spotSampling(int line, String[] w) {
            if (w.length < 4 || !lower(w[2]).equals("sampling"))
                throw error(line, "expected 'goal spot sampling gaussian <rings> <spokes> [<inner radius>]' or 'goal spot sampling hexapolar <rays>'");
            switch (lower(w[3])) {
                case "gaussian" -> {
                    once(line, "goal spot sampling gaussian");
                    if (w.length != 6 && w.length != 7)
                        throw error(line, "expected 'goal spot sampling gaussian <rings> <spokes> [<inner radius>]'");
                    gaussianSampling = new int[]{positiveInt(line, w[4], "rings"), positiveInt(line, w[5], "spokes")};
                    gaussianInnerRadius = w.length == 7 ? nonNegative(line, w[6], "inner radius") : 0.0;
                }
                case "hexapolar" -> {
                    once(line, "goal spot sampling hexapolar");
                    count(line, w, 5, "goal spot sampling hexapolar <rays>");
                    hexapolarRays = positiveInt(line, w[4], "rays");
                }
                default -> throw error(line, "unknown spot sampling '" + w[3] + "'; expected gaussian or hexapolar");
            }
        }

        private void paraxialGoal(int line, String[] w) {
            if (w.length != 4 && w.length != 6)
                throw error(line, "expected 'goal paraxial <quantity> <target> [weight <w>]'");
            String quantity = lower(w[2]);
            Integer id = PARAXIAL_QUANTITIES.get(quantity);
            if (id == null)
                throw error(line, "unknown paraxial quantity '" + w[2] + "'; expected one of: "
                        + String.join(", ", PARAXIAL_QUANTITIES.keySet()));
            once(line, "goal paraxial " + quantity);
            double target = number(line, w[3], "paraxial target");
            double weight = 1.0;
            if (w.length == 6) {
                if (!lower(w[4]).equals("weight"))
                    throw error(line, "expected 'weight' after the target, found '" + w[4] + "'");
                weight = nonNegative(line, w[5], "paraxial weight");
            }
            paraxialGoals.add(new ParaxialGoal(id, target, weight));
        }

        /** Checks that need the whole trial: per-field counts, and rows that depend on others. */
        private void finish() {
            if (fields == null)
                throw new TrialException("trial " + number + ": 'fields' is required");
            if (frequencies == null)
                throw new TrialException("trial " + number + ": 'frequencies' is required");

            checkPerField(contrastSagittal, "weights");
            checkPerField(contrastTangential, "weights");
            contrastSagittalFor.values().forEach(v -> checkPerField(v, "weights"));
            contrastTangentialFor.values().forEach(v -> checkPerField(v, "weights"));
            if (contrastFrequencies == null) {
                int line = firstLine(contrastSagittal, contrastTangential);
                if (line == 0 && !contrastSagittalFor.isEmpty())
                    line = contrastSagittalFor.values().iterator().next().line();
                if (line == 0 && !contrastTangentialFor.isEmpty())
                    line = contrastTangentialFor.values().iterator().next().line();
                if (line == 0 && balanceFields != null)
                    line = balanceLine;
                if (line == 0)
                    line = contrastSettingsLine;
                if (line != 0)
                    throw error(line, "contrast settings need a 'goal contrast <frequencies>' line");
            }
            else {
                for (var entry : contrastSagittalFor.entrySet())
                    checkContrastFrequency(entry.getKey(), entry.getValue().line());
                for (var entry : contrastTangentialFor.entrySet())
                    checkContrastFrequency(entry.getKey(), entry.getValue().line());
            }

            for (var entry : mtf.entrySet()) {
                int frequency = entry.getKey();
                MtfRows rows = entry.getValue();
                if (rows.sagittal == null || rows.tangential == null)
                    throw error(rows.line, "MTF goals at " + frequency + " need both a sag and a tan row of targets");
                OptimizationValidation.frequency(frequency, frequencies,
                        () -> error(rows.line, "MTF goal frequency " + frequency + " is not one of 'frequencies'"));
                checkPerField(rows.sagittal, "targets");
                checkPerField(rows.tangential, "targets");
                for (PerField targets : new PerField[]{rows.sagittal, rows.tangential})
                    OptimizationValidation.range(targets.values(), 100.0,
                            () -> error(targets.line(), "MTF targets are percentages, between 0 and 100"));
                checkPerField(rows.weights, "weights");
                checkPerField(rows.sagittalWeights, "weights");
                checkPerField(rows.tangentialWeights, "weights");
            }

            checkPerField(spotRms, "targets");
            checkPerField(spotRmsWeights, "weights");
            checkPerField(spotMaxRadius, "targets");
            checkPerField(spotMaxRadiusWeights, "weights");
            for (PerField targets : new PerField[]{spotRms, spotMaxRadius})
                if (targets != null)
                    OptimizationValidation.range(targets.values(), Double.POSITIVE_INFINITY,
                            () -> error(targets.line(), "spot targets must be finite and non-negative"));
            if (spotRmsWeights != null && spotRms == null)
                throw error(spotRmsWeights.line(), "spot-rms weights need a 'goal spot-rms <targets>' line");
            if (spotMaxRadiusWeights != null && spotMaxRadius == null)
                throw error(spotMaxRadiusWeights.line(), "spot-max-radius weights need a 'goal spot-max-radius <targets>' line");

            checkPerField(spotDeviation, "weights");
            checkPerField(spotDeviationX, "weights");
            checkPerField(spotDeviationY, "weights");
            if (spotDeviation != null && (spotDeviationX != null || spotDeviationY != null))
                throw error(firstLine(spotDeviationX, spotDeviationY),
                        "give spot deviation weights either as one row or as x and y rows, not both");
            if ((spotDeviationX == null) != (spotDeviationY == null))
                throw error(firstLine(spotDeviationX, spotDeviationY), "spot deviation needs both an x and a y row");
        }

        private void checkContrastFrequency(int frequency, int line) {
            OptimizationValidation.frequency(frequency, contrastFrequencies,
                    () -> error(line, "contrast frequency " + frequency + " is not one of the 'goal contrast' frequencies"));
        }

        private void checkPerField(PerField values, String what) {
            if (values == null)
                return;
            OptimizationValidation.fieldCount(values.values(), fields.length,
                    () -> error(values.line(), "expected " + fields.length + " " + what + ", one per field, but found "
                            + values.values().length));
            if (what.equals("weights"))
                OptimizationValidation.range(values.values(), Double.POSITIVE_INFINITY,
                        () -> error(values.line(), "weights must not be negative"));
        }

        private static int firstLine(PerField... values) {
            for (PerField value : values)
                if (value != null)
                    return value.line();
            return 0;
        }

        /** One flag per field from 'all', 'all except <field values>' or yes/no per field. */
        private boolean[] balanceFlags() {
            boolean[] flags = new boolean[fields.length];
            String first = lower(balanceFields.get(0));
            if (first.equals("all")) {
                Arrays.fill(flags, true);
                if (balanceFields.size() == 1)
                    return flags;
                if (balanceFields.size() < 3 || !lower(balanceFields.get(1)).equals("except"))
                    throw error(balanceLine, "expected 'all' or 'all except <field values>'");
                for (String value : balanceFields.subList(2, balanceFields.size())) {
                    double field = number(balanceLine, value, "field");
                    int index = -1;
                    for (int i = 0; i < fields.length; i++)
                        if (fields[i] == field)
                            index = i;
                    if (index < 0)
                        throw error(balanceLine, "there is no field " + value + " in 'fields'");
                    flags[index] = false;
                }
                return flags;
            }
            if (balanceFields.size() != fields.length)
                throw error(balanceLine, "expected 'all', 'all except <field values>' or " + fields.length
                        + " yes/no values, one per field");
            for (int i = 0; i < fields.length; i++)
                flags[i] = yesNoValue(balanceLine, balanceFields.get(i));
            return flags;
        }

        /** Apply prescription-dependent checks at each stage, retaining source diagnostics. */
        OptimizationBuilder apply(Prescription prescription, OptimizationConfiguration configuration) {
            if (prescription._surfaces.length != radii.size())
                throw new IllegalArgumentException("the prescription has " + prescription._surfaces.length
                        + " surfaces but the trial's [lens data] has " + radii.size()
                        + "; build the prescription from the same text as the trial");
            if (curvatureConstraint != null && curvatures != null
                    && curvatures.kind() == SelectionKind.LIST) {
                for (int surface : curvatures.surfaces())
                    if (prescription._surfaces[surface]._radius == 0.0)
                        throw error(curvatureConstraintLine, "cannot constrain curvature of flat surface "
                                + surface + "; a fractional curvature constraint needs a non-zero starting curvature"
                                + "; remove this surface from 'vary curvatures' or omit 'constrain curvatures'");
            }
            // Add explicit terms through the public API to keep its prescription-dependent
            // validation (asphere kind and coefficient scaling) and source-line errors.
            var stage = configuration.copy();
            stage.asphericTerms.clear();
            var builder = new OptimizationBuilder(prescription, stage);
            for (AsphericRow row : asphericRows) {
                for (Term term : row.terms()) {
                    try {
                        if (term.index() < 0)
                            builder.varyConic(row.surface());
                        else if (term.scale() != null)
                            builder.varyAsphericCoefficient(row.surface(), term.index(), term.scale());
                        else
                            builder.varyAsphericCoefficient(row.surface(), term.index());
                    }
                    catch (IllegalArgumentException e) {
                        throw error(row.line(), e.getMessage());
                    }
                }
            }

            return builder;
        }

        /** Resolve shorthand and omitted values once into the same settings the builder uses. */
        OptimizationConfiguration configuration() {
            var c = new OptimizationConfiguration();
            c.description = description;
            c.outdir = outdir;
            c.fields = fields.clone();
            c.mtfFrequencies = frequencies.clone();
            c.scenario = configuration;
            c.weighted = weighted;
            c.dLineOnly = dLineOnly;
            if (vignetting != null) c.vigType = vignetting;
            c.freezeVignetting = freezeVignetting;
            if (checkSpotApertures != null) c.checkSpotApertures = checkSpotApertures;
            if (curvatures != null) {
                c.allCurvatureSurfaces = curvatures.kind() != SelectionKind.LIST;
                if (curvatures.kind() == SelectionKind.LIST) c.curvatureSurfaces = curvatures.surfaces().clone();
                if (curvatures.kind() == SelectionKind.ALL_EXCEPT) c.curvatureExclusions = curvatures.surfaces().clone();
            }
            if (thicknesses != null) {
                c.allThicknessSurfaces = thicknesses.kind() != SelectionKind.LIST;
                if (thicknesses.kind() == SelectionKind.LIST) c.thicknessSurfaces = thicknesses.surfaces().clone();
                if (thicknesses.kind() == SelectionKind.ALL_EXCEPT) c.thicknessExclusions = thicknesses.surfaces().clone();
            }
            c.includeExistingAspherics = existingAspherics;
            for (AsphericRow row : asphericRows)
                for (Term term : row.terms())
                    c.asphericTerms.add(new OptimizationBuilder.AsphericTerm(row.surface(), term.index(), term.scale()));
            c.curvatureConstraintWeight = curvatureConstraint;
            c.thicknessConstraintWeight = thicknessConstraint;
            c.edgeThicknessConstraintWeight = edgeConstraint;
            if (contrastFrequencies != null) {
                for (int frequency : contrastFrequencies)
                    c.contrastGoals.add(OptimizationBuilder.contrast(frequency,
                            weightsFor(contrastSagittalFor.get(frequency), contrastSagittal),
                            weightsFor(contrastTangentialFor.get(frequency), contrastTangential)));
                if (contrastSampling != null) {
                    c.contrastRings = contrastSampling[0];
                    c.contrastSpokes = contrastSampling[1];
                }
                if (calibrateContrast != null) c.calibrateContrastFrequency = calibrateContrast;
                if (exitPupilAiming != null) c.aimContrastAtExitPupil = exitPupilAiming;
                if (centerContrast != null) c.centerContrastResiduals = centerContrast;
                if (balanceFields != null) {
                    c.contrastBalanceFields = balanceFlags();
                    c.contrastBalanceWeight = balanceWeight;
                }
            }
            for (var entry : mtf.entrySet()) {
                MtfRows rows = entry.getValue();
                double[] both = rows.weights != null ? rows.weights.values() : null;
                c.mtfGoals.add(OptimizationBuilder.mtf(entry.getKey(),
                        rows.sagittal.values(), rows.tangential.values(),
                        rows.sagittalWeights != null ? rows.sagittalWeights.values() : both,
                        rows.tangentialWeights != null ? rows.tangentialWeights.values() : both));
            }
            if (spotRms != null)
                c.spotRmsGoals = new OptimizationBuilder.SpotGoals(
                        spotRms.values(), spotRmsWeights != null ? spotRmsWeights.values() : null);
            if (spotMaxRadius != null)
                c.spotMaxRadiusGoals = new OptimizationBuilder.SpotGoals(
                        spotMaxRadius.values(), spotMaxRadiusWeights != null ? spotMaxRadiusWeights.values() : null);
            if (spotDeviation != null || spotDeviationX != null) {
                c.addSpotDeviationGoals = true;
                c.spotDeviationXWeights = (spotDeviation != null ? spotDeviation : spotDeviationX).values().clone();
                c.spotDeviationYWeights = (spotDeviation != null ? spotDeviation : spotDeviationY).values().clone();
            }
            if (gaussianSampling != null)
                c.gaussianSampling(gaussianSampling[0], gaussianSampling[1], gaussianInnerRadius);
            if (hexapolarRays != null) {
                c.useHexapolarSpotPattern = true;
                c.hexapolarSpotRays = hexapolarRays;
            }
            c.addRayAberrationGoals = rayAberrations;
            for (ParaxialGoal goal : paraxialGoals)
                c.paraxialGoals.add(new OptimizationBuilder.ParaxialGoal(goal.id(), goal.target(), goal.weight()));
            return c;
        }

        private double[] weightsFor(PerField specific, PerField general) {
            if (specific != null)
                return specific.values();
            if (general != null)
                return general.values();
            double[] ones = new double[fields.length];
            Arrays.fill(ones, 1.0);
            return ones;
        }

        // --------------------------------------------------------------
        // Helpers
        // --------------------------------------------------------------

        private TrialException error(int line, String message) {
            return new TrialException("trial " + number + ", line " + line + ": " + message);
        }

        private void once(int line, String key) {
            if (!seen.add(key))
                throw error(line, "'" + key + "' is given more than once");
        }

        private void count(int line, String[] w, int expected, String form) {
            if (w.length != expected)
                throw error(line, "expected '" + form + "'");
        }

        private boolean yesNo(int line, String[] w, int index) {
            if (w.length != index + 1)
                throw error(line, "expected yes or no after '" + String.join(" ", Arrays.copyOf(w, index)) + "'");
            return yesNoValue(line, w[index]);
        }

        private boolean yesNoValue(int line, String value) {
            return switch (lower(value)) {
                case "yes", "true", "on" -> true;
                case "no", "false", "off" -> false;
                default -> throw error(line, "expected yes or no, found '" + value + "'");
            };
        }

        private double number(int line, String value, String what) {
            double d;
            try {
                d = Double.parseDouble(value);
            }
            catch (NumberFormatException e) {
                throw error(line, "expected a number for the " + what + ", found '" + value + "'");
            }
            if (!Double.isFinite(d))
                throw error(line, "the " + what + " must be finite");
            return d;
        }

        private double nonNegative(int line, String value, String what) {
            double d = number(line, value, what);
            if (d < 0.0)
                throw error(line, "the " + what + " must not be negative");
            return d;
        }

        private double positive(int line, String value, String what) {
            double d = number(line, value, what);
            if (d <= 0.0)
                throw error(line, "the " + what + " must be positive");
            return d;
        }

        private double[] numbers(int line, String[] w, int from) {
            if (from >= w.length)
                throw error(line, "expected values after '" + String.join(" ", Arrays.copyOf(w, from)) + "'");
            double[] values = new double[w.length - from];
            for (int i = from; i < w.length; i++)
                values[i - from] = number(line, w[i], "value");
            return values;
        }

        private int integer(int line, String value, String what) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
                throw error(line, "expected a whole number for the " + what + ", found '" + value + "'");
            }
        }

        private int nonNegativeInt(int line, String value, String what) {
            int i = integer(line, value, what);
            if (i < 0)
                throw error(line, "the " + what + " must not be negative");
            return i;
        }

        private int positiveInt(int line, String value, String what) {
            int i = integer(line, value, what);
            if (i <= 0)
                throw error(line, "the " + what + " must be positive");
            return i;
        }

        private int[] positiveInts(int line, String[] w, int from, String what) {
            if (from >= w.length)
                throw error(line, "expected " + what + " after '" + String.join(" ", Arrays.copyOf(w, from)) + "'");
            int[] values = new int[w.length - from];
            Set<Integer> unique = new HashSet<>();
            for (int i = from; i < w.length; i++) {
                values[i - from] = positiveInt(line, w[i], what.replaceAll("s$", ""));
                String token = w[i];
                OptimizationValidation.positiveUnique(values[i - from], unique,
                        () -> error(line, token + " is listed twice"));
            }
            return values;
        }

        /** Field values, or 'fields a to b step s' written out with decimal steps. */
        private double[] fieldList(int line, String[] w) {
            double[] values;
            if (w.length == 6 && lower(w[2]).equals("to") && lower(w[4]).equals("step")) {
                BigDecimal from = decimal(line, w[1]);
                BigDecimal to = decimal(line, w[3]);
                BigDecimal step = decimal(line, w[5]);
                if (step.signum() <= 0)
                    throw error(line, "the field step must be positive");
                List<Double> list = new ArrayList<>();
                for (BigDecimal value = from; value.compareTo(to) <= 0; value = value.add(step))
                    list.add(value.doubleValue());
                values = list.stream().mapToDouble(Double::doubleValue).toArray();
            }
            else {
                if (w.length < 2)
                    throw error(line, "expected field values after 'fields'");
                values = new double[w.length - 1];
                for (int i = 1; i < w.length; i++)
                    values[i - 1] = number(line, w[i], "field");
            }
            OptimizationValidation.range(values, 1.0,
                    () -> error(line, "fields are relative heights, between 0 and 1"));
            if (values.length == 0 || values[0] != 0.0)
                throw error(line, "the first field must be 0");
            return values;
        }

        private BigDecimal decimal(int line, String value) {
            try {
                return new BigDecimal(value);
            }
            catch (NumberFormatException e) {
                throw error(line, "expected a number, found '" + value + "'");
            }
        }
    }

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
