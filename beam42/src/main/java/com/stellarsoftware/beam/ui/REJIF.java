package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.RAYDataModel;

import java.util.ArrayList;

@SuppressWarnings("serial")

/**
 *  REJIF is a concrete ray editor class extending EJIF.
 *  Most of the content is now in RayDataParser.
 *
 * @author M.Lampton (c) STELLAR SOFTWARE 2004, 2015 all rights reserved.
 */
class REJIF extends EJIF 
{
    // public static final long serialVersionUID = 42L;

    REJIF(int iXY, String gfname)
    {
        super(1, iXY, ".RAY", gfname, MAXRAYS, new RAYDataModel(Globals.RT13)); // call EJIF for preliminary parse()
        myFpath = gfname;
    }

    RAYDataModel model() {
        return (RAYDataModel) dataModel;
    }

    //--------- public methods for autoadjust inquiries-----------------

    double getAdjValue(int i) {
        return model().getAdjValue(i);
    }

    int getAdjAttrib(int i) {
        return model().getAdjAttrib(i);
    }

    int getAdjRay(int i) {
        return model().getAdjRay(i);
    }

    int getAdjField(int i) {
        return model().getAdjField(i);
    }

    ArrayList<Integer> getSlaves(int i) {
        return model().getSlaves(i);
    }
}
