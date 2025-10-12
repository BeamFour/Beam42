// Copyright 2017-2015 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import java.util.Objects;

/**
 * Constant refractive index medium.
 */
public class Medium {
    String label;
    double n;
    String catalog_name;

    public Medium(String label, double n, String catalog_name) {
        this.label = label;
        this.n = n;
        this.catalog_name = catalog_name;
    }

    public Medium(String label, double n) {
        this(label, n, "");
    }

    public Medium(double n) {
        this("", n, "");
    }

    /**
     * returns the interpolated refractive index at wv_nm
     * @param wv_nm the wavelength in nm for the refractive index query
     * @return float: the refractive index at wv_nm
     */
    public double rindex(double wv_nm) {
        return n;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (catalog_name != null && !catalog_name.isEmpty() &&
                label != null && !label.isEmpty()) {
            sb.append(catalog_name).append("(")
                    .append(label).append(")");
        } else {
            sb.append("Medium(n=").append(n).append(")");
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
        return Double.compare(medium.n, n) == 0 && Objects.equals(label, medium.label) && Objects.equals(catalog_name, medium.catalog_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, n, catalog_name);
    }
}
