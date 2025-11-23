package org.redukti.importers.agf;

import org.redukti.jfotoptix.light.SpectralLine;

public abstract class AGFBase {

    public String _manufacturer;
    public String _name;
    double[] _coefs;

    public AGFBase(String manufacturer,String name, double[] coefs) {
        this._manufacturer = manufacturer;
        this._name = name;
        this._coefs = coefs;
    }

    /**
     * Get material relative refractive index in measurment medium
     * at specified wavelen in @em nm.
     */
    public abstract double get_measurement_index(double wavelen);

    public double get_refractive_index(double wavelen) {
        return get_measurement_index(wavelen);
    }
    public double get_abbe_vd() {
        return (get_measurement_index(SpectralLine.d) - 1.0)
                / (get_measurement_index(SpectralLine.F)
                - get_measurement_index(SpectralLine.C));
    }

    public double get_abbe_ve() {
        return (get_measurement_index(SpectralLine.e) - 1.0)
                / (get_measurement_index(SpectralLine.F_)
                - get_measurement_index(SpectralLine.C_));
    }
}
