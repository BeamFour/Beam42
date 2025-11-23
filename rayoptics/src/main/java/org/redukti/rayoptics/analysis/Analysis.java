package org.redukti.rayoptics.analysis;

import org.redukti.render.rendering.Rgb;

public class Analysis {
    /** get rgb color associated with wavelen */
    public static Rgb get_wavelen_color (double wl) {
        // based on algorithm from Dan Bruton
        // (www.physics.sfasu.edu/astro/color.html)
        // http://www.physics.sfasu.edu/astro/color/spectra.html

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
