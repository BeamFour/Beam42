// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import java.util.Objects;

/**
 * Constant refractive index medium.
 */
public class Medium {
    public final String label;
    public final double nd;
    public final String catalog_name;

    public Medium(String label, double nd, String catalog_name) {
        this.label = label;
        this.nd = nd;
        this.catalog_name = catalog_name;
    }

    public Medium(String label, double nd) {
        this(label, nd, "");
    }

    public Medium(double nd) {
        this("", nd, "");
    }

    /**
     * returns the interpolated refractive index at wv_nm
     * @param wv_nm the wavelength in nm for the refractive index query
     * @return float: the refractive index at wv_nm
     */
    public double rindex(double wv_nm) {
        return nd;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (catalog_name != null && !catalog_name.isEmpty() &&
                label != null && !label.isEmpty()) {
            sb.append(catalog_name).append("(")
                    .append(label).append(")");
        } else {
            sb.append("Medium(n=").append(nd).append(")");
        }
        return sb;
    }

    public String name() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medium medium = (Medium) o;
        return Double.compare(medium.nd, nd) == 0 && Objects.equals(label, medium.label) && Objects.equals(catalog_name, medium.catalog_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, nd, catalog_name);
    }
}
