package org.redukti.optim_rolmder;

import org.redukti.rayoptics.parax.FirstOrderData;

public class ParaxHelper {
        // efl: effective focal length
    public static final int Effective_focal_length = 0;
    // bfl: back focal length
    public static final int Back_focal_length = 1;
    // opt_inv: optical invariant
    public static final int Optical_invariant = 2;
    // obj_dist: object distance
    public static final int Object_distance = 3;
    // img_dist: paraxial image distance
    public static final int Image_distance = 4;
    public static final int Power = 5;
    // pp1: distance of front principle plane from 1st surface
    public static final int Pp1 = 6;
    // ppk: distance of rear principle plane from last surface
    public static final int Ppk = 7;
    // ffl: front focal length
    public static final int Ffl = 8;
    // fno: focal ratio at working conjugates, f/#
    public static final int Fno = 9;
    // enp_dist: entrance pupil distance from 1st surface
    public static final int Enp_dist = 10;
    // enp_radius: entrance pupil radius
    public static final int Enp_radius = 11;
    // exp_dist: exit pupil distance from last interface
    public static final int Exp_dist = 12;
    // exp_radius: exit pupil radius
    public static final int Exp_radius = 13;
    public static final int M_ = 14;
    // red: reduction ratio
    public static final int Red = 15;
    // n_obj: refractive index at central wavelength in object space
    public static final int N_obj = 16;
    // n_img: refractive index at central wavelength in image space
    public static final int N_img = 17;
    // img_ht: image height
    public static final int Img_ht = 18;
    // obj_ang: maximum object angle (degrees)
    public static final int Obj_ang = 19;
    // obj_na: numerical aperture in object space
    public static final int Obj_na = 20;
    // img_na: numerical aperture in image space
    public static final int Img_na = 21;

    public static String[] Names = {
    "Effective_focal_length",
    "Back_focal_length",
    "Optical_invariant",
    "Object_distance",
    "Image_distance",
    "Power",
    "Pp1",
    "Ppk",
    "Ffl",
    "Fno",
    "Enp_dist",
    "Enp_radius",
    "Exp_dist",
    "Exp_radius",
    "M",
    "Red",
    "N_obj",
    "N_img",
    "Img_ht",
    "Obj_ang",
    "Obj_na",
    "Img_na"
    };

    public static double[] asArray(FirstOrderData fod) {
        var v = new double[22];
        v[Effective_focal_length] = fod.efl;
        v[Back_focal_length] = fod.bfl;
        v[Optical_invariant] = fod.opt_inv;
        v[Object_distance] = fod.obj_dist;
        v[Image_distance] = fod.img_dist;
        v[Power] = fod.power;
        v[Pp1] = fod.pp1;
        v[Ppk] = fod.ppk;
        v[Ffl] = fod.ffl;
        v[Fno] = fod.fno;
        v[Enp_dist] = fod.enp_dist;
        v[Enp_radius] = fod.enp_radius;
        v[Exp_dist] = fod.exp_dist;
        v[Exp_radius] = fod.exp_radius;
        v[M_] = fod.m;
        v[Red] = fod.red;
        v[N_obj] = fod.n_obj;
        v[N_img] = fod.n_img;
        v[Img_ht] = fod.img_ht;
        v[Obj_ang] = fod.obj_ang;
        v[Obj_na] = fod.obj_na;
        v[Img_na] = fod.img_na;
        return v;
    }
}
