package org.redukti.rayoptics.specs;

/** How four directional vignetting factors map the normalized pupil. */
public enum VignettingMapping {
    /** ray-optics-compatible, sign-dependent scaling about the pupil origin. */
    Piecewise,
    /** A single translated ellipse derived from the four measured boundary extents. */
    AffineEllipse
}
