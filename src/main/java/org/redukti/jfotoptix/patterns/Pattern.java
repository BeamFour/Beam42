package org.redukti.jfotoptix.patterns;

public enum Pattern {
    /** Preferred distribution pattern for a given shape */
    DefaultDist,
    /** Sagittal plane distribution (along the X axis, X/Z plane) */
    SagittalDist,
    /** Meridional plane distribution (along the Y axis, Y/Z plane) */
    MeridionalDist,
    /** Tangential plane distribution, same as @ref MeridionalDist */
    //TangentialDist = MeridionalDist,
    /** Sagittal and Meridional distribution combined */
    CrossDist,
    /** Square pattern distribution */
    SquareDist,
    /** Triangular pattern distribution */
    TriangularDist,
    /** Hexapolar pattern, suitable for circular shapes */
    HexaPolarDist,
    /** Random distribution */
    RandomDist
}
