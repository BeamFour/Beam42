package org.redukti.optim;

/**
 * The two meridians a directional goal can be expressed in.
 *
 * <p>Every goal that comes in a sagittal/tangential pair encodes the choice as the
 * same {@code 0}/{@code 1} index, so they share one definition here rather than each
 * declaring its own.
 *
 * <p>Fields lie on the y axis, which puts the sagittal meridian on x and the
 * tangential meridian on y. {@link #X} and {@link #Y} are aliases for goals such as
 * {@link GoalSpotDeviation} that speak in ray coordinates rather than in MTF
 * orientations; they are the same two values, named for the reader.
 */
public final class Orientation {

    /** Sagittal: the x meridian, perpendicular to the field direction. */
    public static final int SAGITTAL = 0;

    /** Tangential: the y meridian, along the field direction. */
    public static final int TANGENTIAL = 1;

    /** The sagittal meridian, named as a ray coordinate. */
    public static final int X = SAGITTAL;

    /** The tangential meridian, named as a ray coordinate. */
    public static final int Y = TANGENTIAL;

    private Orientation() {
    }

    /** Returns the orientation unchanged, rejecting anything that is not one of the two meridians. */
    public static int checked(int orientation) {
        if (orientation != SAGITTAL && orientation != TANGENTIAL)
            throw new IllegalArgumentException("orientation must be SAGITTAL (" + SAGITTAL
                    + ") or TANGENTIAL (" + TANGENTIAL + ") but was " + orientation);
        return orientation;
    }

    /** {@code "sag"} or {@code "tan"}, for goal descriptions. */
    public static String name(int orientation) {
        return orientation == TANGENTIAL ? "tan" : "sag";
    }
}
