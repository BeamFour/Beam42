package org.redukti.rayoptics.elem;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.util.Pair;

import java.util.Objects;

/**
 * Maintains data and actions for position and orientation changes.
 * <p>
 * - 'decenter': pos and orientation applied prior to surface
 * - 'reverse': pos and orientation applied following surface in reverse
 * - 'dec and return': pos and orientation applied prior to surface and then returned to initial frame
 * - 'bend':  used for fold mirrors, orientation applied before and after surface
 */
public class DecenterData {
    String dtype;
    /**
     * x, y, z vertex decenter
     */
    Vector3 dec;
    /**
     * alpha, beta, gamma euler angles
     */
    Vector3 euler;
    /**
     * x, y, z rotation point offset
     */
    Vector3 rot_pt;
    Matrix3 rot_mat;

    public DecenterData(String dtype, double x, double y, double alpha, double beta, double gamma) {
        this.dtype = dtype;
        this.dec = new Vector3(x, y, 0.0);
        this.euler = new Vector3(alpha, beta, gamma);
        this.rot_pt = Vector3.ZERO;
        this.rot_mat = null;
    }

    public DecenterData(String dtype) {
        this(dtype, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private Vector3 convertl2r() {
        return new Vector3(-euler.x, -euler.y, euler.z);
    }

    public void update() {
        if (euler.any())
            rot_mat = Matrix3.euler2mat(convertl2r().deg2rad());
        else
            rot_mat = null;
    }

    public void apply_scale_factor(double scale_factor) {
        dec = dec.times(scale_factor);
        rot_pt = rot_pt.times(scale_factor);
    }

    public Pair<Matrix3, Vector3> tform_before_surf() {
        if (!Objects.equals(dtype, "reverse"))
            return new Pair<>(rot_mat, dec);
        else
            return new Pair<>(null, Vector3.ZERO);
    }

    public Pair<Matrix3, Vector3> tform_after_surf() {
        if (Objects.equals(dtype, "reverse") || Objects.equals(dtype, "dec and return")) {
            Matrix3 rt = rot_mat;
            if (rot_mat != null)
                rt = rot_mat.transpose();
            return new Pair<>(rt, dec.negate());
        } else if (Objects.equals(dtype, "bend"))
            return new Pair<>(rot_mat, Vector3.ZERO);
        else
            return new Pair<>(null, Vector3.ZERO);
    }
}
