# Upstream verification scripts

Tools for checking the `rayoptics` Java port against
[mjhoptics/ray-optics](https://github.com/mjhoptics/ray-optics), the project it is
ported from.

The port has no independent oracle, and upstream's own test suite does not provide one:
`test_sequential.py` traces the same ray two ways and compares them to each other rather
than to fixtures, `test_ideal_imager.py` covers a module that is not ported, and most of
`test_profiles.py` compares booleans. Checking the port therefore means running upstream
and comparing against it, which is what these scripts do.

## Setup

A Python environment with upstream ray-optics installed:

```
python -m venv <venv-dir>
<venv-dir>/Scripts/python -m pip install -e <path-to-ray-optics-checkout>
```

Install from a **local checkout** rather than PyPI. Two of the defects found in the port
were un-ported upstream *fixes* rather than mistranslations, so which upstream sits on the
other side of the comparison matters, and an editable install tracks whatever that checkout
is currently on.

Point `PYTHON` at the resulting interpreter:

```
export PYTHON=<venv-dir>/Scripts/python
```

Everything below also needs a built tree — run `mvn compile` first.

## `generate_upstream_test.sh`

Generates a JUnit regression test for one lens.

```
./generate_upstream_test.sh Examples/jfotoptix/<lens>/<spec>.txt \
    --only-d-line --dont-use-glass-types --vig-type set-vig
```

Three steps, all driven from the same `ModelSpec` so the Python and Java sides describe the
same system by construction: emit the Python model, run it under upstream to capture
reference values, then emit the Java test with those values inlined as literals. Output goes
to `rayoptics/src/test/java/org/redukti/rayoptics/upstream/`.

Values are inlined rather than loaded from a fixture at run time so that when a change moves
a number, the diff names the quantity and shows the delta. Regenerate rather than edit.

The generated test asserts, in separate methods so a failure names the layer that moved:
first order data, paraxial `ax`/`pr` rays per surface, vignetting factors, chief ray aiming,
and Seidel coefficients per surface including aspheric contributions and the sum.

Tolerances are split by kind. Analytic quantities use 1e-12 relative; in practice they agree
bit for bit. Vignetting and aiming use 1e-6, which is not an arbitrary loosening — it is the
tolerance the vignetting search itself converges to, since `iterate_pupil_ray` and upstream's
`newton` both stop at `|p - p1| < 1e-6`. Two correct implementations can land either side of
a root pinned only that far. Measured residual is around 7e-8.

Add a new lens by running the script and committing the result. Pick lenses that exercise
something distinct — the current four cover the three aspheric coefficient conventions plus
a plain double Gauss, and one of them is wide angle.

## `dump_reference.py`

Builds a model from a generated ray-optics script and writes the reference values as flat,
sorted `key=value` text.

```
$PYTHON dump_reference.py model.py > reference.txt
```

Flat text rather than JSON: no JSON dependency is needed on the Java side, and a regenerated
file diffs line by line so a moved number names itself. Normally invoked by
`generate_upstream_test.sh` rather than directly.

## `obench_diff.py`

Compares the sequential model upstream builds from an optical bench file against the one the
exporter describes.

```
$PYTHON obench_diff.py <spec>.txt model.py [--no-glass]
```

Upstream's `rayoptics/optical/obench.py` reads the same optical bench format Beam43 imports,
giving a second independent path to the same lens. This diffs curvature, thickness,
semi-diameter, refractive index, stop surface, profile type, conic constant and aspheric
coefficients — checking the importer and exporter with no ray tracing involved.

Generate the model with `--vig-type none`; vignetting is not compared by this route and only
slows it down.

## Things that will bite you

**obench validates the sequential model, not the optical spec.** It deliberately differs: it
keys fields as `('image', 'real height')` where the exporter emits `('object', 'angle')`, and
hardcodes `WvlSpec([('F', .5), ('d', 1.), ('C', .5)], ref_wl=1)` rather than reading the
prescription. It never runs vignetting, and supports features the port does not — diffractive
elements, `ObjectGlass`, `Magnification` switching to a finite conjugate. Comparing *traced*
results against an obench-built model folds those choices into the answer. Compare structure.

**obench only ever reads scenario 0.** Its `read_float` resolves a variable-distance name as
`var_dists[s][0]`, the first value, so no other zoom configuration can be checked this way.

**Trailing zero coefficients are noise.** `read_float('')` returns `0.`, so an aspheric data
line ending in a tab picks up a phantom coefficient — a real case in the wild has two
surfaces with a trailing tab and one without, which reads as a 9-versus-8 mismatch. It is
numerically inert, since both implementations stop at `max_nonzero_coef`. `obench_diff.py`
trims trailing zeros before comparing for this reason.

**Some files break obench's glass lookup.** Where the lens data puts a second glass name in
the maker column rather than a catalog, obench passes it to `create_glass` and raises
`GlassCatalogNotFoundError`; the Beam43 importer looks it up, misses, and falls back to
nd/vd. Use `--no-glass`, which blanks the name and maker columns so obench takes the numeric
path.

**Use `--only-d-line` for anything wavelength-dependent.** Upstream turns a numeric `nd, vd`
pair into `opticalglass.modelglass.ModelGlass`, a Buchdahl fit, while `Glass` uses a GNU
Optical fit. They agree only at d, where both return `nd` by construction. Away from the
reference wavelength, any difference is the dispersion model rather than the code under test.

**Record which upstream a fixture came from.** Upstream moves under the port — that is how
the `obj_na` and euler-convention defects arose — so a regenerated fixture should be a
visible diff rather than a silent one.
