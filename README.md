# Beam42 Optical Ray Tracer

This project is an attempt to create a basic geometric optical analysis software. My interest is mostly in analyzing 
photographic lens designs.

The project combines solutions from the following open source projects:

* **BEAM FOUR** - incorporates [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour), originally written by the late [Michael Lampton](https://web.archive.org/web/20220919091447/http://www.ssl.berkeley.edu/~mlampton/).
* **RayOptics** - a Java port of Michael Hayford's [Ray-Optics](https://github.com/mjhoptics/ray-optics).

## Features and Goals

The project has two fairly independent components:

### RayOptics

* This is derived from Michael Hayford's Ray-Optics project. This version is mainly focused on photographic lenses.
* The foundational optical computations (optical system definition, ray aiming and tracing, transverse and wavefront
  aberrations, and vignetting) are ported from upstream Ray-Optics and checked numerically against the original
  implementation. The extensions listed below build on this foundation.
* Can import lens specifications in the format supported by [PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
* Can export to Zemax, BEAM FOUR, MJH Ray Optics.
* Features [a command line tool](Documentation/LENSTOOL2.md) that takes in the lens specification and generates the following outputs:
    * Spot diagrams (SVG)
    * Layout diagrams (SVG)
    * Geometric MTF by Field plots (SVG)
    * Ray aberration plots
    * OPD plots
    * Paraxial report
    * Zemax file
    * A markdown README that brings together all of the above
    * For examples see links below.
* Features [an optimizer](Documentation/OPTIMIZER.md) with the following capabilities:
    * Set variables on surface properties, widths. Aspherics are supported.
    * Set goals targeting MTF, ray aberrations or spot sizes.
    * Constrain by paraxial parameters.
    * Constrain curvatures and thicknesses to avoid altering the design drastically.
    * Weights can be set to influence the outcome.
    * The underlying solver is the MINPACK Levenberg-Marquardt solver; constraints are implemented by assigning weights.

### BeamFour

* The BeamFour implementation is undergoing refactoring to separate the UI layer from the core ray tracing and analytics 
  functionality. This is still work in progress.
* Since BeamFour input files are hard to create manually, there is a facility in the RayOptics component to generate 
  BeamFour inputs from a lens specification. This facility is limited to photographic lenses.
* The MTF feature in BeamFour has been updated to match the RayOptics functionality for computing single wavelength 
  MTFs for a specific field.

## Computations and verification

The tables below describe the RayOptics component. **Upstream comparison** means that Java results are checked against
values captured by running [Michael Hayford's Python Ray-Optics](https://github.com/mjhoptics/ray-optics) on equivalent models. The generated regression suite covers five lenses,
including a wide-angle lens and the supported aspheric coefficient conventions. It compares first-order data, paraxial
rays, chief-ray aiming, vignetting, full ray segments, aberration fans and Seidel coefficients. These are numerical
comparisons within tolerances, not a claim of identical results for every possible optical system. Coverage is
limited to these lenses: reflective systems, decentered surfaces and every surface profile or model operation are
not separately compared. The
[upstream regression tests](rayoptics/src/test/java/org/redukti/rayoptics/upstream) contain the captured values;
[the comparison scripts](rayoptics/src/main/python/README.md) describe how they are generated and the tolerances used.

### Ray-Optics foundation

| Computation | Origin and implementation | Verification | Reference |
| --- | --- | --- | --- |
| Sequential optical model: surfaces, gaps, apertures, fields, wavelengths and coordinate transforms | Ported model and specification classes | Model-construction comparisons with upstream's optical-bench importer; generated ray tests exercise the resulting geometry | [Ray-Optics](https://github.com/mjhoptics/ray-optics) |
| Surface sag, normals and intersections: spherical, conic, even-polynomial and radial-polynomial aspheres | Ported surface profiles | Upstream comparison of ray intersection points and normals across the five lens models | [Spencer and Murty (1962)](https://doi.org/10.1364/JOSA.52.000672) |
| Sequential real-ray tracing: local coordinates, refraction, reflection and optical path | Ported `RayTrace` kernel | Upstream comparison of per-surface points, directions, distances, normals and optical-path increments for sampled lens rays | [Spencer and Murty (1962)](https://doi.org/10.1364/JOSA.52.000672) |
| Paraxial tracing and first-order properties: focal lengths, principal planes, pupils, numerical apertures, magnification and optical invariant | Ported `FirstOrder`, `ParaxModel` and supporting calculations | Upstream comparison of axial and principal paraxial rays and all `FirstOrderData` fields | Ray-Optics |
| Third-order aberrations: Seidel sums and aspheric contributions | Ported `ThirdOrder` calculations | Upstream comparison of per-surface coefficients and totals | Ray-Optics |
| Chief-ray and pupil aiming, including wide-angle entrance-pupil location | Ported `Trace` and `Wideangle` calculations, using Java numerical solvers | Upstream comparison of aim coordinates or entrance-pupil position, including the wide-angle case | Ray-Optics |
| Vignetting factors and pupil-coordinate mapping | Ported `VigCalc` and field calculations | Upstream comparison of the four vignetting factors per field; caller-array mutation differs as noted below | Ray-Optics |
| Transverse ray aberrations and defocus | Ported ray/image-plane geometry, exposed through project analysis classes | Upstream comparison of x/y aberration fans by field and wavelength | Ray-Optics |
| Wavefront aberration and optical path difference (OPD), including refocusing and finite/infinite reference-sphere calculations | Ported `WaveAbr`; project analysis classes provide fan evaluation | Upstream comparison of OPD fans and selected wavefront-grid values; additional geometric tests for the finite reference sphere | [Hopkins (1981)](https://doi.org/10.1080/713820605); [Mikš (2002)](https://doi.org/10.1364/JOSAA.19.001187) |
| Wavefront grids, FFT point-spread function (PSF) and PSF sampling/scaling | Ported `RayGrid` and `Analyses`, with a project correction to pupil masking | `AnalysesTest` compares selected upstream grid OPDs, refocusing, PSF values and sampling; zero-OPD and piston-invariance tests cover the correction | Ray-Optics |
| Grid-sampled spot intercepts | Ported `SpotAnalysis.spot()` and `eval_grid()` | Project spot-analysis tests; the generated upstream suite compares ray fans rather than a separate spot-grid result | Ray-Optics |

The port uses Java solvers in place of SciPy, so solver-dependent results can differ within convergence tolerances.
Two intentional differences are relevant here: PSF calculation retains valid zero-OPD pupil samples, and applying
vignetting does not modify the caller's pupil-coordinate array. Glass dispersion also needs separate treatment:
the `nd`/`vd` model uses a GNU Optical formula rather than upstream opticalglass's Buchdahl model, so the generated
comparisons use the d-line. The table does not claim upstream equivalence for dispersion away from that wavelength.

### Project extensions and other numerical sources

"Project" identifies an implementation in Beam42, including implementations of published methods. These calculations
use the foundation above; their tests are not upstream Ray-Optics comparisons unless stated explicitly.

| Computation or output | Origin and implementation | Verification in this repository | Reference |
| --- | --- | --- | --- |
| Hexapolar pupil sampling | Based on GNU Optical, integrated into tracing and spot analysis | Exercised by spot-analysis and MTF sampling-pattern tests | [GNU Optical](https://www.gnu.org/software/goptical/) |
| Gaussian-quadrature pupil sampling, including annular pupils | Project additions to tracing and spot analysis | `GaussianQuadraturePatternTest` checks sampling geometry, weights and polynomial moments | [Forbes (1988, with erratum)](https://doi.org/10.1364/JOSAA.5.001943); [Bauman and Xiao (2010)](https://doi.org/10.1117/12.872773) |
| Gauss-Legendre nodes and weights | Direct implementation plus a port of the Legendre path in Netlib `gaussq.f`, using the Golub-Welsch method | `GaussQTest` checks published nodes/weights, agreement between implementations and polynomial moments | [Golub and Welsch (1969)](https://doi.org/10.1090/S0025-5718-69-99647-1) |
| Weighted spot centroid, RMS radius, maximum radius and polychromatic spot statistics | Project additions around the ported intercept calculation | `WeightedSpotAnalysisTest` checks weighted statistics, units and sampling selection | Forbes; Bauman and Xiao |
| Geometric PSF, line-spread functions and monochromatic/polychromatic geometric MTF | Project histogram and Fourier-transform calculations | `MtfTest` compares pupil-sampling patterns; report and wavelength tests cover integration with the analysis tools | [Steinich and Blahnik (2012)](https://doi.org/10.1515/aot-2012-0002) for wavelength weights |
| Monochromatic diffraction OTF/MTF and polychromatic combination with lateral color | Project implementation built on the ported wavefront/PSF routines; follows Hayford's diffraction-MTF notebook | `DiffractionMTFTest` checks the circular-pupil diffraction limit, pupil-overlap integral, Fourier consistency and phase shifts; `PolyDiffractionMTFTest` checks wavelength combination | [Hayford's notebooks](https://github.com/mjhoptics/ray-optics-notebooks); Steinich and Blahnik for wavelength weights |
| Contrast analysis and optimization residuals from sheared-pupil wavefront differences | Project implementation, including weighted centering and wavelength balance | `ContrastAnalysisTest`, `ContrastResidualCenteringTest` and optimizer goal tests | [Elliott et al. (2017)](https://doi.org/10.1117/12.2292761) |
| Exit-pupil aiming for contrast analysis and frequency calibration | Project inverse pupil mapping and shear calculations | Contrast tests check the requested shear vectors and frequency conversion | Hopkins (1981); Elliott et al. |
| Measured transmitted-pupil maps and contrast pupil maps | Project ray-bundle analyses and plots | `PupilMapAnalysisTest` and `ContrastPupilMapTest` | Uses the tracing and contrast methods above |
| Optimization variables, merit functions, constraints and configuration pipeline | Project optimizer using the MINPACK Levenberg-Marquardt solver | Optimizer unit and integration tests, including goal construction, constraints and trial runs | Elliott et al. for contrast goals; [Moré et al. (1980)](https://doi.org/10.2172/6997568) for MINPACK |
| Nonlinear systems and least squares (MINPACK) | Ported from the Fortran version of MINPACK; used for ray aiming and Levenberg-Marquardt optimization | MINPACK solver tests, two-dimensional aiming tests and optimizer tests | [Moré et al. (1980)](https://doi.org/10.2172/6997568); [MINPACK license](LICENSE-Minpack.txt) |
| Scalar root finding (secant and Brent methods) | Java secant solver ported from SciPy's secant method in `scipy/optimize/_zeros_py.py`; Java Brent solver, used where upstream calls SciPy's `brentq`, is derived from BeamFour's ray-surface intersection code, which follows *Numerical Recipes* | Upstream aiming and vignetting comparisons exercise the solvers | [SciPy secant implementation](https://github.com/scipy/scipy/blob/main/scipy/optimize/_zeros_py.py); [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour); W. H. Press et al., *Numerical Recipes in C*, 2nd ed. (1992) |
| FFT calculations | Ported jfftpack routines with project FFT utilities | `FFTUtilTest`, plus the upstream PSF and analytical diffraction-MTF checks | [jfftpack license and attribution](LICENSE-jfftpack.txt) |
| Glass-catalog lookup and wavelength-dependent refractive indices | Project catalog integration and AGF formulas; `nd`/`vd` dispersion comes from GNU Optical | Glass-catalog and importer tests; no general upstream dispersion-equivalence claim | [Glass catalogs](Documentation/GLASS_CATALOGS.md) |
| Lens layout construction | Project implementation | `Layout2DTest` and `Layout2DIntegrationTest` | [LensTool2](Documentation/LENSTOOL2.md) |
| Plotting and SVG rendering | Derived from GNU Optical; used by the project's layout and analysis outputs | Plotting and SVG renderer tests, plus layout integration tests | [GNU Optical](https://www.gnu.org/software/goptical/); [license notices](#license) |
| Analysis reports, importers and exporters | Project implementation | Report, importer and exporter tests; model-construction comparisons also check imported geometry | [LensTool2](Documentation/LENSTOOL2.md) |

## Roadmap

* Perhaps the biggest challenge is to make the optimizer useful in real world scenarios and be able to compete with the likes of Zemax.
* I would like the project to evolve so that some of the UI capabilities in BeamFour can be combined with the optimization and analytics capabilities in RayOptics. The main issue
  is that BeamFour is more general, whereas RayOptics understands photographic lenses. Still, the BeamFour UI capabilities such as viewing a lens in simulated 3-D is very helpful.
* There are many more analytic capabilities that could be added to RayOptics.

## Examples

* [Reverse Engineered Leica Noctilux M 50mm f1.0](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-m-noctilux-50mm-f1.0/README.md)
* [Reverse Engineered Noct Nikkor 58mm f1.2](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-f1.2/README.md)
* [Nikkor Z 58mm f0.95S Noct from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-z-f0.95/README.md)
* [Leica R Summicron 50mm f2 from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-r-summicron-50mm-f2/README.md)

These are a small selection. The [Examples folder](https://github.com/BeamFour/Beam42/tree/main/Examples/jfotoptix)
holds generated reports for many more lenses.


## Beam42 Resources

* [LensTool2 command line tool](Documentation/LENSTOOL2.md) - options, input format and generated outputs.
* [LensTool2 Optimizer](Documentation/OPTIMIZER.md) - describes how to set up optimization runs in the prescription file.
* [Glass catalogs](Documentation/GLASS_CATALOGS.md) - glass catalogs used by Beam42.
* [rayoptics-cpp](https://github.com/BeamFour/rayoptics-cpp) - a faithful C++ port of the Java RayOptics module, maintained using LLMs.
* [r/LensPatents](https://www.reddit.com/r/LensPatents/) - a related Reddit forum on lens patents and designs.

## BeamFour resources

* [Introduction to BeamFour (YouTube)](https://youtu.be/-buXsCqEnq8) 
  
### Literature

Citations follow the style used by Optica (formerly OSA) journals: author(s), "title," journal volume, pages (year), DOI.

* D. P. Feder, "Optical Calculations with Automatic Computing Machinery," J. Opt. Soc. Am. 41, 630–635 (1951),
  [doi:10.1364/JOSA.41.000630](https://doi.org/10.1364/JOSA.41.000630).
  This short paper provides equations for ray tracing for rotationally symmetric surfaces, including aspherics.
  Equations are provided in a format suitable for computer programs. Additionally, this paper also covers calculation
  of image aberrations. Warren J. Smith, *Modern Optical Engineering*, describes the algorithms in this paper.

* G. H. Spencer and M. V. R. K. Murty, "General Ray-Tracing Procedure," J. Opt. Soc. Am. 52, 672–678 (1962),
  [doi:10.1364/JOSA.52.000672](https://doi.org/10.1364/JOSA.52.000672).
  This paper presents generalized ray tracing equations that cover not only rotationally symmetric surfaces
  (including aspherics) but also diffraction gratings. The paper allows for surfaces to have their own local axes.
  The ray tracing in rayoptics module is based on this paper.

* R. E. Hopkins and R. Hanau, "Fundamental Methods of Ray Tracing," Chap. 5 in *Military Standardization Handbook:
  Optical Design*, MIL-HDBK-141 (U.S. Defense Supply Agency, Washington, DC, 1962).
  Available from [EverySpec](https://everyspec.com/MIL-HDBK/MIL-HDBK-0099-0199/MIL-HDBK-141_24399/).
  It covers ray tracing equations for rotationally symmetric surfaces including aspheric surfaces. The equations are
  presented in a form suited for implementation in computer programs. The final equations in this document are very
  similar to Feder's equations. This document goes into details of how these equations are derived.
  Daniel Malacara, *Handbook of Optical Design*, describes the ray tracing equations found in this document.

* H. H. Hopkins, "Calculation of the Aberrations and Image Assessment for a General Optical System,"
  Optica Acta 28, 667–714 (1981), [doi:10.1080/713820605](https://doi.org/10.1080/713820605).
  RayOptics calculation of optical path difference is based on this paper.

* G. W. Forbes, "Optical system assessment for design: numerical ray tracing in the Gaussian pupil,"
  J. Opt. Soc. Am. A 5, 1943–1956 (1988), [doi:10.1364/JOSAA.5.001943](https://doi.org/10.1364/JOSAA.5.001943);
  erratum, J. Opt. Soc. Am. A 6, 1123 (1989), [doi:10.1364/JOSAA.6.001123](https://doi.org/10.1364/JOSAA.6.001123).
  Original paper on the Gaussian quadrature pupil sampling pattern. Very effective!

* A. Mikš, "Dependence of the wave-front aberration on the radius of the reference sphere,"
  J. Opt. Soc. Am. A 19, 1187–1190 (2002),
  [doi:10.1364/JOSAA.19.001187](https://doi.org/10.1364/JOSAA.19.001187).
  Reference for the infinite-reference-sphere wavefront calculation in the Ray-Optics port.

* G. H. Golub and J. H. Welsch, "Calculation of Gauss Quadrature Rules," Math. Comp. 23, 221–230 (1969),
  [doi:10.1090/S0025-5718-69-99647-1](https://doi.org/10.1090/S0025-5718-69-99647-1).
  Nodes and weights from the eigenvalues and eigenvectors of a tridiagonal Jacobi matrix.

* J. J. Moré, B. S. Garbow, and K. E. Hillstrom, *User Guide for MINPACK-1*, Argonne National Laboratory
  report ANL-80-74 (1980),
  [doi:10.2172/6997568](https://doi.org/10.2172/6997568).
  Nonlinear equation and least-squares solvers used for ray aiming and optimization.

* B. J. Bauman and H. Xiao, "Gaussian quadrature for optical design with noncircular pupils and fields, and broad
  wavelength range," in *International Optical Design Conference 2010*, Proc. SPIE 7652, 76522S (2010),
  [doi:10.1117/12.872773](https://doi.org/10.1117/12.872773).
  Open-access version: LLNL-CONF-442492,
  [UNT Digital Library](https://digital.library.unt.edu/ark:/67531/metadc865679/).

* T. Steinich and V. Blahnik, "Optical design of camera optics for mobile phones," Adv. Opt. Technol. 1, 51–58 (2012),
  [doi:10.1515/aot-2012-0002](https://doi.org/10.1515/aot-2012-0002).
  Source of the wavelength weights used for the weighted polychromatic MTF.

* E. Elliott, K. Moore, C. Normanshire, S. Gay, J. Aiona, and M. G. Nicholson, "Contrast optimization: a faster and
  better technique for optimizing on MTF," in *International Optical Design Conference 2017*, Proc. SPIE 10590,
  1059014 (2017), [doi:10.1117/12.2292761](https://doi.org/10.1117/12.2292761).
  Very effective optimization approach that directly targets MTF.

## Related Projects

* My fork of GNU Optical: https://github.com/dibyendumajumdar/goptical
* RayOptics (python) by Michael Hayford, who worked many years at an optical software company: https://github.com/mjhoptics/ray-optics.
* rayoptics4j - a partial port of above. See https://github.com/BeamFour/rayoptics4j
* Optiland: (python): https://github.com/HarrisonKramer/optiland
* Rayopt (python): https://github.com/quartiq/rayopt
* An older C project 'ray' originally written by Don Wells at NRAO implements Feder's equations for ray tracing. https://github.com/dibyendumajumdar/ray
* Geopter (C++,Qt): https://github.com/heterophyllus/Geopter

Here are some other projects that I have not personally tried out

* Astree (C++): https://github.com/edeforas/Astree
* Opticspy (python): https://github.com/Sterncat/opticspy
* Pyrate (python) https://github.com/mess42/pyrate
* A Matlab/Octave project: https://github.com/heterophyllus/OpticalDesign-Toolbox
* Kraken - Optical Simulator (python): https://github.com/Garchupiter/Kraken-Optical-Simulator
* OpticSim (Julia) by Microsoft: https://github.com/microsoft/OpticSim.jl
* An attempt to maintain a commercial product KDP is here: https://github.com/dinosauria123/Koko. Lots of spaghetti Fortran code, unfortunately. 

## License

The project includes code derived from several open-source projects. See the individual license notices in the source
code and the LICENSE files:

* [LICENSE-GPL-3.0.txt](LICENSE-GPL-3.0.txt) - the overall license for everything in rayoptics module, 
  and the license of the code derived from [Goptical](https://www.gnu.org/software/goptical/): mainly the `org.redukti.data` and `org.redukti.render`
  packages, plus `Quaternion`, `Transform3`, `Triangle2`, `Vector2Pair` and `Vector3Pair` in `org.redukti.mathlib`.
* [LICENSE-ray-optics.txt](LICENSE-ray-optics.txt) - BSD 3-Clause, for the code derived from Michael Hayford's
  [ray-optics](https://github.com/mjhoptics/ray-optics), by way of
  [rayoptics4j](https://github.com/BeamFour/rayoptics4j).
* [LICENSE-Minpack.txt](LICENSE-Minpack.txt) - for the code derived from MINPACK.
* [LICENSE-jfftpack.txt](LICENSE-jfftpack.txt) - for the code derived from jfftpack.
* [beam42/LICENSE.txt](beam42/LICENSE.txt) - GNU GPL v2, for the `beam42` module only; see below.

Each source file carries its own notice identifying which of the above it falls under. Files that are not derived
from any external project - original Beam42 code - are marked with a `This code is part of Beam42 project` header
and are licensed under GPL v3 or later, same as the overall project.

The overall license is GNU GPL v3 or later.

### The `beam42` module LICENSE

The `beam42` module derives from [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour), which is GPL-2.0,
and it is kept under that license.

BeamFour is a standalone product. If the GPL-2.0 licensing of this module is a concern, the `beam42` directory 
can be removed — delete it along with its `<module>` entry in the root `pom.xml`.
