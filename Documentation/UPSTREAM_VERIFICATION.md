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

There are two documents. This one is the background: why the comparison exists, what it has
established, and the traps. `rayoptics/src/main/python/README.md` is the command reference
for the three scripts, with per-script options and the assertion breakdown of a generated
test. Start here, then go there to run something.

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

That is why the generated tests split their tolerances by kind, and none of the three is an
arbitrary loosening:

| quantity | tolerance | why |
| --- | --- | --- |
| analytic | 1e-12 relative | in practice they agree bit for bit |
| vignetting, aiming | 1e-6 | the tolerance the vignetting search itself converges to; measured residual ~7e-8 |
| ray data | 1e-6 | downstream of two iterated solves, so no more exact than the ray it starts from |

Ray data cannot beat its starting ray, which comes from `iterate_ray` for the chief ray aim
point and `calc_vignetted_ray` for the vignetting factors. The residual propagates linearly
rather than amplifying: on the Otus outer field `vuy` differs by 6.7e-8 relative, giving
3.4e-8 on the pupil coordinate, which over a 17.5 mm entrance pupil radius is 5.9e-7 mm of
ray height — against a largest observed difference of 5.7e-7. `op_delta` agrees to a few
times 1e-9. All of it stays six orders tighter than any real kernel defect would produce.

If you are tempted to tighten these: tracing with `apply_vignetting=False` does *not* buy
precision, because the aim point is still iterated, and it costs coverage — unscaled
marginal rays at the outer fields get blocked and drop out of the comparison entirely.

One genuine difference remains: `find_z_enp_on_interval` passes an absolute tolerance of
1.48e-8 where upstream passes `rtol=1e-7` to `newton`. In practice the wide angle `z_enp`
values agree far inside 1e-6.

**Upstream's `apply_vignetting` mutates its argument; Beam43's does not.** Upstream does
`vig_pupil = pupil[:]`, which for a numpy array is a view rather than a copy, so it scales
the caller's array in place. `trace_ray_fan` records the pupil *after* tracing and therefore
captures the vignetted coordinate; Beam43 copies, so its `fan_x` keeps the nominal value.

The rays traced are identical either way — only the recorded abscissa differs — but it looks
alarming when it surfaces:

```
fan.0.0.0.0.pupil ==> expected: <-1.0003278333220664> but was: <-1.0>
```

which is `-1 x (1 - vlx)`. `dump_reference.py` records the nominal fan abscissa, accumulated
the way both sides step it, so the pupil assertion still guards index alignment without
depending on the quirk. Beam43's copying behaviour is the sane one and should stay; just do
not expect a caller's pupil array to come back modified.

**Stamp the upstream git SHA into any fixture.** Upstream moves under the port — that is
how the `obj_na` and euler defects arose — and a regenerated fixture should be a visible
diff rather than a silent one.

## Regenerating the regression tests

This is the workflow that actually gets used. `generate_upstream_test.sh` emits the Python
model, runs it under upstream to capture reference values, and writes a JUnit test with
those values inlined, into `org.redukti.rayoptics.upstream`. See the scripts' README for
what each generated method asserts and at what tolerance.

```
export PYTHON=$VENV/Scripts/python.exe
cd $BEAM43 && mvn compile

FLAGS="--only-d-line --dont-use-glass-types --vig-type set-vig"
./rayoptics/src/main/python/generate_upstream_test.sh \
    Examples/jfotoptix/sigma-14-24mm-f2.8-art/JP2018-189733_Example01P.txt --scenario 0 $FLAGS
./rayoptics/src/main/python/generate_upstream_test.sh \
    Examples/jfotoptix/cosina-otus-ml-50mm-f1.4/JP2026-105585_Example01.txt $FLAGS
./rayoptics/src/main/python/generate_upstream_test.sh \
    Examples/jfotoptix/nikkor-58mm-z-f0.95/nikkor-z-58mmf0.95_ex1.txt $FLAGS
./rayoptics/src/main/python/generate_upstream_test.sh \
    Examples/jfotoptix/leica-r-summicron-50mm-f2/US004123144_Example08P.txt $FLAGS
./rayoptics/src/main/python/generate_upstream_test.sh \
    Examples/jfotoptix/canon-ef50mm-f1.0L/US004717245_Example02P.txt $FLAGS
```

Those five lenses are chosen to cover the three aspheric coefficient conventions, a plain
double Gauss, and — the Sigma — a wide-angle model that exercises `find_real_enp_rev1`.

**Regenerate all five, not only the ones whose assertions failed.** Any change touching
vignetting moves the reference values by ~1e-7, and the tests that still pass are merely
inside tolerance rather than unaffected. Leaving them puts the fixtures on two different
upstream commits, and the next sync produces a confusing partial diff.

`generate_upstream_test.sh` builds its classpath from a hard-coded module list. That list
has gone stale once already, when the modules consolidated — if it aborts with
`Missing .../target/classes`, check it against `<modules>` in `pom.xml`.

Other tests carry values that move with the same changes but are not regenerated by this
script: `ZeissOtusML50mmTest` (see the memory note on harvesting its goldens in one run),
and occasionally a tolerance or a rounded digit in `OptimizationBuilderTest` and `MtfTest`.

## Running the obench structural diff

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
