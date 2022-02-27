package org.redukti.rayoptics.seq;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.Node;
import org.redukti.rayoptics.elem.Surface;
import org.redukti.rayoptics.elem.Transform;
import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.util.Lists;
import org.redukti.rayoptics.util.Pair;
import org.redukti.rayoptics.util.ZDir;

import java.util.*;

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

        Node root_node = opt_model.part_tree.root_node;
        int idx = cur_surface;
        new Node("i" + idx, newSurfaceSpec.surface, "#ifc", root_node);
        if (newSurfaceSpec.gap != null)
            new Node("g" + idx, new Pair<>(newSurfaceSpec.gap, z_dir.get(idx)), "#gap", root_node);
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
        cur_surface = idx;
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
        if (surf_data.max_aperture != null) {
            s.set_max_aperture(surf_data.max_aperture);
        }

        double thi = surf_data.thickness;
        Gap g = new Gap(thi, mat);
        double rndx = mat.rindex(wvl);
        Transform3 tfrm = new Transform3(Matrix3.IDENTITY, new Vector3(0., 0., thi));

        return new NewSurfaceSpec(s, g, rndx, tfrm);
    }

    /**
     * sets the stop surface to the current surface
     */
    public int set_stop() {
        stop_surface = cur_surface;
        return stop_surface;
    }

    public StringBuilder list_surfaces(StringBuilder sb) {
        for (int i = 0; i < ifcs.size(); i++) {
            sb.append(i).append(" ");
            ifcs.get(i).toString(sb);
            sb.append(System.lineSeparator());
        }
        return sb;
    }

    public StringBuilder list_gaps(StringBuilder sb) {
        for (int i = 0; i < gaps.size(); i++) {
            sb.append(i).append(" ");
            gaps.get(i).toString(sb);
            sb.append(System.lineSeparator());
        }
        return sb;
    }

    public static List<Pair<Interface, Gap>> zip_longest(List<Interface> ifcs, List<Gap> gaps) {
        List<Pair<Interface, Gap>> list = new ArrayList<>();
        for (int i = 0; i < Math.max(ifcs.size(), gaps.size()); i++) {
            Interface ifc = i < ifcs.size() ? ifcs.get(i) : null;
            Gap g = i < gaps.size() ? gaps.get(i) : null;
            list.add(new Pair<>(ifc, g));
        }
        return list;
    }

    public void update_model() {
        // delta n across each surface interface must be set to some
        // reasonable default value. use the index at the central wavelength
        OpticalSpecs osp = opt_model.optical_spec;
        int ref_wl = osp.spectral_region.reference_wvl;

        this.wvlns = osp.spectral_region.wavelengths;
        this.rndx = calc_ref_indices_for_spectrum(wvlns);
        double n_before = rndx.get(0)[ref_wl];

        this.z_dir = new ArrayList<>();
        ZDir z_dir_before = ZDir.PROPAGATE_RIGHT;

        List<Pair<Interface, Gap>> seq = zip_longest(this.ifcs, this.gaps);

        for (int i = 0; i < seq.size(); i++) {
            Interface ifc = seq.get(i).first;
            Gap g = seq.get(i).second;
            ZDir z_dir_after = z_dir_before;
            if (ifc.interact_mode.equals("reflect"))
                z_dir_after = z_dir_after.opposite();

            // leave rndx data unsigned, track change of sign using z_dir
            if (g != null) {
                double n_after = this.rndx.get(i)[ref_wl];
                if (z_dir_after.value < 0)
                    n_after = -n_after;
                ifc.delta_n = n_after - n_before;
                n_before = n_after;

                z_dir_after = z_dir_after;
                this.z_dir.add(z_dir_after);
            }

            // call update() on the surface interface
            ifc.update();
        }

        this.gbl_tfrms = this.compute_global_coords(1);
        this.lcl_tfrms = this.compute_local_transforms(null, 1);

        /*
         if self.do_apertures:
            if len(self.ifcs) > 2:
                osp.update_model(**kwargs)

                self.set_clear_apertures()
         */
    }

    /**
     * Return forward surface coordinates (r.T, t) for each interface.
     * @param seq
     * @param step
     * @return
     */
    public List<Transform3> compute_local_transforms(List<Pair<Interface, Gap>> seq, int step) {
        List<Transform3> tfrms = new ArrayList<>();
        if (seq == null) {
            seq = zip_longest(
                    Lists.slice(ifcs, null, null, step),
                    Lists.slice(gaps, null, null, step));
        }
        Iterator<Pair<Interface, Gap>> iter = seq.iterator();
        Pair<Interface, Gap> before = iter.next();
        Interface b4_ifc = before.first;
        Gap b4_gap = before.second;
        while (true) {
            Pair<Interface, Gap> after;
            Interface ifc;
            Gap gap;
            if (iter.hasNext()) {
                after = iter.next();
                ifc = after.first;
                gap = after.second;
            }
            else {
                tfrms.add(new Transform3());
                break;
            }
            double zdist = step * b4_gap.thi;
            Transform3 tr3 = Transform.forward_transform(b4_ifc, zdist, ifc);
            Matrix3 r = tr3.rot_mat;
            Vector3 t = tr3.vec;
            Matrix3 rt = r.transpose();
            tfrms.add(new Transform3(rt, t));
            before = after;
            b4_ifc = ifc;
            b4_gap = gap;
        }
        return tfrms;
    }

    /**
     * Return global surface coordinates (rot, t) wrt surface glo.
     *
     * @param glo
     * @return
     */
    public List<Transform3> compute_global_coords(int glo) {
        List<Transform3> tfrms = new ArrayList<>();
        Transform3 prev = new Transform3();
        tfrms.add(prev);
        List<Pair<Interface, Gap>> seq;
        Interface b4_ifc;
        Gap b4_gap;
        if (glo > 0) {
            // iterate in reverse over the segments before the
            // global reference surface
            int step = -1;
            seq = zip_longest(
                    Lists.slice(ifcs, glo, null, step),
                    Lists.slice(gaps, glo - 1, null, step));
            Iterator<Pair<Interface, Gap>> iter = seq.iterator();
            Pair<Interface, Gap> after = iter.next();
            Interface ifc = after.first;
            Gap gap = after.second;
            // loop of remaining surfaces in path
            while (iter.hasNext()) {
                Pair<Interface, Gap> before = iter.next();
                b4_ifc = before.first;
                b4_gap = before.second;
                double zdist = gap.thi;
                Transform3 tr3 = Transform.reverse_transform(ifc, zdist, b4_ifc);
                Matrix3 r = tr3.rot_mat;
                Vector3 t = tr3.vec;
                t = prev.rot_mat.multiply(t).add(prev.vec); //  t = prev[0].dot(t) + prev[1]
                r = prev.rot_mat.multiply(r);
                prev = new Transform3(r, t);
                tfrms.add(prev);
                after = before;
                ifc = b4_ifc;
                gap = b4_gap;
            }
            tfrms = Lists.slice(tfrms, null, null, -1); // reverse
        }

        seq = zip_longest(Lists.from(ifcs, glo), Lists.from(gaps, glo));
        Iterator<Pair<Interface, Gap>> iter = seq.iterator();
        Pair<Interface, Gap> before = iter.next();
        b4_ifc = before.first;
        b4_gap = before.second;
        prev = new Transform3(Matrix3.IDENTITY, Vector3.ZERO);
        // loop forward over the remaining surfaces in path
        while (iter.hasNext()) {
            Pair<Interface, Gap> after = iter.next();
            Interface ifc = after.first;
            Gap gap = after.second;
            double zdist = b4_gap.thi;
            Transform3 tr3 = Transform.forward_transform(b4_ifc, zdist, ifc);
            Matrix3 r = tr3.rot_mat;
            Vector3 t = tr3.vec;
            t = prev.rot_mat.multiply(t).add(prev.vec); //  t = prev[0].dot(t) + prev[1]
            r = prev.rot_mat.multiply(r);
            prev = new Transform3(r, t);
            tfrms.add(prev);
            before = after;
            b4_ifc = ifc;
            b4_gap = gap;
        }
        return tfrms;
    }

    /**
     * returns a list with refractive indices for all **wvls**
     *
     * @param wvls list of wavelengths in nm
     */
    public List<double[]> calc_ref_indices_for_spectrum(double[] wvls) {
        List<double[]> indices = new ArrayList<>();
        for (Gap g : gaps) {
            double[] ri = new double[wvlns.length];
            Medium mat = g.medium;
            for (int i = 0; i < wvls.length; i++) {
                double rndx = mat.rindex(wvls[i]);
                ri[i] = rndx;
            }
            indices.add(ri);
        }
        return indices;
    }

    public int get_num_surfaces() {
        return ifcs.size();
    }

    /**
     * returns the central refractive index of the model's WvlSpec
     * @param i
     * @return
     */
    public double central_rndx(int i) {
        int central_wvl = opt_model.optical_spec.spectral_region.reference_wvl;
        if (i < 0)
            i += rndx.size();
        return rndx.get(i)[central_wvl];
    }

    public static List<SeqPathComponent> zip_longest(List<Interface> ifcs,
                                                     List<Gap> gaps,
                                                     List<Transform3> lcl_tfrms,
                                                     List<Double> rndx,
                                                     List<ZDir> z_dir) {
        List<SeqPathComponent> list = new ArrayList<>();
        List<Integer> sizes = List.of(ifcs.size(), gaps.size(), lcl_tfrms.size(), rndx.size(), z_dir.size());
        int maxSize = sizes.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int i = 0; i < maxSize; i++) {
            Interface ifc = i < ifcs.size() ? ifcs.get(i) : null;
            Gap gap = i < gaps.size() ? gaps.get(i) : null;
            Transform3 tr3 = i < lcl_tfrms.size() ? lcl_tfrms.get(i) : null;
            Double n = i < rndx.size() ? rndx.get(i) : null;
            ZDir dir = i < z_dir.size() ? z_dir.get(i) : null;
            list.add(new SeqPathComponent(ifc, gap, tr3, n, dir));
        }
        return list;
    }

    public static List<SeqPathComponent> zip_longest(List<Interface> ifcs,
                                                     List<Gap> gaps,
                                                     List<ZDir> z_dir) {
        List<SeqPathComponent> list = new ArrayList<>();
        List<Integer> sizes = List.of(ifcs.size(), gaps.size(), z_dir.size());
        int maxSize = sizes.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int i = 0; i < maxSize; i++) {
            Interface ifc = i < ifcs.size() ? ifcs.get(i) : null;
            Gap gap = i < gaps.size() ? gaps.get(i) : null;
            ZDir dir = i < z_dir.size() ? z_dir.get(i) : null;
            list.add(new SeqPathComponent(ifc, gap, null, null, dir));
        }
        return list;
    }


    /**
     * returns an iterable path tuple for a range in the sequential model
     *
     * @param wl    wavelength in nm for path, defaults to central wavelength
     * @param start start of range
     * @param stop  first value beyond the end of the range
     * @param step  increment or stride of range
     * @return (* * ifcs, gaps, lcl_tfrms, rndx, z_dir * *)
     */
    public List<SeqPathComponent> path(Double wl, Integer start, Integer stop, int step) {
        if (wl == null)
            wl = central_wavelength();
        Integer gap_start;
        if (step < 0)
            gap_start = start != null ? start - 1 : null;
        else
            gap_start = start;
        int wl_idx = index_for_wavelength(wl);
        /* extract the refractive index for given wavelength and list of surfaces */
        List<double[]> rndx_list = Lists.slice(rndx, start, stop, step);
        List<Double> rndx = new ArrayList<>();
        for (double[] narr: rndx_list) {
            rndx.add(narr[wl_idx]);
        }
        return zip_longest(
                Lists.slice(ifcs, start, stop, step),
                Lists.slice(gaps, gap_start, stop, step),
                Lists.slice(lcl_tfrms, start, stop, step),
                rndx,
                Lists.slice(z_dir, start, stop, step)
        );
    }

    public double central_wavelength() {
        return opt_model.optical_spec.spectral_region.central_wvl();
    }

    /**
     * returns index into rndx array for wavelength `wvl` in nm
     * @param wvl
     * @return
     */
    public int index_for_wavelength(double wvl) {
        this.wvlns = opt_model.optical_spec.spectral_region.wavelengths;
        for (int i = 0; i < wvlns.length; i++) {
            if (wvlns[i] == wvl)
                return i;
        }
        throw new IllegalArgumentException();
    }

    /*
        def calc_ref_indices_for_spectrum(self, wvls: List[float]):
        """ returns a list with refractive indices for all **wvls**

        Args:
            wvls: list of wavelengths in nm
        """
        indices = []
        for g in self.gaps:
            ri = []
            mat = g.medium
            for w in wvls:
                rndx = mat.rindex(w)
                ri.append(rndx)
            indices.append(ri)

        return indices
     */
}
