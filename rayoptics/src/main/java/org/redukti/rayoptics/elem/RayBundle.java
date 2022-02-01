package org.redukti.rayoptics.elem;

import org.redukti.rayoptics.math.Transform3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.RaySeg;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* class for ray bundle from a single field point */
public class RayBundle {

    OpticalModel opt_model;
    Field fld;
    final String fld_label;
    double wvl;
    private Map<String, RayPkg> rayset;
    double start_offset;

    public RayBundle(OpticalModel opt_model, Field fld, String fld_label, double wvl, double start_offset) {
        this.opt_model = opt_model;
        this.fld = fld;
        this.fld_label = fld_label;
        this.wvl = wvl;
        this.start_offset = start_offset;
    }

    public void update_shape() {
        double wvl = opt_model.optical_spec.spectral_region.central_wvl();
        this.wvl = wvl;
        List<RayPkg> rayset = Trace.trace_boundary_rays_at_field(opt_model,
                fld, wvl);
        this.rayset = Trace.boundary_ray_dict(opt_model, rayset);
        // If the object distance (tfrms[0][1][2]) is greater than the
        //  start_offset, then modify rayset start to match start_offset.
        // Remember object transformation for resetting at the end.
        SequentialModel seq_model = opt_model.seq_model;
        List<Transform3> tfrms = seq_model.gbl_tfrms;
        Transform3 tfrtm0 = tfrms.get(0);

        List<RaySeg> start_bundle = this.rayset.values().stream().map(pkg -> pkg.ray.get(0))
                .collect(Collectors.toList());
        List<RayPkg> ray_list = this.rayset.values().stream().collect(Collectors.toList());
        if (Math.abs(tfrtm0.vec.z) > this.start_offset) {
            Transform3 tfrm = Layout.setup_shift_of_ray_bundle(seq_model, this.start_offset);
            tfrms.set(0, tfrm);
            Layout.shift_start_of_ray_bundle(start_bundle, ray_list, tfrm, 0);
        }
//        # try:
//                #     if view.do_draw_beams:
//            #         poly, bbox = self.render_shape(self.rayset,
//            #                                        start_bundle, tfrms)
//            #
//            #         p = view.create_polygon(poly, fill_color=lo_rgb['rayfan_fill'])
//            #         self.handles['shape'] = GUIHandle(p, bbox)
//        #
//                #     if view.do_draw_edge_rays:
//            #         cr = self.render_ray(self.rayset['00'].ray,
//            #                              start_bundle[0], tfrms)
//            #         upr = self.render_ray(self.rayset['+Y'].ray,
//            #                               start_bundle[3], tfrms)
//            #         lwr = self.render_ray(self.rayset['-Y'].ray,
//            #                               start_bundle[4], tfrms)
//            #         kwargs = {
//        #             'linewidth': lo_lw['line'],
//        #             'color': lo_rgb['ray'],
//        #             'hilite_linewidth': lo_lw['hilite'],
//        #             'hilite': lo_rgb['ray'],
//        #             }
//        #         cr_poly = view.create_polyline(cr, **kwargs)
//            #         self.handles['00'] = GUIHandle(cr_poly, bbox_from_poly(cr))
//            #
//            #         upr_poly = view.create_polyline(upr, **kwargs)
//            #         self.handles['+Y'] = GUIHandle(upr_poly, bbox_from_poly(upr))
//            #
//            #         lwr_poly = view.create_polyline(lwr, **kwargs)
//            #         self.handles['-Y'] = GUIHandle(lwr_poly, bbox_from_poly(lwr))
//            #
//            # finally:
//            #     tfrms[0] = tfrtm0
    }

}
