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
