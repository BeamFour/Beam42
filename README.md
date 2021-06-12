# Beam42 Optical Ray Tracer

This is a fork of the BEAM FOUR Optical Ray Tracer, by www.StellarSoftware.com.

Original author is [Michael Lampton](https://www.ssl.berkeley.edu/~mlampton/).

Copyright (c) M.Lampton, 2003-2020, STELLAR SOFTWARE all rights reserved.

For any inquiries regarding this version, please raise issues here and do not contact StellarSoftware.com, as this
version is not maintained by Mike Lampton.

## About Beam42

There are three kinds of ray tracers out there...

1. Graphical ray tracers make photo-realistic images of computed scenes, using geometrical methods. Beautiful artwork, but that's not us.

2. Illumination ray tracers compute the sum of diffuse and discrete light sources and predict the intensity at any point in an enclosed space. These are non-sequential: you do not have to pre-specify your trace sequence. These are crucial for designing lighting systems; for light guides; for interior illumination; essential for stray light calculations in optical systems; but that's not us either.

3. Optical ray tracing runs geometrical rays through lenses, gratings, irises, mirrors, prisms, etc and evaluates the image that a specified optical system delivers. Bingo!

The project also includes a port of GNU Optical to Java.
  
## Changes / Plan

* The original BeamFour implementation made it difficult to use the backend ray tracing functions independently of the GUI. I am working on improving the de-coupling of the UI from the ray tracing functions.
* The original BeamFour implementation assumed that there is a single workspace that a user is working in, and therefore used static data structures - this design is not as friendly to server side use cases where multiple simultaneous workspaces can be in use. I am modifying the system so that all the data structures are encapsulated in objects.
* My Java port of GNU Optical has been merged into this project. For now there are two parallel implementations of ray tracing - longer term BeamFour implementation will be the main one because it is simpler and easier to understand.
* I plan to add some features that are more specific to photo lenses - such as paraxial calculations.

## Literature

## Literature

* Donald P. Feder, "Optical Calculations with Automatic Computing Machinery," J. Opt. Soc. Am. 41, 630-635 (1951). This short paper provides equations for ray tracing for rotationally symmetric surfaces, including aspherics. Equations are provided in a format suitable for computer programs. Additionally, this paper also covers calculation of image aberrations. Warren J. Smith: Modern Optical Engineering book has a description of the algorithms in this paper. 

* G. H. Spencer and M. V. R. K. Murty, "General Ray-Tracing Procedure," J. Opt. Soc. Am. 52, 672-678 (1962). This paper presents generalized ray tracing equations that cover not only rotationally symmetric surfaces (including aspherics) but also diffraction gratings. The paper allows for surfaces to have their own local axes. BeamFour implementation of ray tracing is based on this papeer. 

* R. E. Hopkins and R. Hanau, "Fundamental Methods of Ray Tracing," in Military Standardization Handbook: Optical Design, MIL-HDBK 141, U.S. Defense Supply Agency, Washington, DC, 1962. This is the fifth chapter in the document. It covers ray tracing equations for rotationally symmetric surfaces including aspheric surfaces. The equations are presented in a form suited for implementation in computer programs. The final equations in this document are very similar to Feder's equations. This document goes into details of how these equations are derived. Daniel Malacara: Handbook of Optical Design has a description of the ray tracing equations found in this document. 

* Bram de Greve, "Reflections and Refractions in Ray Tracing," 2004. This paper appears to be the source for the refraction equations originally used by GNU Optical. My plan is to use Feder's equations instead. 

* Telescope Optics - GNU Optical source code has references to this, it is unclear whether this is a reference to the book of this name by Rutten and Venrooij. It appears that the equations for intersection of rays with surfaces may have been based upon this; however my plan is to use Feder's equations instead.

## Related Projects

* My fork of GNU Optical: https://github.com/dibyendumajumdar/goptical
* Java port of GNU Optical: https://github.com/dibyendumajumdar/jfotoptix - merged into this project.
* For a product developed by Michael Hayford who worked many years at optical software company - see https://github.com/mjhoptics/ray-optics. 
* Another Python project is https://github.com/quartiq/rayopt
* An older C project 'ray' originally written by Don Wells at NRAO implements Feder's equations for ray tracing. https://github.com/dibyendumajumdar/ray
* An attempt to maintain a commercial product KDP is here: https://github.com/dinosauria123/Koko. Lots of sphagetti Fortran code unfortunately. 

Here are some other projects that I have not personally tried out

* A new Julia project by Microsoft is https://github.com/microsoft/OpticSim.jl
* A C++ project: https://github.com/edeforas/Astree
* A Java project: https://github.com/StellarSoftwareBerkeley/BeamFour
* Python project OpticsSpy: https://github.com/Sterncat/opticspy 
* Another Python project https://github.com/mess42/pyrate
* Geopter (C++,Qt): https://github.com/heterophyllus/Geopter 
* A Matlab/Octave project: https://github.com/heterophyllus/OpticalDesign-Toolbox
