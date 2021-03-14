package org.redukti.jfotoptix.material;

import static org.redukti.jfotoptix.math.MathUtils.square;

/**
 * Sellmeier model for optical glass material
 * This class models optical properties of dielectric
 * materials using @url http://en.wikipedia.org/wiki/Sellmeier_equation
 * {Sellmeier} refractive index dispersion formula:
 * @math $ n_\lambda = \sqrt{ A + \sum\limits_{i}^{} \frac{B_i \times
 * \lambda^2}{\lambda^2 - C_i}} $
 * <p>
 * with @math $\lambda$ the micrometer wavelength.
 */
public class Sellmeier extends Dielectric {

    double[] _coeff;
    double _constant;

    /**
     * Create an empty sellmeier model
     */
    public Sellmeier() {
        super("Sellmeier");
        _coeff = new double[0];
        _constant = 1.0;
    }

    /**
     * Create an 3rd order sellmeier model with given coefficients
     * and 1.0 constant
     */
    public Sellmeier(double K1, double L1, double K2, double L2, double K3, double L3) {
        _coeff = new double[6];
        _constant = 1.0;
        _coeff[0] = K1;
        _coeff[1] = L1;
        _coeff[2] = K2;
        _coeff[3] = L2;
        _coeff[4] = K3;
        _coeff[5] = L3;
    }

    public double get_measurement_index(double wavelen) {
        double w2 = square(wavelen / 1000.0);
        double n = _constant;

        for (int i = 0; i < _coeff.length; i += 2)
            n += (w2 * _coeff[i]) / (w2 - _coeff[i + 1]);

        return Math.sqrt(n);
    }
}
