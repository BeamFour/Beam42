package org.redukti.jfotoptix.patterns;

/**
 Ray distribution pattern descriptor

 This class describes distribution pattern and ray density used
 for light ray distribution over surfaces during light
 propagation.

 Ray density is expressed as average number of rays along
 surface radius.
 */
public class Distribution {

    Pattern _pattern;
    int _radial_density;
    double _scaling;

    /** Creates a distribution pattern with specified pattern,
     radial ray density and scaling.

     The scaling ratio parameter may be used to avoid
     distributing rays too close to the surface edge. */
    public Distribution (Pattern pattern,
                         int radial_density,
                         double scaling)
    {
        if (radial_density < 1)
            throw new IllegalArgumentException ("ray distribution radial density must be greater than 1");
        this._pattern = pattern;
        this._radial_density = radial_density;
        this._scaling = scaling;
    }

    void set_pattern (Pattern p)
    {
        _pattern = p;
    }

    Pattern get_pattern ()
    {
        return _pattern;
    }

    int get_radial_density ()
    {
        return _radial_density;
    }

    void set_radial_density (int density)
    {
        _radial_density = density;
    }

    double get_scaling ()
    {
        return _scaling;
    }

    void set_scaling (double margin)
    {
        _scaling = margin;
    }

    void set_uniform_pattern ()
    {
        switch (_pattern)
        {
            case SagittalDist:
            case MeridionalDist:
            case CrossDist:
                _pattern = Pattern.DefaultDist;
            default:;
        }
    }

}
