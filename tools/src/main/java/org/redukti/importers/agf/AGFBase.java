package org.redukti.importers.agf;

import org.redukti.rayoptics.seq.Glass;

public abstract class AGFBase {

    public String _manufacturer;
    public String _name;
    double[] _coefs;
    double _dpgF;
    double _relative_cost;

    public AGFBase(String manufacturer,String name, double[] coefs) {
        this._manufacturer = manufacturer;
        this._name = name;
        this._coefs = coefs;
        this._dpgF = 0;
        this._relative_cost = 0;
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
        return (get_measurement_index(Glass.d) - 1.0)
                / (get_measurement_index(Glass.F)
                - get_measurement_index(Glass.C));
    }

    public double get_abbe_ve() {
        return (get_measurement_index(Glass.e) - 1.0)
                / (get_measurement_index(Glass.F_)
                - get_measurement_index(Glass.C_));
    }

    public void set_dgpF(double value) {
        _dpgF = value;
    }
    public void set_relative_cost(double value) {
        _relative_cost = value;
    }

    public double get_dpgF() {
        return _dpgF;
    }
    public double get_relative_cost() {
        return _relative_cost;
    }
}
