package org.redukti.optim;

import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Evaluates every configuration of a prescription, so an optimization aimed at one of
 * them can be checked against the rest.
 *
 * <p>Only the varying spaces of a zoom carry a value per configuration. Curvatures,
 * aspheric terms and the fixed spaces are shared, so optimizing any of them against a
 * single configuration is a bet that the others will hold up. Nothing in the merit checks
 * that bet - the solve never evaluates the configurations it was not pointed at - which
 * makes the check an after-the-fact measurement rather than something the builder could
 * enforce.
 *
 * <p>Usage is capture, solve, capture, compare:
 *
 * <pre>{@code
 * var before = ConfigurationReport.capture(prescription, fields, frequencies);
 * solver.solve();
 * var after = ConfigurationReport.capture(prescription, fields, frequencies);
 * System.out.println(ConfigurationReport.compare(before, after));
 * }</pre>
 *
 * <p>A single-configuration prescription reports one row, so the same call is harmless
 * where there is nothing to compare across.
 */
public final class ConfigurationReport {

    /** Anything worse than this counts as a regression rather than numerical noise. */
    private static final double MTF_REGRESSION = 0.002;
    private static final double SPOT_REGRESSION_FRACTION = 0.01;

    private ConfigurationReport() {
    }

    /** What one configuration measured at one moment. */
    public record Configuration(int scenario, String name, double focalLength,
                                double fNumber, int[] frequencies,
                                double[][] sagittal, double[][] tangential,
                                double[] spotRms, String failure) {
    }

    /** Every configuration of a prescription, measured together. */
    public record Snapshot(double[] fields, List<Configuration> configurations) {
    }

    public static Snapshot capture(Prescription prescription, double[] fields, int[] frequencies) {
        return capture(prescription, fields, frequencies, analysis -> analysis
                .vignetting(VigType.SetPupil)
                .using_gauss_quadrature_pattern(6, 12));
    }

    /**
     * @param configure applied to each configuration's analysis before it computes; use it
     *                  to match whatever the optimization ran under, since a report taken
     *                  on different sampling is not comparable with the merit
     */
    public static Snapshot capture(Prescription prescription, double[] fields,
                                   int[] frequencies, Consumer<Analysis> configure) {
        int count = Math.max(1, prescription.get_num_configurations());
        var configurations = new ArrayList<Configuration>(count);
        for (int scenario = 0; scenario < count; scenario++) {
            configurations.add(measure(prescription, fields, frequencies, scenario, configure));
        }
        return new Snapshot(fields.clone(), configurations);
    }

    private static Configuration measure(Prescription prescription, double[] fields,
                                         int[] frequencies, int scenario,
                                         Consumer<Analysis> configure) {
        String name = prescription._configuration_names != null
                && scenario < prescription._configuration_names.length
                ? prescription._configuration_names[scenario]
                : "config " + scenario;
        try {
            var analysis = new Analysis(prescription, fields.clone(), frequencies.clone(), scenario);
            configure.accept(analysis);
            analysis.required_analyses(true, false, true);
            analysis.compute();

            double[][] sagittal = new double[frequencies.length][];
            double[][] tangential = new double[frequencies.length][];
            for (int i = 0; i < frequencies.length; i++) {
                sagittal[i] = analysis._mtfs[i].sag_mtf_by_field.clone();
                tangential[i] = analysis._mtfs[i].tan_mtf_by_field.clone();
            }
            double[] spot = new double[analysis._spots.length];
            for (int i = 0; i < spot.length; i++)
                spot[i] = analysis._spots[i].get_mean_radius();

            return new Configuration(scenario, name,
                    analysis._pfo[ParaxHelper.Effective_focal_length],
                    analysis._pfo[ParaxHelper.Fno],
                    frequencies.clone(), sagittal, tangential, spot, null);
        } catch (Exception e) {
            return new Configuration(scenario, name, Double.NaN, Double.NaN,
                    frequencies.clone(), null, null, null,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Side-by-side before and after for every configuration, with a verdict per
     * configuration so the question "did anything get worse" has a one-line answer.
     */
    public static String compare(Snapshot before, Snapshot after) {
        var sb = new StringBuilder();
        sb.append("Configuration report\n");
        sb.append("fields: ");
        for (double field : after.fields()) sb.append(String.format("%.2f ", field));
        sb.append("\n");

        for (int c = 0; c < after.configurations().size(); c++) {
            var now = after.configurations().get(c);
            var was = c < before.configurations().size() ? before.configurations().get(c) : null;
            sb.append("\n").append("-".repeat(78)).append("\n");
            sb.append(String.format("[%d] %s   efl %.3f -> %.3f   f/# %.3f -> %.3f%n",
                    now.scenario(), now.name(),
                    was == null ? Double.NaN : was.focalLength(), now.focalLength(),
                    was == null ? Double.NaN : was.fNumber(), now.fNumber()));

            if (now.failure() != null) {
                sb.append("    FAILED: ").append(now.failure()).append("\n");
                continue;
            }
            if (was == null || was.failure() != null) {
                sb.append("    no comparable baseline\n");
                appendAbsolute(sb, now);
                continue;
            }

            int regressions = 0;
            double worstMtf = 0.0;
            String worstWhere = "";
            for (int f = 0; f < now.frequencies().length; f++) {
                regressions += appendRow(sb, now.frequencies()[f] + " sag",
                        was.sagittal()[f], now.sagittal()[f]);
                regressions += appendRow(sb, now.frequencies()[f] + " tan",
                        was.tangential()[f], now.tangential()[f]);
                for (int i = 0; i < now.sagittal()[f].length; i++) {
                    double dSag = was.sagittal()[f][i] - now.sagittal()[f][i];
                    double dTan = was.tangential()[f][i] - now.tangential()[f][i];
                    if (dSag > worstMtf) { worstMtf = dSag; worstWhere = now.frequencies()[f] + " sag field " + i; }
                    if (dTan > worstMtf) { worstMtf = dTan; worstWhere = now.frequencies()[f] + " tan field " + i; }
                }
            }

            sb.append("    spot RMS  ");
            int spotRegressions = 0;
            for (int i = 0; i < now.spotRms().length; i++) {
                double delta = now.spotRms()[i] - was.spotRms()[i];
                boolean worse = delta > was.spotRms()[i] * SPOT_REGRESSION_FRACTION;
                if (worse) spotRegressions++;
                sb.append(String.format("%8.3f%s", now.spotRms()[i], worse ? "*" : " "));
            }
            sb.append("\n");

            if (regressions == 0 && spotRegressions == 0) {
                sb.append("    OK - nothing worse than the baseline\n");
            } else {
                sb.append(String.format(
                        "    REGRESSED - %d MTF value(s) and %d spot value(s) worse; "
                                + "worst MTF drop %.4f at %s%n",
                        regressions, spotRegressions, worstMtf, worstWhere));
            }
        }
        return sb.toString();
    }

    /** One MTF row as before -> after, returning how many values regressed. */
    private static int appendRow(StringBuilder sb, String label, double[] was, double[] now) {
        int regressions = 0;
        sb.append(String.format("    %-8s", label));
        for (int i = 0; i < now.length; i++) {
            boolean worse = was[i] - now[i] > MTF_REGRESSION;
            if (worse) regressions++;
            sb.append(String.format("%7.3f%s", now[i], worse ? "*" : " "));
        }
        sb.append("\n");
        return regressions;
    }

    private static void appendAbsolute(StringBuilder sb, Configuration configuration) {
        for (int f = 0; f < configuration.frequencies().length; f++) {
            sb.append(String.format("    %-8s", configuration.frequencies()[f] + " sag"));
            for (double v : configuration.sagittal()[f]) sb.append(String.format("%7.3f ", v));
            sb.append(String.format("%n    %-8s", configuration.frequencies()[f] + " tan"));
            for (double v : configuration.tangential()[f]) sb.append(String.format("%7.3f ", v));
            sb.append("\n");
        }
    }
}
