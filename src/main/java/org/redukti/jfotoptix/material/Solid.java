package org.redukti.jfotoptix.material;

public abstract class Solid extends MaterialBase {
    public final double _thermal_expansion;    // thermal expansion coefficient
    public final double _thermal_conductivity; // thermal conductivity W/m.K
    public final double _density;              // density g/cm^3
    public final double _young_modulus;
    public final double _poisson_ratio;

    public Solid (String name)
    {
        super(name);
        _thermal_expansion = 0.0;
        _thermal_conductivity = 0.0;
        _density = 0.0;
        _young_modulus = 0.0;
        _poisson_ratio = 0.0;
    }
}
