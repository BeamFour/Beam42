package org.redukti.jfotoptix.material;

import static org.redukti.jfotoptix.math.MathUtils.square;

/**
 * Air optical material model
 * This class models optical properties of air. Refractive index
 * of air depends on temperature and pressure.
 * <p>
 * This class provides different formulas:
 * <ol>
 * <li>AirBirch94 : described in Birch, Metrologia, 1994, 31, 315.</li>
 * <li>AirKohlrausch68 : described in F. Kohlrausch, Praktische Physik, 1968, 1, 408}</li>
 * </ol>
 * <p>
 * Global variables air and std_air are available with
 * default parameters and Birch model.
 */
public class Air extends MaterialBase {

    /**
     * Standard air pressure is 101325 @em Pa
     */
    static final double std_pressure = 101325.;
    public static final Air std_air = new Air(AirFormula.AirBirch94Formula);
    public static final Air air = new Air(AirFormula.AirBirch94Formula);

    public final double _pressure;
    public final AirFormula airFormula;

    public Air(AirFormula formula, double pressure) {
        super("air", 20.0);
        this.airFormula = formula;
        this._pressure = pressure;
    }

    public Air(AirFormula formula, double pressure, double temp) {
        super("air", temp);
        this.airFormula = formula;
        this._pressure = pressure;
    }

    public Air(AirFormula formula) {
        this(formula, std_pressure);
    }


    @Override
    public boolean is_opaque() {
        return false;
    }

    @Override
    public boolean is_reflecting() {
        return false;
    }

    @Override
    public double get_internal_transmittance(double wavelen, double thickness) {
        // FIXME find a formula
        return 1.0;
    }

    @Override
    public double get_refractive_index(double wavelen) {
        switch (airFormula) {
            case AirBirch94Formula: {
                // Birch, Metrologia, 1994, 31, 315

                // _temperature in celsius
                // _pressure in pascal

                double s2 = square(1 / (wavelen / 1000.0));

                double ref
                        = /*1.0 +*/ 1e-8
                        * (+8342.54 + 2406147.0 / (130.0 - s2) + 15998.0 / (38.9 - s2));

                return 1.0
                        + (ref /*- 1.0*/)
                        * (_pressure
                        * (1.0
                        + _pressure * (60.1 - 0.972 * _temperature)
                        * 1e-10))
                        / (96095.43 * (1.0 + 0.003661 * _temperature));
            }

            case AirKohlrausch68Formula: {
                // F. Kohlrausch, Praktische Physik, 1968, 1, 408

                double w2 = square(wavelen / 1000.0);

                double nref = 1.0
                        + (+6432.8 + (2949810.0 * w2) / (146.0 * w2 - 1.0)
                        + (25540.0 * w2) / (41.0 * w2 - 1.0))
                        * 1e-8;

                return 1.0
                        + (((nref - 1.0) * (_pressure / std_pressure))
                        / (1.0 + (_temperature - 15.0) * 0.0034785));
            }
            default:
                throw new IllegalStateException();
        }
    }

    public double get_extinction_coef(double wavelen) {
        // FIXME find a formula
        return 0.0;
    }

}
