package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.OPTDataModel;

import java.util.ArrayList;


@SuppressWarnings("serial")

/**
 * This file has just one class: OEJIF that extends EJIF,
 * supplying EJIF's abstract method parse().
 *
 * @see OPTDataModel
 *
 * @author M.Lampton (c) 2004-2012 STELLAR SOFTWARE all rights reserved.
 */
class OEJIF extends EJIF 
{
    // public static final long serialVersionUID = 42L;

    OEJIF(int iXY, String gfname)
    {
        super(0, iXY, ".OPT", gfname, MAXSURFS, new OPTDataModel()); // call EJIF
        myFpath = gfname;                        // field of EJIF.
    }

    OPTDataModel model() {
        return (OPTDataModel) dataModel;
    }


//    //-----------public functions for AutoAdjust------------
//    //-----Now that Adjustment is a public class,
//    //-----cannot Auto get its own data?----------------
//    //-----Nope. ArrayList adjustments is private.----------
//    //
//    //---Yikes, sometimes at startup adjustables is all -1 even with good adjustables.
//    //-----What should initialize adjustables??

    double getOsize() {
        return model().getOsize();
    }

    double getAdjValue(int i) {
        return model().getAdjValue(i);
    }

    int getAdjAttrib(int i) {
        return model().getAdjAttrib(i);
    }

    int getAdjSurf(int i) {
        return model().getAdjSurf(i);
    }

    int getAdjField(int i) {
        return model().getAdjField(i);
    }

    ArrayList<Integer> getSlaves(int i) {
        return model().getSlaves(i);
    }
}
