package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector2;
import org.redukti.rayoptics.exceptions.TraceRayBlockedException;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.specs.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Which part of the pupil a field can actually use, measured rather than modelled.
 *
 * <p>A dense grid of relative pupil coordinates is traced with the physical surface
 * apertures checked and the field's vignetting factors NOT applied, so every sample is a
 * raw pupil coordinate and the passed ones map out the bundle the lens really transmits.
 * The factors are then a hypothesis about that region which the map can be compared
 * against - see {@link #piecewise_quality} and {@link #ellipse_quality}.
 *
 * <p>The grid deliberately reaches beyond the nominal pupil, because the factors do:
 * {@code scale = 1 - factor}, so a negative factor asks for samples outside the unit
 * circle, and on a wide angle lens the real bundle is indeed wider than the nominal pupil
 * off axis. A map that stopped at the unit circle could not show that.
 */
public class PupilMapAnalysis {

    /** One sampled pupil coordinate and what became of the ray traced through it. */
    public record Sample(double x, double y, boolean passed, int blocked_by) {}

    /**
     * How well a mapping of the four vignetting factors describes the measured bundle.
     *
     * @param sampled   fraction of the mapped region that the lens passes; the rest is rays
     *                  the mapping spends on light that is blocked
     * @param covered   fraction of the passed bundle that falls inside the mapped region;
     *                  the rest is light the mapping never samples
     */
    public record MappingQuality(double sampled, double covered) {}

    public static final class PupilMapForField {
        public final int fi;
        public final Field fld;
        /** Half width of the sampled square, in nominal pupil radii. */
        public final double reach;
        /** Samples per axis; {@link #samples} is column major, index = i * num_samples + j. */
        public final int num_samples;
        public final List<Sample> samples;
        /** Largest passed |x| and |y|, in nominal pupil radii. */
        public final double passed_x;
        public final double passed_y;
        /** Fraction of the nominal pupil the lens passes. */
        public final double nominal_passed;
        public final MappingQuality piecewise_quality;
        public final MappingQuality ellipse_quality;

        PupilMapForField(int fi, Field fld, double reach, int num_samples, List<Sample> samples) {
            this.fi = fi;
            this.fld = fld;
            this.reach = reach;
            this.num_samples = num_samples;
            this.samples = samples;
            double maxX = 0.0, maxY = 0.0;
            int nominal = 0, nominalPassed = 0;
            int passed = 0;
            int inPiecewise = 0, inPiecewisePassed = 0;
            int inEllipse = 0, inEllipsePassed = 0;
            for (Sample s : samples) {
                boolean piecewise = inside_piecewise(s.x(), s.y(), fld);
                boolean ellipse = inside_ellipse(s.x(), s.y(), fld);
                if (piecewise)
                    inPiecewise++;
                if (ellipse)
                    inEllipse++;
                if (s.x() * s.x() + s.y() * s.y() <= 1.0)
                    nominal++;
                if (!s.passed())
                    continue;
                maxX = Math.max(maxX, Math.abs(s.x()));
                maxY = Math.max(maxY, Math.abs(s.y()));
                passed++;
                if (piecewise)
                    inPiecewisePassed++;
                if (ellipse)
                    inEllipsePassed++;
                if (s.x() * s.x() + s.y() * s.y() <= 1.0)
                    nominalPassed++;
            }
            this.passed_x = maxX;
            this.passed_y = maxY;
            this.nominal_passed = nominal == 0 ? 0.0 : (double) nominalPassed / nominal;
            this.piecewise_quality = new MappingQuality(
                    inPiecewise == 0 ? 0.0 : (double) inPiecewisePassed / inPiecewise,
                    passed == 0 ? 0.0 : (double) inPiecewisePassed / passed);
            this.ellipse_quality = new MappingQuality(
                    inEllipse == 0 ? 0.0 : (double) inEllipsePassed / inEllipse,
                    passed == 0 ? 0.0 : (double) inEllipsePassed / passed);
        }

        public Sample sample(int i, int j) {
            return samples.get(i * num_samples + j);
        }

        /** Pupil coordinate of grid index i or j along an axis. */
        public double coordinate(int index) {
            return -reach + 2 * reach * index / (num_samples - 1.0);
        }
    }

    public static final class PupilMapResult {
        public final List<PupilMapForField> maps = new ArrayList<>();

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("field  vig scales x-,x+,y-,y+       passed |x|,|y|   nominal pupil"
                    + "   piecewise sampled/covered   ellipse sampled/covered\n");
            for (PupilMapForField map : maps) {
                sb.append(String.format(
                        "%5.2f  %6.3f %6.3f %6.3f %6.3f   %6.3f %6.3f   %11.0f%%   %14.0f%% %.0f%%   %12.0f%% %.0f%%%n",
                        map.fld.yv(),
                        scale(map.fld.vlx), scale(map.fld.vux),
                        scale(map.fld.vly), scale(map.fld.vuy),
                        map.passed_x, map.passed_y, 100 * map.nominal_passed,
                        100 * map.piecewise_quality.sampled(), 100 * map.piecewise_quality.covered(),
                        100 * map.ellipse_quality.sampled(), 100 * map.ellipse_quality.covered()));
            }
            return sb.toString();
        }
    }

    /** Default samples per axis: enough to place a boundary to about 1% of the pupil radius. */
    public static final int DEFAULT_NUM_SAMPLES = 121;

    public static PupilMapResult eval(OpticalModel opm) {
        return eval(opm, DEFAULT_NUM_SAMPLES, null);
    }

    /**
     * Maps the pupil of each requested field.
     *
     * @param num_samples samples per axis across the sampled square
     * @param fields      indices into the field spec, or null for every field
     */
    public static PupilMapResult eval(OpticalModel opm, int num_samples, int[] fields) {
        if (num_samples < 3)
            throw new IllegalArgumentException("a pupil map needs at least 3 samples per axis");
        var osp = opm.optical_spec;
        double wvl = opm.seq_model.central_wavelength();
        var options = new TraceOptions();
        options.check_apertures = true;
        // Raw pupil coordinates: the map measures the bundle, so it must not be told
        // where the bundle is supposed to be.
        options.apply_vignetting = false;
        // trace_safe only reports the blocking surface when asked for the error.
        options.rayerr_filter = "summary";

        var result = new PupilMapResult();
        for (int fi = 0; fi < osp.fov.fields.length; fi++) {
            if (fields != null && !contains(fields, fi))
                continue;
            Field fld = osp.fov.fields[fi];
            double reach = reach_for(fld);
            var samples = new ArrayList<Sample>(num_samples * num_samples);
            for (int i = 0; i < num_samples; i++) {
                for (int j = 0; j < num_samples; j++) {
                    double x = -reach + 2 * reach * i / (num_samples - 1.0);
                    double y = -reach + 2 * reach * j / (num_samples - 1.0);
                    var ray = Trace.trace_safe(opm, new Vector2(x, y), fld, wvl, options);
                    boolean passed = ray.pkg != null && ray.err == null;
                    int blocked_by = ray.err instanceof TraceRayBlockedException ? ray.err.surf : -1;
                    samples.add(new Sample(x, y, passed, blocked_by));
                }
            }
            result.maps.add(new PupilMapForField(fi, fld, reach, num_samples, samples));
        }
        return result;
    }

    /** Far enough out to hold the whole vignetted region, with a margin to see its edge. */
    public static double reach_for(Field fld) {
        double widest = Math.max(
                Math.max(scale(fld.vlx), scale(fld.vux)),
                Math.max(scale(fld.vly), scale(fld.vuy)));
        return 1.15 * Math.max(1.0, widest);
    }

    /** The factor as {@link Field#apply_vignetting} uses it; a negative factor expands. */
    public static double scale(double factor) {
        return factor == 0.0 ? 1.0 : 1.0 - factor;
    }

    /** Whether a pupil coordinate lies in the region the factors sample as ray-optics maps them. */
    public static boolean inside_piecewise(double x, double y, Field fld) {
        double xScale = fld.vignetting_scale_x(x);
        double yScale = fld.vignetting_scale_y(y);
        if (!(xScale > 0.0) || !(yScale > 0.0))
            return false;
        double u = x / xScale, v = y / yScale;
        return u * u + v * v <= 1.0;
    }

    /**
     * Whether a pupil coordinate lies in the single translated ellipse whose four extremes
     * are the measured factors: {@code x' = (a - b)/2 + x (a + b)/2} with {@code a = 1 - vux}
     * and {@code b = 1 - vlx}, and likewise in y.
     *
     * <p>The same family as the piecewise map and through the same four points, but without
     * the kink on the axes - the experiment recorded in Documentation/OPTIMIZER.md. Kept here
     * so a map can be compared against both candidates whether or not the tracer offers it.
     */
    public static boolean inside_ellipse(double x, double y, Field fld) {
        double xScale = ellipse_scale(fld.vlx, fld.vux);
        double yScale = ellipse_scale(fld.vly, fld.vuy);
        if (!(xScale > 0.0) || !(yScale > 0.0))
            return false;
        double u = (x - ellipse_offset(fld.vlx, fld.vux)) / xScale;
        double v = (y - ellipse_offset(fld.vly, fld.vuy)) / yScale;
        return u * u + v * v <= 1.0;
    }

    public static double ellipse_scale(double lower, double upper) {
        return 1.0 - 0.5 * (lower + upper);
    }

    public static double ellipse_offset(double lower, double upper) {
        return 0.5 * (lower - upper);
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values)
            if (v == value)
                return true;
        return false;
    }
}
