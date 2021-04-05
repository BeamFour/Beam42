package org.redukti.jfotoptix.material;

public class Abbe extends Dielectric {

    /**
     * Specify Abbe number type used by the @ref Abbe class
     */
    public enum AbbeFormula {
        AbbeVd, //< Abbe number of Fraunhofer @em d line
        AbbeVe, //< Abbe number of Fraunhofer @em e line
    }

    double _n, _q, _a;
    AbbeFormula _m;

    /**
     * Create an abbe glass model
     */
    public Abbe(AbbeFormula m, double n, double v, double dpgF) {
        super(m.name());
        this._m = m;
        _n = n;
        _q = (n - 1.) / v;
        _a = (v * -0.001682) + 0.6438 + dpgF;
    }

    Abbe(AbbeFormula m, double n, double v) {
        this(m, n, v, 0.0);
    }

    @Override
    public double get_measurement_index(double wavelen) {
        double wl = wavelen / 1000.;
        double w2 = wl * wl;
        double w3 = w2 * wl;
        double f;

        switch (_m) {
            // fitting code can be found is /extra/abbe_model_fit/ directory

            case AbbeVd:
                f = (_a * -6.11873891971188577088 + 1.17752614766485175224)
                        + (_a * 18.27315722388047447566 + -8.93204522498095698779) / wl
                        + (_a * -14.55275321129051135927 + 7.91015964461522003148) / w2
                        + (_a * 3.48385106908642905310 + -1.80321117937358499361) / w3;
                break;

            case AbbeVe:
                f = (_a * -5.70205459879993181715 + 0.73560912822245871912)
                        + (_a * 17.84619335902774039937 + -8.71504708663084315390) / wl
                        + (_a * -14.30050903441605747446 + 7.77787634432116181671) / w2
                        + (_a * 3.41225047218704347074 + -1.76619259848202947438) / w3;
                break;
            default:
                throw new IllegalStateException();
        }

        return _n + _q * f;
    }
}
