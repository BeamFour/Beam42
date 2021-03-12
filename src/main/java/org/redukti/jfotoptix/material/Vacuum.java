package org.redukti.jfotoptix.material;

public class Vacuum extends MaterialBase {

    public Vacuum() {
        super("Vacuum");
    }

    public boolean is_opaque() {
        return false;
    }

    public boolean is_reflecting() {
        return false;
    }

    public double get_internal_transmittance(double wavelen, double thickness) {
        return 1.0;
    }

    public double get_extinction_coef(double wavelen) {
        return 0.0;
    }

    public double get_refractive_index(double wavelen) {
        return 1.0;
    }
}
