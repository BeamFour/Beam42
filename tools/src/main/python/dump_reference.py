"""Build a model from a generated ray-optics script and dump reference values.

The script is the one RayOpticsExporter emits, so the Python and Java sides are
built from the same ModelSpec and describe the same system by construction.

Output is a flat, sorted key=value text file: no JSON dependency needed on the
Java side, and a regenerated file diffs line by line so a moved number names
itself.

usage: dump_reference.py <generated.py> > reference.txt
"""
import sys

FOD_FIELDS = [
    'opt_inv', 'power', 'efl', 'fl_obj', 'fl_img', 'pp1', 'ppk', 'pp_sep',
    'ffl', 'bfl', 'fno', 'm', 'red', 'n_obj', 'n_img', 'obj_dist', 'img_dist',
    'obj_ang', 'img_ht', 'enp_dist', 'enp_radius', 'exp_dist', 'exp_radius',
    'obj_na', 'img_na',
]
SEIDEL_COLS = ['S-I', 'S-II', 'S-III', 'S-IV', 'S-V']


def build(path):
    src = open(path, encoding='utf-8').read()
    ns = {}
    exec(compile(src, path, 'exec'), ns)
    opm = ns['opm']
    opm.update_model()
    return opm


FAN_NUM_RAYS = 21

# Relative pupil points traced surface by surface: the chief ray, the upper
# meridional marginal, and a sagittal marginal that produces skew rays at any
# non-zero field. Trim this list if the generated tests get unwieldy - one ray
# is enough to detect a kernel regression, the rest add diagnostic breadth.
RAY_PUPILS = [(0., 0.), (0., 1.), (1., 0.)]


def collect_rays(opm):
    """Full ray segments and accumulated optical path, per field and pupil point.

    This is the only thing that pins the trace kernel directly - bend, reflect,
    the surface intersections and the per-surface transforms. Everything else
    reaches those through aggregates, where a defect shows up indirectly if at
    all.

    op_delta is the optical path with respect to equally inclined chords, which
    the chief ray entry (pupil 0,0) covers.
    """
    import numpy as np
    from rayoptics.raytr import trace
    from rayoptics.raytr.traceerror import TraceError

    out = {}
    osp = opm['optical_spec']
    wvl = osp['wvls'].central_wvl

    for fi, fld in enumerate(osp['fov'].fields):
        for pi, pupil in enumerate(RAY_PUPILS):
            try:
                # Vignetting left on, matching the Java TraceOptions default.
                # Turning it off does not buy any precision - see below - and
                # costs coverage, since unscaled marginal rays at the outer
                # fields get blocked and drop out of the comparison entirely.
                ray, op_delta, _ = trace.trace_base(opm, np.array(pupil), fld, wvl)
            except TraceError:
                continue  # a blocked or missed ray has nothing comparable
            key = f'ray.{fi}.{pi}'
            # The pupil point is recorded so the generator can emit the same
            # trace call rather than having to know RAY_PUPILS itself.
            out[f'{key}.pupil.x'] = float(pupil[0])
            out[f'{key}.pupil.y'] = float(pupil[1])
            out[f'{key}.op_delta'] = float(op_delta)
            out[f'{key}.num_segments'] = len(ray)
            for si, seg in enumerate(ray):
                p, d, dst, nrml = seg[0], seg[1], seg[2], seg[3]
                for axis, v in enumerate('xyz'):
                    out[f'{key}.{si}.p.{v}'] = float(p[axis])
                    out[f'{key}.{si}.d.{v}'] = float(d[axis])
                    out[f'{key}.{si}.n.{v}'] = float(nrml[axis])
                out[f'{key}.{si}.dst'] = float(dst)
    return out


def collect_fans(opm):
    """Transverse ray aberration and OPD along x and y pupil fans.

    Mirrors SequentialModel.trace_fan on the Java side, which is the entry point
    the optimizer's ray-aberration and MTF goals go through. The important
    detail to match is that every wavelength is evaluated against the *central*
    wavelength's reference image point, so the fans share one origin rather than
    each re-centring on its own chief ray.

    focus_fan returns OPD already divided by nm_to_sys_units, i.e. in waves,
    which is what WavefrontAberrationAnalysis.opd does too.
    """
    from rayoptics.raytr import analyses
    from rayoptics.raytr import trace

    out = {}
    osp = opm['optical_spec']
    foc = osp['focus'].get_focus()
    wavelengths = osp['wvls'].wavelengths
    central_wvl = osp['wvls'].central_wvl

    for fi, fld in enumerate(osp['fov'].fields):
        ref_sphere, _ = trace.setup_pupil_coords(opm, fld, central_wvl, foc)
        ref_img_pt = ref_sphere[0][:2]
        for wi, wvl in enumerate(wavelengths):
            for xy in (0, 1):
                fan_pkg = analyses.trace_fan(opm, fld, wvl, foc, xy,
                                             image_pt_2d=ref_img_pt,
                                             num_rays=FAN_NUM_RAYS)
                data = analyses.focus_fan(opm, fan_pkg, fld, wvl, foc,
                                          image_pt_2d=ref_img_pt)
                # focus_fan yields ((px, py), (t_abr_x, t_abr_y, opd)) for a
                # traced ray and a flat (px, py, nan) for one that failed. A
                # partial fan is skipped rather than recorded, because the Java
                # side drops failed rays instead of padding them and the two
                # would no longer be index aligned.
                rays = []
                for item in data:
                    if len(item) != 2:
                        rays = None
                        break
                    _, values = item
                    rays.append((float(values[xy]), float(values[2])))
                if rays is None or len(rays) != FAN_NUM_RAYS:
                    continue
                # The pupil coordinate is recorded as the nominal fan abscissa,
                # accumulated exactly as both sides step it, rather than as the
                # value focus_fan reports.
                #
                # Upstream's Field.apply_vignetting does `vig_pupil = pupil[:]`,
                # which for a numpy array is a view rather than a copy, so it
                # scales the caller's array in place and trace_ray_fan - which
                # records the pupil after tracing - captures the vignetted
                # coordinate. Beam43's apply_vignetting copies, so its fan_x
                # keeps the nominal value. The rays traced are identical either
                # way; only the recorded abscissa differs. Comparing the nominal
                # value keeps this as an index-alignment check without importing
                # the aliasing quirk.
                nominal = -1.0
                step = 2.0 / (FAN_NUM_RAYS - 1)
                for i, (t_abr, opd) in enumerate(rays):
                    key = f'fan.{fi}.{wi}.{xy}.{i}'
                    out[f'{key}.pupil'] = nominal
                    out[f'{key}.t_abr'] = t_abr
                    out[f'{key}.opd'] = opd
                    nominal += step
    return out


def collect(opm):
    out = {}
    sm, osp = opm['seq_model'], opm['optical_spec']

    out['meta.num_surfaces'] = len(sm.ifcs)
    out['meta.stop_surface'] = sm.stop_surface

    fod = opm['analysis_results']['parax_data'].fod
    for name in FOD_FIELDS:
        out[f'fod.{name}'] = float(getattr(fod, name))

    ax, pr, _ = opm['analysis_results']['parax_data']
    for tag, ray in (('ax', ax), ('pr', pr)):
        for i, seg in enumerate(ray):
            out[f'{tag}.{i}.ht'] = float(seg[0])
            out[f'{tag}.{i}.slp'] = float(seg[1])
            out[f'{tag}.{i}.aoi'] = float(seg[2])

    for i, f in enumerate(osp['fov'].fields):
        for s in ('vlx', 'vux', 'vly', 'vuy'):
            out[f'fld.{i}.{s}'] = float(getattr(f, s))
        # Chief ray aiming. In wide angle mode aim_info holds the scalar z_enp,
        # otherwise the x,y aim point on the paraxial entrance pupil plane.
        # Beam43 splits these into two fields; record whichever upstream set.
        aim = getattr(f, 'aim_info', None)
        if aim is None:
            continue
        if osp['fov'].is_wide_angle:
            out[f'aim.{i}.z_enp'] = float(aim)
        else:
            out[f'aim.{i}.x'] = float(aim[0])
            out[f'aim.{i}.y'] = float(aim[1])

    out.update(collect_fans(opm))
    out.update(collect_rays(opm))

    from rayoptics.parax.thirdorder import compute_third_order
    df = compute_third_order(opm)
    for idx, row in df.iterrows():
        for col in SEIDEL_COLS:
            out[f'seidel.{idx}.{col}'] = float(row[col])

    return out


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    values = collect(build(sys.argv[1]))
    print('# Reference values from upstream ray-optics. Generated - do not edit.')
    for k in sorted(values):
        v = values[k]
        print(f'{k}={v!r}' if isinstance(v, float) else f'{k}={v}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
