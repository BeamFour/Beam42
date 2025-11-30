// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
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
    /**
     * optical power of system
     */
    public double power;
    /**
     * effective focal length
     */
    public double efl;
    /**
     * object space focal length, f
     */
    public double fl_obj;
    /**
     * image space focal length, f'
     */
    public double fl_img;
    /**
     * distance from the 1st interface to the front principle plane
     */
    public double pp1;
    /**
     * distance from the last interface to the rear principle plane
     */
    public double ppk;
    /**
     * distance from the front principle plane to the rear principle
     * plane
     */
    public double pp_sep;
    /**
     * front focal length, distance from the 1st interface to the front
     * focal point
     */
    public double ffl;
    /**
     * back focal length, distance from the last interface to the back
     * focal point
     */
    public double bfl;
    /**
     * focal ratio at working conjugates, f/#
     */
    public double fno;
    /**
     * transverse magnification
     */
    public double m;
    /**
     * reduction ratio, -1/m
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


    public StringBuilder toString(StringBuilder sb) {
        // list the first order properties
        sb.append(String.format("efl        %12.4g", efl)).append(System.lineSeparator());
        sb.append(String.format("f          %12.4g", fl_obj)).append(System.lineSeparator());
        sb.append(String.format("f'         %12.4g", fl_img)).append(System.lineSeparator());
        sb.append(String.format("ffl        %12.4g", ffl)).append(System.lineSeparator());
        sb.append(String.format("pp1        %12.4g", pp1)).append(System.lineSeparator());
        sb.append(String.format("bfl        %12.4g", bfl)).append(System.lineSeparator());
        sb.append(String.format("ppk        %12.4g", ppk)).append(System.lineSeparator());
        sb.append(String.format("pp sep     %12.4g", pp_sep)).append(System.lineSeparator());
        sb.append(String.format("f/#        %12.4g", fno)).append(System.lineSeparator());
        sb.append(String.format("m          %12.4g", m)).append(System.lineSeparator());
        sb.append(String.format("red        %12.4g", red)).append(System.lineSeparator());
        sb.append(String.format("obj_dist   %12.4g", obj_dist)).append(System.lineSeparator());
        sb.append(String.format("obj_ang    %12.4g", obj_ang)).append(System.lineSeparator());
        sb.append(String.format("enp_dist   %12.4g", enp_dist)).append(System.lineSeparator());
        sb.append(String.format("enp_radius %12.4g", enp_radius)).append(System.lineSeparator());
        sb.append(String.format("na obj     %12.4g", obj_na)).append(System.lineSeparator());
        sb.append(String.format("n obj      %12.4g", n_obj)).append(System.lineSeparator());
        sb.append(String.format("img_dist   %12.4g", img_dist)).append(System.lineSeparator());
        sb.append(String.format("img_ht     %12.4g", img_ht)).append(System.lineSeparator());
        sb.append(String.format("exp_dist   %12.4g", exp_dist)).append(System.lineSeparator());
        sb.append(String.format("exp_radius %12.4g", exp_radius)).append(System.lineSeparator());
        sb.append(String.format("na img     %12.4g", img_na)).append(System.lineSeparator());
        sb.append(String.format("n img      %12.4g", n_img)).append(System.lineSeparator());
        sb.append(String.format("optical invariant %12.4g", opt_inv)).append(System.lineSeparator());
        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
