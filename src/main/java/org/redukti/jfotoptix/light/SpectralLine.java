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


package org.redukti.jfotoptix.light;

import org.redukti.jfotoptix.io.Rgb;

/**
 Describe a spectral line

 This class can be used to describe a spectral line. It contains
 constants for wave length of standard rays
 */
public class SpectralLine {
    double _wavelen;
    double _intensity;

    /** red colored line at 645nm */
    public static final double red = 645.0;
    /** green colored line at 510nm */
    public static final double green = 510.0;
    /** blue colored line at 440nm */
    public static final double blue = 440.0;

    /** infrared mercury line at 1013.98nm */
    public static final double t = 1013.98;
    /** infrared cesium line at 852.11nm */
    public static final double s = 852.11;
    /** red helium line at 706.5188nm */
    public static final double r = 706.5188;
    /** red hydrogen line at 656.2725nm */
    public static final double C = 656.2725;
    /** red cadmium line at 643.8469nm */
    public static final double C_ = 643.8469; // C'
    /** yellow sodium line at 589.2938nm */
    public static final double D = 589.2938;
    /** yellow helium line at 587.5618nm */
    public static final double d = 587.5618;
    /** green mercury line at 546.074nm */
    public static final double e = 546.074;
    /** blue hydrogen line at 486.1327nm */
    public static final double F = 486.1327;
    /** blue cadmium line at 479.9914nm */
    public static final double F_ = 479.9914; // F'
    /** blue mercury line at 435.8343nm */
    public static final double g = 435.8343;
    /** violet mercury line at 404.6561nm */
    public static final double h = 404.6561;
    /** ultraviolet mercury line at 365.0146nm */
    public static final double i = 365.0146;
    
    /** Create a spectral line with specified wavelen and
     intensity. */
    public SpectralLine (double wavelen, double intensity) {
        this._wavelen = wavelen;
        this._intensity = intensity;
    }

    /** get spectral line wavelen */
    public double get_wavelen ()
    {
        return _wavelen;
    }
    /** get spectral line intensity */
    public double get_intensity ()
    {
        return _intensity;
    }
    /** get rgb color associated spectral line, ignore intensity */
    public Rgb get_color ()
    {
        return get_wavelen_color (_wavelen);
    }

    /** get rgb color associated with wavelen */
    public static Rgb get_wavelen_color (double wl) {
        // based on algorithm from Dan Bruton
        // (www.physics.sfasu.edu/astro/color.html)

        if (wl < 380.0 || wl > 780.0)
            return Rgb.rgb_black;

        double s = 1.0;

        if (wl < 420.0)
            s = 0.3 + 0.7 * (wl - 380.0f) / 40.0;
        else if (wl > 700.0)
            s = 0.3 + 0.7 * (780.0 - wl) / 80.0;

        if (wl < 510.0)
        {
            if (wl < 490.0)
            {
                if (wl < 440.0)
                    // 380 to 440
                    return new Rgb (s * -(wl - 440.0) / 60.0, 0.0, s, 1.0);
          else
                // 440 to 490
                return new Rgb (0.0, s * (wl - 440.0) / 50.0, s, 1.0);
            }
            else
                // 490 to 510
                return new Rgb (0.0, s, s * -(wl - 510.0) / 20.0, 1.0);
        }
        else
        {
            if (wl < 645.0)
            {
                if (wl < 580.0)
                    // 510 to 580
                    return new Rgb (s * (wl - 510.0) / 70.0, s, 0.0, 1.0);
          else
                // 580 to 645
                return new Rgb (s, s * -(wl - 645.0) / 65.0, 0.0, 1.0);
            }
            else
            {
                // 645 to 780
                return new Rgb (s, 0.0, 0.0, 1.0);
            }
        }
    }

}
