package org.redukti.spec;

import org.redukti.rayoptics.elem.profiles.EvenPolynomial;
import org.redukti.rayoptics.elem.profiles.RadialPolynomial;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.VigCalc;
import org.redukti.rayoptics.seq.Glass;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;
import org.redukti.rayoptics.specs.*;
import org.redukti.rayoptics.util.Pair;

import java.util.ArrayList;

public class RayOpticsModelBuilder {

    public final Prescription _prescription;

    public RayOpticsModelBuilder(Prescription _prescription) {
        this._prescription = _prescription;
    }

    public OpticalModel build_optical_model(boolean fov_angle, double[] fields, boolean do_apertures, VigType vig_type, boolean use_wideangle_aiming, int config) {
        if (fields == null)
            fields = new double[]{0., .707, 1.};
        OpticalModel opm = new OpticalModel();
        SequentialModel sm = opm.seq_model;
        OpticalSpecs osp = opm.optical_spec;
        var angle_of_view_deg = config == 0 ? _prescription._angle_of_view_in_degrees : _prescription._angle_of_views_by_scenario[config];
        double half_angle_deg =  angle_of_view_deg/2.0;
        var fno = config == 0 ? _prescription._fno : _prescription._f_number_by_scenario[config];
        osp.pupil = new PupilSpec(osp, new Pair<>(ImageKey.Image, ValueKey.Fnum), fno);
        if (fov_angle) {
            osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Object, ValueKey.Angle), fields);
            osp.fov.value = half_angle_deg;
        }
        else {
            osp.fov = new FieldSpec(osp, new Pair<>(ImageKey.Image, ValueKey.RealHeight), fields);
            osp.fov.value = _prescription._diameter_image_circle/2.0;
        }
        osp.fov.is_relative = true; // Fields are specified as 0, 0.7, 1.0 etc - without actual sizes
        osp.fov.is_wide_angle = (half_angle_deg > 45.) || use_wideangle_aiming;
        var wvls = new ArrayList<WvlWt>();
        for (int i = 0; i < _prescription._wvls.length; i++)
            wvls.add(new WvlWt(_prescription._wvls[i], _prescription._wts[i]));
        osp.wvls = new WvlSpec(wvls.toArray(new WvlWt[0]), 0);
        opm.system_spec.title = _prescription._title;
        opm.system_spec.dimensions = "mm";
        opm.radius_mode = true;
        sm.gaps.get(0).thi = 1e10;
        for (int i = 0; i < _prescription._surfaces.length; i++) {
            var s = _prescription._surfaces[i];
            add_surface(sm,s,config);
        }
        sm.do_apertures = do_apertures;
        opm.update_model();
        switch (vig_type)  {
            case Paraxial -> {
                Trace.apply_paraxial_vignetting(opm);
                opm.update_model();
            }
            case SetPupil -> {
                VigCalc.set_pupil(opm);
                opm.update_model();
            }
            case SetStopAperture -> {
                VigCalc.set_stop_aperture(opm);
                opm.update_model();
            }
            case SetApertures -> {
                // Vignetting first. set_ape sizes each aperture to just pass the
                // boundary rays, so running it first - against unvignetted rays -
                // leaves apertures wide enough that nothing clips, and the
                // vignetting that follows comes out zero on the unclipped side.
                VigCalc.set_vig(opm,false);
                VigCalc.set_ape(opm);
                opm.update_model();
            }
            case SetFnum -> {
                // Trust the quoted f/#: size the stop to satisfy it (which
                // recalculates vignetting), then size everything else to pass
                // the resulting rays. set_ape re-derives the same stop diameter,
                // so it does not need excluding.
                VigCalc.set_stop_aperture(opm);
                VigCalc.set_ape(opm);
                opm.update_model();
            }
            case SetVig -> {
                VigCalc.set_vig(opm,false);
                opm.update_model();
            }
        }
        return opm;
    }

    private void add_surface(SequentialModel sm, SurfaceType s, int config) {
        var diameter = s.get_diameter_by_scenario(config);
        double ap_radius = diameter / 2.0;
        double thickness = s.get_thickness_by_scenario(config);

        if (s.get_refractive_index() != 0.0) {
            var glass = Glass.glass_by_catalog_name(s.get_catalog_name(), s.get_glass_name());
            if (glass == null) {
                sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                        .max_aperture(ap_radius)
                        .rindex(s.get_refractive_index(), s.get_abbe_vd()));
            }
            else {
                sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                        .max_aperture(ap_radius)
                        .rindex(s.get_refractive_index(), s.get_abbe_vd(), glass.label, glass.catalog_name));
            }
        } else {
            sm.add_surface(new SurfaceData(s.get_radius_of_curvature(), thickness)
                    .max_aperture(ap_radius));
        }
        if (s.is_aspheric()) {
            if (s.is_odd_asphere())
                sm.ifcs.get(sm.cur_surface).profile = new RadialPolynomial().r(s.get_radius_of_curvature()).cc(s.get_cc()).coefs(s.get_aspheric_coeffs());
            else
                sm.ifcs.get(sm.cur_surface).profile = new EvenPolynomial().r(s.get_radius_of_curvature()).cc(s.get_cc()).coefs(s.get_aspheric_coeffs());
        }
        if (s.is_aperture_stop()) sm.set_stop();
    }
}
