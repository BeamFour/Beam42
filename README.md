# Beam42 Optical Ray Tracer

This project is an attempt to create a basic geometric optical analysis software. My interest is mostly in analysing photographic lens designs.

The project combines solutions from following open source projects:

* **BEAM FOUR** - incorporates [BeamFour](https://github.com/StellarSoftwareBerkeley/BeamFour), originally written by Late  [Michael Lampton](https://www.ssl.berkeley.edu/~mlampton/).
* **JFotoptix** - a Java port of [GNU Optical](https://github.com/dibyendumajumdar/goptical).
* **RayOptics** - a Java port of Michael Hayford's [Ray-Optics](https://github.com/mjhoptics/ray-optics).

## Features and Goals

**Note: This is work-in-progress**

* Mainly focused on Photographic Lenses.
* Can import lens specifications in the format supported by [PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
* Can export to Zemax, BEAM FOUR, MJH Ray Optics.
* Features a command line tool that takes in the lens specification and generates following outputs:
    * Spot diagrams (SVG)
    * Layout diagrams (SVG)
    * Geometric MTF (SVG)
    * Ray aberration plots
    * OPD plots
    * Paraxial report
    * Zemax file
    * A markdown README that brings together all of above
* Features a Levenberg Marquardt Lampton solver based optimizer with following features
    * Set variables on surface properties
    * Fit to spot size or ray aberration goals or MTF targets
    * Constrain by paraxial parameters
    * The optimization functions above are available only via Java API calls. There is no UI for this. 
    * Note: There are some optimization functions in BeamFour that do have a UI; but BeamFour is a more general ray tracing and
      analysis software that doesn't do many things that are typical of photographic lenses.

## Examples

* [Reverse Engineered Leica Noctilux M 50mm f1.0](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-m-noctilux-50mm-f1.0/README.md)
* [Reverse Engineered Noct Nikkor 58mm f1.2](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-f1.2/README.md)
* [Nikkor Z 58mm f0.95S Noct from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/nikkor-58mm-z-f0.95/README.md)
* [Leica R Summicron 50mm f2 from Patent](https://github.com/BeamFour/Beam42/blob/main/Examples/jfotoptix/leica-r-summicron-50mm-f2/README.md)


## Resources

* [Introduction to BeamFour (YouTube)](https://youtu.be/-buXsCqEnq8)
  
### Literature

* Donald P. Feder, "Optical Calculations with Automatic Computing Machinery," J. Opt. Soc. Am. 41, 630-635 (1951). This short paper provides equations for ray tracing for rotationally symmetric surfaces, including aspherics. Equations are provided in a format suitable for computer programs. Additionally, this paper also covers calculation of image aberrations. Warren J. Smith: Modern Optical Engineering book has a description of the algorithms in this paper. 

* G. H. Spencer and M. V. R. K. Murty, "General Ray-Tracing Procedure," J. Opt. Soc. Am. 52, 672-678 (1962). This paper presents generalized ray tracing equations that cover not only rotationally symmetric surfaces (including aspherics) but also diffraction gratings. The paper allows for surfaces to have their own local axes. **BeamFour implementation of ray tracing is based on this paper**. 

* R. E. Hopkins and R. Hanau, "Fundamental Methods of Ray Tracing," in Military Standardization Handbook: Optical Design, MIL-HDBK 141, U.S. Defense Supply Agency, Washington, DC, 1962. This is the fifth chapter in the document. It covers ray tracing equations for rotationally symmetric surfaces including aspheric surfaces. The equations are presented in a form suited for implementation in computer programs. The final equations in this document are very similar to Feder's equations. This document goes into details of how these equations are derived. Daniel Malacara: Handbook of Optical Design has a description of the ray tracing equations found in this document. 

* Bram de Greve, "Reflections and Refractions in Ray Tracing," 2004. This paper appears to be the source for the refraction equations used by GNU Optical. 

* Telescope Optics - GNU Optical source code has references to this, it is unclear whether this is a reference to the book of this name by Rutten and Venrooij. 

## Related Projects

* My fork of GNU Optical: https://github.com/dibyendumajumdar/goptical
* RayOptics (python) by Michael Hayford who worked many years at optical software company: https://github.com/mjhoptics/ray-optics.
* rayoptics4j - a partial port of above. See https://github.com/BeamFour/rayoptics4j
* Rayopt (python): https://github.com/quartiq/rayopt
* An older C project 'ray' originally written by Don Wells at NRAO implements Feder's equations for ray tracing. https://github.com/dibyendumajumdar/ray
* Geopter (C++,Qt): https://github.com/heterophyllus/Geopter
* An attempt to maintain a commercial product KDP is here: https://github.com/dinosauria123/Koko. Lots of sphagetti Fortran code, unfortunately. 

Here are some other projects that I have not personally tried out

* OpticSim (Julia) by Microsoft: https://github.com/microsoft/OpticSim.jl
* Astree (C++): https://github.com/edeforas/Astree
* OpticsSpy (python): https://github.com/Sterncat/opticspy 
* Pyrate (python) https://github.com/mess42/pyrate
* A Matlab/Octave project: https://github.com/heterophyllus/OpticalDesign-Toolbox
* Optiland: (python): https://github.com/HarrisonKramer/optiland
* Kraken - Optical Simulator (python): https://github.com/Garchupiter/Kraken-Optical-Simulator
