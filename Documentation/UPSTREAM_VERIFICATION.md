# Upstream verification

How to run upstream ray-optics alongside the Java port, what that has established,
and the traps in comparing the two.

Paths are written as variables so nothing here is tied to one machine:

| variable | what it points at |
| --- | --- |
| `$BEAM43` | this checkout |
| `$RAYOPTICS` | the upstream [mjhoptics/ray-optics](https://github.com/mjhoptics/ray-optics) checkout |
| `$VENV` | the Python virtualenv, deliberately outside both checkouts |
| `$PYTHON` | `$VENV/Scripts/python.exe` on Windows, `$VENV/bin/python` elsewhere — the name the scripts read |

This file is currently gitignored through the `*.local.md` rule, which it earned when it
held hard-coded paths. Nothing machine-specific is left, so it can be committed under a
plain name whenever that is wanted.

## Why

The `rayoptics` module is a Java port of
[mjhoptics/ray-optics](https://github.com/mjhoptics/ray-optics), and it has no independent
oracle. Upstream's own suite does not provide one: `test_sequential.py` traces the same ray
two ways and compares them to each other rather than to fixtures, `test_ideal_imager.py`
covers `idealimager.py` which is not ported, and most of `test_profiles.py` is written as
`(a, b.all()) == (c, d.all())`, which compares booleans and passes near-vacuously. Checking
the port means running upstream ourselves and comparing.

## Environment

Python 3.13, with the virtualenv at `$VENV` — deliberately outside both checkouts so it
never shows up in `git status`.

```
python -m venv $VENV
$PYTHON -m pip install -e $RAYOPTICS
```

The editable install off the **local checkout** is the important part. It resolves to
`$RAYOPTICS/src/rayoptics`, so comparisons run against the working tree being ported from
rather than a PyPI release, and an upstream `git pull` is picked up with no reinstall. Two
of the defects found in the port were un-ported upstream *fixes* rather than
mistranslations, so which upstream sits on the other side of the comparison matters.

Check what the venv actually resolves to before trusting a comparison:

```
$PYTHON -c "import rayoptics; print(rayoptics.__file__)"
```

As installed: rayoptics 0.6.4.post1.dev663+gcf58cf1af, numpy 2.5.2, scipy 1.18.0,
transforms3d 0.4.2, opticalglass 2.0.2. Nothing here needs the GUI packages, but
`pip install -e` pulls PySide6 as a hard dependency.

## What it has established

**The euler convention.** Upstream commit `2d54408` added `axes='rxyz'` to
`misc_math.euler2rot3d`; the port still used transforms3d's default `sxyz`. Checked against
the installed library: single-axis tilts agree exactly, compound tilts differ by up to
2.0e-2 in matrix elements at 2°/-6°/11°, and `euler2mat(-e).T` — the identity
`Matrix3.euler2mat_rxyz` now implements — reproduces upstream to zero. This was derived by
hand first and the library confirmed the derivation exactly.

**Model construction, via obench.** `rayoptics/optical/obench.py` reads the same optical
bench format Beam43 imports, giving a second independent path to the same lens. Building
both ways and diffing the sequential models — curvature, thickness, semi-diameter,
refractive index, stop surface, profile type, conic constant, aspheric coefficients —
checks the importer and exporter with no ray tracing involved.

| Lens | Asphere type | Result |
| --- | --- | --- |
| Cosina Otus ML 50/1.4 | `Aspherical` | match |
| Canon FL 300/5.6 | `Aspherical` | match |
| Sigma 14-24/2.8 Art (scenario 0) | `AsphericalOddCount` | match, `[0., 0.]` padding confirmed on 5 surfaces |
| Canon EF 50/1.0L | `AsphericalA2` | match, after fixing the infinity bug below |

All three coefficient-padding conventions are covered. The odd case was the one worth
checking: upstream's `RadialPolynomial` coefficients start at the **linear** term while
`EvenPolynomial` starts at the **quadratic**, so the two leading zeros skip r¹ and r², and
Beam43 pads identically. The A2 case starts at the A2 term with no leading zero, unlike
plain `Aspherical` which forces `coefs[0] = 0.`.

**One defect found.** `Double.toString` spells non-finite values `Infinity`, `-Infinity`
and `NaN` — none a Python name, none a Java literal. A plane surface recorded as an
infinite radius emitted a script dying with `NameError` and Java source that would not
compile. `num()` in both writers now maps them to `float('inf')` and
`Double.POSITIVE_INFINITY` and so on. No lens tested before the Canon EF 50 had an infinite
radius, which is exactly the gap a second implementation exists to close.

## Traps

**obench validates the sequential model, not the optical spec.** It deliberately differs:
it keys fields as `('image', 'real height')` with value `Image Height/2` where the exporter
emits `('object', 'angle')`, and hardcodes
`WvlSpec([('F', .5), ('d', 1.), ('C', .5)], ref_wl=1)` rather than reading the prescription.
It never runs vignetting, and supports features we do not — diffractive elements,
`ObjectGlass` on the object gap, `Magnification` switching to a finite conjugate. Comparing
*traced* results against an obench-built model folds those choices into the answer. Compare
structure.

**obench only ever reads scenario 0.** `read_float` resolves a variable-distance name as
`var_dists[s][0]`, the first value. On a zoom lens no other configuration can be checked by
this route.

**Trailing zero coefficients are noise.** `read_float('')` returns `0.`, so an aspheric line
ending in a tab picks up a phantom coefficient. In the Otus file surfaces 11 and 26 have a
trailing tab and 25 does not, reading as a 9-versus-8 mismatch on two surfaces out of three.
Numerically inert — both implementations stop at `max_nonzero_coef` — but a comparison must
trim trailing zeros or it reports a source-file artifact as a port defect.

**Some files break obench's glass lookup.** The Canon EF 50 lens data puts a second glass
name in the maker column (`FDS90` / `N-SF57`) rather than a catalog; obench passes it to
`create_glass` and dies with `GlassCatalogNotFoundError`. The Beam43 importer looks it up,
misses, and falls back to nd/vd. Use the diff tool's `--no-glass`, which blanks the name and
maker columns so obench takes the numeric path.

**Use `--only-d-line` for anything wavelength-dependent.** Upstream turns a numeric
`nd, vd` pair into `opticalglass.modelglass.ModelGlass`, a Buchdahl fit, while `Glass` uses
a GNU Optical fit; the two agree only at d, where both return `nd` by construction. Away
from the reference wavelength any difference is the dispersion model, not the code under
test.

**Loosen tolerances on solver-dependent quantities** — vignetting factors, `z_enp`,
ray-aimed pupil coordinates. `SecantSolver` stands in for scipy's `newton`,
`MinPack.hybrd1` for `fsolve`, and `BrentSolver` for `brentq`.

`SecantSolver` was aligned with scipy in `80fb8aa5`: it now bails out on `q1 == q0`
exactly, as `newton` does, rather than on `|q1-q0| < tol`. So the remaining disagreement
is not a solver mismatch — it is the search's own convergence tolerance. `iterate_pupil_ray`
stops when `|p - p1| < 1e-6` and upstream's `newton` does the same, so the root is only
pinned to about 1e-6 and two correct implementations can land either side of it. Measured
residual on the Otus outer-field vignetting factors is 6.7e-8, well inside that.

That is why the generated tests use 1e-6 for vignetting and aiming rather than the 1e-12
used for analytic quantities: it is the tolerance the search itself converges to, not an
arbitrary loosening. Tightening it would produce failures unrelated to correctness.

One genuine difference remains: `find_z_enp_on_interval` passes an absolute tolerance of
1.48e-8 where upstream passes `rtol=1e-7` to `newton`. In practice the wide angle `z_enp`
values agree far inside 1e-6.

**Stamp the upstream git SHA into any fixture.** Upstream moves under the port — that is
how the `obj_na` and euler defects arose — and a regenerated fixture should be a visible
diff rather than a silent one.

## Running the diff

`RayOpticsExporter` emits Python and Java model builders from one `ModelSpec` over a shared
`Prescription`, so field list, spectral region, wide-angle aiming and vignetting are single
decisions rendered twice rather than two traversals that can drift.

Run from `$BEAM43`, after `mvn compile`. The classpath is every module's `target/classes`;
since the consolidation that is `rayoptics` and `beam42`, so check it against the current
`<modules>` in `pom.xml` rather than trusting this line.

```
CP=rayoptics/target/classes:beam42/target/classes
SPEC=Examples/jfotoptix/canon-ef50mm-f1.0L/US004717245_Example02P.txt

java -cp "$CP" org.redukti.exporters.RayOpticsExporter \
    --specfile $SPEC --dont-use-glass-types --vig-type none > model.py
$PYTHON $BEAM43/rayoptics/src/main/python/obench_diff.py $SPEC model.py --no-glass
```

On Windows the JVM needs `;` between classpath entries and native paths, not the `/c/...`
form Git Bash uses — `generate_upstream_test.sh` handles that with `cygpath`, but a
hand-typed command has to do it itself.

`--vig-type none` keeps the structural comparison fast; vignetting is not compared by this
route anyway.
