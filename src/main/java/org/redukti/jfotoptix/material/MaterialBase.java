package org.redukti.jfotoptix.material;

import org.redukti.jfotoptix.io.Rgb;

import static org.redukti.jfotoptix.util.MathUtils.square;

public abstract class MaterialBase {
    public final String name;
    public double _temperature; // celcius

    public MaterialBase(String name, double temp) {
        this.name = name;
        this._temperature = temp;
    }

    public MaterialBase(String name) {
        this(name, 20.0);
    }

    /** Return true if material must be considered opaque for ray
     tracing */
    public abstract boolean is_opaque ();

    /** Return true if material may reflect most of the light and
     must be considered as a mirror when ignoring ray intensity
     computation during ray tracing. */
    public abstract boolean is_reflecting ();

    /** Get material internal transmittance for thickness in
     mm. Subclasses _must_ provide this function or the
     get_extinction_coef() function. */
    public double get_internal_transmittance (double wavelen,
                                               double thickness) {
        // compute internal transmittance from extinction coefficient
        // Beer-Lambert law

        // FIXME simplify and check
        double tr
                = Math.exp (-(4 * Math.PI * get_extinction_coef (wavelen) * 0.001 /* 1 mm */)
                / (wavelen * 1e-9f));

        return Math.pow (tr, thickness);
    }

    /** Get material internal transmittance for 1mm thickness. */
    public double get_internal_transmittance (double wavelen) {
        // compute internal transmittance from extinction coefficient
        return get_internal_transmittance (wavelen, 1.0);
    }

    /** Get material absolute refractive index at specified wavelen in @em nm. */
    public abstract double get_refractive_index (double wavelen);

    /** Get material relative refractive index in given medium at specified
     * wavelen in @em nm. */
    public double get_refractive_index (double wavelen, MaterialBase env) {
        return get_refractive_index (wavelen) / env.get_refractive_index (wavelen);
    }

    /** Get extinction coefficient. Subclasses _must_ provide this
     function or the get_internal_transmittance() function. */
    public double get_extinction_coef (double wavelen) {
        // Beer-Lambert law
        // FIXME check this formula
        return -(Math.log (get_internal_transmittance (wavelen, 1.0)) * (wavelen * 1e-9f))
                / (4 * Math.PI * 0.001 /* 1 mm */);
    }


    /** Get reflectance at normal incidence */
    public double get_normal_reflectance (MaterialBase from,
                                           double wavelen) {
        // default reflectance at normal incidence, valid for metal and dielectric
        // material
        // McGraw Hill, Handbook of optics, vol1, 1995, 5-10 (47)

        double n0 = from.get_refractive_index (wavelen);
        double k12 = square (get_extinction_coef (wavelen));
        double n1 = get_refractive_index (wavelen);
        double res = (square (n0 - n1) + k12) / (square (n0 + n1) + k12);

        return res;
    }

    /** Get transmittance at normal incidence */
    public double get_normal_transmittance (MaterialBase from,
                                             double wavelen) {
        // default transmittance at normal incidence, valid for non absorbing material
        // McGraw Hill, Handbook of optics, vol1, 1995, 5-8 (23)

        double n0 = from.get_refractive_index (wavelen);
        double n1 = get_refractive_index (wavelen);

        return (4.0 * n0 * n1) / square (n0 + n1);
    }

    /** Get material color and alpha */
    public Rgb get_color () {
        // FIXME color depends on material properties
        return new Rgb (1, 1, 1, 1);
    }

    public double get_temperature() {
        return _temperature;
    }

    public void set_temperature(double temp) {
        _temperature = temp;
    }
}
