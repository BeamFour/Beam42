package org.redukti.jfotoptix.material;

public class Mirror extends MaterialBase {

    public static final Mirror mirror = new Mirror();

    public Mirror() {
        super("mirror");
    }

    @Override
    public boolean is_opaque() {
        return true;
    }

    @Override
    public boolean is_reflecting() {
        return true;
    }

    @Override
    public double get_refractive_index(double wavelen) {
        return 1.0;
    }

    @Override
    public double get_internal_transmittance (double wavelen,
                                              double thickness) {
        return 0.0;
    }

    @Override
    public double get_extinction_coef (double wavelen) {
        return 9999.0;
    }

}
