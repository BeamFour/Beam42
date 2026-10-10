# LensTool2

`LensTool2` is the command line front end to Beam42's RayOptics module. It takes a
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
By default, all output files are written next to the input spec file.

### Fetching a lens from the Optical Bench

Instead of supplying a spec file you can name a patent and example number, and the
prescription is downloaded from the Optical Bench into a directory you specify:

```bash
java -jar rayoptics/target/lenstool.jar --patent JP1993-034592 --example 2 --outdir ef14mm
```

The example number is padded to two digits to match the convention used by Optical Bench, thus `2` is converted to `02`. 
Examples that carry a suffix are passed through unchanged, therefore 
`--example 08P` looks for a spec with that suffix.

The patent numbers must be specified in the format used by Optical Bench.

If the Optical Bench has no matching patent/example then the tool emits an error message such as:

```
Patent / example not found on the Optical Bench: JP1993-034592 example 47
(looked for https://www.photonstophotos.net/.../JP1993-034592_Example47.txt)
```

The downloaded prescription is saved under the name the
Optical Bench publishes it, such as (`JP1993-034592_Example02.txt`). All outputs are saved
alongside.

* `--specfile` and `--patent` are alternatives; giving both is an error.
* `--patent` requires both `--example` and `--outdir`.

## Input file format

The input is the tab delimited format used by the
[PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm),
with several extensions added by Beam42. Sections are introduced by a name in square
brackets. Lines beginning with `#` are treated as comments.

| Section | Origin | Purpose                                                                                                                               |
| --- | --- |---------------------------------------------------------------------------------------------------------------------------------------|
| `[descriptive data]` | Optical Bench | `title`, and other free form descriptive fields.                                                                                      |
| `[constants]` | Optical Bench | Selects how `[aspherical data]` coefficients are interpreted. See below.                                                              |
| `[variable distances]` | Optical Bench | System values and named airspaces. Three rows are mandatory, see below. Each row may carry **several values**, one per configuration. |
| `[lens data]` | Optical Bench, **extended** | The surface table. Beam42 accepts glass and catalog name columns, see below.                                                          |
| `[aspherical data]` | Optical Bench | Aspheric coefficients.                                                                                                                |
| `[notes]` | **Beam42 extension** | Source prescription filename recorded in the generated prescription. Informational only. |
| `[patent info]` | **Beam42 extension** | Provenance for the patent report.                                                                                                     |
| `[report data]` | **Beam42 extension** | Report title and, importantly, which configurations to process.                                                                       |
| `[trial n]` | **Beam42 extension** | An optimization run described in the file. See [OPTIMIZER.md](OPTIMIZER.md) for details.                                                      |
| `[pipeline n]` | **Beam42 extension** | A set of optimization runs, each starting from the result of the one before. See [OPTIMIZER.md](OPTIMIZER.md) for details.                                         |

Unrecognized sections are skipped.

### `[constants]` and aspheric types

The importer uses two constants to choose the coefficient convention for
`[aspherical data]`:

| Constant present | Aspheric type |
| --- | --- |
| Positive `AsphericalOddCount` | `RADIAL` (radial, both odd and even powers) |
| `AsphericalA2` (without a positive `AsphericalOddCount`) | `EVEN A2` |
| Neither | `EVEN` |

`AsphericalOddCount` is a non-negative integer that counts odd powers starting at
A3. Coefficients are ordered by increasing power, including the declared odd
powers interleaved with even powers, then continuing with even powers only:
count 1 means A3, A4, A6, A8, ...; count 2 means A3, A4, A5, A6, A8, ... .
A zero count selects an even format. A positive count takes precedence over
`AsphericalA2` if both constants are present. 

Example:

```text
[constants]
AsphericalOddCount	1
```

Omitting or choosing the wrong constant changes the interpretation of the
coefficients and therefore the surface shape. When writing `prescription.txt` the tool 
regenerates the aspheric constants rather than preserving both as supplied. 
It can increase `AsphericalOddCount` when added terms require it; a zero count is 
omitted, and `AsphericalA2` is emitted only when applicable without a positive 
odd count. Other constants are discarded.

`[constants]` must appear before `[aspherical data]`.

See [Aspheric coefficient ordering in the outputs](#aspheric-coefficient-ordering-in-the-outputs)
for how README and Zemax coefficient lists differ from this input convention.

### `[variable distances]`

Each row is a name followed by one value per configuration / scenario. The first value after 
the variable name is given scenario number 0, and rest are numbered sequentially.

Three of the variables are **mandatory** for LensTool2.

If multiple configurations are present in `scenarios` (see `[report data]` below) then each
must have a corresponding value. It is an error if the required value is missing, `undefined` or non-positive.

| Row | Meaning |
| --- | --- |
| `Focal Length` | Effective focal length in mm. |
| `F-Number` | The f/#, which becomes the pupil specification. |
| `Angle of View` | **Full** angle of view in degrees, not the half angle. |

```
Failed due to: The prescription does not specify 'Angle of View'; add it to the
[variable distances] section as the full angle of view in degrees
```
If the Optical Bench spec file omits values required by LensTool2, you must manually edit the file.

LensTool2 treats the following as optional:

| Row | If present                                                                                                                                                                | If absent                                                                                     |
| --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `Image Height` | Sets the image circle diameter. Only the **first** value is read, so it does not vary per configuration.                                                                  | Defaults to 43.2 mm, i.e. 35 mm format.                                                       |
| `Aperture Diameter` | Overrides the diameter given on the aperture stop (`AS`) row in `[lens data]`, one value per configuration. This allows a zoom to vary its stop diameter by focal length. | The diameter defined on the aperture stop (`AS`) row in `[lens data]` is used.                |
| Named airspaces (`Bf`, `d12`, `d20`, ...) | Any row whose name appears in a thickness column of `[lens data]`, supplying one thickness per configuration. Zero is a valid thickness. | An undefined variable, or a missing, empty or non-numeric value for a selected scenario, is an error. |

### `[lens data]` glass columns

The surface table is tab delimited, one row per surface:

| Column | Contents                                                                                                   |
| --- |------------------------------------------------------------------------------------------------------------|
| 1 | Surface id, also referenced by `[aspherical data]`. Note that this is a string value.                      |
| 2 | Radius of curvature, or `Infinity`, or one of `AS` (aperture stop), `FS` (field stop), `CG` (cover glass). |
| 3 | Thickness to the next surface. May name a variable from `[variable distances]`, e.g. `d12`.                |
| 4 | Refractive index nd.                                                                                       |
| 5 | Clear diameter. For `AS` row can be overridden by variable named `Aperture Diameter`.                      |
| 6 | Abbe number vd.                                                                                            |
| 7 | **Beam42 extension** - glass name, e.g. `S-LAH66`.                                                         |
| 8 | **Beam42 extension** - catalog name, e.g. `Ohara`.                                                         |

Columns 7 and 8 let a surface name a real catalog glass instead of relying on
the tabulated nd/vd in columns 4 and 6. The glass is then looked up in the
available catalogs (see [GLASS_CATALOGS.md](./GLASS_CATALOGS.md)), which gives the full dispersion curve.

```
1	115.495	7.84	1.85025	70.5	30.05	S-NBH57	Ohara
```

Recognised catalog names, matched case insensitively, are `Hoya`, `Ohara`,
`Schott`, `Hikari`, `CORNING`, `SUMITA` and `CDGM`. Column 8 may be omitted, in
which case those catalogs are searched in that order for the glass name.
[GLASS_CATALOGS.md](GLASS_CATALOGS.md) lists the supported glass types.

The glass catalog is **compiled into the code**, not read at run time. The AGF files
under `glassdata/` are pre-processed to generate the code, by running a utility.
To update the catalogs a new build is necessary.

Note that the named glass is used **only** if it resolves to a catalog
entry. If the glass or catalog name is not recognised, LensTool2 falls back
to the given index and Abbe number - there is no warning. Passing
`--dont-use-glass-types` forces this behaviour for every surface.

### `[patent info]`

This is only used to produce the generated report header and does not
affect any calculation. Following keys, each a `key<tab>value` line, are accepted:

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

### `[report data]`

| Key | Effect                                                                                                                                    |
| --- |-------------------------------------------------------------------------------------------------------------------------------------------|
| `lens name` | Display name used as the report heading.                                                                                                  |
| `status` | `TODO`, `Candidate` or `Accepted`. If absent defaults to `TODO`. This is purely for documentation.                                        |
| `scenarios` | **Selects which configurations are processed.** A list of zero based column indices into the multi valued rows of `[variable distances]`. |
| `names` | A label for each selected configuration, in the same order.                                                                               |

### `scenarios`

A patent can often have multiple configurations, typically for different object distances, such as object at infinity or
at close distance. Or different focal lengths for a zoom.

The Optical Bench input file implicitly defines scenarios in `[variable distances]` section.
Each available scenario is given a value. Some values may be empty or `undefined` or missing.
The three mandatory variables need valid values only for the selected scenarios. `Image Height` always uses 
its first value.

Example:

```
[variable distances]
Focal Length	24.700	50.000	82.500	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined
Angle of View	84.7	undefined	28.4	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined
F-Number	2.79	3.62	4.12	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined
Image Height	43.2	43.2	43.2	43.2	43.2	43.2	43.2	43.2	43.2	43.2	43.2
Total Length	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined	undefined
Magnification	0	0	0	-0.03333	-0.03333	-0.03333	-0.05966	-0.11558	-0.17171	-0.50000	-0.50000
d0	Infinity	Infinity	Infinity	698.89	1415.22	2312.23	372.79	351.52	333.09	33.32	43.45
d5	2.47033	16.71393	29.30687	1.96836	16.27860	28.77817	1.57789	15.23959	26.75685	10.90021	22.71918
d15	12.21507	4.35255	0.86585	12.71704	4.78788	1.39455	13.10751	5.82689	3.41587	10.16627	7.45354
d21	5.32672	1.81712	0.79966	5.32672	1.81712	0.79966	5.32672	1.81712	0.79966	1.81712	0.79966
Bf	37.8484	56.38741	66.6403	37.98862	56.38741	66.73943	37.98862	56.38741	66.73943	56.38741	66.73943
```

LensTool2 does not currently support scenarios where `Magnification` is non-zero.
Additionally, the variables `Focal Length`, `Angle of View` and `F-Number` are mandatory for LensTool2.
Scenarios are identified by 0-based index - so above, the `Angle of View` for scenario `1` corresponding to
focal length `50.0` is `undefined`. This means LensTool2 cannot process this.

In order for LensTool2 to know which of the scenarios it should process, a separate `scenarios` entry can optionally be 
provided in the `[report data]` section. This should contain the 0-based index of each scenario present in 
`[variable distances]`. For example:

```
[report data]
scenarios	0	2
names	24mm f2.8	85mm f4.0
```

Above, `0` and `2` identify the value columns in `[variable distances]` that correspond to focal
lengths `24.7` and `82.5` respectively. 

The `names` row provides the ability to set a name for each scenario - this is typically based on the
manufacturer specified lens parameters, not the actual focal lengths in the spec file.

`LensTool2` produces a complete set of outputs per configuration in `[report data]`. The
generated file names are suffixed with the configuration's position (ordinal) number, independent
of the scenario number. The first set is suffixed with `-0`, the next with `-1` and so on, 
and the README links to them (some outputs are optional and are not linked to the README). 

Important:

* If defined, `scenarios` and `names` must **both** be present and hold the **same number of
  values**. If either is missing, or the counts differ, the configurations are
  silently ignored and the lens is treated as having a single configuration.
* With no `scenarios` row in `[report data]`, the tool processes scenario `0` in 
  `[variable distances]` only and output files carry no numeric suffix. That is usually the right thing for a prime,
  because the first scenario is normally the infinity object scenario.


### `status`

The status marker is mainly for documentation:

| Value | Meaning                                                                                         |
| --- |-------------------------------------------------------------------------------------------------|
| `TODO` | Work in progress, not to be relied upon. This is the default status. |
| `Candidate` | Complete enough to look at, but not signed off.                                                 |
| `Accepted` | Signed off. This is the version a report or a test should be based on.                          |

The value is case insensitive and stored in its canonical spelling, so `accepted`
and `Accepted` are the same. If absent it defaults to `TODO`.

### Traceability

A generated `README.md` records the prescription it is based on as well as the prescription's status:

```
Generated from `Otus55.txt`, status **Accepted**
```

## Options

Either `--specfile` or `--patent` with `--example` and `--outdir` is required. 
Everything else has a default.

| Option | Default | Effect                                                                                                                                                                                                                                                                                                                                                         |
| --- | --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `--specfile <file>` | *one of these two* | The lens specification to analyse.                                                                                                                                                                                                                                                                                                                             |
| `--patent <number> --example <n>` | *one of these two* | Download the prescription from the Optical Bench instead of reading a file. Requires `--outdir`.                                                                                                                                                                                                                                                               |
| `--outdir <dir>` | alongside the spec file | Directory for generated files, the Zemax export included. **Required** with `--patent`.                                                                                                                                                                                                                                                                        |
| `--only-d-line` | off | Build the prescription and Zemax export for the d-line alone instead of the full wavelength set.                                                                                                                                                                                                                                                               |
| `--dont-use-glass-types` | off (glass types used) | Ignore named glass types in the spec and use the supplied index/dispersion instead. Useful when a catalogue glass is unavailable or suspect.                                                                                                                                                                                                                   |
| `--vig-type <type>` | `set-pupil` | Aperture and vignetting calculation run once the model is built, for the models the **analysis** outputs are computed from. See below. Accepts either the enum spelling (`SetPupil`) or kebab case (`set-pupil`), case insensitive.                                                                                                                            |
| `--use-spot-pattern <pattern>` | `hex` | Pupil sampling pattern for the spot report and MTF. The spot diagrams themselves are always drawn with `hex`. One of `hex` (hexapolar), `grid`, or `gaussian` (also accepted as `gq`). See below.                                                                                                                                                              |
| `--spot-grid-size <n>` | 64 | Samples per dimension for the rectangular grid. Only consulted when `--use-spot-pattern grid` is in effect; ignored for the other patterns. Minimum 2.                                                                                                                                                                                                         |
| `--auto-size-spot-diagrams` | off | Scale each spot diagram to its own spot size. By default all spot diagrams share a fixed 600 unit radius so that fields stay visually comparable.                                                                                                                                                                                                              |
| `--mtf <f1,f2,...>` | `10,30,50` | Spatial frequencies in cycles/mm for the MTF by field plots. Every report under `Examples/` uses the default, so change it only when comparing against a manufacturer's own choice of frequencies.                                                                                                                                                             |
| `--output-wavelength-mtfs` | off | Additionally emit a per wavelength monochromatic MTF plot for each field. Not linked to the README.                                                                                                                                                                                                                                                            |
| `--output-pupil-maps` | off | Additionally measure and draw which part of each field's pupil the lens passes, and which surface blocks the rest. See below.  Not linked to the README.                                                                                                                                                                                                       |
| `--pupil-map-samples <n>` | 121 | Samples per axis in a pupil map. Only consulted with `--output-pupil-maps`. Minimum 2.                                                                                                                                                                                                                                                                         |
| `--output-ray-aberration-plots` | off | Additionally emit transverse ray aberration **and** wavefront (OPD) fan plots, tangential and sagittal, for each field.  Not linked to the README.                                                                                                                                                                                                             |
| `--verbose` | off | Log the optimizer's progress to stderr, one line per iteration: merit, evaluations so far, elapsed time. Warnings are always shown.                                                                                                                                                                                                                            |
| `--debug` | off | Log everything, including the ray-optics info messages (each field's vignetting and real entrance pupil) and debug traces (the vignetting, pupil and wide angle searches). These fire on every chief ray aim, so during an optimization they run to thousands of lines per iteration. For a subset, pass a standard `-Djava.util.logging.config.file` instead. |
| `--assign-glass-types` | off | Match each surface's refractive index and Abbe number to a catalog glass before analysing, so the model uses the full dispersion curve instead of a two number approximation. Applies to this run only.                                                                                                                                                        |
| `--index-line <d\|e>` | `d` | Specifies how the refractive index is quoted in the input file, for `--assign-glass-types`. See below.                                                                                                                                                                                                                                                         |
| `--abbe-line <d\|e>` | `d` | Specifies how the Abbe number is quoted in the input file. Independent of `--index-line`.                                                                                                                                                                                                                                                                      |
| `--force` | off | With `--assign-glass-types`, re-match surfaces that already name a recognised glass.                                                                                                                                                                                                                                                                           |
| `--update-specfile` | off | With `--assign-glass-types`, also write the matched prescription back over the input file. Rejected on its own.                                                                                                                                                                                                                                                |
| `--optimize` | off | Run the routine airspace optimization before reporting: the back focus on a prime, or airspaces whose thickness differs across the selected configurations on a zoom. See below. |
| `--optimize <n>` | off | Run the spec file's `[trial n]` or `[pipeline n]` section instead, and report on its result. See [OPTIMIZER.md](OPTIMIZER.md).                                                                                                                                                                                                                                 |
| `--optimize-goal <contrast\|mtf>` | `contrast` | Objective for `--optimize`.                                                                                                                                                                                                                                                                                                                                    |
| `--real-ray-aiming` / `--paraxial-ray-aiming` | real | Chief ray aiming algorithm. Real aiming traces an actual ray at the entrance pupil; paraxial aiming is faster but does not hold up on very wide angle lenses. Applies to the analysis model, not the layout diagrams.                                                                                                                                          |

Invalid values for `--mtf`, `--vig-type`, `--use-spot-pattern`,
`--spot-grid-size` and `--pupil-map-samples` are rejected with an error rather than silently falling back
to the default, since each of them changes the numbers that come out.

### Vignetting types

These differ in the way the calculation runs. `set-pupil` derives the pupil
from the defined stop diameter, so it changes the f/# and leaves the apertures alone.
`set-stop-aperture` and `set-fnum` go the other way: they hold the f/# and resize
the stop diameter to satisfy it.

`--vig-type` affects the spot diagrams, the MTF, the ray aberration fans and the
vignetting and paraxial dumps. 

The layout diagrams always use `set-pupil`, since a  layout draws each bundle's rim
rays and needs to know where the bundle ends, and
`--optimize` does too, because a merit function wants a ray set that keeps its
sensitivity to the variables rather than one that measures the lens exactly. 

A `[trial n]` section states its own with a `vignetting` line; see
[OPTIMIZER.md](OPTIMIZER.md).

| Value | What it does                                                                                    | When to use it |
| --- |-------------------------------------------------------------------------------------------------| --- |
| `none` | No aperture or vignetting calculation at all.                                                   | Trace the prescription exactly as authored. |
| `paraxial` | Applies paraxial vignetting factors.                                                            | Cheap approximation. |
| `set-vig` | Computes vignetting factors from the apertures already in the file.                             | The apertures are trusted and you want the vignetting that follows from them. |
| `set-pupil` *(default)* | Derives the pupil spec from the authored stop diameter. Apertures unchanged, f/# may shift.     | The stop diameter in the prescription is the reliable number. |
| `set-stop-aperture` | Sizes the stop diameter to satisfy the pupil spec, then recomputes vignetting.                  | The quoted f/# is trusted and the stop diameter is not. |
| `set-apertures` | Computes vignetting, then sizes every clear aperture to just pass the vignetted rays.           | Apertures were estimated and you want them rebuilt from the rays. |
| `set-fnum` | Sizes the stop from the quoted f/#, then sizes every other aperture to pass the resulting rays. | A prescription quoting an exact f/# whose apertures were scaled off a patent drawing. |

### Spot patterns

The pattern decides how the pupil is sampled for the spot report (the spot radii
in `spot-report.txt` and the README) and for the geometric MTF, so it changes
every spot and MTF number. It does not affect the spot diagram SVGs: those are
always drawn from `hex` sampling at 21 rings, so they look the same whichever
pattern is chosen.

| Value | Sampling | Density |
| --- | --- | --- |
| `hex` *(default)* | Concentric rings with the ray count per ring growing with radius, giving roughly uniform area coverage. | 64 rings |
| `grid` | A square lattice across the pupil, clipped to the aperture. Density set by `--spot-grid-size`. | 64 x 64 |
| `gaussian` | Gaussian quadrature nodes: fewer rays for the same accuracy, because the nodes and weights are chosen to integrate the pupil exactly. | 14 rings, 20 spokes |

`gaussian` is an efficient choice, particularly for the optimizer; see
[GAUSSIAN_QUADRATURE.md](GAUSSIAN_QUADRATURE.md) for the implementation details. `hex` is the default and used by the committed
reports under `Examples/`.

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
full coefficient array. For `EVEN` surfaces, the array contains only even
powers, starting with a zero for A2; `EVEN A2` supplies A2 directly. For
`RADIAL` surfaces, the array contains every power starting at A1, with zeros
for A1, A2 and any odd powers not included by `AsphericalOddCount`. Optical
Bench input and the generated `prescription.txt` omit these zero placeholders,
as shown below.

`An` is the coefficient multiplying radius to power n. README labels `P1`,
`P2`, ... identify array positions: for even surfaces they correspond to A2,
A4, A6, ...; for radial surfaces they correspond to A1, A2, A3, ... . The
polynomial coefficients follow the radius and conic constant in an Optical
Bench aspheric row:

| Type | Optical Bench / `prescription.txt` coefficient sequence | Full coefficients / README `P1`, `P2`, ... |
| --- | --- |--------------------------------------------|
| `EVEN` | `A4, A6, ...` | `0, A4, A6, ...` (the first slot is A2) |
| `EVEN A2` | `A2, A4, A6, ...` | `A2, A4, A6, ...` |
| `RADIAL`, count 1 | `A3, A4, A6, A8, ...` | `0, 0, A3, A4, 0, A6, 0, A8, ...`          |
| `RADIAL`, count 2 | `A3, A4, A5, A6, A8, ...` | `0, 0, A3, A4, A5, A6, 0, A8, ...`         |

These zero placeholders are restored on import and omitted again when writing
`prescription.txt`; they do not represent a change to the surface. Do not copy
the full README or Zemax sequence into `[aspherical data]` without removing the
unused positions for the selected type and odd count. The README labels both even
variants as `EVEN`, and also pads shorter rows with trailing zeros to align the
table columns.

Zemax export uses `XOSPHERE` for `RADIAL` surfaces. `XDAT 1` holds the
number of radial terms, `XDAT 2` is the normalization radius (1), and
`XDAT 3` onward contain A1, A2, A3, ... including every zero slot. `CONI`
is the conic constant, unchanged. Even surfaces use `EVENASPH` and
their `PARM` coefficient records. Zemax's term count is not Optical Bench's
odd count. For a coefficient sequence ending at A20, `AsphericalOddCount 9`
includes the nine odd powers A3 through A19. Zemax stores 20 radial slots,
including zeros for A1 and A2.

### Routine optimization

`--optimize` runs the airspace optimization that an imported patent prescription
usually needs, before the report is generated, so every output reflects the
optimized design. What it varies depends on the lens:

| Lens | Varied |
| --- | --- |
| Prime | The back focus airspace, located automatically. |
| Zoom | The airspaces whose thickness changes between the configurations being reported, one configuration at a time. |

On a **prime** ending in a cover glass the back focus is taken as the airspace
**in front of** the cover glass, not the short gap between cover glass and
image. `CG` rows in `[lens data]` are what makes this exact rather than a guess.

A zoom is optimized one configuration at a time. Shared optimization variables
are not yet supported, so airspaces whose thickness is identical across the
selected configurations are held fixed. Selection does not depend on the
variable's name or whether it represents back focus: a varying back-focus
airspace is eligible.

Example:

```
[variable distances]
d8	19.08	10.32	6.94	2.65	20.03	11.12	7.69	3.46	22.29	13.66	10.37	6.38
d11	10.76	8.35	7.26	5.85	9.77	7.55	6.52	5.04	7.50	5.02	3.83	2.13
d28	20.38	25.195	28.045	33.19	22.54	27.74	30.59	35.22	22.54	27.74	30.59	35.22
Bf	0.97	0.97	0.97	0.97	0.97	0.97	0.97	0.97	0.97	0.97	0.97	0.97
```

The variable `Bf` will be skipped as its value is a constant across configurations.

Effective focal length and f-number are anchored to the prescription throughout,
so an optimized airspace cannot quietly turn the lens into a different one.

**On the objective.** Both goals target the central field at the `--mtf`
frequencies. `contrast` is the default, as it is the more effective option.
Use `--optimize-goal mtf` only if you want to check that out.

`--optimize` **with a number** triggers an optimization 
run specified in a `[trial n]` or a `[pipeline n]` section. See [OPTIMIZER.md](OPTIMIZER.md)
for details.

### `prescription.txt` round trips

`prescription.txt` contains the data and configurations used for the report;
unused input data is discarded.

When `--vig-type` resizes apertures (`set-stop-aperture`, `set-apertures` or
`set-fnum`), the generated prescription, Zemax export and README surface table
include the resulting diameters, rounded to four decimal places. Stop diameters
are saved per configuration. Other surfaces have shared apertures, so their
saved diameter is the largest required across the selected configurations.
Sizing uses the final geometry after any optimization; it does not change the
optimizer's vignetting or freezing settings.

It can be fed straight back in. For a run using the default analysis options:

```bash
java -jar rayoptics/target/lenstool.jar --specfile prescription.txt --outdir rerun
```

The generated `[notes]` section records the source filename in a
`source prescription<tab>filename` row, matching the README's `Generated from`
line, without its local filesystem path. Command line arguments are not saved.
These notes are informational: LensTool2 does not apply them when reading the
prescription. To reproduce a non-default run, supply
the same `--mtf`, `--only-d-line`, `--vig-type`, ray aiming,
spot sampling, diagram sizing and optional plot flags again. For example:

```bash
java -jar rayoptics/target/lenstool.jar --specfile prescription.txt --outdir rerun --mtf 10,40 --use-spot-pattern gaussian --only-d-line
```

Optimized airspaces and assigned glasses are already saved in the prescription;
do not repeat `--optimize` or glass assignment to reproduce that geometry.
With matching analysis settings, the regenerated outputs should reproduce the
same numerical results when apertures have not been resized. After resizing,
rounding and shared aperture sizes can affect a subsequent analysis or sizing
pass. Use `--vig-type set-vig` to measure vignetting from the saved apertures
without resizing them again. Two output details can still vary:

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

The `mtf-w` weights are taken from T. Steinich and V. Blahnik, "Optical design of
camera optics for mobile phones," Adv. Opt. Technol. **1**(1–2), 51–58 (2012),
[doi:10.1515/aot-2012-0002](https://doi.org/10.1515/aot-2012-0002), normalized
to the d line. Wavelengths are as quoted in the paper.

| Line | Wavelength | Weight | Relative |
| --- | --- | --- | --- |
| C | 656.28 nm | 151 | 0.475 |
| d | 587.56 nm | 318 | 1.0 |
| e | 546.07 nm | 312 | 0.98 |
| F | 486.13 nm | 157 | 0.49 |
| g | 435.84 nm | 49 | 0.15 |

A polychromatic MTF depends on the weights used. Manufacturers rarely publish
the weighting behind their MTF charts, so curves from different brands are not
directly comparable. Beam42 applies the same weights to every lens so that its
own results can be compared.

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

Resolve glass-matching problems before optimizing. Most patents specify using
nd/vd but Leica always uses ne/ve. Some patents appear to erroneously
prescribe ne/vd.

If you are working with Leica patents then always specify `--index-line e
--abbe-line e`. See also [When the index is not quoted at the d line](#when-the-index-is-not-quoted-at-the-d-line).

Where several catalog glasses fit within tolerance and none is an exact match,
the surface is left without a glass and `candidate=...` fields are appended to
the row, listing each option with its offsets, for you to choose from by hand.
A surface that *was* assigned may also carry `candidate=` fields, listing the
alternatives that were within tolerance; those are information, not a failure.

Expect equivalent glasses from a different maker: matching is on the refractive
index and vd, and the catalogs are searched in priority order, so an Ohara
S-FPL51 may come back as the equivalent Hoya FCD1. That is the same glass
optically, not a mismatch.

#### When the index is not quoted at the d line

Most prescriptions quote the refractive index as nd and the Abbe number as vd,
and that is what matching assumes. Not all do, and the two columns vary
independently, so they are selected independently:

| Option | Meaning |
| --- | --- |
| `--index-line d` *(default)* | The index column is nd. |
| `--index-line e` | The index column is ne, at 546.07 nm. |
| `--abbe-line d` *(default)* | The Abbe column is vd. |
| `--abbe-line e` | The Abbe column is ve. |

Two combinations turn up in practice, and they have different causes:

* **ne with ve.** A house convention - Leica patents quote
  the pair throughout. Use `--index-line e --abbe-line e`.
* **ne with vd.** Usually a transcription error in a patent rather than
  anything systematic. Try `--index-line e` alone.

Examples:

| Lens | nd/vd | ne/vd | ne/ve |
| --- | --- | --- | --- |
| Leica R Elmarit 28mm f2.8 | 0 assigned | 0 assigned | **8 of 8** |
| Leica R APO 280mm f2.8 | 0 assigned | - | **8 of 8** |
| Sony FE 14mm F1.8 GM | 0 assigned | **14 of 14** | 0 assigned |

```bash
java -jar rayoptics/target/lenstool.jar --specfile input.txt --assign-glass-types \
     --index-line e --abbe-line e
```

Whenever either column is at the e line, an assigned surface has its index and
Abbe columns **rewritten to the catalog's d line values**, so the file is left
consistently on the d line rather than half converted.

#### When no pairing helps

Be aware that sometimes patents are obfuscated and do not specify exact glass types.
For such patents, it is necessary to manually select glasses and the patent
may also need full reoptimization. You can look at the suggested `candidate=`
glass types.

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
for the variables selected and the airspaces that are held fixed.

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
