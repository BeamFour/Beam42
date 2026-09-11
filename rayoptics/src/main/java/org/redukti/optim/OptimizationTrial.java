package org.redukti.optim;

import org.redukti.spec.Prescription;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;
import org.redukti.util.Args;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One {@code [trial n]} section of a prescription file: an optimization run described in
 * text rather than built in code. The format is documented in
 * Documentation/OPTIMIZER_SPEC.md.
 *
 * <p>{@link #parse} checks everything the text alone can settle - syntax, per-field value
 * counts, the surfaces named - and reports a problem with its line number.
 * {@link #builder} then applies the trial to a prescription built from the same text.
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

    /** A [lens data] row; only its radius column matters here, to recognise the stops. */
    private record LensRow(String radius) {}

    /** Values given one per field, kept with their line until the field count is known. */
    private record PerField(double[] values, int line) {}

    private enum SelectionKind { ALL, ALL_EXCEPT, LIST }

    private record Selection(SelectionKind kind, int[] surfaces) {}

    /** One aspheric term: K when power is 0, otherwise the coefficient of r^power. */
    private record Term(String name, int power, Double scale) {}

    private record AsphericRow(int surface, List<Term> terms, int line) {}

    private record ParaxialGoal(String quantity, int id, double target, double weight) {}

    private static final class MtfRows {
        final int line;
        PerField sagittal, tangential, weights, sagittalWeights, tangentialWeights;

        MtfRows(int line) {
            this.line = line;
        }
    }

    private final int number;
    private final List<LensRow> lensRows;
    private final int fileAsphereType;
    private final String section;
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
    private boolean hexapolarSampling;
    private int[] spotSampling;
    private double spotInnerRadius;

    private boolean rayAberrations;
    private final List<ParaxialGoal> paraxialGoals = new ArrayList<>();

    private OptimizationTrial(int number, List<LensRow> lensRows, int fileAsphereType, String section) {
        this.number = number;
        this.lensRows = lensRows;
        this.fileAsphereType = fileAsphereType;
        this.section = section;
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Reads trial {@code number} from the text of a prescription file.
     *
     * @throws TrialException if the file has no such trial, or the trial has a problem
     */
    public static OptimizationTrial parse(String text, int number) {
        String[] lines = text.split("\\r?\\n", -1);

        // The lens data as the prescription reader sees it: a trial's surface numbers are
        // positions in it.
        List<LensRow> lensRows = new ArrayList<>();
        boolean odd = false, a2 = false;
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
                lensRows.add(new LensRow(words[1]));
            else if ("[constants]".equals(section)) {
                if (words[0].equals("AsphericalOddCount"))
                    odd = true;
                else if (words[0].equals("AsphericalA2"))
                    a2 = true;
            }
        }
        int asphereType = odd ? SurfaceType.ASPH_ODD
                : a2 ? SurfaceType.ASPH_EVEN_A2 : SurfaceType.ASPH_EVEN;

        // Find the trial's lines.
        Map<Integer, Integer> headers = new TreeMap<>();
        List<Integer> trialLines = new ArrayList<>();
        boolean inTrial = false;
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("[")) {
                inTrial = false;
                Matcher matcher = TRIAL_HEADER.matcher(trimmed);
                if (matcher.matches()) {
                    int found = Integer.parseInt(matcher.group(1));
                    Integer earlier = headers.put(found, i + 1);
                    if (earlier != null)
                        throw new TrialException("[trial " + found + "] is defined twice, at lines "
                                + earlier + " and " + (i + 1));
                    inTrial = found == number;
                    if (inTrial)
                        start = i;
                }
                else if (trimmed.toLowerCase(Locale.ROOT).startsWith("[trial"))
                    throw new TrialException("line " + (i + 1) + ": expected [trial <number>], found "
                            + trimmed);
                continue;
            }
            if (inTrial)
                trialLines.add(i);
        }
        if (!headers.containsKey(number))
            throw new TrialException("there is no [trial " + number + "] in this prescription; "
                    + (headers.isEmpty() ? "it defines no trials"
                    : "it defines " + (headers.size() == 1 ? "trial " : "trials ")
                    + String.join(", ", headers.keySet().stream().map(String::valueOf).toList())));

        // The section's own text, header to last non-blank line, to carry into the output.
        int end = start;
        for (int i = start + 1; i < lines.length && !lines[i].trim().startsWith("["); i++)
            if (!lines[i].trim().isEmpty())
                end = i;
        String sectionText = String.join("\n", Arrays.copyOfRange(lines, start, end + 1));

        var trial = new OptimizationTrial(number, lensRows, asphereType, sectionText);
        for (int i : trialLines)
            trial.read(i + 1, lines[i]);
        trial.finish();
        return trial;
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
     * {@link OptimizationBuilder} does. The ids in the file's first column are not used;
     * they need not be numbers, nor in order.
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
        if (surface < 0 || surface >= lensRows.size())
            throw error(line, "there is no surface " + value + "; [lens data] has surfaces 0 to "
                    + (lensRows.size() - 1));
        return surface;
    }

    private boolean isStop(int surface) {
        String radius = lensRows.get(surface).radius();
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
        if (isStop(surface))
            throw error(line, "surface " + surface + " is a stop; it cannot be aspheric");
        List<Term> terms = new ArrayList<>();
        Set<Integer> powers = new HashSet<>();
        for (int i = 3; i < w.length; i++) {
            String token = w[i];
            int colon = token.indexOf(':');
            String name = colon >= 0 ? token.substring(0, colon) : token;
            Double scale = null;
            if (colon >= 0)
                scale = positive(line, token.substring(colon + 1), "scale of " + name);
            int power;
            if (lower(name).equals("k")) {
                if (scale != null)
                    throw error(line, "K takes no scale");
                power = 0;
            }
            else if (lower(name).startsWith("a") && name.length() > 1) {
                try {
                    power = Integer.parseInt(name.substring(1));
                }
                catch (NumberFormatException e) {
                    throw error(line, "unknown aspheric term '" + name + "'; expected K or A<n>");
                }
                if (power < 1)
                    throw error(line, "unknown aspheric term '" + name + "'; expected K or A<n>");
            }
            else
                throw error(line, "unknown aspheric term '" + name + "'; expected K or A<n>");
            if (!powers.add(power))
                throw error(line, "term " + name + " is listed twice");
            terms.add(new Term(power == 0 ? "K" : "A" + power, power, scale));
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
            case "spot" -> spotSampling(line, w);
            case "ray-aberrations" -> {
                once(line, "goal ray-aberrations");
                rayAberrations = yesNo(line, w, 2);
            }
            case "paraxial" -> paraxialGoal(line, w);
            default -> throw error(line, "unknown goal '" + w[1]
                    + "'; expected contrast, mtf, spot-rms, spot-max-radius, spot, ray-aberrations or paraxial");
        }
    }

    private void contrastGoal(int line, String[] w) {
        String what = lower(w[2]);
        switch (what) {
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
        once(line, "goal spot sampling");
        switch (lower(w[3])) {
            case "gaussian" -> {
                if (w.length != 6 && w.length != 7)
                    throw error(line, "expected 'goal spot sampling gaussian <rings> <spokes> [<inner radius>]'");
                spotSampling = new int[]{positiveInt(line, w[4], "rings"), positiveInt(line, w[5], "spokes")};
                spotInnerRadius = w.length == 7 ? nonNegative(line, w[6], "inner radius") : 0.0;
            }
            case "hexapolar" -> {
                count(line, w, 5, "goal spot sampling hexapolar <rays>");
                hexapolarSampling = true;
                spotSampling = new int[]{positiveInt(line, w[4], "rays")};
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
        paraxialGoals.add(new ParaxialGoal(quantity, id, target, weight));
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
            if (Arrays.stream(frequencies).noneMatch(f -> f == frequency))
                throw error(rows.line, "MTF goal frequency " + frequency + " is not one of 'frequencies'");
            checkPerField(rows.sagittal, "targets");
            checkPerField(rows.tangential, "targets");
            for (PerField targets : new PerField[]{rows.sagittal, rows.tangential})
                for (double target : targets.values())
                    if (target < 0.0 || target > 100.0)
                        throw error(targets.line(), "MTF targets are percentages, between 0 and 100");
            checkPerField(rows.weights, "weights");
            checkPerField(rows.sagittalWeights, "weights");
            checkPerField(rows.tangentialWeights, "weights");
        }

        checkPerField(spotRms, "targets");
        checkPerField(spotRmsWeights, "weights");
        checkPerField(spotMaxRadius, "targets");
        checkPerField(spotMaxRadiusWeights, "weights");
        if (spotRmsWeights != null && spotRms == null)
            throw error(spotRmsWeights.line(), "spot-rms weights need a 'goal spot-rms <targets>' line");
        if (spotMaxRadiusWeights != null && spotMaxRadius == null)
            throw error(spotMaxRadiusWeights.line(), "spot-max-radius weights need a 'goal spot-max-radius <targets>' line");
    }

    private void checkContrastFrequency(int frequency, int line) {
        if (Arrays.stream(contrastFrequencies).noneMatch(f -> f == frequency))
            throw error(line, "contrast frequency " + frequency + " is not one of the 'goal contrast' frequencies");
    }

    private void checkPerField(PerField values, String what) {
        if (values == null)
            return;
        if (values.values().length != fields.length)
            throw error(values.line(), "expected " + fields.length + " " + what + ", one per field, but found "
                    + values.values().length);
        if (what.equals("weights"))
            for (double value : values.values())
                if (value < 0.0)
                    throw error(values.line(), "weights must not be negative");
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

    // ------------------------------------------------------------------
    // Applying
    // ------------------------------------------------------------------

    public int number() {
        return number;
    }

    /** The trial's description, or null when it has none. */
    public String description() {
        return description;
    }

    /**
     * The directory the trial's output goes to, as written - relative paths are relative
     * to the prescription file's directory - or null when the trial does not say.
     */
    public String outdir() {
        return outdir;
    }

    public int configuration() {
        return configuration;
    }

    /** Whether the prescription for this trial should be built with weighted wavelengths. */
    public boolean weighted() {
        return weighted;
    }

    /** Whether the prescription for this trial should be built for the d line only. */
    public boolean dLineOnly() {
        return dLineOnly;
    }

    /**
     * A builder configured as the trial describes. The prescription must be built from the
     * same text the trial was read from. Surfaces the trial makes aspheric, or gives new
     * aspheric terms, are extended in the prescription so the variables have somewhere to
     * write to.
     */
    public OptimizationBuilder builder(Prescription prescription) {
        if (prescription._surfaces.length != lensRows.size())
            throw new IllegalArgumentException("the prescription has " + prescription._surfaces.length
                    + " surfaces but the trial's [lens data] has " + lensRows.size()
                    + "; build the prescription from the same text as the trial");
        var builder = OptimizationBuilder.builder(prescription)
                .fields(fields)
                .mtfFrequencies(frequencies)
                .scenario(configuration)
                .weighted(weighted)
                .dLineOnly(dLineOnly);
        if (vignetting != null)
            builder.vignetting(vignetting);
        if (freezeVignetting)
            builder.freezeVignetting();
        if (checkSpotApertures != null)
            builder.checkSpotApertures(checkSpotApertures);

        if (curvatures != null) {
            switch (curvatures.kind()) {
                case ALL -> builder.varyAllCurvatures();
                case ALL_EXCEPT -> builder.varyAllCurvaturesExcept(curvatures.surfaces());
                case LIST -> builder.varyCurvatures(curvatures.surfaces());
            }
        }
        if (thicknesses != null) {
            switch (thicknesses.kind()) {
                case ALL -> builder.varyAllThicknesses();
                case ALL_EXCEPT -> builder.varyAllThicknessesExcept(thicknesses.surfaces());
                case LIST -> builder.varyThicknesses(thicknesses.surfaces());
            }
        }
        int[] explicitAspherics = asphericRows.stream().mapToInt(AsphericRow::surface).toArray();
        if (existingAspherics) {
            if (explicitAspherics.length == 0)
                builder.varyExistingAspherics();
            else
                builder.varyExistingAsphericsExcept(explicitAspherics);
        }
        List<Var> asphericVariables = new ArrayList<>();
        for (AsphericRow row : asphericRows)
            asphericVariables.addAll(asphericVariables(prescription, row));
        if (!asphericVariables.isEmpty())
            builder.additionalVariables(asphericVariables.toArray(new Var[0]));

        if (curvatureConstraint != null)
            builder.applyCurvatureConstraints(curvatureConstraint);
        if (thicknessConstraint != null)
            builder.applyThicknessConstraints(thicknessConstraint);
        if (edgeConstraint != null)
            builder.applyEdgeThicknessConstraints(edgeConstraint);

        if (contrastFrequencies != null) {
            var goals = new OptimizationBuilder.ContrastGoals[contrastFrequencies.length];
            for (int i = 0; i < contrastFrequencies.length; i++) {
                int frequency = contrastFrequencies[i];
                goals[i] = OptimizationBuilder.contrast(frequency,
                        weightsFor(contrastSagittalFor.get(frequency), contrastSagittal),
                        weightsFor(contrastTangentialFor.get(frequency), contrastTangential));
            }
            builder.contrastGoals(goals);
            if (contrastSampling != null)
                builder.contrastSampling(contrastSampling[0], contrastSampling[1]);
            if (calibrateContrast != null)
                builder.calibrateContrastFrequency(calibrateContrast);
            if (exitPupilAiming != null)
                builder.aimContrastAtExitPupil(exitPupilAiming);
            if (centerContrast != null)
                builder.centerContrastResiduals(centerContrast);
            if (balanceFields != null)
                builder.contrastBalanceGoals(balanceFlags(), balanceWeight);
        }

        for (var entry : mtf.entrySet()) {
            MtfRows rows = entry.getValue();
            double[] both = rows.weights != null ? rows.weights.values() : null;
            builder.mtfGoals(OptimizationBuilder.mtf(entry.getKey(),
                    rows.sagittal.values(), rows.tangential.values(),
                    rows.sagittalWeights != null ? rows.sagittalWeights.values() : both,
                    rows.tangentialWeights != null ? rows.tangentialWeights.values() : both));
        }

        if (spotRms != null)
            builder.spotRmsGoals(spotRms.values(), spotRmsWeights != null ? spotRmsWeights.values() : null);
        if (spotMaxRadius != null)
            builder.spotMaxRadiusGoals(spotMaxRadius.values(),
                    spotMaxRadiusWeights != null ? spotMaxRadiusWeights.values() : null);
        if (spotSampling != null) {
            if (hexapolarSampling)
                builder.hexapolarSampling(spotSampling[0]);
            else
                builder.gaussianQuadratureSampling(spotSampling[0], spotSampling[1], spotInnerRadius);
        }
        if (rayAberrations)
            builder.rayAberrationGoals();

        for (ParaxialGoal goal : paraxialGoals) {
            switch (goal.quantity()) {
                case "efl" -> builder.focalLengthGoal(goal.target(), goal.weight());
                case "fno" -> builder.fNumberGoal(goal.target(), goal.weight());
                default -> builder.additionalGoals(
                        analysis -> new GoalParax(analysis, goal.id(), goal.target(), goal.weight()));
            }
        }
        return builder;
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

    /**
     * The variables for one explicit aspherics row. A spherical surface is made an asphere
     * of the file's type, and the coefficient array is extended to hold every term named.
     */
    private List<Var> asphericVariables(Prescription prescription, AsphericRow row) {
        SurfaceType surface = prescription._surfaces[row.surface()];
        int type = surface.is_aspheric() ? surface._asph_type : fileAsphereType;
        int highest = -1;
        for (Term term : row.terms())
            if (term.power() > 0)
                highest = Math.max(highest, coefficientIndex(type, term, row.line()));
        if (!surface.is_aspheric())
            surface._asph_type = type;
        if (surface._coeffs == null)
            surface._coeffs = new double[0];
        if (surface._coeffs.length <= highest)
            surface._coeffs = Arrays.copyOf(surface._coeffs, highest + 1);

        List<Var> result = new ArrayList<>();
        for (Term term : row.terms()) {
            if (term.power() == 0) {
                result.add(new VarAsphK(prescription, row.surface()));
                continue;
            }
            int index = coefficientIndex(type, term, row.line());
            double scale;
            if (term.scale() != null)
                scale = term.scale();
            else if (surface._coeffs[index] != 0.0)
                scale = OptimizationBuilder.scalingFor(surface._coeffs[index]);
            else {
                double h = surface._diameter / 2.0;
                if (!(h > 0.0))
                    throw error(row.line(), "surface " + row.surface()
                            + " has no diameter to derive a scale from; give " + term.name()
                            + " a scale, as in " + term.name() + ":1e6");
                scale = Math.pow(10.0, Math.round(term.power() * Math.log10(h)));
            }
            result.add(new VarAsphCoeff(prescription, row.surface(), index, scale));
        }
        return result;
    }

    /** Index into {@link SurfaceType#_coeffs} of the coefficient of r^power, for the asphere type. */
    private int coefficientIndex(int type, Term term, int line) {
        int power = term.power();
        switch (type) {
            case SurfaceType.ASPH_ODD -> {
                if (power >= 3)
                    return power - 1;
                throw error(line, term.name() + " is not a term of an odd asphere; use A3, A4, ...");
            }
            case SurfaceType.ASPH_EVEN_A2 -> {
                if (power % 2 == 0)
                    return power / 2 - 1;
                throw error(line, term.name() + " is not a term of an even asphere; use A2, A4, ...");
            }
            default -> {
                if (power % 2 == 0 && power >= 4)
                    return power / 2 - 1;
                throw error(line, term.name() + " is not a term of an even asphere; use A4, A6, ...");
            }
        }
    }

    /** How a variable is named in the trial's terms, for reporting. */
    public String describe(Var variable) {
        if (variable instanceof VarRadius radius)
            return "radius " + radius._surface_id;
        if (variable instanceof VarThickness thickness)
            return "thickness " + thickness._surface_id;
        if (variable instanceof VarAsphK conic)
            return "K " + conic._surface_id;
        if (variable instanceof VarAsphCoeff coefficient) {
            int type = coefficient._prescription._surfaces[coefficient._surface_id]._asph_type;
            int power = type == SurfaceType.ASPH_ODD ? coefficient._index + 1 : 2 * (coefficient._index + 1);
            return "A" + power + " " + coefficient._surface_id;
        }
        return variable.toString();
    }

    /** The trial's section as written in the file, from its header to its last line. */
    public String section() {
        return section;
    }

    /**
     * The optimized prescription to save: the prescription as Beam42 writes it, followed
     * by this trial, so the result can be reported on or the trial run again. Surface
     * positions are the same in both, so the trial still refers to the same surfaces.
     */
    public String optimizedPrescription(Prescription prescription) {
        return prescription.to_opt_bench_str(new StringBuilder())
                .append('\n').append(section).append('\n').toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
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
            if (!unique.add(values[i - from]))
                throw error(line, w[i] + " is listed twice");
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
        for (double value : values)
            if (value < 0.0 || value > 1.0)
                throw error(line, "fields are relative heights, between 0 and 1");
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
