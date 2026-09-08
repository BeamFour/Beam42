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
* It features a number of extensions on top of the upstream project.
* Can import lens specifications in the format supported by [PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
* Can export to Zemax, BEAM FOUR, MJH Ray Optics.
* Features a command line tool that takes in the lens specification and generates the following outputs:
    * Spot diagrams (SVG)
    * Layout diagrams (SVG)
    * Geometric MTF by Field plots (SVG)
    * Ray aberration plots
    * OPD plots
    * Paraxial report
    * Zemax file
    * A markdown README that brings together all of the above
    * For examples see links below.
* Features an optimizer with the following features
    * Set variables on surface properties, widths. Aspherics are supported.
    * Set goals targeting MTF, ray aberrations or spot sizes.
    * Constrain by paraxial parameters.
    * Constrain curvatures and thicknesses to avoid altering the design drastically.
    * Constrain the difference between tangential and sagittal MTF to reduce astigmatism.
    * Weights can be set to influence the outcome.
    * The optimization functions are available as Java API calls. There is no UI for this.
    * The underlying solver is the MINPACK Levenberg-Marquardt solver; constraints are implemented by assigning weights.

### BeamFour

* The BeamFour implementation is undergoing refactoring to separate the UI layer from the core ray tracing and analytics 
  functionality. This is still work in progress.
* Since BeamFour input files are hard to create manually, there is a facility in the RayOptics component to generate 
  BeamFour inputs from a lens specification. This facility is limited to photographic lenses.
* The MTF feature in BeamFour has been updated to match the RayOptics functionality for computing single wavelength 
  MTFs for a specific field.

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


## Resources

* [LensTool2 command line tool](Documentation/LENSTOOL2.md) - options and generated outputs.
* [Introduction to BeamFour (YouTube)](https://youtu.be/-buXsCqEnq8)
* [r/LensPatents](https://www.reddit.com/r/LensPatents/) - a related Reddit forum on lens patents and designs.
  
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

* B. J. Bauman and H. Xiao, "Gaussian quadrature for optical design with noncircular pupils and fields, and broad
  wavelength range," in *International Optical Design Conference 2010*, Proc. SPIE 7652, 76522S (2010),
  [doi:10.1117/12.872773](https://doi.org/10.1117/12.872773).
  Open-access version: LLNL-CONF-442492,
  [UNT Digital Library](https://digital.library.unt.edu/ark:/67531/metadc865679/).

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

Beam42 as a whole is distributed under the [GNU General Public License, version 2](LICENSE.txt).

The two components have different upstream origins:

* The `beam42` module derives from [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour), which is GPL-2.0.
  This is what makes the combined work GPL-2.0.
* The `rayoptics` module derives from [ray-optics](https://github.com/mjhoptics/ray-optics) and
  [rayoptics4j](https://github.com/BeamFour/rayoptics4j), both of which are BSD-3-Clause.
