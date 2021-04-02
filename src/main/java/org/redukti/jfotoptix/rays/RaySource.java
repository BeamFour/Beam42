package org.redukti.jfotoptix.rays;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;

public class RaySource {

    Vector3Pair position;
    List<SpectralLine> _spectrum;
    double _min_intensity, _max_intensity;

    public RaySource(Vector3Pair position) {
        this.position = position;
        _max_intensity = 1.0;
        _max_intensity = 1.0;
        add_spectral_line(new SpectralLine(SpectralLine.d, 1.0));
    }


    public void add_spectral_line (SpectralLine l)
    {
        _spectrum.add (l);
        _max_intensity = Math.max (_max_intensity, l.get_intensity ());
        _min_intensity = Math.min (_min_intensity, l.get_intensity ());
    }
}
