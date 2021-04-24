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

import org.redukti.jfotoptix.math.Transform3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Transform3Cache {

    Map<ElementPair, Transform3> _cache = new HashMap<>();

    public Transform3 get(int from, int to) {
        ElementPair pair = new ElementPair(from, to);
        return _cache.get(pair);
    }

    private void put(int from, int to, Transform3 transform) {
        ElementPair pair = new ElementPair(from, to);
        _cache.put(pair, transform);
    }

    public void put_local_2_global_transform(int id, Transform3 t) {
        put(id, 0, t);
    }

    public Transform3 local_2_global_transform(int id) {
        return get(id, 0);
    }

    public void put_global_2_local_transform(int id, Transform3 t) {
        put(0, id, t);
    }

    public Transform3 global_2_local_transform(int id) {
        return get(0, id);
    }

    public Transform3 transform_cache_update(int from, int to) {
        assert (from != to);
        Transform3 e = get(from, to);
        if (e == null) {
            Transform3 t1 = local_2_global_transform(from);
            Transform3 t2 = local_2_global_transform(to);
            e = Transform3.compose(t1, t2.inverse());
            put(from, to, e);
        }
        return e;
    }

    static final class ElementPair {
        final int from;
        final int to;

        public ElementPair(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElementPair that = (ElementPair) o;
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

}
