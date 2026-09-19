# AGENTS.md

Guidance for AI coding agents working in this repository. Read this before making changes.
Most substantive documentation already exists — see [Where the documentation lives](#where-the-documentation-lives)
— so this file covers only the rules that are easy to violate without being told.

## What this repository is

Beam42 combines two independently-originated components:

* `beam42/` — derived from [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour) (GPL-2.0),
  currently being refactored to separate the UI from the core.
* `rayoptics/` — a Java port of Michael Hayford's [ray-optics](https://github.com/mjhoptics/ray-optics)
  (BSD-3-Clause), synced from [rayoptics4j](https://github.com/BeamFour/rayoptics4j).

See [README.md](README.md) for features, goals and the full literature list.

## 1. Stay close to upstream

The ported code tracks its upstream projects closely, because bug fixes are ported *down*
from upstream and every local deviation makes that harder.

* Under `rayoptics/`, a file carrying the `// Copyright 2017-2025 Michael J. Hayford`
  header is ported from upstream. Check for that header before editing anything.
  The same principle applies to `beam42/` code derived from BeamFour.
* **Do not refactor, restructure or "improve" ported code.** Renaming, extracting helpers,
  tidying literals, changing control flow — none of these are free, even when they look
  harmless. A deviation inside a ported file needs the author's explicit approval, and it
  should be argued for on its merits, not applied as a default.
* **Adding is fine.** New files, and new functions alongside the ported ones, are the
  normal way to extend the project — the contrast analysis is exactly this. Extensions
  such as layout diagrams, MTF and spot analysis, the optimizer and the exporters are
  Beam42's own and are not constrained by upstream.
* When a cross-cutting change would touch ported code, make the change in the Beam42-only
  files and say explicitly which ported files you left alone and why.
* Upstream behaviour is verified by running both implementations and comparing; see
  [Documentation/UPSTREAM_VERIFICATION.md](Documentation/UPSTREAM_VERIFICATION.md).

## 2. Changes here must be back-ported to the C++ port

[rayoptics-cpp](https://github.com/BeamFour/rayoptics-cpp) is a downstream C++ port of the
Java `rayoptics/` module, and this repository is its ground truth. A change to ported or
shared logic here leaves the two out of step until it is carried across.

* When you change `rayoptics/`, say so and either back-port it or flag it explicitly as
  needing back-porting. The same applies to removals and to licence-header corrections.
* The C++ port is verified by dumping expected values from the Java classes and asserting
  them exactly, so a Java change usually means regenerating C++ expectations, not editing
  them by hand.
* The C++ work happens in its own checkout; C++ builds and tests do not run from here.

## 3. Never cite the local documents directly

`Documentation/` holds papers, manuals and guides that are **not freely redistributable**
and are not committed. They are local reading material only.

* Never refer to those files — by filename or path — in committed documentation, code
  comments, or commit messages. Do not quote from them at length, and do not paraphrase
  them at a length that substitutes for the original.
* Refer to the work by its **public citation** instead: author(s), "title," journal volume,
  pages (year), DOI — the Optica style used in [README.md](README.md). Link the DOI.
  [Documentation/OPD.md](Documentation/OPD.md) and
  [Documentation/GAUSSIAN_QUADRATURE.md](Documentation/GAUSSIAN_QUADRATURE.md) show the
  convention in practice.
* Using them as a local reference while working is fine — it is the citation that must be
  public.

## 4. Copyright and licence headers

* Code **derived from** ray-optics or BeamFour keeps the copyright header of its origin.
  Never invent an attribution, and never remove or change one without the author's
  confirmation.
* Code that is **not** derived from either — the extensions, tools, exporters, importers,
  optimizer and analysis code — is copyright this project. New files belong in this
  category unless they are ports.
* If a file's provenance is unclear, ask rather than guessing: ray-optics is BSD-3-Clause
  and BeamFour is GPL-2.0, so a wrong header is a licensing error, not a cosmetic one.
* Licence texts live in the repository root (`LICENSE.txt`, `LICENSE-ray-optics.txt`,
  `LICENSE-Minpack.txt`).

## Building and testing

Maven multi-module build (`beam42`, `rayoptics`), Java 17. `mvn` may not be on `PATH`;
invoke your local Maven installation directly.

* `mvn -o test` works offline. `mvn -o install` does **not** — the install plugin's
  dependencies were never fetched.
* Running a single test needs **both** `-Dtest=Class#method` and
  `-Dsurefire.failIfNoSpecifiedTests=false`, otherwise the reactor fails on the first
  module with no matching test.
* Single-module runs (`-pl`) need `-am`, since sibling `1.0-SNAPSHOT` artifacts are not in
  the local repository.
* Stale `surefire-reports/*.txt` from earlier runs linger under `target/`; read the reactor
  totals rather than grepping blindly.

## Examples and test data

`Examples/` is a working area — the author edits prescriptions in place, so a file being
present or even tracked does not mean it is finished.

* Tests may only read prescriptions that are **committed and unmodified**. Check
  `git status --short -- <file>` is empty before pinning a test to a lens.
* Prefer lenses that existing tests already use.

## Where the documentation lives

| Document | Covers |
| --- | --- |
| [README.md](README.md) | Project overview, features, literature, licensing |
| [Documentation/LENSTOOL2.md](Documentation/LENSTOOL2.md) | Command line tool: options, input format, outputs |
| [Documentation/OPTIMIZER.md](Documentation/OPTIMIZER.md) | What an optimization run targets and how to describe one |
| [Documentation/OPTIMIZER_NOTES.md](Documentation/OPTIMIZER_NOTES.md) | Optimizer implementation notes |
| [Documentation/REVIEW.md](Documentation/REVIEW.md) | Measurements made on the contrast optimizer (parts may be dated) |
| [Documentation/OPD.md](Documentation/OPD.md) | Optical path difference and the finite reference sphere |
| [Documentation/GAUSSIAN_QUADRATURE.md](Documentation/GAUSSIAN_QUADRATURE.md) | Pupil sampling and its relation to the published rule |
| [Documentation/GLASS_CATALOGS.md](Documentation/GLASS_CATALOGS.md) | Glass catalogs known to Beam42 |
| [Documentation/UPSTREAM_VERIFICATION.md](Documentation/UPSTREAM_VERIFICATION.md) | Verifying the Java port against upstream ray-optics |

Before changing an optimizer parameter, check whether the question was already settled in
`REVIEW.md` — and add new measurements there. Treat its code descriptions as historical and
verify them against current code; `OPTIMIZER.md` and `OPTIMIZER_NOTES.md` are the current
behaviour.
