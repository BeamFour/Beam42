/*
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
 */

package org.redukti.jfotoptix.data;

/** Specifies data interpolation methods. Availability depends on data
 * container used. */
public enum Interpolation {
    /** 1d and 2d nearest interpolation */
    Nearest,

    /** 1d linear and 2d bilinear interpolations */
    Linear,

    /** 1d quadratic interpolation */
    Quadratic,

    /** 1d cubic piecewise interpolation. It has continuous
     piecewise first derivative, non-continuous piecewise
     linear second derivative. Use segments slope as first
     derivative. Less accurate than other cubic interpolants
     but requires less computation on data set change. */
    CubicSimple,

    /** 1d cubic piecewise interpolation. It has smooth first
     derivative and continuous piecewise linear second
     derivative. Derivatives for first and last entries are
     based on first and last segments slope. It uses linear
     extrapolation (continuous but non-smooth first derivative
     on both ends). */
    Cubic,

    /** Same interpolation as Cubic, with quadratic extrapolation
     (continous and smooth first derivative on both ends). */
    Cubic2,

    /** Same as Cubic with first derivative prescribed for first
     and last entries only. */
    CubicDerivInit,

    /** Same as Cubic2 with first derivative prescribed for first
     and last entries only. */
    Cubic2DerivInit,

    /** 1d cubic piecewise interpolation. First derivatives must
     be provided for all entries. It uses linear extrapolation.*/
    CubicDeriv,

    /** 1d cubic piecewise interpolation. First derivatives must
     be provided for all entries. It uses quadratic extrapolation.*/
    Cubic2Deriv,

    /** 2d bicubic interpolation. Use smooth first derivative and
     continuous piecewise linear second derivative. Use 1d
     cubic curve to extract gradients (smooth first derivative
     and continuous piecewise linear second derivative). This
     is the best 2d interpolation when derivatives are
     non-prescribed. */
    Bicubic,

    /** 2d bicubic interpolation. Use numerical differencing to
     extract gradients. Less accurate than @ref Bicubic but
     requires less computation on data set change.*/
    BicubicDiff,

    /** 2d bicubic interpolation. x and y gradients must be
     provided. This is the best 2d interpolation when
     derivatives values are available. */
    BicubicDeriv,
}
