package org.redukti.util;

import org.redukti.rayoptics.analysis.SpotOptions;
import org.redukti.spec.VigType;

public final class Args {
    public int scenario = 0;
    public String specfile = null;
    public String outputType = "layout";
    public String outputFile = null;
    public String outdir = null;
    public boolean dumpSystem = false;
    public boolean use_glass_types = true;
    public boolean include_lost_rays = false;
    public boolean only_d_line = false;
    public boolean do_ray_aberrations = false;
    public boolean do_mono_chrome_mtfs = false;
    /**
     * Spatial frequencies in cycles/mm at which the MTF is reported. The
     * default is what every report under Examples/ uses, so that lenses stay
     * comparable across reports; override it only when checking a design
     * against a manufacturer's own choice of frequencies.
     */
    public int[] mtf_freqs = default_mtf_freqs();
    /**
     * Ray pattern used for spot diagrams, one of the
     * {@link SpotOptions}.PATTERN_* constants. Defaults to hexapolar, which is
     * also {@link SpotOptions}' own default.
     */
    public int spot_pattern = SpotOptions.PATTERN_HEXAPOLAR;
    /** Number of samples along each dimension of a rectangular spot grid. */
    public int spot_grid_size = 64;
    public boolean auto_size_spots = false;
    public boolean do_wideangle_layout = false;
    public boolean force = false;
    /**
     * Vignetting calculation applied once the model is built. Defaults to the
     * value every tool in this module already hard-codes, so wiring an existing
     * tool up to this field is a no-op.
     */
    public VigType vig_type = VigType.SetPupil;
    /**
     * Forces the field spec's wide angle ray aiming on or off. Null leaves it
     * derived from the half angle of view, which is the existing behaviour.
     * <p>
     * This is not {@link #do_wideangle_layout}: that one only affects how the
     * layout is drawn, this one selects which chief ray aiming algorithm runs.
     */
    public Boolean wide_angle = null;
    /** Emit Java model building code rather than Python. */
    public boolean generate_java = false;
    /** Emit the original plotting notebook script rather than a comparison model. */
    public boolean legacy_notebook = false;
    /**
     * Path to the upstream reference values produced by dump_reference.py. When
     * set, the exporter emits a JUnit regression test instead of a model builder.
     */
    public String reference_file = null;

    public static Args parseArguments(String[] args) {
        Args arguments = new Args();
        for (int i = 0; i < args.length; i++) {
            String arg1 = args[i];
            String arg2 = i+1 < args.length ? args[i+1] : null;
            if (arg1.equals("--specfile")) {
                arguments.specfile = arg2;
                i++;
            }
            else if (arg1.equals("-o")) {
                arguments.outputFile = arg2;
                i++;
            }
            else if (arg1.equals("--scenario")) {
                arguments.scenario = Integer.parseInt(arg2);
                i++;
            }
            else if (arg1.equals("--output") || arg1.equals("--type")) {
                arguments.outputType = arg2;
                i++;
            }
            else if (arg1.equals("--outdir")) {
                arguments.outdir = arg2;
                i++;
            }
            else if (arg1.equals("--dont-use-glass-types")) {
                arguments.use_glass_types = false;
            }
            else if (arg1.equals("--dump-system")) {
                arguments.dumpSystem = true;
            }
            else if (arg1.equals("--exclude-lost-rays")) {
                arguments.include_lost_rays = false;
            }
            else if (arg1.equals("--force")) {
                arguments.force = true;
            }
            else if (arg1.equals("--only-d-line")) {
                arguments.only_d_line = true;
            }
            else if (arg1.equals("--output-ray-aberration-plots")) {
                arguments.do_ray_aberrations = true;
            }
            else if (arg1.equals("--output-wavelength-mtfs")) {
                arguments.do_mono_chrome_mtfs = true;
            }
            else if (arg1.equals("--mtf")) {
                arguments.mtf_freqs = parse_mtf_freqs(arg2);
                i++;
            }
            else if (arg1.equals("--use-spot-pattern")) {
                arguments.spot_pattern = parse_spot_pattern(arg2);
                i++;
            }
            else if (arg1.equals("--spot-grid-size")) {
                arguments.spot_grid_size = parse_positive_int(arg1, arg2);
                i++;
            }
            else if (arg1.equals("--auto-size-spot-diagrams")) {
                arguments.auto_size_spots = true;
            }
            else if (arg1.equals("--do-wideangle-layout")) {
                arguments.do_wideangle_layout = true;
            }
            else if (arg1.equals("--vig-type")) {
                arguments.vig_type = parse_vig_type(arg2);
                i++;
            }
            else if (arg1.equals("--wide-angle")) {
                arguments.wide_angle = Boolean.TRUE;
            }
            else if (arg1.equals("--no-wide-angle")) {
                arguments.wide_angle = Boolean.FALSE;
            }
            else if (arg1.equals("--generate-java")) {
                arguments.generate_java = true;
            }
            else if (arg1.equals("--legacy-notebook")) {
                arguments.legacy_notebook = true;
            }
            else if (arg1.equals("--reference")) {
                arguments.reference_file = arg2;
                i++;
            }
        }
        return arguments;
    }

    /** The MTF frequencies used by every report under Examples/. */
    public static int[] default_mtf_freqs() {
        return new int[] {10, 30, 50};
    }

    /**
     * Parses a comma separated list of spatial frequencies in cycles/mm, e.g.
     * "10,20,40". As with the other typed options a bad value is rejected
     * rather than defaulted, since silently reporting the standard 10/30/50
     * would look like a valid answer to a question that was never asked.
     */
    public static int[] parse_mtf_freqs(String value) {
        if (value == null)
            throw new IllegalArgumentException(
                    "--mtf requires a comma separated list of frequencies in cycles/mm, e.g. 10,30,50");
        String[] parts = value.split(",");
        int[] freqs = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            int freq;
            try {
                freq = Integer.parseInt(part);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unrecognized --mtf frequency '" + part
                        + "' in '" + value + "', expected a comma separated list such as 10,30,50");
            }
            if (freq <= 0)
                throw new IllegalArgumentException("--mtf frequency must be positive, got " + freq);
            freqs[i] = freq;
        }
        if (freqs.length == 0)
            throw new IllegalArgumentException(
                    "--mtf requires at least one frequency, e.g. 10,30,50");
        return freqs;
    }

    /**
     * Accepts either the enum constant (SetPupil) or its kebab-case spelling
     * (set-pupil), ignoring case and any - or _ separators.
     * <p>
     * An unrecognized value is rejected rather than quietly falling back to the
     * default: the vignetting type changes the model that gets built, so a typo
     * would otherwise shift every number downstream with nothing to show for it.
     */
    public static VigType parse_vig_type(String value) {
        if (value == null)
            throw new IllegalArgumentException(
                    "--vig-type requires a value, one of: " + vig_type_names());
        String normalized = value.replace("-", "").replace("_", "");
        for (VigType vig_type : VigType.values()) {
            if (vig_type.name().equalsIgnoreCase(normalized))
                return vig_type;
        }
        throw new IllegalArgumentException(
                "Unrecognized --vig-type '" + value + "', expected one of: " + vig_type_names());
    }

    /** The accepted --vig-type spellings, for usage and error messages. */
    public static String vig_type_names() {
        StringBuilder sb = new StringBuilder();
        for (VigType vig_type : VigType.values()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(to_kebab_case(vig_type.name()));
        }
        return sb.toString();
    }

    /**
     * Accepts hex, grid or gaussian, returning the matching
     * {@link SpotOptions}.PATTERN_* constant. As with --vig-type an
     * unrecognized value is rejected rather than defaulted, since the sampling
     * pattern changes every spot number it produces.
     */
    public static int parse_spot_pattern(String value) {
        if (value == null)
            throw new IllegalArgumentException(
                    "--use-spot-pattern requires a value, one of: " + spot_pattern_names());
        String normalized = value.replace("-", "").replace("_", "").toLowerCase();
        switch (normalized) {
            case "hex":
            case "hexapolar":
                return SpotOptions.PATTERN_HEXAPOLAR;
            case "grid":
                return SpotOptions.PATTERN_GRID;
            case "gq":
            case "gauss":
            case "gaussian":
            case "gaussianquadrature":
                return SpotOptions.PATTERN_GAUSS_QUADRATURE;
            default:
                throw new IllegalArgumentException("Unrecognized --use-spot-pattern '" + value
                        + "', expected one of: " + spot_pattern_names());
        }
    }

    /** The accepted --use-spot-pattern spellings, for usage and error messages. */
    public static String spot_pattern_names() {
        return "hex, grid, gaussian";
    }

    private static int parse_positive_int(String option, String value) {
        if (value == null)
            throw new IllegalArgumentException(option + " requires a positive integer");
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 2)
                throw new IllegalArgumentException(option + " must be at least 2, got " + parsed);
            return parsed;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " requires a positive integer, got '"
                    + value + "'");
        }
    }

    private static String to_kebab_case(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c))
                sb.append('-');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }


}
