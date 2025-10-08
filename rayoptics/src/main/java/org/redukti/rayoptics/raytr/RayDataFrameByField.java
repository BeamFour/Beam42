package org.redukti.rayoptics.raytr;

import org.redukti.rayoptics.specs.Field;

import java.util.List;

public class RayDataFrameByField {
    public final Field fld;
    public final List<RayDataFrame> rdf_list;

    public RayDataFrameByField(Field fld, List<RayDataFrame> rdf_list) {
        this.fld = fld;
        this.rdf_list = rdf_list;
    }
}
