package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.rayoptics.util.Pair;

public class Analysis {

    /**
     * Get the chief ray package at **fld**, computing it if necessary.
     *
     *     Args:
     *         opt_model: :class:`~.OpticalModel` instance
     *         fld: :class:`~.Field` point for wave aberration calculation
     *         wvl: wavelength of ray (nm)
     *         foc: defocus amount
     *
     *     Returns:
     *         chief_ray_pkg: tuple of chief_ray, cr_exp_seg
     *
     *             - chief_ray: chief_ray, chief_ray_op, wvl
     *             - cr_exp_seg: chief ray exit pupil segment (pt, dir, dist)
     *
     *                 - pt: chief ray intersection with exit pupil plane
     *                 - dir: direction cosine of the chief ray in exit pupil space
     *                 - dist: distance from interface to the exit pupil point
     * @param opt_model
     * @param fld
     * @param wvl
     * @param foc
     * @return
     */
    public static RayPkg get_chief_ray_pkg(OpticalModel opt_model, Field fld, double wvl, double foc) {
        if (fld.chief_ray == null) {
            Trace.aim_chief_ray(opt_model, fld, wvl);
            Pair<RayPkg, RayTrace.TransferResults>  chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg.first;
        }
        else if (fld.chief_ray.wvl != wvl) {
            Pair<RayPkg, RayTrace.TransferResults>  chief_ray_pkg = Trace.trace_chief_ray(opt_model, fld, wvl, foc);
            fld.chief_ray = chief_ray_pkg.first;
        }
        return fld.chief_ray;
    }
}
