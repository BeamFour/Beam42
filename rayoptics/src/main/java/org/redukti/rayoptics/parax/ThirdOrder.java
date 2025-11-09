// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.parax;

import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.elem.profiles.SurfaceProfile;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;

import java.util.Map;
import java.util.TreeMap;

public class ThirdOrder {

    /**
     * Compute Seidel aberration coefficents.
     */
    public static  Map<Integer,ThirdOrderData> compute_third_order(OpticalModel opt_model) {
        var seq_model = opt_model.seq_model;
        var n_before = seq_model.central_rndx(0);
        var parax_data  = opt_model.optical_spec.parax_data;
        var ax_ray = parax_data.ax_ray;
        var pr_ray  = parax_data.pr_ray;
        var fod = parax_data.fod;
        var opt_inv = fod.opt_inv;
        var opt_inv_sqr = opt_inv*opt_inv;

        Map<Integer,ThirdOrderData> third_order = new TreeMap<>();
        int p = 0;
        for (int c = 1; c < ax_ray.size()-1; c++) {
            var n_after = seq_model.central_rndx(c);
            n_after = seq_model.z_dir.get(c).value > 0 ? n_after : -n_after;
            var cv = seq_model.ifcs.get(c).profile.cv;

            var A = n_after * ax_ray.get(c).aoi;
            var Abar = n_after * pr_ray.get(c).aoi;
            var P = cv*(1./n_after - 1./n_before);
            var delta_slp = ax_ray.get(c).slp/n_after - ax_ray.get(p).slp/n_before;
            var SIi = -(A*A) * ax_ray.get(c).ht * delta_slp;
            var SIIi = -A*Abar * ax_ray.get(c).ht * delta_slp;
            var SIIIi = -(Abar*Abar) * ax_ray.get(c).ht * delta_slp;
            var SIVi = -opt_inv_sqr * P;
            var delta_n_sqr = 1./(n_after*n_after) - 1./(n_before*n_before);
            var SVi = -Abar*(Abar * Abar * delta_n_sqr * ax_ray.get(c).ht -
                     (opt_inv + Abar * ax_ray.get(c).ht)*pr_ray.get(c).ht*P);
            var data = new ThirdOrderData(c,SIi, SIIi, SIIIi, SIVi, SVi);
            third_order.put(c,data);
            if (seq_model.ifcs.get(c).profile instanceof EvenPolynomial) {
                aspheric_seidel_contribution(seq_model,parax_data,c,n_before,n_after,data);
            }
        }
        return third_order;
    }

    public static double calc_4th_order_aspheric_term(SurfaceProfile p) {
        double G = 0.;
        if (p instanceof EvenPolynomial evp) {
            var cv = evp.cv;
            var cc = evp.cc;
            G = cc*(cv*cv*cv)/8.0 + evp.get_by_order(4);
        }
        return G;
    }

    private static double delta_E(double z, double y, double u, double n) {
        return -z/(n*y*(y + z*u));
    }

    public static void aspheric_seidel_contribution(SequentialModel seq_model,ParaxData parax_data,int i, double n_before, double n_after,ThirdOrderData third_order) {
        var ax_ray = parax_data.ax_ray;
        var pr_ray  = parax_data.pr_ray;
        var fod = parax_data.fod;
        double e;
        if (pr_ray.get(i).slp == 0) {
            e = pr_ray.get(i).ht/ax_ray.get(i).ht;
        }
        else {
            var z = -pr_ray.get(i).ht/pr_ray.get(i).slp;
            e = fod.opt_inv*delta_E(z, ax_ray.get(i).ht, ax_ray.get(i).slp, n_after);
        }
        var G = calc_4th_order_aspheric_term(seq_model.ifcs.get(i).profile);
        if (G == 0.0)
            return;
        var delta_n = n_after - n_before;
        third_order.SI_star = 8.0*G*delta_n*Math.pow(ax_ray.get(i).ht,4);
        third_order.SII_star = third_order.SI_star*e;
        third_order.SIII_star = third_order.SI_star*e*e;
        third_order.SIV_star = 0.0;
        third_order.SV_star = third_order.SI_star*e*e*e;
    }

    /**
     * Convert Seidel coefficients to wavefront aberrations
     */
    public static Seidel_WaveFront seidel_to_wavefront(ThirdOrderData seidel, double central_wvl) {
        double W040 = 0.125*seidel.SI /central_wvl;
        double W131 = 0.5*seidel.SII /central_wvl;
        double W222 = 0.5*seidel.SIII /central_wvl;
        double W220 = 0.25*(seidel.SIV + seidel.SIII)/central_wvl;
        double W311 = 0.5*seidel.SV /central_wvl;
        return new Seidel_WaveFront(W040, W131, W222, W220, W311);
    }

    /**
     * Convert Seidel coefficients to transverse ray aberrations
     */
    public static Seidel_Transverse seidel_to_transverse_aberration(ThirdOrderData seidel, double ref_index, double slope)  {
        double cnvrt = 1.0/(2.0*ref_index*slope);
        // TSA = transverse spherical aberration
        var TSA = cnvrt*seidel.SI;
        // TCO = tangential coma
        var TCO = cnvrt*3.0*seidel.SII;
        // TAS = tangential astigmatism
        var TAS = cnvrt*(3.0*seidel.SIII + seidel.SIV);
        // SAS = sagittal astigmatism
        var SAS = cnvrt*(seidel.SIII + seidel.SIV);
        // PTB = Petzval blur
        var PTB = cnvrt*seidel.SIV;
        // DST = distortion
        var DST = cnvrt*seidel.SV;
        return new Seidel_Transverse(TSA, TCO, TAS, SAS, PTB, DST);
    }

    /**
     * Convert Seidel coefficients to astigmatic and Petzval curvatures
     */
    public static Seidel_FieldCurv seidel_to_field_curv(ThirdOrderData seidel, double ref_index, double opt_inv) {
        double cnvrt = ref_index/(opt_inv*opt_inv);
        // TCV = curvature of the tangential image surface
        var TCV = cnvrt*(3.0*seidel.SIII + seidel.SIV);
        // SCV = curvature of the sagittal image surface
        var SCV = cnvrt*(seidel.SIII + seidel.SIV);
        // PCV = curvature of the Petzval surface
        var PCV = cnvrt*seidel.SIV;
        return new Seidel_FieldCurv(TCV, SCV, PCV);
    }
}
