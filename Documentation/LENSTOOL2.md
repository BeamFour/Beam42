# LensTool2

`LensTool2` is the command line front end to the RayOptics component. It takes a
single lens specification file and generates a full set of analysis outputs:
layout and spot diagrams, geometric MTF plots, a paraxial report, a Zemax export
and a `README.md` that ties them together. The reports under `Examples/` are all
produced by this tool.

Source: `rayoptics/src/main/java/org/redukti/tools/LensTool2.java`.

## Running it

Build the runnable jar from the repository root:

```bash
mvn package
```

Then run it against a spec file:

```bash
java -jar rayoptics/target/lenstool.jar --specfile Examples/jfotoptix/nikkor-58mm-f1.4g/JP2013-019993_Example01.txt
```

The input is a lens specification in the format used by the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
By default every output file is written next to the spec file.

### Fetching a lens from the Optical Bench

Instead of supplying a file you can name a patent and example, and the
prescription is downloaded from the Optical Bench into a directory you name:

```bash
java -jar rayoptics/target/lenstool.jar --patent JP1993-034592 --example 2 --outdir ef14mm
```

The downloaded prescription is saved alongside the report, under the name the
Optical Bench publishes it as (`JP1993-034592_Example02.txt`), so the report
always keeps the source it was built from. Output naming then follows that file
exactly as it would for a local specfile.

An example is padded to two digits to match the site's naming, so `2` and `02`
are the same lens. Examples that carry a suffix are passed through unchanged, so
`--example 08P` works.

If the Optical Bench has no such lens the tool says so and stops:

```
Patent / example not found on the Optical Bench: JP1993-034592 example 47
(looked for https://www.photonstophotos.net/.../JP1993-034592_Example47.txt)
```

`--specfile` and `--patent` are alternatives; giving both is an error.
`--patent` requires both `--example` and `--outdir`.

Running with no arguments prints a usage summary.

## Input file format

The input is the tab delimited format used by the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm),
with several extensions added by Beam42. Sections are introduced by a name in square
brackets, and lines beginning with `#` are comments.

| Section | Origin | Purpose |
| --- | --- | --- |
| `[descriptive data]` | Optical Bench | `title`, and other free form descriptive fields. |
| `[constants]` | Optical Bench | Selects how `[aspherical data]` coefficients are interpreted: `ODD`, `EVEN A2`, or default `EVEN`. See below. |
| `[variable distances]` | Optical Bench | System values and named airspaces. Three rows are mandatory, see below. Each row may carry **several values**, one per configuration. |
| `[lens data]` | Optical Bench, **extended** | The surface table. Beam42 adds glass and catalog name columns, see below. |
| `[aspherical data]` | Optical Bench | Aspheric coefficients. |
| `[patent info]` | **Beam42 extension** | Provenance for the report header. |
| `[report data]` | **Beam42 extension** | Report title and, importantly, which configurations to process. |
| `[trial n]` | **Beam42 extension** | An optimization run described in the file: what may vary, what it aims at, and what holds the design together. Read only when `--optimize n` asks for it. |
| `[pipeline n]` | **Beam42 extension** | Trials run in order, each starting from the result of the one before. |

A section the reader does not know is skipped, which is what lets the optimizer's
`[trial n]` and `[pipeline n]` sections sit in the same file as the lens they belong to.
Both are described in [OPTIMIZER.md](OPTIMIZER.md).

### `[constants]` and aspheric types

The importer uses two constants to choose the coefficient convention for
`[aspherical data]`:

| Constant present | Aspheric type |
| --- | --- |
| `AsphericalOddCount` | `ODD` |
| `AsphericalA2` (without `AsphericalOddCount`) | `EVEN A2` |
| Neither | `EVEN` |

These are presence checks: the value of `AsphericalOddCount` is not used as a
count, and it takes precedence if both constants are present. Put `[constants]`
before `[aspherical data]`, because the importer selects the type as it reads
each aspheric row. For example:

```text
[constants]
AsphericalOddCount	1
```

Omitting or choosing the wrong constant changes the interpretation of the
coefficients and therefore the surface shape. The generated `prescription.txt`
recreates the applicable aspheric constant; other constants are discarded.

See [Aspheric coefficient ordering in the outputs](#aspheric-coefficient-ordering-in-the-outputs)
for how README and Zemax coefficient lists differ from this input convention.

### `[variable distances]`

Each row is a name followed by one value per configuration / scenario. Three are
**mandatory** and the tool fails with an explicit error if any is missing or not
positive:

| Row | Meaning |
| --- | --- |
| `Focal Length` | Effective focal length in mm. |
| `F-Number` | The f/#, which becomes the pupil specification. |
| `Angle of View` | **Full** angle of view in degrees, not the half angle. |

```
Failed due to: The prescription does not specify 'Angle of View'; add it to the
[variable distances] section as the full angle of view in degrees
```

Every configuration named in `scenarios` (see `[report data]` below) needs its own value in all three rows.
An Optical Bench file may contain `undefined` for a value not specified in the patent; this causes a failure:

```
Failed due to: The prescription specifies 'F-Number' as 'undefined' for
scenario 0; expected the f-number, which must be positive
```

You must manually update the input file in such cases.

The remaining rows in this section are optional:

| Row | If present | If absent |
| --- | --- | --- |
| `Image Height` | Sets the image circle radius. Only the **first** value is read, so it does not vary per configuration even for a zoom. | Defaults to 43.2 mm, i.e. 35 mm format. |
| `Aperture Diameter` | Overrides the diameter given on the `AS` row, one value per configuration. This is how a zoom varies its stop. | The literal diameter on the `AS` row is used. |
| Named airspaces (`Bf`, `d12`, `d20`, ...) | Any row whose name appears in a thickness column of `[lens data]`, supplying one thickness per configuration. | A thickness naming a variable that does not exist silently becomes **0.0**, with no warning. |

### `[lens data]` glass columns

The surface table is tab delimited, one row per surface:

| Column | Contents |
| --- | --- |
| 1 | Surface id, referenced by `[aspherical data]`. |
| 2 | Radius of curvature, or `Infinity`, or one of `AS` (aperture stop), `FS` (field stop), `CG` (cover glass). |
| 3 | Thickness to the next surface. May name a variable from `[variable distances]`, e.g. `d12`. |
| 4 | Refractive index nd. |
| 5 | Clear diameter. |
| 6 | Abbe number vd. |
| 7 | **Beam42 extension** - glass name, e.g. `S-LAH66`. |
| 8 | **Beam42 extension** - catalog name, e.g. `Ohara`. |

Columns 7 and 8 let a surface name a real catalog glass instead of relying on
the tabulated nd/vd in columns 4 and 6. The glass is then looked up in the
catalogs under `glassdata/`, which gives the full dispersion curve rather than a
two number approximation, so the polychromatic results are better.

```
1	115.495	7.84	1.85025	70.5	30.05	S-NBH57	Ohara
```

Recognised catalog names, matched case insensitively, are `Hoya`, `Ohara`,
`Schott`, `Hikari`, `CORNING`, `SUMITA` and `CDGM`. Column 8 may be omitted, in
which case those catalogs are searched in that order for the glass name.
[GLASS_CATALOGS.md](GLASS_CATALOGS.md) lists every glass available, with its
index at each wavelength.

The catalogs are **compiled into the code**, not read at run time. The AGF files
under `glassdata/` are the source material.
Glass types must be updated by running a utility and then the jar rebuilt.

Worth knowing: the named glass is used **only** if it resolves to a catalog
entry. If the glass or catalog name is not recognised, the tool falls back
to the tabulated index and Abbe number - there is no warning. Passing
`--dont-use-glass-types` forces that same fallback for every surface.

### `[patent info]`

Purely descriptive: it is rendered into the generated report header and does not
affect any calculation. Recognised keys, each a `key<tab>value` line:

`country`, `number`, `example`, `year applied`, `inventors`,
`original assignee`, `current assignee`, `link`.

```
[patent info]
country	US
number	US20150146085
example	1
year applied	2013
inventors	Takahiro Hatada
current assignee	Canon Inc
original assignee	Canon Inc
link	https://patents.google.com/patent/US20150146085A1/en
```

An older style that packed the same fields as positional values into a `patent`
row under `[descriptive data]` is still read, but `[patent info]` supersedes it.

### `[report data]`

| Key | Effect |
| --- | --- |
| `lens name` | Display name used as the report heading. |
| `scenarios` | **Selects which configurations are processed.** A list of zero based column indices into the multi valued rows of `[variable distances]`. |
| `names` | A label for each selected configuration, in the same order. |

This is how a zoom is handled. `[variable distances]` carries one column per
focal length, and `scenarios` picks the ones to report on:

```
[variable distances]
Focal Length	11.33	23.28
Angle of View	124.72	85.8
F-Number	4.12	4.12
...
[report data]
lens name	Canon EF11-24mm f4L USM
scenarios	0	1
names	11mm f4.0	24mm f4.0
```

`LensTool2` then produces a complete set of outputs per configuration, suffixed
`-0`, `-1` and so on, and one report covering all of them. The indices need not
be contiguous: `scenarios 0 2` reports on the first and third columns and skips
the second.

Two things to watch:

* `scenarios` and `names` must **both** be present and hold the **same number of
  values**. If either is missing, or the counts differ, the configurations are
  silently ignored and the lens is treated as having a single configuration
  built from column 0.
* With no `scenarios` row at all, the tool processes column 0 only and output
  files carry no numeric suffix. That is usually the right thing for a prime,
  because the first column is normally the infinity object scenario.

## Options

`--specfile` is the only required option. Everything else has a working default,
and the defaults are what the committed examples use.

| Option | Default | Effect |
| --- | --- | --- |
| `--specfile <file>` | *one of these two* | The lens specification to analyse. |
| `--patent <number> --example <n>` | *one of these two* | Download the prescription from the Optical Bench instead of reading a file. Requires `--outdir`. |
| `--outdir <dir>` | alongside the spec file | Directory for generated output. **Required** with `--patent`. See the note on the Zemax file below. |
| `--only-d-line` | off | Build the prescription and Zemax export for the d line alone instead of the full wavelength set. |
| `--dont-use-glass-types` | off (glass types used) | Ignore named glass types in the spec and use the tabulated index/dispersion instead. Useful when a catalogue glass is unavailable or suspect. |
| `--vig-type <type>` | `set-pupil` | Aperture and vignetting calculation run once the model is built, for the models the **analysis** outputs are computed from. See below. Accepts either the enum spelling (`SetPupil`) or kebab case (`set-pupil`), case insensitive. |
| `--use-spot-pattern <pattern>` | `hex` | Pupil sampling pattern for spot diagrams and MTF. One of `hex` (hexapolar), `grid`, or `gaussian` (also accepted as `gq`). See below. |
| `--spot-grid-size <n>` | 64 | Samples per dimension for the rectangular grid. Only consulted when `--use-spot-pattern grid` is in effect; ignored for the other patterns. Minimum 2. |
| `--auto-size-spot-diagrams` | off | Scale each spot diagram to its own spot size. By default all spot diagrams share a fixed 600 unit radius so that fields stay visually comparable. |
| `--mtf <f1,f2,...>` | `10,30,50` | Spatial frequencies in cycles/mm for the MTF by field plots. Every report under `Examples/` uses the default, so change it only when comparing against a manufacturer's own choice of frequencies. |
| `--output-wavelength-mtfs` | off | Additionally emit a per wavelength monochromatic MTF plot for each field. |
| `--output-pupil-maps` | off | Additionally measure and draw which part of each field's pupil the lens passes, and which surface blocks the rest. See below. |
| `--pupil-map-samples <n>` | 121 | Samples per axis in a pupil map. Only consulted with `--output-pupil-maps`. Minimum 2. |
| `--output-ray-aberration-plots` | off | Additionally emit transverse ray aberration **and** wavefront (OPD) fan plots, tangential and sagittal, for each field. |
| `--assign-glass-types` | off | Match each surface's nd and vd to a catalog glass before analysing, so the model uses the full dispersion curve instead of a two number approximation. Applies to this run only. |
| `--index-line <d\|e>` | `d` | Which line the prescription's refractive index column is quoted at, for `--assign-glass-types`. See below. |
| `--force` | off | With `--assign-glass-types`, re-match surfaces that already name a recognised glass. |
| `--update-specfile` | off | With `--assign-glass-types`, also write the matched prescription back over the input file. Refused on its own. |
| `--optimize` | off | Run the routine airspace optimization before reporting: the back focus on a prime, the other variable airspaces on a zoom. See below. |
| `--optimize <n>` | off | Run the spec file's `[trial n]` or `[pipeline n]` section instead, and report on its result. See [OPTIMIZER.md](OPTIMIZER.md). |
| `--optimize-goal <contrast\|mtf>` | `contrast` | Objective for `--optimize`. |
| `--real-ray-aiming` / `--paraxial-ray-aiming` | real | Chief ray aiming algorithm. Real aiming traces an actual ray at the entrance pupil; paraxial aiming is faster but does not hold up on very wide angle lenses. Applies to the analysis model, not the layout diagrams. |

Invalid values for `--mtf`, `--vig-type`, `--use-spot-pattern`,
`--spot-grid-size` and `--pupil-map-samples` are rejected with an error rather than silently falling back
to the default, since each of them changes the numbers that come out.

### Vignetting types

These differ in which way the calculation runs. `set-pupil` derives the pupil
from the authored stop, so it changes the f/# and leaves the apertures alone.
`set-stop-aperture` and `set-fnum` go the other way: they hold the f/# and size
the stop to satisfy it.

`--vig-type` settles the spot diagrams, the MTF, the ray aberration fans and the
vignetting and paraxial dumps. Two other users of vignetting set their own and
are not affected by it: the layout diagrams always use `set-pupil`, since a
layout draws each bundle's rim rays and needs to know where the bundle ends, and
`--optimize` does too, because a merit function wants a ray set that keeps its
sensitivity to the variables rather than one that measures the lens exactly. A
`[trial n]` states its own with a `vignetting` line; see
[OPTIMIZER.md](OPTIMIZER.md).

| Value | What it does | When to use it |
| --- | --- | --- |
| `none` | No aperture or vignetting calculation at all. | Trace the prescription exactly as authored. |
| `paraxial` | Applies paraxial vignetting factors. | Cheap approximation. |
| `set-vig` | Computes vignetting factors from the apertures already in the file. | The apertures are trusted and you want the vignetting that follows from them. |
| `set-pupil` *(default)* | Derives the pupil spec from the authored stop diameter. Apertures unchanged, f/# may shift. | The stop diameter in the prescription is the reliable number. |
| `set-stop-aperture` | Sizes the stop to satisfy the pupil spec, then recomputes vignetting. | The quoted f/# is trusted and the stop diameter is not. |
| `set-apertures` | Computes vignetting, then sizes every clear aperture to just pass the vignetted rays. | Apertures were estimated and you want them rebuilt from the rays. |
| `set-fnum` | Sizes the stop from the quoted f/#, then sizes every other aperture to pass the resulting rays. | A prescription quoting an exact f/# whose apertures were scaled off a patent drawing. |

### Spot patterns

The pattern decides how the pupil is sampled for spot diagrams and for the
geometric MTF derived from them, so it changes every spot and MTF number.

| Value | Sampling | Default density |
| --- | --- | --- |
| `hex` *(default)* | Concentric rings with the ray count per ring growing with radius, giving roughly uniform area coverage. | 64 rings |
| `grid` | A square lattice across the pupil, clipped to the aperture. Density set by `--spot-grid-size`. | 64 x 64 |
| `gaussian` | Gaussian quadrature nodes: far fewer rays for the same accuracy, because the nodes and weights are chosen to integrate the pupil exactly. | 14 rings, 20 spokes |

`gaussian` is the efficient choice and is what the optimizer uses; see
[GAUSSIAN_QUADRATURE.md](GAUSSIAN_QUADRATURE.md) for the implementation and the
paper it follows. `hex` is the historical default and is what the committed
reports under `Examples/` use.

### Pupil maps

`--output-pupil-maps` measures, rather than assumes, which part of each field's
pupil the lens passes. A dense grid of raw pupil coordinates is traced with the
physical apertures checked and the vignetting factors **not** applied, so the
rays that survive map out the bundle the lens really transmits. Each map draws
that region, colours the rest by the surface that blocks it, and overlays the
nominal pupil and the region the vignetting factors describe.

It answers a question the other outputs cannot: whether the four measured
factors put the sampled region where the light actually is. On a normal lens
they shrink the pupil and the two agree closely. On a wide angle lens the
factors are negative - `scale = 1 - factor`, so they *expand* the pupil - and
the bundle off axis reaches well outside the nominal pupil, which is why
sampling confined to the unit circle flatters those lenses.

`pupil-report<suffix>.txt` carries the numbers for every field:

```text
field  vig scales x-,x+,y-,y+       passed |x|,|y|   nominal pupil   piecewise sampled/covered   ellipse sampled/covered
 0.80   1.300  1.300  1.120  1.084    1.296  1.097           100%               98% 98%             98% 98%
 1.00   1.418  1.418  0.654  1.049    1.413  1.032            86%               90% 100%             90% 99%
```

* **vig scales** - the factors as the tracer applies them, `1 - factor`.
* **passed |x|,|y|** - how far the traced bundle actually reaches.
* **nominal pupil** - how much of the unit circle the lens passes.
* **sampled / covered** - for each candidate mapping of the factors, the share of
  the mapped region the lens passes (rays not wasted on blocked light) and the
  share of the bundle the region reaches (light not missed). `piecewise` is what
  the tracer uses; `ellipse` is the single translated ellipse through the same
  four measured extremes, described in [OPTIMIZER.md](OPTIMIZER.md).

`--pupil-map-samples` sets the grid resolution per axis, 121 by default. The cost
is its square, so raise it only for a close look at a boundary.

## Output files

`<suffix>` is empty for a single configuration lens, or `-0`, `-1`, ... for a
spec with multiple configurations (a zoom, for instance).

| File | Always generated | Contents                                                                                            |
| --- | --- |-----------------------------------------------------------------------------------------------------|
| `README.md` | yes | Markdown report collecting the prescription, diagrams and tables below.                             |
| `prescription.txt` | yes | Optical Bench compatible prescription, tab delimited, reduced to what was actually used. See below. |
| `<specfile>.zmx` | yes | Zemax export.                                                                                       |
| `paraxial<suffix>.txt` | yes | First order / paraxial data.                                                                        |
| `vig<suffix>.txt` | yes | Field and vignetting summary.                                                                       |
| `layout<suffix>.svg` | yes | Layout with reference rays.                                                                         |
| `layoutonly<suffix>.svg` | yes | Elements only, no rays.                                                                             |
| `layout-fan<suffix>.svg` | yes | Layout with a 9 ray fan. Generated but not currently linked from the report.                        |
| `spot<suffix>.svg` | yes | Spot diagram, axial field.                                                                          |
| `spot-semi-skew<suffix>.svg` | yes | Spot diagram, 0.7 field.                                                                            |
| `spot-skew<suffix>.svg` | yes | Spot diagram, full field.                                                                           |
| `spot-report<suffix>.txt` | yes | Mean and max spot radius per field.                                                                 |
| `mtf<suffix>.svg` / `.csv` | yes | Geometric MTF by field, equal weighted across wavelengths.                                          |
| `mtf-w<suffix>.svg` / `.csv` | yes | Geometric MTF by field, weighted across wavelengths.                                                |
| `mtf-fld<i>-<wavelength><suffix>.svg` | `--output-wavelength-mtfs` | Monochromatic MTF per field and wavelength. Not included in the README.                             |
| `pupil<suffix>.svg` | `--output-pupil-maps` | Measured pupil map, axial field. Not included in the README. |
| `pupil-semi-skew<suffix>.svg` | `--output-pupil-maps` | Measured pupil map, 0.7 field. Not included in the README. |
| `pupil-skew<suffix>.svg` | `--output-pupil-maps` | Measured pupil map, full field. Not included in the README. |
| `pupil-report<suffix>.txt` | `--output-pupil-maps` | Per field: the vignetting scales, how far the bundle reaches, and how well each candidate mapping describes it. |
| `rayabbr-fld<i>-{tan,sag}<suffix>.svg` | `--output-ray-aberration-plots` | Transverse ray aberration fans. Not included in the README.                               |
| `opdabbr-fld<i>-{tan,sag}<suffix>.svg` | `--output-ray-aberration-plots` | Wavefront (OPD) fans. Not included in the README.                                         |

### Aspheric coefficient ordering in the outputs

The Zemax file and the generated README's **Aspherical Data** table include the
full coefficient array, with zeros in unused leading positions. Optical Bench
input and the generated `prescription.txt` omit those positions:

| Type | Optical Bench / `prescription.txt` coefficient sequence | Zemax parameters / README `P1`, `P2`, ... |
| --- | --- | --- |
| `EVEN` | `a, b, ...` | `0, a, b, ...` |
| `EVEN A2` | `a, b, ...` | `a, b, ...` |
| `ODD` | `a, b, ...` | `0, 0, a, b, ...` |

Here `a` and `b` stand for the first two supplied polynomial coefficients,
after the radius and conic constant in an Optical Bench aspheric row. The
leading zeros are restored on import and omitted again when writing
`prescription.txt`; they do not represent a change to the surface. Do not copy
the full README or Zemax sequence into `[aspherical data]` without removing the
unused leading positions for the selected type. The README labels both even
variants as `EVEN`, and also pads shorter rows with trailing zeros to align the
table columns.

### Routine optimization

`--optimize` runs the airspace optimization that an imported patent prescription
usually needs, before the report is generated, so every output reflects the
optimized design. What it varies depends on the lens:

| Lens | Varied |
| --- | --- |
| Prime | The back focus airspace, located automatically. |
| Zoom | The variable airspaces other than the back focus, one configuration at a time. |

On a design ending in a cover glass the back focus is taken as the airspace **in
front of** the cover glass, not the short gap between cover glass and image.
`CG` rows in `[lens data]` are what makes this exact rather than a guess.

A zoom is optimized one configuration at a time. There is no support yet for a
variable shared across configurations, so the back focus is deliberately left
out of the zoom case: it normally has to stay common across zoom settings, and
nothing here can enforce that.

Effective focal length and f-number are anchored to the prescription throughout,
so an optimized airspace cannot quietly turn the lens into a different one.

**On the objective.** Both goals target the central field at the `--mtf`
frequencies. `contrast` is the default, as it is the more effective option.
Use `--optimize-goal mtf` only if you want to check that out.

`--optimize` **with a number** is a different thing: it runs an optimization the
prescription itself describes - a `[trial n]` section, or a `[pipeline n]` that runs
several trials in order, each starting from the last result. There you choose the
variables, the goals and the constraints. The optimized prescription is written as
`<specfile>-trial<n>.txt` or `<specfile>-pipeline<n>.txt`, and the report is built from
it. See [OPTIMIZER.md](OPTIMIZER.md).

### `prescription.txt` round trips

`prescription.txt` contains the data and configurations used for the report;
unused input data is discarded.

It can be fed straight back in. For a run using the default analysis options:

```bash
java -jar rayoptics/target/lenstool.jar --specfile prescription.txt --outdir rerun
```

The prescription does not save command line analysis settings. To reproduce a
non-default run, supply the same `--mtf`, `--only-d-line`, `--vig-type`, ray aiming,
spot sampling, diagram sizing and optional plot flags again. For example:

```bash
java -jar rayoptics/target/lenstool.jar --specfile prescription.txt --outdir rerun --mtf 10,40 --use-spot-pattern gaussian --only-d-line
```

Optimized airspaces and assigned glasses are already saved in the prescription;
do not repeat `--optimize` or glass assignment to reproduce that geometry.
With matching analysis settings, the regenerated outputs should reproduce the
same numerical results. Two output details can still vary:

* The report footer carries the generation date, `Report / Zemax file generated
  using Beam42 on <date>`.
* The Zemax file is named after the input file, so re-running on
  `prescription.txt` yields `prescription.zmx` and the report links to that name.

### MTF wavelengths and weights

The two MTF by field curves differ only in which wavelengths they combine, and
both sets are **fixed in the code** - there is no option to choose wavelengths or
weights.

| Output | Wavelengths | Weights |
| --- | --- | --- |
| `mtf` | d, F, C | 1.0, 1.0, 1.0 - equal |
| `mtf-w` | d, C, e, F, g | 1.0, 0.475, 0.98, 0.49, 0.15 |

`--only-d-line` is the one thing that changes this: it reduces **both** to the d
line alone, which makes the two outputs identical.

Per wavelength MTF plots, from `--output-wavelength-mtfs`, are **cut off at 100
cycles/mm**. The limit applies to the plotted data as well as the axis, and is
not configurable.

## Behaviour worth knowing

* **Only an object at infinity is supported.** The object distance is fixed at 1e10
  mm and fields are specified as object angles, so the tool cannot analyse a
  lens at a finite conjugate. A `Magnification` row in `[variable distances]` is
  carried through to the regenerated prescription but does not build a finite
  conjugate model.
* **Only one field and pupil specification is supported.** The model is always
  built with the field as an object space **angle** with **relative** field
  heights, and the pupil as an image space **f/#**. RayOptics itself also
  supports image space real height and object or image height for the field, and
  entrance pupil diameter or numerical aperture for the pupil, but none of those
  are reachable from this tool.
* **Fields are fixed.** Analysis runs at eleven relative field heights, 0.0 to
  1.0 in steps of 0.1. Layout diagrams use only 0.0 and 1.0. Neither set is
  configurable from the command line.
* **Configurations are chosen in the file, not on the command line.** The
  `scenarios` row of `[report data]` selects them; the tool then loops over all
  of them in one run. There is no command line option to report on just one.
* **`--outdir` does not move the Zemax file.** Every other output honours it,
  but the `.zmx` is always written next to the spec file.
* **`--vig-type` does not reach the layout diagrams.** They are always built
  with `SetPupil`; the option only affects the model used for spot, MTF and
  aberration analysis.
* **Ray aiming defaults to real.** Real aiming is used unless
  `--paraxial-ray-aiming` is given. Layout diagrams always use real aiming
  regardless of the flag.

## Gotchas when working on prescriptions

A prescription imported straight from the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm)
will usually produce **poor MTF on the first run**. This is normal and is not
necessarily a sign that the import went wrong. Patents are not written to be
manufacturable prescriptions: they quote values to limited precision, they
sometimes contain outright errors, and the example tabulated is often not the
configuration the shipping lens actually used.

Recovering a sensible design is an iterative process. Work through these in
order, checking the MTF after each step, and stop as soon as the result looks
right. The aim throughout is usually to **reproduce the manufacturer's published
MTF curves** - those are the reference to judge against, not an abstract notion
of good performance.

### 1. Assign glass types

Patents quote a refractive index and an Abbe number only. Substituting real
catalog glasses gives the full dispersion curve instead of a two number
approximation, which alone often fixes a large part of the polychromatic error -
frequently it is the single biggest improvement available.

`LensTool2` can do this for you with `--assign-glass-types`:

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types
```

It reports what it managed, for example
`Assigned 17 glass types; 0 ambiguous; 0 unmatched`. By default the matched
glasses are used for **that run only** and the input file is left untouched; add
`--update-specfile` to write them back. `--force` re-matches surfaces that
already name a glass.

Resolve glass-matching problems before optimizing. If most surfaces are ambiguous
or unmatched, check
[When the index is quoted at the e line](#when-the-index-is-quoted-at-the-e-line).

Where several catalog glasses fit within tolerance and none is an exact match,
the surface is left without a glass and `candidate=...` fields are appended to
the row, listing each option with its offsets, for you to choose from by hand.
A surface that *was* assigned may also carry `candidate=` fields, listing the
alternatives that were within tolerance; those are information, not a failure.

Expect equivalent glasses from a different maker: matching is on the refractive
index and vd, and the catalogs are searched in priority order, so an Ohara
S-FPL51 may come back as the equivalent Hoya FCD1. That is the same glass
optically, not a mismatch.

#### When the index is quoted at the e line

Most prescriptions quote the refractive index at the **d** line, and that is what
matching assumes. Some patents instead tabulate it at the **e** line, 546.07 nm,
while still quoting the Abbe number as vd. Matching these against d-line indices
can leave most surfaces ambiguous or unmatched. A few unmatched surfaces may
instead indicate obsolete or in-house glasses.

Signs of an index-line mismatch include failures concentrated among high-index,
low-Abbe glasses and candidate indices consistently below the file's values.
For example, on the Sony FE 14mm F1.8 GM:

| Matching | Result |
| --- | --- |
| d line, the default | `Assigned 0 glass types; 9 ambiguous; 5 unmatched` |
| `--index-line e` | `Assigned 14 glass types; 0 ambiguous; 0 unmatched` |

`--index-line e` matches the index against ne instead. The Abbe number is still
matched as vd, since that is what these files quote.

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types --index-line e
```

When it assigns a glass this way it also **rewrites the index and Abbe columns to
the catalog's d line values**, so later imports use the correct convention.

The same matching is available standalone, which is useful when you only want to
enrich a file:

```bash
java -cp rayoptics/target/lenstool.jar org.redukti.tools.GlassFinder --specfile input.txt -o specs.txt
```

### 2. Optimize the back focus, for a prime

If the design is a prime, the back focus is the single most productive variable
and is frequently the only thing wrong. Patents often round it, and a small
error there costs a lot of MTF.

`--optimize` locates and adjusts this airspace. See
[Routine optimization](#routine-optimization) for cover-glass handling.

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types --optimize
```

### 3. Optimize the variable thicknesses

If back focus alone does not recover the design, widen the search to the other
variable airspaces - the rows in `[variable distances]` referenced from the
thickness column.

This is the usual path for **zooms**, and `--optimize` does it automatically on a
multi configuration prescription. See [Routine optimization](#routine-optimization)
for the variables selected and the limitation on shared back focus.

### 4. Optimize curvatures and aspherics

Only if the steps above fail. Changing curvatures means you are no longer
reproducing the patent but redesigning from it, so it is the last resort rather
than the first tool to reach for.

Beyond what `--optimize` covers, these steps mean driving the optimizer through
its Java API - `OptimizationBuilder` - as there is no command line front end for
varying curvatures or aspherics.

### Silent failures to rule out first

Before concluding that a design is genuinely poor, check that the file is being
read the way you think. Three import problems degrade the model without
producing any warning:

* A **glass or catalog name that does not resolve** falls back to the tabulated
  nd and vd. Check `prescription.txt` in the output to see which glasses were
  actually applied.
* A **thickness naming a variable that does not exist** silently becomes 0.0,
  collapsing that airspace.
* A **`scenarios` and `names` mismatch** in `[report data]` causes every
  configuration to be ignored, and the lens is analysed as a single
  configuration built from the first column.
