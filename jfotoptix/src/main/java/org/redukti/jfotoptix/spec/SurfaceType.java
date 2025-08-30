package org.redukti.jfotoptix.spec;

import org.redukti.jfotoptix.medium.GlassMap;

public class SurfaceType {
    public String id;
    public double radius;
    public double thickness;
    public double diameter;
    public boolean isStop;
    public boolean isFieldStop;
    public double nd;
    public double vd;
    public String glassName;
    // Aspheric
    public double k;
    public double[] coeffs;

    public double[] _thickness_by_scenario;
    public double[] _diameter_by_scenario;

    public SurfaceType(String id, boolean isStop, double radius, double thickness, double diameter, double nd, double vd, String glassName) {
        this.id = id;
        this.radius = radius;
        this.thickness = thickness;
        this.diameter = diameter;
        this.isStop = isStop;
        this.nd = nd;
        this.vd = vd;
        this.glassName = glassName;
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
        return isStop;
    }
    public boolean is_field_stop() {
        return isFieldStop;
    }
    public double get_diameter() {
        return diameter;
    }
    public double get_thickness() {
        return thickness;
    }
    public double get_radius() {
        return radius;
    }
    public double[] get_aspheric_coeffs() {
        return coeffs;
    }
    public double get_conic_k() {
        return k;
    }
    public double get_refractive_index() {
        return nd;
    }
    public double get_abbe_vd() {
        return vd;
    }
    public String get_glass_name() {
        return glassName;
    }
    public StringBuilder toOptBenchStr(StringBuilder sb) {
        sb.append(id).append("\t");
        if (isStop)
            sb.append("AS");
        else if (isFieldStop)
            sb.append("FS");
        else
            sb.append(radius);
        sb.append("\t").append(thickness).append("\t");
        if (nd != 0.0)
            sb.append(nd);
        sb.append("\t");
        sb.append(diameter).append("\t");
        if (nd != 0.0)
            sb.append(vd);
        sb.append("\t");
        if (nd != 0.0 && glassName != null)
            sb.append(glassName);
        sb.append("\n");
        return sb;
    }
    public StringBuilder asphericsToOptBenchStr(StringBuilder sb) {
        if (k == 0 && (coeffs == null || coeffs.length == 0))
            return sb;
        sb.append(id).append("\t");
        sb.append(radius).append("\t");
        sb.append(k).append("\t");
        for (int i = 0; i < coeffs.length; i++) {
            if (coeffs[i] == 0.0)
                break;
            sb.append(coeffs[i]).append("\t");
        }
        sb.append("\n");
        return sb;
    }
    public boolean is_aspheric() {
        return k != 0 || (coeffs != null && coeffs.length > 0);
    }
    public static StringBuilder asphericMarkdownTableHeader(StringBuilder sb) {
        sb.append("## Aspherical Data").append("\n");
        sb.append("| ID  | k   | A4  | A6  | A8  | A10 | A12 | A14 | A16 | A18 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        return sb;
    }
    public StringBuilder ashericToMarkdownTableRow(StringBuilder sb) {
        sb.append("| ").append(id);
        sb.append(" | ").append(k);
        for (int i = 0; i < 8; i++) {
            if (coeffs != null && i < coeffs.length)
                sb.append(" | ").append(coeffs[i]);
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
        sb.append("| ").append(id).append(" | ");
        if (isStop)
            sb.append("AS");
        else if (isFieldStop)
            sb.append("FS");
        else
            sb.append(radius);
        sb.append(" | ").append(thickness).append(" | ");
        sb.append(diameter).append(" | ");
        String glassMaker = "";
        if (nd != 0.0 && glassName != null) {
            GlassMap glass = GlassMap.glassByName(glassName);
            if (glass != null) {
                nd = glass.nd;
                vd = glass.vd;
                glassMaker = glass.get_manufacturer();
            }
        }
        if (nd != 0.0)
            sb.append(nd);
        sb.append(" | ");
        if (nd != 0.0)
            sb.append(vd);
        sb.append(" | ");
        if (nd != 0.0 && glassName != null) {
            sb.append(glassMaker)
                    .append(" | ")
                    .append(glassName);
        }
        sb.append(" |\n");
        return sb;
    }

    public String toString() {
        return toOptBenchStr(new StringBuilder()).toString();
    }
}
