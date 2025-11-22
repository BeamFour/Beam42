// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.seq;

import org.redukti.mathlib.Matrix3;
import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.elem.transform.Transform;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.*;
import org.redukti.rayoptics.specs.Field;
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
    public List<ZDir> z_dir = new ArrayList<>();
    public Integer stop_surface;    // index of stop interface
    public Integer cur_surface;     // insertion index for next interface
    public boolean do_apertures = true;

    // derived attributes
    public List<Tfm3d> gbl_tfrms = new ArrayList<>();  // global coordinates of each interface wrt the 1st interface
    public List<Tfm3d> lcl_tfrms = new ArrayList<>();  // forward transform, interface to interface

    //  data for a wavelength vs index vs gap data arrays
    /**
     * sampling wavelengths in nm
     */
    public double[] wvlns;
    /**
     * a list with refractive indices for all **wvls**
     * refractive index vs wv and gap
     */
    public List<double[]> rndx = new ArrayList<>();

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
        ifcs.add(new Surface("Obj", InteractMode.DUMMY));

        Tfm3d tfrm = new Tfm3d( Matrix3.IDENTITY,Vector3.ZERO);
        gbl_tfrms.add(tfrm);
        lcl_tfrms.add(tfrm);

        // add object gap
        gaps.add(new Gap());
        z_dir.add(ZDir.PROPAGATE_RIGHT);
        rndx.add(new double[]{1.0});

        // interfaces are inserted after cur_surface
        cur_surface = 0;

        // add image interface
        ifcs.add(new Surface("Img", InteractMode.DUMMY));
        gbl_tfrms.add(tfrm);
        lcl_tfrms.add(tfrm);
    }

    public int get_num_surfaces() {
        return ifcs.size();
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
    public List<PathSeg> path(Double wl, Integer start, Integer stop, Integer step) {
        if (wl == null)
            wl = central_wavelength();
        Integer gap_start;
        if (step == null)
            step = 1;
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
    public List<PathSeg> path() {
        return path(null,null,null,null);
    }

    /**
     * returns an iterable path tuple for a range in the sequential model
     *
     *         Args:
     *             wl: wavelength in nm for path, defaults to central wavelength
     *             start: start of range
     *             stop: first value beyond the end of the range
     *             step: increment or stride of range
     *
     *         Returns:
     *             (**ifcs, gaps, lcl_tfrms, rndx, z_dir**)
     */
    public List<PathSeg> reverse_path(Double wl, Integer start, Integer stop, Integer step) {
        if (step == null)
            step = -1;
        if (wl == null)
            wl = central_wavelength();
        Integer gap_start = null;
        Integer rndx_start = null;
        if (step < 0) {
            if (start != null) {
                gap_start = start - 1;
                rndx_start = start - 1;
            }
            else {
                gap_start = start;
                rndx_start = -1;
            }
        }
        else {
            gap_start = start;
        }
        var trfms = compute_local_transforms(-1);
        var wl_idx = index_for_wavelength(wl);
        List<double[]> rndx_list = Lists.slice(rndx, rndx_start, stop, step);
        List<ZDir> zdir_list = Lists.slice(z_dir, start, stop, step);
        List<Double> rndx = new ArrayList<>();
        List<ZDir> z_dir = new ArrayList<>();
        for (double[] narr: rndx_list) {
            rndx.add(narr[wl_idx]);
        }
        for (ZDir zdir: zdir_list) {
            z_dir.add(zdir.opposite());
        }
        return zip_longest(
                Lists.slice(ifcs, start, stop, step),
                Lists.slice(gaps, gap_start, stop, step),
                Lists.slice(trfms, -(start+1), null, 1),
                rndx,
                z_dir
        );
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

    /**
     * returns the central wavelength in nm of the model's `WvlSpec`
     */
    public double central_wavelength() {
        return opt_model.optical_spec.wvls.central_wvl();
    }

    /**
     * returns index into rndx array for wavelength `wvl` in nm
     */
    public int index_for_wavelength(double wvl) {
        this.wvlns = opt_model.optical_spec.wvls.wavelengths;
        for (int i = 0; i < wvlns.length; i++) {
            if (wvlns[i] == wvl)
                return i;
        }
        throw new IllegalArgumentException("Wavelength " + wvl + " not found");
    }

    /**
     * returns the central refractive index of the model's WvlSpec
     */
    public double central_rndx(int i) {
        int central_wvl = opt_model.optical_spec.wvls.reference_wvl;
        if (i < 0)
            i += rndx.size();
        return rndx.get(i)[central_wvl];
    }

    public Pair<Interface,Gap> get_surface_and_gap(Integer srf) {
        if (srf == null)
            srf = cur_surface;
        var s = ifcs.get(srf);
        Gap g = null;
        if (srf < gaps.size())
            g = gaps.get(srf);
        return new Pair<>(s,g);
    }

    public void set_cur_surface(int s) {
        cur_surface = s;
    }

    /**
     * sets the stop surface to the current surface
     */
    public Integer set_stop(Integer cur_idx) {
        if (cur_idx == null)
            cur_idx = cur_surface;
        if (cur_idx != null && ifcs.size() > 2)
            stop_surface = cur_idx > 0 ? cur_idx : 1;
        else
            stop_surface = null;
        return stop_surface;
    }
    public Integer set_stop() {
        return set_stop(null);
    }

    /**
     * insert ifc and gap *after* cur_surface in seq_model lists
     */
    void insert(Interface ifc, Gap gap, ZDir z_dir, Integer idx) {
        if (idx == null) {
            var num_ifcs = ifcs.size();
            if (stop_surface != null) {
                if (num_ifcs > 2) {
                    if (stop_surface > cur_surface &&
                            stop_surface < num_ifcs - 2)
                        stop_surface += 1;
                }
            }
            idx = (num_ifcs < 1) ? 0 : cur_surface + 1;
        }

        cur_surface = idx;

        ifcs.add(idx, ifc);
        if (gap != null) {
            gaps.add(idx, gap);
            z_dir = z_dir == null ? ZDir.PROPAGATE_RIGHT : z_dir;
            ZDir new_z_dir = (idx > 1) ? ZDir.from(z_dir.value * this.z_dir.get(idx - 1).value) : z_dir;
            this.z_dir.add(idx, new_z_dir);
        } else {
            gap = gaps.get(idx);
        }

        Tfm3d tfrm = new Tfm3d(Matrix3.IDENTITY,Vector3.ZERO);
        gbl_tfrms.add(idx, tfrm);
        lcl_tfrms.add(idx, tfrm);

        double[] wvls = opt_model.optical_spec.wvls.wavelengths;
        double[] rindex = new double[wvls.length];
        for (int i = 0; i < wvls.length; i++)
            rindex[i] = gap.medium.rindex(wvls[i]);
        rndx.add(idx, rindex);

        //         if ifc.interact_mode == 'reflect':
        //            self.update_reflections(start=idx)
    }

    // TODO scan_for_reflections

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
     *       - **refractive_index, v-number** (numeric)
     *       - **refractive_index** only -> constant index model
     *       - **glass_name, catalog_name** as 1 or 2 strings
     *       - an instance with a :meth:`~opticalglass.opticalmedium.OpticalMedium.rindex` attribute
     *       - **air**, str -> :class:`~opticalglass.opticalmedium.Air`
     *       - blank -> defaults to :class:`~opticalglass.opticalmedium.Air`
     *       - **'REFL'** -> set interact_mode to 'reflect'
     * <p>
     * The `semi-diameter` entry is optional
     *
     * @param surf_data
     */
    public void add_surface(SurfaceData surf_data) {
        boolean radius_mode = opt_model.radius_mode;
        Medium mat = null;
        if (surf_data.interact_mode == InteractMode.REFLECT) {
            Objects.requireNonNull(cur_surface);
            mat = gaps.get(cur_surface).medium;
        }
        NewSurfaceSpec newSurfaceSpec = create_surface_and_gap(surf_data, radius_mode, mat, null);
        insert(newSurfaceSpec.surface, newSurfaceSpec.gap, newSurfaceSpec.z_dir, null);
    }

    public void update_model() { update_model(null);}
    public void update_model(Integer start) {
        // delta n across each surface interface must be set to some
        // reasonable default value. use the index at the central wavelength
        OpticalSpecs osp = opt_model.optical_spec;
        int ref_wl = osp.wvls.reference_wvl;

        this.wvlns = osp.wvls.wavelengths;
        this.rndx = calc_ref_indices_for_spectrum(wvlns);

        var num_ifcs = ifcs.size();
        if (cur_surface != null) {
            if (num_ifcs == 2)
                cur_surface = 0;
            else if (cur_surface >= num_ifcs)
                cur_surface = num_ifcs - 1;
        }
        else {
            // if None set cur_surface to insert before image surface
            cur_surface = num_ifcs - 2;
        }

        if (start == null)
            start = 0;
        var b4_idx = start == 0 ? start : start - 1;
        double n_before = Lists.get(rndx,b4_idx)[ref_wl];
        ZDir z_dir_before = Lists.get(z_dir,b4_idx);

        List<Pair<Interface, Gap>> seq = Lists.zip_longest(Lists.from(this.ifcs,start), Lists.from(this.gaps,start));

        for (int j = 0, i = start; j < seq.size(); j++) {
            var ifc = seq.get(j).first;
            var g = seq.get(j).second;
            ZDir z_dir_after = z_dir_before;
            if (ifc.interact_mode == InteractMode.REFLECT)
                z_dir_after = z_dir_after.opposite();

            // leave rndx data unsigned, track change of sign using z_dir
            if (g != null) {
                double n_after = this.rndx.get(i)[ref_wl];
                if (z_dir_after.value < 0)
                    n_after = -n_after;
                ifc.delta_n = n_after - n_before;
                n_before = n_after;

                z_dir_after = z_dir_after;
                this.z_dir.set(i,z_dir_after);
            }

            // call update() on the surface interface
            ifc.update();
            i++;
        }

        this.gbl_tfrms = this.compute_global_coords();
        this.lcl_tfrms = this.compute_local_transforms();

        // self.seq_def.update()
    }

    public void update_optical_properties() {
        if (do_apertures) {
            if (ifcs.size() > 2)
                set_clear_apertures();
        }
    }

    public void apply_scale_factor(double scale_factor) {
        apply_scale_factor_over(scale_factor, null);
    }

    /**
     * Apply the `scale_factor` to the `surfs` arg.
     *
     *         - If `surfs` isn't present, the `scale_factor` is applied to all interfaces and gaps.
     *         - If `surfs` contains a single value, it is applied to that interface and gap.
     *         - If `surfs` contains 2 values it is considered an interface range and the `scale_factor` is applied to the interface range and the gaps contained between the outer interfaces.
     */
    public void apply_scale_factor_over(double scale_factor, int[] surfs) {
        if (surfs == null || surfs.length == 0)
            surfs = new int[]{0, ifcs.size()};

        if (surfs.length == 1) {
            var idx = surfs[0];
            ifcs.get(idx).apply_scale_factor(scale_factor);
            if (idx < gaps.size())
                gaps.get(idx).apply_scale_factor(scale_factor);
        }
        else if (surfs.length == 2) {
            var idx1 = surfs[0];
            var idx2 = surfs[1];
            for (int i = idx1; i < idx2+1; i++) {
                try {
                    if (i < idx2)
                        gaps.get(i).apply_scale_factor(scale_factor);
                }
                catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
        }
        gbl_tfrms = this.compute_global_coords();
        lcl_tfrms = this.compute_local_transforms();
    }

    /**
     * Sum gap thicknesses from `os_idx` to `is_idx`
     *
     *         The default arguments return the thickness sum between the 1st and last surfaces.
     *
     *         To include the image surface, is_idx=len(sm.gaps)
     *
     *         Args:
     *             os_idx: starting gap index
     *             is_idx: final gap index
     *
     *         Returns:
     *             oal: float, overal length of gap range
     */
    public double overall_length(Integer os_idx, Integer is_idx) {
        if (os_idx == null)
            os_idx = 1;
        if (is_idx == null)
            is_idx = -1;
        double oal = 0;
        for (Gap g: Lists.slice(gaps,os_idx,is_idx,null)) {
            oal += g.thi;
        }
        return oal;
    }
    public double overall_length() {
        return overall_length(1,-1);
    }

    /**
     * Total track length, distance from object to image.
     */
    public double total_track() {
        return overall_length(0,gaps.size());
    }

    public void set_clear_aperture_paraxial() {
        var osp = opt_model.optical_spec;
        var ax_ray = osp.parax_data.ax_ray;
        var pr_ray = osp.parax_data.pr_ray;
        for (int i = 0; i < ifcs.size(); i++) {
            var ifc = ifcs.get(i);
            var sd = Math.abs(ax_ray.get(i).ht) + Math.abs(pr_ray.get(i).ht);
            ifc.set_max_aperture(sd);
        }
    }

    public void set_clear_apertures() {
        VigCalc.set_ape(opt_model);
    }

    /**
     * Return global surface coordinates (rot, t) wrt surface `glo`.
     *
     *         If origin isn't None, it should be a tuple (r, t) being the transform
     *         from the desired global origin to the specified global surface.
     */
    public List<Tfm3d> compute_global_coords(Integer glo, Tfm3d origin) {
        if (glo == null) glo = 1;
        return Transform.compute_global_coords(this, glo, origin);
    }
    public List<Tfm3d> compute_global_coords() {
        return compute_global_coords(null,null);
    }

    public List<Tfm3d> compute_local_transforms(List<Pair<Interface, Gap>> seq, Integer step) {
        if (step == null) step = 1;
        return Transform.compute_local_transforms(this, seq, step);
    }
    public List<Tfm3d> compute_local_transforms(int step) {
        return compute_local_transforms(null,step);
    }
    public List<Tfm3d> compute_local_transforms() {
        return compute_local_transforms(null,null);
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

    public static List<PathSeg> zip_longest(List<Interface> ifcs,
                                            List<Gap> gaps,
                                            List<Tfm3d> lcl_tfrms,
                                            List<Double> rndx,
                                            List<ZDir> z_dir) {
        List<PathSeg> list = new ArrayList<>();
        List<Integer> sizes = List.of(ifcs.size(), gaps.size(), lcl_tfrms.size(), rndx.size(), z_dir.size());
        int maxSize = sizes.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int i = 0; i < maxSize; i++) {
            Interface ifc = i < ifcs.size() ? ifcs.get(i) : null;
            Gap gap = i < gaps.size() ? gaps.get(i) : null;
            Tfm3d tr3 = i < lcl_tfrms.size() ? lcl_tfrms.get(i) : null;
            Double n = i < rndx.size() ? rndx.get(i) : null;
            ZDir dir = i < z_dir.size() ? z_dir.get(i) : null;
            list.add(new PathSeg(ifc, gap, tr3, n, dir));
        }
        return list;
    }

    public static List<PathSeg> zip_longest(List<Interface> ifcs,
                                                List<Gap> gaps,
                                                List<ZDir> z_dir) {
        List<PathSeg> list = new ArrayList<>();
        List<Integer> sizes = List.of(ifcs.size(), gaps.size(), z_dir.size());
        int maxSize = sizes.stream().max(Comparator.naturalOrder()).orElse(0);
        for (int i = 0; i < maxSize; i++) {
            Interface ifc = i < ifcs.size() ? ifcs.get(i) : null;
            Gap gap = i < gaps.size() ? gaps.get(i) : null;
            ZDir dir = i < z_dir.size() ? z_dir.get(i) : null;
            list.add(new PathSeg(ifc, gap, null, null, dir));
        }
        return list;
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
     *   - **refractive_index, v-number** (numeric)
     *   - **refractive_index** only -> constant index model
     *   - **glass_name, catalog_name** as 1 or 2 strings
     *   - an instance with a :meth:`~opticalglass.opticalmedium.OpticalMedium.rindex` attribute
     *   - **air**, str -> :class:`~opticalglass.opticalmedium.Air`
     *   - blank -> defaults to :class:`~opticalglass.opticalmedium.Air`
     *   - **'REFL'** -> set interact_mode to 'reflect'
     * <p>
     * The `semi-diameter` entry is optional
     *
     * @param surf_data
     * @param radius_mode
     * @param prev_medium
     * @param wvl
     */
    public NewSurfaceSpec create_surface_and_gap(SurfaceData surf_data,
                                                 boolean radius_mode,
                                                 Medium prev_medium,
                                                 Double wvl) {

        if (wvl == null)
            wvl = 587.5618;
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
        ZDir z_dir = ZDir.PROPAGATE_RIGHT;

        if (surf_data.refractive_index != null) {
            Glass g = surf_data.glass_name != null ? Glass.glassByName(surf_data.glass_name) : null;
            if (surf_data.v_number == null) {
                if (surf_data.refractive_index == 1.0)
                    mat = new Air();
                else if (g != null)
                    mat = g;
                else
                    mat = new Medium(surf_data.refractive_index);
            } else {
                if (surf_data.refractive_index == 1.0)
                    mat = new Air();
                else if (g != null)
                    mat = g;
                else {
                    mat = new Glass(surf_data.refractive_index, surf_data.v_number, 0.0);
                }
            }
        } else if (surf_data.interact_mode == InteractMode.REFLECT) {
            s.interact_mode = InteractMode.REFLECT;
            mat = prev_medium;
            z_dir = ZDir.PROPAGATE_LEFT;
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
        Tfm3d tfrm = new Tfm3d(Matrix3.IDENTITY, new Vector3(0., 0., thi));

        return new NewSurfaceSpec(s, g, rndx, tfrm, z_dir);
    }

    static final class ImgFilterImp implements ImageFilter {
        final int wi;
        final Field fld;
        final double wvl;
        final double foc;
        final TraceGridCallback fct;

        public ImgFilterImp(int wi, Field fld, double wvl, double foc, TraceGridCallback fct) {
            this.wi = wi;
            this.fld = fld;
            this.wvl = wvl;
            this.foc = foc;
            this.fct = fct;
        }

        @Override
        public GridItem apply(Vector2 p, RayPkg pkg) {
            return fct.apply(p,wi,pkg,fld,wvl,foc);
        }
    }

    static final class FanFilter implements ImageFilter {
        final OpticalModel opt_model;
        final int wi;
        final Field fld;
        final double wvl;
        final double foc;
        final RayFanCallback fct;

        public FanFilter(OpticalModel opt_model, int wi, Field fld, double wvl, double foc, RayFanCallback fct) {
            this.opt_model = opt_model;
            this.wi = wi;
            this.fld = fld;
            this.wvl = wvl;
            this.foc = foc;
            this.fct = fct;
        }

        @Override
        public GridItem apply(Vector2 p, RayPkg pkg) {
            var result = fct.apply(opt_model,p,wi,pkg,fld,wvl,foc);
            if (result != null)
                return new GridItem(p, pkg, result);
            else
                return new GridItem(p, pkg);
        }
    }

    /**
     * xy determines whether x (=0) or y (=1) fan
     */
    public TraceFanResult trace_fan(
            RayFanCallback fct,
            int fi,
            int xy,
            int num_rays,
            TraceOptions trace_options)
    {
        var osp = opt_model.optical_spec;
        var fld = osp.fov.fields[fi];
        var foc = osp.defocus().get_focus();

        Vector2 ref_img_pt;
        {
            var wvl = central_wavelength();
            var coords = Trace.setup_pupil_coords(opt_model,fld,wvl,foc,null,null);
            var rs_pkg = coords.ref_sphere;
            var cr_pkg = coords.chief_ray_pkg;

            fld.chief_ray = cr_pkg;
            fld.ref_sphere = rs_pkg;
            ref_img_pt = rs_pkg.image_pt.project_xy();
        }
        // Use the central wavelength reference image point for the wavefront error calculations
        var wvls = osp.spectral_region();
        var fans = new ArrayList<TraceFanPoints>();
        var fan_start = Vector2.vector2_0;
        var fan_stop = Vector2.vector2_0;
        fan_start = fan_start.set(xy,-1.0);
        fan_stop = fan_stop.set(xy,1.0);
        var fan_def = new TraceFanDef(fan_start,fan_stop,num_rays);
        var max_rho_val = 0.0;
        var max_y_val = 0.0;
        for (int wi = 0; wi < wvls.wavelengths.length; wi++) {
            double wvl = wvls.wavelengths[wi];
            var coords = Trace.setup_pupil_coords(opt_model,fld,wvl,foc,ref_img_pt,null);
            var rs_pkg = coords.ref_sphere;
            var cr_pkg = coords.chief_ray_pkg;
            fld.chief_ray = cr_pkg;
            fld.ref_sphere = rs_pkg;
            var fan = Trace.trace_fan(opt_model,fan_def,fld,wvl,foc,new FanFilter(opt_model,xy,fld,wvl,foc,fct),trace_options);
            var f_x = new ArrayList<Double>();
            var f_y = new ArrayList<Double>();
            for (int i = 0; i < fan.size(); i++) {
                var p = fan.get(i).pupil;
                var y_val = fan.get(i).result;
                f_x.add(p.v(xy));
                f_y.add(y_val);
                if (Math.abs(p.v(xy)) > max_rho_val)
                    max_rho_val = Math.abs(p.v(xy));
                if (Math.abs(y_val) > max_y_val)
                    max_y_val = Math.abs(y_val);
            }
            fans.add(new TraceFanPoints(wvl,f_x,f_y));
        }
        return new TraceFanResult(fld,xy,fans,max_rho_val,max_y_val);
    }

    public List<TraceGridByWvl> trace_grid(
        TraceGridCallback fct,
        int fi,
        Integer wl,
        int num_rays,
        boolean append_if_none,
        TraceOptions trace_options) {
        // fct is applied to the raw grid and returned as a grid
        var osp = opt_model.optical_spec;
        var wvls = osp.wvls;
        var wvl = central_wavelength();
        double[] wv_list;
        if (wl == null) {
            wv_list = wvls.wavelengths;
        }
        else {
            wv_list = new double[]{wvl};
        }
        var fld = osp.fov.fields[fi];
        var foc = osp.defocus().get_focus();

        var pc = Trace.setup_pupil_coords(opt_model, fld, wvl, foc, null, null);
        var rs_pkg = pc.ref_sphere;
        var cr_pkg = pc.chief_ray_pkg;

        fld.chief_ray = cr_pkg;
        fld.ref_sphere = rs_pkg;

        List<TraceGridByWvl> grids = new ArrayList<>();
        var grid_start = new Vector2(-1.0, -1.0);
        var grid_stop =  new Vector2(1.0, 1.0);
        var grid_def = new TraceGridDef(grid_start,grid_stop,num_rays);
        for (int wi = 0; wi < wv_list.length; wi++) {
            wvl =  wv_list[wi];
            var grid = Trace.trace_grid(opt_model,grid_def,fld,wvl,foc,
                    new ImgFilterImp(wi,fld,wvl,foc,fct),
                    append_if_none,trace_options);
            grids.add(new TraceGridByWvl(wvl,grid));
        }
        return grids;
    }

    public List<TraceGridByWvl> trace_rings(
        TraceGridCallback fct,
        int fi,
        Integer wl,
        Integer num_rings,
        boolean append_if_none,
        TraceOptions trace_options) {
        // fct is applied to the raw grid and returned as a grid
        var osp = opt_model.optical_spec;
        var wvls = osp.wvls;
        var wvl = central_wavelength();
        double[] wv_list;
        if (wl == null) {
            wv_list = wvls.wavelengths;
        }
        else {
            wv_list = new double[]{wvl};
        }
        var fld = osp.fov.fields[fi];
        var foc = osp.defocus().get_focus();
        var pc = Trace.setup_pupil_coords(opt_model, fld, wvl, foc, null, null);
        var rs_pkg = pc.ref_sphere;
        var cr_pkg = pc.chief_ray_pkg;

        fld.chief_ray = cr_pkg;
        fld.ref_sphere = rs_pkg;
        if (num_rings == null) num_rings = 21;

        List<TraceGridByWvl> grids = new ArrayList<>();
        var grid_def = new TraceRingsDef();
        grid_def.num_rings = num_rings;
        for (int wi = 0; wi < wv_list.length; wi++) {
            wvl =  wv_list[wi];
            var grid = Trace.trace_rings(opt_model,grid_def,fld,wvl,foc,
                    new ImgFilterImp(wi,fld,wvl,foc,fct),
                    append_if_none,trace_options);
            grids.add(new TraceGridByWvl(wvl,grid));
        }
        return grids;
    }

}
