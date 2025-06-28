# Beam42 Optical Ray Tracer

This project contains three different optical design and analysis software.

* **BEAM FOUR** 
* **JFotoptix** - a Java tool derived from [GNU Optical](https://github.com/dibyendumajumdar/goptical).  
* **RayOptics** - a partial ongoing port of [MJH RayOptics](https://github.com/mjhoptics/ray-optics). 

## BEAM FOUR

This is a fork of the BEAM FOUR Optical Ray Tracer, by www.StellarSoftware.com.

Original author is [Michael Lampton](https://www.ssl.berkeley.edu/~mlampton/).

Copyright (c) M.Lampton, 2003-2020, STELLAR SOFTWARE all rights reserved.

### About BEAM FOUR

From  www.StellarSoftware.com:

There are three kinds of ray tracers out there...

1. Graphical ray tracers make photo-realistic images of computed scenes, using geometrical methods. Beautiful artwork, but that's not us.

2. Illumination ray tracers compute the sum of diffuse and discrete light sources and predict the intensity at any point in an enclosed space. These are non-sequential: you do not have to pre-specify your trace sequence. These are crucial for designing lighting systems; for light guides; for interior illumination; essential for stray light calculations in optical systems; but that's not us either.

3. Optical ray tracing runs geometrical rays through lenses, gratings, irises, mirrors, prisms, etc and evaluates the image that a specified optical system delivers. Bingo!

### Resources

* [Introduction to BeamFour (YouTube)](https://youtu.be/-buXsCqEnq8)
  
### Changes / Development Plan

* The original implementation made it difficult to use the ray tracing functions independently of the GUI. I am working on improving the de-coupling of the UI from the ray tracing functions.
* The implementation assumed that there is a single workspace that a user is working in, and therefore used static data structures - this design is not as friendly to server side use cases where multiple simultaneous workspaces can be in use.

## JFotoptix

This started off as a Java port of [GNU Optical](https://github.com/dibyendumajumdar/goptical), but now has a different set of features

### Features

* Mainly focused on Photo Lenses.
* Can import lens specifications in the format supported by [PhotonsToPhotos Optical Bench](https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm).
* Can export to Zemax, BEAM FOUR, MJH Ray Optics.
* Features a command line tool that takes in the lens specification and generates following outputs:
  * Spot diagrams (SVG)
  * Layout diagrams (SVG)
  * Spot report
  * Paraxial report
  * Zemax file
* Features a Levenberg Marquardt Lampton solver based optimizer with following features
  * Set variables on surface properties
  * Fit to spot size goals
  * Constrain by paraxial parameters
  * This is still **work in progress**.

## RayOptics

This is an ongoing partial port of [MJH RayOptics](https://github.com/mjhoptics/ray-optics).

## Literature

* Donald P. Feder, "Optical Calculations with Automatic Computing Machinery," J. Opt. Soc. Am. 41, 630-635 (1951). This short paper provides equations for ray tracing for rotationally symmetric surfaces, including aspherics. Equations are provided in a format suitable for computer programs. Additionally, this paper also covers calculation of image aberrations. Warren J. Smith: Modern Optical Engineering book has a description of the algorithms in this paper. 

* G. H. Spencer and M. V. R. K. Murty, "General Ray-Tracing Procedure," J. Opt. Soc. Am. 52, 672-678 (1962). This paper presents generalized ray tracing equations that cover not only rotationally symmetric surfaces (including aspherics) but also diffraction gratings. The paper allows for surfaces to have their own local axes. **BeamFour implementation of ray tracing is based on this paper**. 

* R. E. Hopkins and R. Hanau, "Fundamental Methods of Ray Tracing," in Military Standardization Handbook: Optical Design, MIL-HDBK 141, U.S. Defense Supply Agency, Washington, DC, 1962. This is the fifth chapter in the document. It covers ray tracing equations for rotationally symmetric surfaces including aspheric surfaces. The equations are presented in a form suited for implementation in computer programs. The final equations in this document are very similar to Feder's equations. This document goes into details of how these equations are derived. Daniel Malacara: Handbook of Optical Design has a description of the ray tracing equations found in this document. 

* Bram de Greve, "Reflections and Refractions in Ray Tracing," 2004. This paper appears to be the source for the refraction equations used by GNU Optical. 

* Telescope Optics - GNU Optical source code has references to this, it is unclear whether this is a reference to the book of this name by Rutten and Venrooij. 

## Related Projects

* My fork of GNU Optical: https://github.com/dibyendumajumdar/goptical
* RayOptics (python) by Michael Hayford who worked many years at optical software company: https://github.com/mjhoptics/ray-optics. 
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
