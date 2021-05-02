/*
The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet
 */


package org.redukti.jfotoptix.material;

import org.redukti.jfotoptix.light.SpectralLine;

public class Abbe extends Dielectric {

    /**
     * Specify Abbe number type used by the @ref Abbe class
     */
    public enum AbbeFormula {
        AbbeVd, //< Abbe number of Fraunhofer @em d line
        AbbeVe, //< Abbe number of Fraunhofer @em e line
    }

    double _n, _q, _a, _v;
    AbbeFormula _m;

    /**
     * Create an abbe glass model
     */
    public Abbe(AbbeFormula m, double n, double v, double dpgF) {
        super(m.name());
        this._m = m;
        _n = n;
        _v = v;
        _q = (n - 1.) / v;
        _a = (v * -0.001682) + 0.6438 + dpgF;
    }

    Abbe(AbbeFormula m, double n, double v) {
        this(m, n, v, 0.0);
    }

    @Override
    public double get_refractive_index(double wavelen) {
        if (m == AbbeFormula.AbbeVd && wavelen == SpectralLine.d)
            return _n;
        return super.get_refractive_index(wavelen);
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

    @Override
    public String toString() {
        return _m.name() +  "{nd=" + _n + ",vd="+ _v + ",d=" + get_refractive_index(SpectralLine.d) +'}';
    }
}
