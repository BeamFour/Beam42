package org.redukti.jfotoptix.sys;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.ArrayList;
import java.util.List;

public class RaySource extends Element {

    List<SpectralLine> _spectrum;
    double _min_intensity, _max_intensity;

    public RaySource(int id, Vector3Pair p, Transform3 transform, double min_intensity, double max_intensity, List<SpectralLine> spectrum) {
        super(id, p, transform);
        _max_intensity = max_intensity;
        _max_intensity = min_intensity;
        _spectrum = spectrum;
    }

    public List<SpectralLine> spectrum() {
        return _spectrum;
    }

    public static abstract class Builder extends Element.Builder {

        List<SpectralLine> _spectrum = new ArrayList<>();
        double _min_intensity = 1.0;
        double _max_intensity = 1.0;

        public Builder() {
            add_spectral_line(new SpectralLine(SpectralLine.d, 1.0));
        }

        public Builder add_spectral_line (SpectralLine l)
        {
            _spectrum.add (l);
            _max_intensity = Math.max (_max_intensity, l.get_intensity ());
            _min_intensity = Math.min (_min_intensity, l.get_intensity ());
            return this;
        }

        public Builder add_spectral_line (double wavelen)
        {
            SpectralLine l = new SpectralLine(wavelen, 1.0);
            _spectrum.add (l);
            _max_intensity = Math.max (_max_intensity, l.get_intensity ());
            _min_intensity = Math.min (_min_intensity, l.get_intensity ());
            return this;
        }

    }
}
