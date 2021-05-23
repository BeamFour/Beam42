package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.MEDDataModel;

@SuppressWarnings("serial")

/**
 * MEJIF is a media editor class concreting the abstract class EJIF.
 * The actual implementation is now in MediaParser.
 *
 * @see MEDDataModel
 * @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
 */
class MEJIF extends EJIF
{
    // public static final long serialVersionUID = 42L;

    public MEJIF(int iXY, String gfname)
    // constructor creates a media editor using EJIF
    {
        super(2, iXY, ".MED", gfname, MAXMEDIA, new MEDDataModel());
        myFpath = gfname; 
    }

    public MEDDataModel model() {
        return (MEDDataModel) dataModel;
    }
}

