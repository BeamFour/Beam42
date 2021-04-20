# jFotoptix

This is work-in-progress Java port of https://github.com/dibyendumajumdar/goptical.

## Current Features

* Setup an optical system, draw it and do basic ray tracing
* So spot analysis 
* Contains a rendering kit that outputs to SVG
* Contains a small plotting framework

## Why?

The C++ code base is hard to maintain, refactor or change.
There is no real benefit to using C++ for Goptical, apart from availability of numeric libraries. 
But since everything used by the version I maintain is self contained, there are no external dependencies
to worry about.

I chose Java as the language as it is the quickest option for me to 
port to, but Kotlin would be nice as it will allow targeting JavaScript or the JVM.

C# is another option - and there is an initial port at https://github.com/dibyendumajumdar/nfotoptix.

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

