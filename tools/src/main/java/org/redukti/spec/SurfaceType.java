package org.redukti.spec;

import org.redukti.jfotoptix.medium.GlassMap;

public class SurfaceType {
    public String _id;
    public double _radius;
    public double _thickness;
    public double _diameter;
    public boolean _is_aperture_stop;
    public boolean _is_field_stop;
    public double _nd;
    public double _vd;
    public String _glass_name;
    // Aspheric
    public double _k;
    public double[] _coeffs;

    public double[] _thickness_by_scenario;
    public double[] _diameter_by_scenario;

    public SurfaceType(String id, boolean isStop, double radius, double thickness, double diameter, double nd, double vd, String glassName) {
        this._id = id;
        this._radius = radius;
        this._thickness = thickness;
        this._diameter = diameter;
        this._is_aperture_stop = isStop;
        this._nd = nd;
        this._vd = vd;
        this._glass_name = glassName;
    }
    public SurfaceType set_thickness_by_scenario(double[] thickness_by_scenario) {
        this._thickness_by_scenario = thickness_by_scenario;
        return this;
    }
    public SurfaceType set_diameter_by_scenario(double[] diameter_by_scenario) {
        this._diameter_by_scenario = diameter_by_scenario;
        return this;
    }
    public boolean is_aperture_stop() {
        return _is_aperture_stop;
    }
    public boolean is_field_stop() {
        return _is_field_stop;
    }
    public double get_diameter() {
        return _diameter;
    }
    public double get_thickness() {
        return _thickness;
    }
    public double get_radius_of_curvature() {
        return _radius;
    }
    public double[] get_aspheric_coeffs() {
        return _coeffs;
    }
    public double get_conic_k() {
        return _k;
    }
    public double get_refractive_index() {
        return _nd;
    }
    public double get_abbe_vd() {
        return _vd;
    }
    public String get_glass_name() {
        return _glass_name;
    }
    public boolean is_aspheric() {
        return _k != 0 || (_coeffs != null && _coeffs.length > 0);
    }
    public StringBuilder toOptBenchStr(StringBuilder sb) {
        sb.append(_id).append("\t");
        if (_is_aperture_stop)
            sb.append("AS");
        else if (_is_field_stop)
            sb.append("FS");
        else
            sb.append(_radius);
        sb.append("\t").append(_thickness).append("\t");
        if (_nd != 0.0)
            sb.append(_nd);
        sb.append("\t");
        sb.append(_diameter).append("\t");
        if (_nd != 0.0)
            sb.append(_vd);
        sb.append("\t");
        if (_nd != 0.0 && _glass_name != null)
            sb.append(_glass_name);
        sb.append("\n");
        return sb;
    }
    public StringBuilder asphericsToOptBenchStr(StringBuilder sb) {
        if (_k == 0 && (_coeffs == null || _coeffs.length == 0))
            return sb;
        sb.append(_id).append("\t");
        sb.append(_radius).append("\t");
        sb.append(_k).append("\t");
        for (int i = 0; i < _coeffs.length; i++) {
            if (_coeffs[i] == 0.0)
                break;
            sb.append(_coeffs[i]).append("\t");
        }
        sb.append("\n");
        return sb;
    }
    public static StringBuilder asphericMarkdownTableHeader(StringBuilder sb) {
        sb.append("## Aspherical Data").append("\n");
        sb.append("| ID  | k   | A4  | A6  | A8  | A10 | A12 | A14 | A16 | A18 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        return sb;
    }
    public StringBuilder ashericToMarkdownTableRow(StringBuilder sb) {
        sb.append("| ").append(_id);
        sb.append(" | ").append(_k);
        for (int i = 0; i < 8; i++) {
            if (_coeffs != null && i < _coeffs.length)
                sb.append(" | ").append(_coeffs[i]);
            else
                sb.append(" | 0 ");
        }
        sb.append(" |\n");
        return sb;
    }

    public static StringBuilder toMarkdownTableHeader(StringBuilder sb) {
        sb.append("## Surface Data").append("\n");
        sb.append("Note that where glass types are shown the refractive index and abbe number is as per assigned glass type\n\n");
        sb.append("| ID  | Radius | Thickness | Diameter | nd  | vd  | Glass Make | Glass |\n");
        sb.append("| --- | ---    | ---       | ---      | --- | --- | ---        | ---   |\n");
        return sb;
    }
    public StringBuilder toMarkdownTableRow(StringBuilder sb) {
        sb.append("| ").append(_id).append(" | ");
        if (_is_aperture_stop)
            sb.append("AS");
        else if (_is_field_stop)
            sb.append("FS");
        else
            sb.append(_radius);
        sb.append(" | ").append(_thickness).append(" | ");
        sb.append(_diameter).append(" | ");
        String glassMaker = "";
        if (_nd != 0.0 && _glass_name != null) {
            GlassMap glass = GlassMap.glassByName(_glass_name);
            if (glass != null) {
                _nd = glass.nd;
                _vd = glass.vd;
                glassMaker = glass.get_manufacturer();
            }
        }
        if (_nd != 0.0)
            sb.append(_nd);
        sb.append(" | ");
        if (_nd != 0.0)
            sb.append(_vd);
        sb.append(" | ");
        if (_nd != 0.0 && _glass_name != null) {
            sb.append(glassMaker)
                    .append(" | ")
                    .append(_glass_name);
        }
        sb.append(" |\n");
        return sb;
    }

    public String toString() {
        return toOptBenchStr(new StringBuilder()).toString();
    }
}
