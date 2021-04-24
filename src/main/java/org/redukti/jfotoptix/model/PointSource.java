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


package org.redukti.jfotoptix.model;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Transform3;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

import java.util.List;

public class PointSource extends RaySource {

    /**
     * Specifies point source location mode
     */
    public enum SourceInfinityMode {
        /**
         * In finite distance mode the point source is located at
         * specified position and all rays are traced from this
         * point.
         */
        SourceAtFiniteDistance,
        /**
         * In infinity mode the point source generate parallel rays
         * oriented along source direction vector.
         */
        SourceAtInfinity,
    }

    SourceInfinityMode _mode;

    public PointSource(int id, Vector3Pair p, Transform3 transform, double min_intensity, double max_intensity, List<SpectralLine> spectrum, SourceInfinityMode mode) {
        super(id, p, transform, min_intensity, max_intensity, spectrum);
        _mode = mode;
    }

    public SourceInfinityMode mode() {
        return _mode;
    }

    public String toString() {
        return "PointSource{" + super.toString() + "}";
    }

    public static class Builder extends RaySource.Builder {

        SourceInfinityMode _mode;

        public Builder(SourceInfinityMode m, Vector3 pos_dir) {
            position(m == SourceInfinityMode.SourceAtInfinity
                    // position of infinity source is only used for trace::Sequence
                    // sort See
                    // https://lists.gnu.org/archive/html/goptical/2013-06/msg00004.html
                    ? new Vector3Pair(pos_dir.times(-1e9), pos_dir)
                    : new Vector3Pair(pos_dir, Vector3.vector3_001));
            _mode = m;
        }

        @Override
        public PointSource.Builder add_spectral_line(double wavelen) {
            super.add_spectral_line(wavelen);
            return this;
        }

        @Override
        public PointSource build() {
            return new PointSource(id, position, transform, _min_intensity, _max_intensity, _spectrum, _mode);
        }
    }

}
