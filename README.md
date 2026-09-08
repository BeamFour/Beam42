# Beam42 Optical Ray Tracer

This project is an attempt to create a basic geometric optical analysis software. My interest is mostly in analyzing 
photographic lens designs.

The project combines solutions from the following open source projects:

* **BEAM FOUR** - incorporates [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour), originally written by the Late  [Michael Lampton](https://www.ssl.berkeley.edu/~mlampton/).
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
    * The underlying solver is the MinPack Levenberg Marquardt solver, constrains are implemented by assigning weights.

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
  is that BeamFour is more general, whereas RayOptics understands photographic lenses. Sill, the BeamFour UI capabilities such as viewing a lens in simulated 3-D is very helpful.
* There are many more analytic capabilities that could be added to RayOptics.

## Examples

* [Reverse Engineered Leica Noctilux M 50mm f1.0](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-m-noctilux-50mm-f1.0/README.md)
* [Reverse Engineered Noct Nikkor 58mm f1.2](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-f1.2/README.md)
* [Nikkor Z 58mm f0.95S Noct from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-z-f0.95/README.md)
* [Leica R Summicron 50mm f2 from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-r-summicron-50mm-f2/README.md)


## Resources

* [Introduction to BeamFour (YouTube)](https://youtu.be/-buXsCqEnq8)
  
### Literature

* Donald P. Feder, "Optical Calculations with Automatic Computing Machinery," J. Opt. Soc. Am. 41, 630-635 (1951). 
  This short paper provides equations for ray tracing for rotationally symmetric surfaces, including aspherics. 
  Equations are provided in a format suitable for computer programs. Additionally, this paper also covers calculation
  of image aberrations. Warren J. Smith: Modern Optical Engineering book has a description of the algorithms in this paper. 

* G. H. Spencer and M. V. R. K. Murty, "General Ray-Tracing Procedure," J. Opt. Soc. Am. 52, 672-678 (1962).
  This paper presents generalized ray tracing equations that cover not only rotationally symmetric surfaces 
  (including aspherics) but also diffraction gratings. The paper allows for surfaces to have their own local axes.

* R. E. Hopkins and R. Hanau, "Fundamental Methods of Ray Tracing," in Military Standardization Handbook: Optical 
  Design, MIL-HDBK 141, U.S. Defense Supply Agency, Washington, DC, 1962. This is the fifth chapter in the document.
  It covers ray tracing equations for rotationally symmetric surfaces including aspheric surfaces. The equations are 
  presented in a form suited for implementation in computer programs. The final equations in this document are very 
  similar to Feder's equations. This document goes into details of how these equations are derived. 
  Daniel Malacara: Handbook of Optical Design has a description of the ray tracing equations found in this document. 

* Hopkins, H. H. (1981). Calculation of the Aberrations and Image Assessment for a General Optical System. 
  Optica Acta: International Journal of Optics, 28(5), 667–714. https://doi.org/10.1080/713820605.
  RayOptics calculation of optical path difference is based on this paper.

* G. W. Forbes, "Optical system assessment for design: numerical ray tracing in the Gaussian pupil,"
  J. Opt. Soc. Am. A 5, 1943-1956 (1988) - original paper on Gaussian Quadrature pattern. Very effective!

* Bauman, B J & Xiao, H. Gaussian Quadrature for Optical Design with Non-circular Pupils and Fields, and 
  Broad Wavelength Ranges, article, June 25, 2010; Livermore, California. 
  (https://digital.library.unt.edu/ark:/67531/metadc865679/: accessed September 8, 2026), 
  University of North Texas Libraries, UNT Digital Library, https://digital.library.unt.edu; 
  crediting UNT Libraries Government Documents Department.

* Contrast Optimization: "A faster and better technique for optimizing on MTF", 
  Ken Moore, Erin Elliott, Mark Nicholson, Chris Normanshire, Shawn Gay, Jade Aiona. Zemax, LLC.
  Very effective optimization approach that directly targets MTF.

## Related Projects

* My fork of GNU Optical: https://github.com/dibyendumajumdar/goptical
* RayOptics (python) by Michael Hayford who worked many years at optical software company: https://github.com/mjhoptics/ray-optics.
* rayoptics4j - a partial port of above. See https://github.com/BeamFour/rayoptics4j
* Optiland: (python): https://github.com/HarrisonKramer/optiland
* Rayopt (python): https://github.com/quartiq/rayopt
* An older C project 'ray' originally written by Don Wells at NRAO implements Feder's equations for ray tracing. https://github.com/dibyendumajumdar/ray
* Geopter (C++,Qt): https://github.com/heterophyllus/Geopter

Here are some other projects that I have not personally tried out

* Astree (C++): https://github.com/edeforas/Astree
* OpticsSpy (python): https://github.com/Sterncat/opticspy 
* Pyrate (python) https://github.com/mess42/pyrate
* A Matlab/Octave project: https://github.com/heterophyllus/OpticalDesign-Toolbox
* Kraken - Optical Simulator (python): https://github.com/Garchupiter/Kraken-Optical-Simulator
* OpticSim (Julia) by Microsoft: https://github.com/microsoft/OpticSim.jl
* An attempt to maintain a commercial product KDP is here: https://github.com/dinosauria123/Koko. Lots of spaghetti Fortran code, unfortunately. 
