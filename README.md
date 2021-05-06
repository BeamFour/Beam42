# jFotoptix

This is work-in-progress Java port of https://github.com/dibyendumajumdar/goptical.

## Features ported from GNU Optical

* Setup an optical system, draw it and do basic ray tracing
* Do spot analysis 
* Contains a rendering kit that outputs to SVG
* Contains a small plotting framework

## New features not available in GNU Optical

* WIP Paraxial calculations
* WIP Find chief ray using a solver - this is required in order to do ray-fan plots. 
  GNU Optical has ray fan plots, but these are wrong because it lacks ability to find the
  chief ray.

## Other plans

* Merge [BEAMFOUR](https://github.com/dibyendumajumdar/BeamFour) project with this one.

## Why?

The GNU Optical C++ code base is hard to maintain, read, refactor or change.
There is no real benefit to using C++ for GNU Optical, apart from availability of numeric libraries. 
But since everything used by the version I maintain is self-contained, there are no external dependencies
to worry about. Performance benefits of C++ are not a concern as we can make the Java version
fast too, if we need it. Right now though my focus is on correctness, improving the code structure, and
adding important functionality such as being able to perform paraxial calculations, identify chief ray,
etc.

I chose Java as the language as it is the quickest option for me to 
port to, but Kotlin would be nice as it will allow targeting JavaScript or the JVM.

C# is another option - and there is an initial port at https://github.com/dibyendumajumdar/nfotoptix.

Update: Given BEAMFOUR is in Java, this project will likely stay in Java.

To ease comparison with the original C++ version - fow now we have some naming conventions that are carried over from the C++ code. This will be changed
in due course to align with the Java coding style.

## License

The software is ported from Goptical, hence is licensed under the GPL.
Copyright (c) 2021 Dibyendu Majumdar

Original GNU Optical License and Authors are as follows:

      The Goptical library is free software; you can redistribute it
      and/or modify it under the terms of the GNU General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.

      The Goptical library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied
      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      See the GNU General Public License for more details.

      You should have received a copy of the GNU General Public
      License along with the Goptical library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307 USA

      Copyright (C) 2010-2011 Free Software Foundation, Inc
      Author: Alexandre Becoulet

