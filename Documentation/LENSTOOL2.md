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
| `[variable distances]` | Optical Bench | `Focal Length`, `Angle of View`, `F-Number`, `Image Height`, and the named airspaces (`Bf`, `d12`, ...). Each row may carry **several values**, one per configuration. |
| `[lens data]` | Optical Bench, **extended** | The surface table. Beam42 adds glass and catalog name columns, see below. |
| `[aspherical data]` | Optical Bench | Aspheric coefficients. |
| `[patent info]` | **Beam42 extension** | Provenance for the report header. |
| `[report data]` | **Beam42 extension** | Report title and, importantly, which configurations to process. |

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
  files carry no numeric suffix.

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
| `prescription.txt` | yes | Optical Bench compatible prescription, tab delimited. |
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
