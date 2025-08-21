package org.redukti.jfotoptix.medium;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.MathUtils;

import java.text.DecimalFormat;

public abstract class AGFBase extends Dielectric {

    String _manufacturer;
    double[] _coefs;

    public static DecimalFormat decimalFormat = MathUtils.decimal_format(5);
    public static DecimalFormat decimalFormat2 = MathUtils.decimal_format(2);

    public AGFBase(String manufacturer,String name, double[] coefs) {
        super(name);
        this._manufacturer = manufacturer;
        this._coefs = coefs;
    }

    public GlassMap toGlassMap() {
        return new GlassMap(
                _manufacturer,
                get_name(),
                get_refractive_index(SpectralLine.d),
                get_refractive_index(SpectralLine.C),
                get_refractive_index(SpectralLine.F),
                get_abbe_vd(),
                0.0);
    }

    public String toCodeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("glasses.put(\"").append(get_name()).append("\", new GlassMap(\"");
        sb.append(_manufacturer).append("\", \"").append(get_name()).append("\", ");
        sb.append(decimalFormat.format(get_refractive_index(SpectralLine.d))).append(", ");
        sb.append(decimalFormat.format(get_refractive_index(SpectralLine.C))).append(", ");
        sb.append(decimalFormat.format(get_refractive_index(SpectralLine.F))).append(", ");
        sb.append(decimalFormat2.format(get_abbe_vd())).append(",0.0));");
        return sb.toString();
    }
}
