package org.redukti.spec;

import org.redukti.jfotoptix.medium.GlassMap;

public class SurfaceType {

    public final static int ASPH_EVEN = 1;
    public final static int ASPH_EVEN_A2 = 2;
    public final static int ASPH_ODD = 3;

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
    public int _asph_type;
    public double _k;
    /**
     * Coefficients are stored in  a normalized way
     * For Even polynomials, first coefficient is 0, as this is the A2 term
     * For Odd polynomials first 2 coefficients are 0.
     * But in OpticalBench the data is output so that
     * these values are skipped
     */
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
        this._asph_type = 0;
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
    public double get_diameter_by_scenario(int scenario) {
        if (_diameter_by_scenario != null)
            return _diameter_by_scenario[scenario];
        return _diameter;
    }
    public double get_thickness() {
        return _thickness;
    }
    public double get_thickness_by_scenario(int scenario) {
        if (_thickness_by_scenario != null)
            return _thickness_by_scenario[scenario];
        return _thickness;
    }
    public double get_radius_of_curvature() {
        return _radius;
    }
    public double[] get_aspheric_coeffs() {
        return _coeffs;
    }
    public double get_cc() {
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
        return _asph_type != 0;
    }
    public boolean is_odd_asphere() {
        return is_aspheric() && _asph_type == ASPH_ODD;
    }
    public boolean is_even_a2_asphere() {
        return is_aspheric() && _asph_type == ASPH_EVEN_A2;
    }
    public StringBuilder toOptBenchStr(StringBuilder sb, boolean is_last) {
        sb.append(_id).append("\t");
        if (_is_aperture_stop)
            sb.append("AS");
        else if (_is_field_stop)
            sb.append("FS");
        else
            sb.append(_radius);
        sb.append("\t");
        if (_thickness_by_scenario != null) {
            if (is_last) sb.append("Bf");
            else sb.append("d").append(_id);
        }
        else sb.append(_thickness);
        sb.append("\t");
        GlassMap glass = null;
        double nd = _nd;
        double vd = _vd;
        if (nd != 0.0 && _glass_name != null) {
            glass = GlassMap.glassByName(_glass_name);
            if (glass != null) {
                nd = glass.nd;
                vd = glass.vd;
            }
        }
        if (nd != 0.0)
            sb.append(nd);
        sb.append("\t");
        sb.append(_diameter).append("\t");
        if (nd != 0.0)
            sb.append(vd);
        sb.append("\t");
        if (glass != null)
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
        int i = 0;
        // Skip the unused params for optical bench format
        if (_asph_type == ASPH_EVEN)
            i = 1;
        else if (_asph_type == ASPH_ODD)
            i = 2;
        for (; i < _coeffs.length; i++) {
            if (_coeffs[i] == 0.0)
                break;
            sb.append(_coeffs[i]).append("\t");
        }
        sb.append("\n");
        return sb;
    }
    public static StringBuilder asphericMarkdownTableHeader(StringBuilder sb) {
        sb.append("## Aspherical Data").append("\n");
        sb.append("| ID  | k   | P1  | P2  | P3  | P3 | P5 | P6 | P7 | P8 | P9 | P10 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        return sb;
    }
    public StringBuilder ashericToMarkdownTableRow(StringBuilder sb) {
        sb.append("| ").append(_id);
        sb.append(" | ").append(_k);
        for (int i = 0; i < 10; i++) {
            if (_coeffs != null && i < _coeffs.length)
                sb.append(" | ").append(_coeffs[i]);
            else
                sb.append(" | 0 ");
        }
        sb.append(" |\n");
        return sb;
    }
    public StringBuilder variablesToMarkdownTableRow(StringBuilder sb) {
        if (_diameter_by_scenario != null) {
            sb.append("| a").append(_id).append(" |");
            for (int i = 0; i < _diameter_by_scenario.length; i++) {
                sb.append(" ").append(_diameter_by_scenario[i]).append(" |");
            }
            sb.append("\n");
        }
        if (_thickness_by_scenario != null) {
            sb.append("| d").append(_id).append(" |");
            for (int i = 0; i < _thickness_by_scenario.length; i++) {
                sb.append(" ").append(_thickness_by_scenario[i]).append(" |");
            }
            sb.append("\n");
        }
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
        sb.append(" | ");
        if (_thickness_by_scenario != null)
            sb.append("d").append(_id);
        else
            sb.append(_thickness);
        sb.append(" | ");
        if (_diameter_by_scenario != null)
            sb.append("a").append(_id);
        else
            sb.append(_diameter);
        sb.append(" | ");
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
        // FIXME is last
        return toOptBenchStr(new StringBuilder(),false).toString();
    }
}
