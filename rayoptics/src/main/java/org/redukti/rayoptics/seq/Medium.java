package org.redukti.rayoptics.seq;

/**
 * Constant refractive index medium.
 */
public class Medium {
    String label;
    double n;
    String catalog_name;

    public Medium(String label, double n, String catalog_name) {
        this.label = label;
        this.n = n;
        this.catalog_name = catalog_name;
    }

    public Medium(String label, double n) {
        this(label, n, "");
    }


    /**
     * returns the interpolated refractive index at wv_nm
     * @param wv_nm the wavelength in nm for the refractive index query
     * @return float: the refractive index at wv_nm
     */
    public double rindex(double wv_nm) {
        return n;
    }
}
