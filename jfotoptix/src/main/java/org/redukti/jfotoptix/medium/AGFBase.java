package org.redukti.jfotoptix.medium;

import org.redukti.jfotoptix.light.SpectralLine;

public abstract class AGFBase extends Dielectric {

    String _manufacturer;
    double[] _coefs;

    public AGFBase(String manufacturer,String name, double[] coefs) {
        super(name);
        this._manufacturer = manufacturer;
        this._coefs = coefs;
    }

    public GlassMap toGlassMap() {
        return new GlassMap(
                _manufacturer,
                get_name(),
                get_refractive_index(SpectralLine.d),
                get_refractive_index(SpectralLine.C),
                get_refractive_index(SpectralLine.F),
                get_abbe_vd(),
                0.0);
    }
}
