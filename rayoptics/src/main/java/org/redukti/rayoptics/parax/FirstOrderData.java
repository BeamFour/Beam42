package org.redukti.rayoptics.parax;

/**
 * Container class for first order optical properties
 *
 *     All quantities are based on paraxial ray tracing. The last interface is
 *     the image-1 interface.
 *
 *     Attributes:
 *         opt_inv: optical invariant
 *         efl: effective focal length
 *         pp1: distance of front principle plane from 1st interface
 *         ppk: distance of rear principle plane from last interface
 *         ffl: front focal length
 *         bfl: back focal length
 *         fno: focal ratio at working conjugates, f/#
 *         red: reduction ratio
 *         n_obj: refractive index at central wavelength in object space
 *         n_img: refractive index at central wavelength in image space
 *         obj_dist: object distance
 *         img_dist: paraxial image distance
 *         obj_ang: maximum object angle (degrees)
 *         img_ht: image height
 *         enp_dist: entrance pupil distance from 1st interface
 *         enp_radius: entrance pupil radius
 *         exp_dist: exit pupil distance from last interface
 *         exp_radius: exit pupil radius
 *         obj_na: numerical aperture in object space
 *         img_na: numerical aperture in image space
 */
public class FirstOrderData {
    /**
     * optical invariant
     */
    public double opt_inv;
    public double power;
    /**
     * effective focal length
     */
    public double efl;
    /**
     * distance of front principle plane from 1st interface
     */
    public double pp1;
    /**
     * distance of rear principle plane from last interface
     */
    public double ppk;
    /**
     * front focal length
     */
    public double ffl;
    /**
     * back focal length
     */
    public double bfl;
    /**
     * focal ratio at working conjugates, f/#
     */
    public double fno;
    public double m;
    /**
     * reduction ratio
     */
    public double red;
    /**
     * refractive index at central wavelength in object space
     */
    public double n_obj;
    /**
     * refractive index at central wavelength in image space
     */
    public double n_img;
    /**
     * object distance
     */
    public double obj_dist;
    /**
     * paraxial image distance
     */
    public double img_dist;
    /**
     * maximum object angle (degrees)
     */
    public double obj_ang;
    /**
     * image height
     */
    public double img_ht;
    /**
     * entrance pupil distance from 1st interface
     */
    public double enp_dist;
    /**
     * entrance pupil radius
     */
    public double enp_radius;
    /**
     * exit pupil distance from last interface
     */
    public double exp_dist;
    /**
     * exit pupil radius
     */
    public double exp_radius;
    /**
     * numerical aperture in object space
     */
    public double obj_na;
    /**
     * numerical aperture in image space
     */
    public double img_na;
}
