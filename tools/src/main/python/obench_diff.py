"""Compare the sequential model upstream builds from an optical bench file
against the one Beam43's exporter describes.

Model A is built by rayoptics' own obench reader, straight from the .txt.
Model B is built by exec'ing the Python script RayOpticsExporter generated from
the same .txt. Any difference is an importer or exporter bug on the Beam43 side,
with no ray tracing involved.

Only the sequential model is compared. The optical specs deliberately differ:
obench keys fields as ('image','real height') and hardcodes an F/d/C spectral
region, where the exporter emits ('object','angle') and the prescription's
wavelengths. Comparing traced results would fold those choices into the answer.

usage: obench_diff.py <obench.txt> <generated.py>
"""
import sys
import numpy as np
from rayoptics.optical import obench

D_LINE = 587.5618


def read_local(path, drop_glass_names=False):
    """obench.read_url without the network: tab split into [section] buckets.

    drop_glass_names blanks the glass name and maker columns of the lens data so
    obench falls back to numeric nd/vd, pairing with --dont-use-glass-types on
    the exporter. Two reasons to want it: it is the only regime where the two
    dispersion models agree away from d, and some files put a second glass name
    in the maker column rather than a catalog, which obench passes to
    create_glass and dies on where the Beam43 importer falls back to nd/vd.
    """
    with open(path, encoding='utf-8', errors='replace') as f:
        lines = f.read().splitlines()
    sections, key = {}, None
    for line in [l.split('\t') for l in lines]:
        if len(line[0]) > 0:
            if line[0][0] == '[':
                key = line[0][1:-1]
                sections[key] = []
            elif key is not None:
                if drop_glass_names and key == 'lens data':
                    line = line[:6] + [''] * max(0, len(line) - 6)
                sections[key].append(line)
    return sections


def build_from_obench(path, drop_glass_names=False):
    opm = obench.read_lens(read_local(path, drop_glass_names))
    opm.update_model()
    return opm


def build_from_script(path):
    src = open(path, encoding='utf-8').read()
    namespace = {}
    exec(compile(src, path, 'exec'), namespace)
    opm = namespace['opm']
    opm.update_model()
    return opm


def trim_trailing_zeros(coefs):
    """Drop trailing zero coefficients before comparing.

    They are numerically inert - both implementations stop at
    max_nonzero_coef - and obench manufactures them: read_float('') returns 0.,
    so a line ending in a tab picks up a spurious final coefficient. Without
    this, a stray tab in the source file reads as a mismatch.
    """
    end = len(coefs)
    while end > 0 and coefs[end - 1] == 0.0:
        end -= 1
    return coefs[:end]


def profile_of(ifc):
    p = getattr(ifc, 'profile', None)
    if p is None:
        return ('none',)
    name = type(p).__name__
    cv = getattr(p, 'cv', 0.0)
    cc = getattr(p, 'cc', None)
    coefs = trim_trailing_zeros(list(getattr(p, 'coefs', []) or []))
    return (name, cv, cc, coefs)


def medium_of(gap):
    m = gap.medium
    try:
        n = m.rindex(D_LINE)
    except Exception:
        n = float('nan')
    return (type(m).__name__, n)


def close(a, b, tol=1e-12):
    if a is None or b is None:
        return a is b
    return abs(a - b) <= tol * max(1.0, abs(a), abs(b))


def compare(a, b):
    sma, smb = a['seq_model'], b['seq_model']
    problems = []

    if len(sma.ifcs) != len(smb.ifcs):
        problems.append(f'surface count: obench={len(sma.ifcs)} generated={len(smb.ifcs)}')
        return problems

    for i, (ia, ib) in enumerate(zip(sma.ifcs, smb.ifcs)):
        pa, pb = profile_of(ia), profile_of(ib)
        if pa[0] != pb[0]:
            problems.append(f'surf {i}: profile type {pa[0]} vs {pb[0]}')
            continue
        if pa[0] != 'none':
            if not close(pa[1], pb[1]):
                problems.append(f'surf {i}: cv {pa[1]!r} vs {pb[1]!r}')
            if pa[2] is not None and not close(pa[2], pb[2]):
                problems.append(f'surf {i}: cc {pa[2]!r} vs {pb[2]!r}')
            if len(pa[3]) != len(pb[3]):
                problems.append(f'surf {i}: {len(pa[3])} coefs vs {len(pb[3])}\n'
                                f'          obench   ={pa[3]}\n'
                                f'          generated={pb[3]}')
            else:
                for k, (ca, cb) in enumerate(zip(pa[3], pb[3])):
                    if not close(ca, cb):
                        problems.append(f'surf {i}: coef[{k}] {ca!r} vs {cb!r}')
        if not close(getattr(ia, 'max_aperture', None), getattr(ib, 'max_aperture', None), 1e-10):
            problems.append(f'surf {i}: max_aperture {ia.max_aperture!r} vs {ib.max_aperture!r}')
        if ia.interact_mode != ib.interact_mode:
            problems.append(f'surf {i}: interact_mode {ia.interact_mode} vs {ib.interact_mode}')

    for i, (ga, gb) in enumerate(zip(sma.gaps, smb.gaps)):
        if not close(ga.thi, gb.thi, 1e-10):
            problems.append(f'gap {i}: thi {ga.thi!r} vs {gb.thi!r}')
        ma, mb = medium_of(ga), medium_of(gb)
        if not close(ma[1], mb[1], 1e-12):
            problems.append(f'gap {i}: n(d) {ma} vs {mb}')

    if sma.stop_surface != smb.stop_surface:
        problems.append(f'stop surface: {sma.stop_surface} vs {smb.stop_surface}')

    return problems


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    drop_glass_names = '--no-glass' in sys.argv[1:]
    if len(args) != 2:
        print(__doc__)
        return 2
    a = build_from_obench(args[0], drop_glass_names)
    b = build_from_script(args[1])
    problems = compare(a, b)
    print(f'obench   : {len(a["seq_model"].ifcs)} surfaces, stop={a["seq_model"].stop_surface}')
    print(f'generated: {len(b["seq_model"].ifcs)} surfaces, stop={b["seq_model"].stop_surface}')
    if not problems:
        print('\nSequential models MATCH')
        return 0
    print(f'\n{len(problems)} difference(s):')
    for p in problems:
        print('  ' + p)
    return 1


if __name__ == '__main__':
    sys.exit(main())
