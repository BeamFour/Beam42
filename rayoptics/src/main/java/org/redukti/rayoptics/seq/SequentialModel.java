package org.redukti.rayoptics.seq;

import org.redukti.rayoptics.elem.Surface;
import org.redukti.rayoptics.math.Matrix3;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.math.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.util.ZDir;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manager class for a sequential optical model
 * <p>
 * A sequential optical model is a sequence of surfaces and gaps.
 * <p>
 * The sequential model has this structure
 * <pre>
 *
 *         IfcObj  Ifc1  Ifc2  Ifc3 ... Ifci-1   IfcImg
 *              \  /  \  /  \  /             \   /
 *              GObj   G1    G2              Gi-1
 *
 *     where
 *
 *         - Ifc is a :class:`~rayoptics.seq.interface.Interface` instance
 *         - G   is a :class:`~rayoptics.seq.gap.Gap` instance
 *
 *     </pre>
 * <p>
 * There are N interfaces and N-1 gaps. The initial configuration has an
 * object and image Surface and an object gap.
 * <p>
 * The Interface API supports implementation of an optical action, such as
 * refraction, reflection, scatter, diffraction, etc. The Interface may be
 * realized as a physical profile separating the adjacent gaps or an idealized
 * object, such as a thin lens or 2 point HOE.
 * <p>
 * The Gap class maintains a simple separation (z translation) and the medium
 * filling the gap. More complex coordinate transformations are handled
 * through the Interface API.
 * <p>
 * Attributes:
 * opt_model: parent optical model
 * ifcs: list of :class:`~rayoptics.seq.interface.Interface`
 * gaps: list of :class:`~rayoptics.seq.gap.Gap`
 * lcl_tfrms: forward transform, interface to interface
 * rndx: a list with refractive indices for all **wvls**
 * z_dir: -1 if gap follows an odd number of reflections, otherwise +1
 * gbl_tfrms: global coordinates of each interface wrt the 1st interface
 * stop_surface (int): index of stop interface
 * cur_surface (int): insertion index for next interface
 */
public class SequentialModel {

    public OpticalModel opt_model;
    public List<Interface> ifcs = new ArrayList<>();
    public List<Gap> gaps = new ArrayList<>();
    public List<Transform3> gbl_tfrms = new ArrayList<>();
    public List<Transform3> lcl_tfrms = new ArrayList<>();
    public List<ZDir> z_dir = new ArrayList<>();
    public Integer stop_surface;
    public Integer cur_surface;
    public double[] wvlns;
    public List<double[]> rndx = new ArrayList<>();
    public boolean do_apertures = true;

    public SequentialModel(OpticalModel opm, boolean do_init) {
        this.opt_model = opm;
        if (do_init)
            initialize_arrays();
    }

    public SequentialModel(OpticalModel opm) {
        this(opm, true);
    }

    /**
     * initialize object and image interfaces and intervening gap
     */
    private void initialize_arrays() {
        // add object interface
        ifcs.add(new Surface("Obj", "dummy"));

        Transform3 tfrm = new Transform3();
        gbl_tfrms.add(tfrm);
        lcl_tfrms.add(tfrm);

        // add object gap
        gaps.add(new Gap());
        z_dir.add(ZDir.PROPAGATE_RIGHT);
        rndx.add(new double[]{1.0});

        // interfaces are inserted after cur_surface
        cur_surface = 0;

        // add image interface
        ifcs.add(new Surface("Img", "dummy"));
        gbl_tfrms.add(tfrm);
        lcl_tfrms.add(tfrm);
    }

    /**
     * add a surface where `surf_data` is a list that contains:
     * <p>
     * [curvature, thickness, refractive_index, v-number, semi-diameter]
     * <p>
     * The `curvature` entry is interpreted as radius if `radius_mode` is **True**
     * <p>
     * The `thickness` is the signed thickness
     * <p>
     * The `refractive_index, v-number` entry can have several forms:
     * <p>
     * - **refractive_index, v-number**
     * - **refractive_index** only -> constant index model
     * - **'REFL'** -> set interact_mode to 'reflect'
     * - **glass_name, catalog_name** as 1 or 2 strings
     * - blank -> defaults to air
     * <p>
     * The `semi-diameter` entry is optional
     *
     * @param surf_data
     */
    public void add_surface(SurfaceData surf_data) {
        boolean radius_mode = opt_model.radius_mode;
        Medium mat = null;
        if (Objects.equals(surf_data.interact_mode, "REFL")) {
            Objects.requireNonNull(cur_surface);
            mat = gaps.get(cur_surface).medium;
        }
        NewSurfaceSpec newSurfaceSpec = create_surface_and_gap(surf_data, radius_mode, mat, 550.0);
        insert(newSurfaceSpec.surface, newSurfaceSpec.gap, false);
    }

    /**
     * insert surf and gap at the cur_gap edge of the sequential model
     * graph
     *
     * @param ifc
     * @param gap
     * @param prev
     */
    void insert(Interface ifc, Gap gap, boolean prev) {
        if (stop_surface != null) {
            Objects.requireNonNull(cur_surface);
            int num_ifcs = ifcs.size();
            if (num_ifcs > 2) {
                if (stop_surface > cur_surface &&
                        stop_surface < num_ifcs - 2)
                    stop_surface += 1;
            }
        }
        int idx = cur_surface == null ? 0 : cur_surface + 1;
        ifcs.add(idx, ifc);
        if (gap != null) {
            int idx_g = prev ? idx - 1 : idx;
            gaps.add(idx_g, gap);
        } else {
            gap = gaps.get(idx);
        }
        Transform3 tfrm = new Transform3();
        gbl_tfrms.add(tfrm);
        lcl_tfrms.add(tfrm);

        ZDir new_z_dir = (idx > 1) ? z_dir.get(idx - 1) : ZDir.PROPAGATE_RIGHT;
        z_dir.add(idx, new_z_dir);

        double[] wvls = opt_model.optical_spec.spectral_region.wavelengths;
        double[] rindex = new double[wvls.length];
        for (int i = 0; i < wvls.length; i++)
            rindex[i] = gap.medium.rindex(wvls[i]);
        rndx.add(idx, rindex);

        //         if ifc.interact_mode == 'reflect':
        //            self.update_reflections(start=idx)
    }


    /**
     * create a surface and gap where `surf_data` is a list that contains:
     * <p>
     * [curvature, thickness, refractive_index, v-number, semi-diameter]
     * <p>
     * The `curvature` entry is interpreted as radius if `radius_mode` is **True**
     * <p>
     * The `thickness` is the signed thickness
     * <p>
     * The `refractive_index, v-number` entry can have several forms:
     * <p>
     * - **refractive_index, v-number**
     * - **refractive_index** only -> constant index model
     * - **'REFL'** -> set interact_mode to 'reflect'
     * - **glass_name, catalog_name** as 1 or 2 strings
     * - blank -> defaults to air
     * <p>
     * The `semi-diameter` entry is optional
     *
     * @param surf_data
     * @param radius_mode
     * @param prev_medium
     * @param wvl
     */
    public NewSurfaceSpec create_surface_and_gap(SurfaceData surf_data, boolean radius_mode, Medium prev_medium, double wvl) {

        Surface s = new Surface();

        if (radius_mode) {
            if (surf_data.curvature != 0.0)
                s.profile.cv = 1.0 / surf_data.curvature;
            else
                s.profile.cv = 0.0;
        } else {
            s.profile.cv = surf_data.curvature;
        }

        Medium mat = null;

        if (surf_data.refractive_index != null) {
            if (surf_data.v_number == null) {
                if (surf_data.refractive_index == 1.0)
                    mat = new Air();
                else
                    mat = new Medium(surf_data.refractive_index);
            } else {
                if (surf_data.refractive_index == 1.0)
                    mat = new Air();
                else
                    mat = new Glass(surf_data.refractive_index, surf_data.v_number);
            }
        } else if (surf_data.interact_mode != null && surf_data.interact_mode.toUpperCase().equals("REFL")) {
            s.interact_mode = "reflect";
            mat = prev_medium;
        } else if (surf_data.glass_name != null && surf_data.catalog_name != null) {
            throw new UnsupportedOperationException(); // Not implemented yet
        } else {
            mat = new Air();
        }
        if (surf_data.semi_diameter != null) {
            s.set_max_aperture(surf_data.semi_diameter);
        }

        double thi = surf_data.thickness;
        Gap g = new Gap(thi, mat);
        double rndx = mat.rindex(wvl);
        Transform3 tfrm = new Transform3(Matrix3.IDENTITY, new Vector3(0., 0., thi));

        return new NewSurfaceSpec(s, g, rndx, tfrm);
    }
}
