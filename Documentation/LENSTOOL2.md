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
java -jar rayoptics/target/lenstool.jar --specfile Examples/jfotoptix/nikkor-58mm-f1.4g/specs.txt
```

The input is a lens specification in the format used by the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
By default every output file is written next to the spec file.

Running with no arguments prints a usage summary.

## Input file format

The input is the tab delimited format used by the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm),
with two extensions added by Beam42. Sections are introduced by a name in square
brackets, and lines beginning with `#` are comments.

| Section | Origin | Purpose |
| --- | --- | --- |
| `[descriptive data]` | Optical Bench | `title`, and other free form descriptive fields. |
| `[constants]` | Optical Bench | Unused by this tool, carried through. |
| `[variable distances]` | Optical Bench | System values and named airspaces. Three rows are mandatory, see below. Each row may carry **several values**, one per configuration. |
| `[lens data]` | Optical Bench, **extended** | The surface table. Beam42 adds glass and catalog name columns, see below. |
| `[aspherical data]` | Optical Bench | Aspheric coefficients. |
| `[patent info]` | **Beam42 extension** | Provenance for the report header. |
| `[report data]` | **Beam42 extension** | Report title and, importantly, which configurations to process. |

### `[variable distances]`

Each row is a name followed by one value per configuration. Three are
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

Every configuration named in `scenarios` needs its own value in all three rows.
Note that these files use `undefined` as a placeholder for a column that was
never filled in; that reads back as zero and is rejected the same way:

```
Failed due to: The prescription specifies 'F-Number' as 'undefined' for
scenario 0; expected the f-number, which must be positive
```

The remaining rows are optional:

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
under `glassdata/` are the source material: `GlassMapGenerator` parses them
offline and emits `Glass.java`, which registers every glass with its index at
each wavelength in a static initializer. So a run never touches an AGF file, and
there is currently no way to point the tool at one - adding or updating a
catalog means regenerating `Glass.java` and rebuilding.

Worth knowing: the named glass is used **only** if it resolves to a catalog
entry. If the glass or catalog name is not recognised, the tool falls back
silently to the tabulated index and Abbe number - there is no warning. Passing
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
| `--specfile <file>` | *required* | The lens specification to analyse. |
| `--outdir <dir>` | alongside the spec file | Directory for generated output. See the note on the Zemax file below. |
| `--only-d-line` | off | Build the prescription and Zemax export for the d line alone instead of the full wavelength set. |
| `--dont-use-glass-types` | off (glass types used) | Ignore named glass types in the spec and use the tabulated index/dispersion instead. Useful when a catalogue glass is unavailable or suspect. |
| `--vig-type <type>` | `set-pupil` | Aperture and vignetting calculation run once the model is built. See below. Accepts either the enum spelling (`SetPupil`) or kebab case (`set-pupil`), case insensitive. |
| `--use-spot-pattern <pattern>` | `hex` | Pupil sampling pattern for spot diagrams and MTF. One of `hex` (hexapolar), `grid`, or `gaussian` (also accepted as `gq`). See below. |
| `--spot-grid-size <n>` | 64 | Samples per dimension for the rectangular grid. Only consulted when `--use-spot-pattern grid` is in effect; ignored for the other patterns. Minimum 2. |
| `--auto-size-spot-diagrams` | off | Scale each spot diagram to its own spot size. By default all spot diagrams share a fixed 600 unit radius so that fields stay visually comparable. |
| `--mtf <f1,f2,...>` | `10,30,50` | Spatial frequencies in cycles/mm for the MTF by field plots. Every report under `Examples/` uses the default, so change it only when comparing against a manufacturer's own choice of frequencies. |
| `--output-wavelength-mtfs` | off | Additionally emit a per wavelength monochromatic MTF plot for each field. |
| `--output-ray-aberration-plots` | off | Additionally emit transverse ray aberration **and** wavefront (OPD) fan plots, tangential and sagittal, for each field. |
| `--assign-glass-types` | off | Match each surface's nd and vd to a catalog glass before analysing, so the model uses the full dispersion curve instead of a two number approximation. Applies to this run only. |
| `--force` | off | With `--assign-glass-types`, re-match surfaces that already name a recognised glass. |
| `--update-specfile` | off | With `--assign-glass-types`, also write the matched prescription back over the input file. Refused on its own. |
| `--optimize` | off | Run the routine airspace optimization before reporting: the back focus on a prime, the other variable airspaces on a zoom. See below. |
| `--optimize-goal <contrast\|mtf>` | `contrast` | Objective for `--optimize`. |
| `--real-ray-aiming` / `--paraxial-ray-aiming` | real | Chief ray aiming algorithm. Real aiming traces an actual ray at the entrance pupil; paraxial aiming is faster but does not hold up on very wide angle lenses. Applies to the analysis model, not the layout diagrams. |

Invalid values for `--mtf`, `--vig-type`, `--use-spot-pattern` and
`--spot-grid-size` are rejected with an error rather than silently falling back
to the default, since each of them changes the numbers that come out.

### Vignetting types

These differ in which way the calculation runs. `set-pupil` derives the pupil
from the authored stop, so it changes the f/# and leaves the apertures alone.
`set-stop-aperture` and `set-fnum` go the other way: they hold the f/# and size
the stop to satisfy it.

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

## Output files

`<suffix>` is empty for a single configuration lens, or `-0`, `-1`, ... for a
spec with multiple configurations (a zoom, for instance).

| File | Always generated | Contents |
| --- | --- | --- |
| `README.md` | yes | Markdown report collecting the prescription, diagrams and tables below. |
| `prescription.txt` | yes | Optical Bench compatible prescription, tab delimited, reduced to what was actually used. See below. |
| `<specfile>.zmx` | yes | Zemax export. |
| `paraxial<suffix>.txt` | yes | First order / paraxial data. |
| `vig<suffix>.txt` | yes | Field and vignetting summary. |
| `layout<suffix>.svg` | yes | Layout with reference rays. |
| `layoutonly<suffix>.svg` | yes | Elements only, no rays. |
| `layout-fan<suffix>.svg` | yes | Layout with a 9 ray fan. Generated but not currently linked from the report. |
| `spot<suffix>.svg` | yes | Spot diagram, axial field. |
| `spot-semi-skew<suffix>.svg` | yes | Spot diagram, 0.7 field. |
| `spot-skew<suffix>.svg` | yes | Spot diagram, full field. |
| `spot-report<suffix>.txt` | yes | Mean and max spot radius per field. |
| `mtf<suffix>.svg` / `.csv` | yes | Geometric MTF by field, equal weighted across wavelengths. |
| `mtf-w<suffix>.svg` / `.csv` | yes | Geometric MTF by field, weighted across wavelengths. |
| `mtf-fld<i>-<wavelength><suffix>.svg` | `--output-wavelength-mtfs` | Monochromatic MTF per field and wavelength. |
| `rayabbr-fld<i>-{tan,sag}<suffix>.svg` | `--output-ray-aberration-plots` | Transverse ray aberration fans. |
| `opdabbr-fld<i>-{tan,sag}<suffix>.svg` | `--output-ray-aberration-plots` | Wavefront (OPD) fans. |

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
frequencies. `contrast` is the default because the geometric MTF merit surface is
rough at the scale the solver steps. Detuning a known good back focus by 1.5 mm
and asking each objective to recover it:

| Objective | Result |
| --- | --- |
| `contrast` | Recovers 37.3265 against a true 37.32, merit 0.750 to 0.019. |
| `mtf` | Does not move at all: the local gradient points the wrong way and the solver stops at once. |

Sampling the merit against back focus shows why - the contrast merit falls
smoothly to a single clean minimum at the right place, while the geometric MTF
merit wanders non-monotonically with local minima throughout. Use
`--optimize-goal mtf` only if you want to see that directly.

### `prescription.txt` round trips

The generated prescription is not a copy of the input. It is narrowed to the
subset `LensTool2` understands and actually used: data the tool does not consume
and scenarios that were not selected are dropped. What is left is a prescription
that corresponds exactly to the report beside it.

It can be fed straight back in, and doing so reproduces the run:

```bash
java -jar rayoptics/target/lenstool.jar --specfile prescription.txt --outdir rerun
```

Every generated file comes back byte for byte identical, and `prescription.txt`
regenerates itself unchanged, so it is a fixed point rather than merely
idempotent in the numbers. Two things do legitimately vary:

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

* **Only an object at infinity is supported.** The object gap is fixed at 1e10
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

Patents quote nd and vd only. Substituting real catalog glasses gives the full
dispersion curve instead of a two number approximation, which alone often fixes
a large part of the polychromatic error.

`LensTool2` can do this for you with `--assign-glass-types`:

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types
```

It reports what it managed, for example
`Assigned 17 glass types; 0 ambiguous; 0 unmatched`. By default the matched
glasses are used for **that run only** and the input file is left untouched; add
`--update-specfile` to write them back. `--force` re-matches surfaces that
already name a glass.

Where several catalog glasses fit within tolerance and none is an exact match,
the surface is left without a glass and `candidate=...` fields are appended to
the row, listing each option with its nd and vd offsets, for you to choose from
by hand.

Expect equivalent glasses from a different maker: matching is on nd and vd, and
the catalogs are searched in priority order, so an Ohara S-FPL51 may come back as
the equivalent Hoya FCD1. That is the same glass optically, not a mismatch.

The same matching is available standalone, which is useful when you only want to
enrich a file:

```bash
java -cp rayoptics/target/lenstool.jar org.redukti.tools.GlassFinder --specfile input.txt -o specs.txt
```

### 2. Optimize the back focus, for a prime

If the design is a prime, the back focus is the single most productive variable
and is frequently the only thing wrong. Patents often round it, and a small
error there costs a lot of MTF.

`--optimize` does this for you, including finding the right airspace when there
is **a cover glass near the image** - in those designs the distance that matters
is the one to the cover glass, not the figure the patent labels as back focus:

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types --optimize
```

### 3. Optimize the variable thicknesses

If back focus alone does not recover the design, widen the search to the other
variable airspaces - the rows in `[variable distances]` referenced from the
thickness column.

This is the usual path for **zooms**, and `--optimize` does it automatically on a
multi configuration prescription. Note that back focus is not a free variable in
a zoom the way it is in a prime: it normally has to stay the same across all zoom
settings, so it is left out rather than optimized per configuration.

### 4. Optimize curvatures and aspherics

Only if the steps above fail. Changing curvatures means you are no longer
reproducing the patent but redesigning from it, so it is the last resort rather
than the first tool to reach for.

Steps 2 to 4 use the optimizer, which is a Java API and has no command line
front end; see [OPTIMIZER.md](OPTIMIZER.md).

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
