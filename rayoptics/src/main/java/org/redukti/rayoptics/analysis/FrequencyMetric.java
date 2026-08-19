package org.redukti.rayoptics.analysis;

/**
 * How the spatial frequency a pair of contrast rays realises is measured.
 *
 * <p>The two are not equivalent, and the choice affects both the calibration probe and
 * per-sample normalization. See REVIEW.md finding 9.
 */
public enum FrequencyMetric {

    /**
     * The difference of the two rays' image-space direction cosines.
     *
     * <p>Two rays converging on the image form fringes at {@code n.|delta d|/lambda}
     * wherever the exit pupil lies, so this needs no pupil location. But a ray's direction
     * is the wavefront normal, so the difference carries the pupil separation
     * <em>plus</em> the difference in wavefront slope between the two points - transverse
     * ray aberration, which is what optimization changes. Measured divergence from
     * {@link #EXIT_PUPIL} runs to 2.7 percent at full field tangential on the Otus
     * 50/1.4.
     *
     * <p>The historical default, and what every committed regression value was produced
     * with.
     */
    RAY_DIRECTION,

    /**
     * The separation of the two rays' coordinates on the exit-pupil reference sphere.
     *
     * <p>This is the coordinate Hopkins' OTF is defined over: his (10.26) shears the pupil
     * function in reduced exit-pupil coordinates and (10.28) fixes the scale. Unlike
     * {@link #RAY_DIRECTION} it is independent of the aberration being optimized.
     *
     * <p>Unavailable for an afocal system, where the reference sphere is infinite; the
     * calibration falls back to no correction there rather than silently using the other
     * metric.
     */
    EXIT_PUPIL
}
