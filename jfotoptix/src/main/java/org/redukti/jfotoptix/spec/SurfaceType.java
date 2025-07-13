package org.redukti.jfotoptix.spec;

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
    public String toString() {
        return toOptBenchStr(new StringBuilder()).toString();
    }
}
