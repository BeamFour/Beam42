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

## Options

`--specfile` is the only required option. Everything else has a working default,
and the defaults are what the committed examples use.

| Option | Default | Effect |
| --- | --- | --- |
| `--specfile <file>` | *required* | The lens specification to analyse. |
| `--outdir <dir>` | alongside the spec file | Directory for generated output. See the note on the Zemax file below. |
| `--only-d-line` | off | Build the prescription and Zemax export for the d line alone instead of the full wavelength set. |
| `--dont-use-glass-types` | off (glass types used) | Ignore named glass types in the spec and use the tabulated index/dispersion instead. Useful when a catalogue glass is unavailable or suspect. |
| `--vig-type <type>` | `set-pupil` | Vignetting treatment applied when the analysis model is built. Accepts either the enum spelling (`SetPupil`) or kebab case (`set-pupil`), case insensitive. |
| `--use-spot-pattern <pattern>` | `hex` | Pupil sampling pattern for spot diagrams and MTF. One of `hex` (hexapolar), `grid`, or `gaussian` (also accepted as `gq`). |
| `--spot-grid-size <n>` | 64 | Samples per dimension for the rectangular grid. Only consulted when `--use-spot-pattern grid` is in effect; ignored for the other patterns. Minimum 2. |
| `--auto-size-spot-diagrams` | off | Scale each spot diagram to its own spot size. By default all spot diagrams share a fixed 600 unit radius so that fields stay visually comparable. |
| `--mtf <f1,f2,...>` | `10,30,50` | Spatial frequencies in cycles/mm for the MTF by field plots. Every report under `Examples/` uses the default, so change it only when comparing against a manufacturer's own choice of frequencies. |
| `--output-wavelength-mtfs` | off | Additionally emit a per wavelength monochromatic MTF plot for each field. |
| `--output-ray-aberration-plots` | off | Additionally emit transverse ray aberration **and** wavefront (OPD) fan plots, tangential and sagittal, for each field. |
| `--real-ray-aiming` / `--paraxial-ray-aiming` | real | Chief ray aiming algorithm. Real aiming traces an actual ray at the entrance pupil; paraxial aiming is faster but does not hold up on very wide angle lenses. Applies to the analysis model, not the layout diagrams. |

Invalid values for `--mtf`, `--vig-type`, `--use-spot-pattern` and
`--spot-grid-size` are rejected with an error rather than silently falling back
to the default, since each of them changes the numbers that come out.

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

* **Fields are fixed.** Analysis runs at eleven relative field heights, 0.0 to
  1.0 in steps of 0.1. Layout diagrams use only 0.0 and 1.0. Neither set is
  configurable from the command line.
* **Configurations are looped, not selected.** A multi configuration spec
  produces a full set of outputs for every configuration in one run.
* **`--outdir` does not move the Zemax file.** Every other output honours it,
  but the `.zmx` is always written next to the spec file.
* **`--vig-type` does not reach the layout diagrams.** They are always built
  with `SetPupil`; the option only affects the model used for spot, MTF and
  aberration analysis.
* **Ray aiming defaults to real.** Real aiming is used unless
  `--paraxial-ray-aiming` is given. Layout diagrams always use real aiming
  regardless of the flag.
