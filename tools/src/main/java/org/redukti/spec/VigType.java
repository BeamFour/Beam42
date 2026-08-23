package org.redukti.spec;

/**
 * Which aperture and vignetting calculation to run once a model is built.
 * <p>
 * Note the direction each one runs in. {@link #SetPupil} derives the pupil spec
 * from the authored stop diameter, so it changes the f/# and leaves apertures
 * alone. {@link #SetStopAperture} and {@link #SetFnum} go the other way: they
 * hold the f/# and size the stop to satisfy it.
 */
public enum VigType {
    None,
    Paraxial,
    /** Vignetting factors from the existing apertures. */
    SetVig,
    /** Pupil spec derived from the existing stop diameter. Apertures unchanged. */
    SetPupil,
    /** Stop sized to satisfy the pupil spec, then vignetting recalculated. */
    SetStopAperture,
    /** Vignetting, then every clear aperture sized to pass the vignetted rays. */
    SetApertures,
    /**
     * Drive the whole aperture set from the defined f/#: size the stop to
     * satisfy it, then size every other clear aperture to pass the resulting
     * vignetted rays.
     * <p>
     * For prescriptions that quote an exact f/# but whose apertures were
     * estimated off a drawing - the stop is trusted to the spec and everything
     * else is rebuilt from the rays.
     */
    SetFnum
}
