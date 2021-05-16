package com.stellarsoftware.beam;

import javax.swing.*; 

@SuppressWarnings("serial")

/**
 * MEJIF is a media editor class concreting the abstract class EJIF.
 * The actual implementation is now in MediaParser.
 *
 * @see MediaParser
 * @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
 */
class MEJIF extends EJIF
{
    // public static final long serialVersionUID = 42L;

    public MEJIF(int iXY, String gfname)
    // constructor creates a media editor using EJIF
    {
        super(2, iXY, ".MED", gfname, MAXMEDIA, new MediaParser());
        myFpath = gfname; 
    }

    public MediaParser model() {
        return (MediaParser) parser;
    }
}

