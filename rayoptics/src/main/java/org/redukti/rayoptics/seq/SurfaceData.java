package org.redukti.rayoptics.seq;

public class SurfaceData {
    Double refractive_index;
    Double v_number;
    double curvature;
    double thickness;
    Double semi_diameter;
    String catalog_name;
    String glass_name;
    String interact_mode;

    public SurfaceData(double curvature, double thickness) {
        this.curvature = curvature;
        this.thickness = thickness;
    }

    public SurfaceData rindex(double index, double vd) {
        this.refractive_index = index;
        this.v_number = vd;
        this.glass_name = null;
        this.catalog_name = null;
        return this;
    }

    public SurfaceData mode(String mode) {
        this.interact_mode = mode;
        this.glass_name = null;
        this.catalog_name = null;
        this.refractive_index = null;
        this.v_number = null;
        return this;
    }

}
