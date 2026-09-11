package org.redukti.optim;

import java.util.Arrays;

/**
 * A {@code [pipeline n]} section: trials run one after another, each starting from the
 * design the one before it produced.
 *
 * <p>A zoom is the case it was made for. A configuration is optimized at a time, so the
 * wide end is optimized first and the tele end then starts from that result; since the
 * two share every radius and glass thickness, the second stage partly undoes the first,
 * and the pipeline is written to alternate - {@code trials 1 2 1 2} - until the design
 * settles. Nothing is specific to zooms, though: a coarse stage followed by a fine one,
 * or spot goals followed by contrast, chain the same way.
 *
 * <p>Trial and pipeline numbers share one numbering, so {@code --optimize 3} runs whichever
 * of them the file defines.
 */
public final class OptimizationPipeline {

    private final int number;
    private final String description;
    private final String outdir;
    private final int[] trials;

    OptimizationPipeline(int number, String description, String outdir, int[] trials) {
        this.number = number;
        this.description = description;
        this.outdir = outdir;
        this.trials = trials;
    }

    public int number() {
        return number;
    }

    /** The pipeline's description, or null when it has none. */
    public String description() {
        return description;
    }

    /**
     * Where LensTool2 puts the optimized prescription and the report, relative to the
     * prescription's file unless absolute, or null when the pipeline does not say. The
     * stages' own outdirs are not used: a pipeline writes one result.
     */
    public String outdir() {
        return outdir;
    }

    /** The trials to run, in order; a trial may appear more than once. */
    public int[] trials() {
        return Arrays.copyOf(trials, trials.length);
    }

    /** The stages as the trials line lists them, "1 2 1 2", for reporting. */
    public String trialsText() {
        return OptimizationTrial.format(trials);
    }

    /** The trials the pipeline names, each once, in order of number. */
    public int[] distinctTrials() {
        return Arrays.stream(trials).distinct().sorted().toArray();
    }

    /** This pipeline as a {@code [pipeline n]} section, which {@link OptimizationTrial#readPipeline} reads back. */
    public String toPipeline() {
        var sb = new StringBuilder();
        sb.append("[pipeline ").append(number).append("]\n");
        if (description != null)
            OptimizationTrial.line(sb, "description", description);
        if (outdir != null)
            OptimizationTrial.line(sb, "outdir", outdir);
        OptimizationTrial.line(sb, "trials", OptimizationTrial.format(trials));
        return sb.toString();
    }
}
