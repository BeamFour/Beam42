// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.specs;

import java.util.HashMap;
import java.util.Map;

public class WvlSpec {

    static Map<String, Double> spectra;
    static Map<String, Double> spectra_uc;

    static {
        spectra = new HashMap<>();
        spectra.put("Nd", 1060.0);
        spectra.put("t", 1013.98);
        spectra.put("s", 852.11);
        spectra.put("r", 706.5188);
        spectra.put("C", 656.2725);
        spectra.put("C'", 643.8469);
        spectra.put("He-Ne", 632.8);
        spectra.put("D", 589.2938);
        spectra.put("d", 587.5618);
        spectra.put("e", 546.074);
        spectra.put("F", 486.1327);
        spectra.put("F'", 479.9914);
        spectra.put("g", 435.8343);
        spectra.put("h", 404.6561);
        spectra.put("i", 365.014);

        spectra_uc = new HashMap<>();
        for (String k: spectra.keySet()) {
            spectra_uc.put(k.toUpperCase(), spectra.get(k));
        }
    }

    public int reference_wvl;
    public double coating_wvl;

    public double[] wavelengths;
    public double[] spectral_wts;

    public WvlSpec(WvlWt[] wlwts, int ref_wl, boolean do_init) {
        if (do_init) {
            set_from_list(wlwts);
        } else {
            wavelengths = new double[0];
            spectral_wts = new double[0];
        }
        reference_wvl = ref_wl;
        coating_wvl = 550.0;
    }

    public WvlSpec(WvlWt[] wlwts, int ref_wl) {
        this(wlwts, ref_wl, true);
    }

    void set_from_list(WvlWt[] wlwts) {
        wavelengths = new double[wlwts.length];
        spectral_wts = new double[wlwts.length];
        for (int i = 0; i < wlwts.length; i++) {
            wavelengths[i] = wlwts[i].wvl;
            spectral_wts[i] = wlwts[i].wt;
        }
    }

    /**
     * Return wvl in nm, where wvl can be a spectral line
     *
     * @param key a string with a spectral line identifier. Case insensitive
     * @return float: the wavelength in nm
     */
    public static double get_wavelength(String key) {
        Double d = spectra_uc.get(key.toUpperCase());
        if (d == null)
            throw new IllegalArgumentException("Unknown wavelength '" + key + "'");
        return d;
    }

    public void update_model() {
        // TODO self.calc_colors()
    }

    public void apply_scale_factor(double scale_factor) {}

    public double central_wvl() {
        return wavelengths[reference_wvl];
    }
}
