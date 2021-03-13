package org.redukti.jfotoptix.shape;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;
import org.redukti.jfotoptix.patterns.Distribution;

import java.util.function.Function;

public interface Shape {

    /** Check if the (x,y) 2d point is inside 2d shape area */
    boolean inside (Vector2 point);

    /** Get points distributed on shape area with given pattern */
    void get_pattern (Function<Vector2,Void> f,
                            Distribution d,
                      boolean unobstructed);

    /** Get distance between origin and farthest shape edge */
    double max_radius ();

    /** Get distance between origin and nearest shape outter edge */
    double min_radius ();

    /** Get distance between origin and farthest shape edge in specified
     * direction */
    double get_outter_radius (Vector2 dir);

    /** Get distance between origin and nearest shape outter edge in specified
     * direction */
    double get_hole_radius (Vector2 dir);

    /** Get shape bounding box */
    Vector2Pair get_bounding_box ();

    /** Get number of contours polygones. This function returns
     value is greater than 1 if shape has hole(s). @see get_contour */
    int get_contour_count ();

    /** Get contour polygone points for specified contour id. First
     contour is always outter edge. @see get_contour_count */
    void get_contour (int contour,
                            Function<Vector2,Void> f,
                              double resolution);

//    /** Get shape teselation triangles */
//    void get_triangles (const math::Triangle<2>::put_delegate_t &f,
//                                double resolution) const = 0;

}
