package com.stellarsoftware.beam.core;

import static com.stellarsoftware.beam.core.B4constants.*;

public class B4DataParser {

    private OPTDataModel optDataModel;
    private RAYDataModel rayDataModel;
    private MEDDataModel medDataModel;
    private RT13 rt13;

    public B4DataParser(OPTDataModel optDataModel, RAYDataModel rayDataModel, MEDDataModel medDataModeln, RT13 rt13) {
        this.optDataModel = optDataModel;
        this.rayDataModel = rayDataModel;
        this.medDataModel = medDataModel;
        this.rt13 = rt13;
    }

    public String parse(boolean bActive)
    // Called by DMF_RunGrayingListener, editors, showError.
    // Called also by Options menu constructor for graying.
    // Also called by blinker if bNeedsParse, to freshen titlebar status.
    // Called upon opening or closing any table.
    // Posts each new DMF status title.
    // Set bActive=false to delay posting any judgment outputs
    // Set bActive=true to allow full parse action.
    // Potential danger: must not mess up AutoAdjust's surfs[] work.
    {
        if (Globals.bAutoBusy)
            return null;

        if (!bActive)
        {
            Globals.giFlags[STATUS] = GUNKNOWN;
            return "";
        }

        if ((optDataModel==null) && (rayDataModel==null) && (medDataModel==null))
        {
            Globals.giFlags[STATUS] = GNOFILES;
            //postTitle(""); // was "No files loaded"
            return "";
        }

        if (optDataModel == null)
            Globals.giFlags[OPRESENT] = 0;
        else
            optDataModel.parse();

        if (rayDataModel == null)
            Globals.giFlags[RPRESENT] = 0;
        else
            rayDataModel.parse();

        if (medDataModel == null)
            Globals.giFlags[MPRESENT] = 0;
        else
            medDataModel.parse();

        //---------now compute and post giFlags[STATUS]---------

        if (Globals.giFlags[OPRESENT] == 0)
        {
            Globals.giFlags[STATUS] = GOABSENT;
            return "";
        }
        if ((Globals.giFlags[ONSURFS] < 1) || (Globals.giFlags[ONFIELDS] < 1))
        {
            Globals.giFlags[STATUS] = GOEMPTY;
            return "";
        }
        if (Globals.giFlags[OSYNTAXERR] > 0)
        {
            Globals.giFlags[STATUS] = GOSYNTAXERR;
            return "";
        }

        boolean rayOK = (Globals.giFlags[RPRESENT] > 0)
                && (Globals.giFlags[RNRAYS] > 0)
                && (Globals.giFlags[RNFIELDS] > 0);

        // expand rayAbsence to all its metrics for Layout:AllDiams.
        // I hope this does not confuse the ray parser!
        if (!rayOK)
        {
            Globals.giFlags[RNRAYS] = 0;
            Globals.giFlags[RNFIELDS] = 0;
        }

        if ((!rayOK) && (Globals.giFlags[OALLDIAMSPRESENT] > 0))
        {
            Globals.giFlags[STATUS] = GLAYOUTONLY;
            return "";
        }
        if (Globals.giFlags[RPRESENT] == 0)
        {
            Globals.giFlags[STATUS] = GRABSENT;
            return "";
        }
        if ((Globals.giFlags[RNRAYS] < 1) || (Globals.giFlags[RNFIELDS] < 1))
        {
            Globals.giFlags[STATUS] = GREMPTY;
            return "";
        }
        if (Globals.giFlags[RSYNTAXERR] > 0)
        {
            Globals.giFlags[STATUS] = GRSYNTAXERR;
            return "";
        }

        //-------past this point we have a good .OPT and .RAY---------

        if ((Globals.giFlags[OGRATINGPRESENT]==1) && (Globals.giFlags[RALLWAVESNUMERIC]==0))
        {
            Globals.giFlags[STATUS] = GRNEEDNUMERWAVES;
            return "";
        }

        if (Globals.giFlags[OMEDIANEEDED] == FALSE)
        {
            // so refraction=OK, grating=OK
            Globals.giFlags[STATUS] = GPARSEOK;
            return "";
        }
        else if (Globals.giFlags[RALLWAVESPRESENT] == FALSE)
        {
            // making media LUT fail
            Globals.giFlags[STATUS] = GRLACKWAVE;
            return "";
        }

        //-------from here on, media are required------------------
        //  Two things to verify:
        //  1.  Every glass named in .OPT appears among the listed glasses
        //  2.  Every wavel named in .RAY appears amont the listed wavels.
        //

        if (Globals.giFlags[MPRESENT] == FALSE)
        {
            Globals.giFlags[STATUS] = GMABSENT;
            return "";
        }
        if ((Globals.giFlags[MNGLASSES] < 1) || (Globals.giFlags[MNWAVES] < 1))
        {
            Globals.giFlags[STATUS] = GMEMPTY;
            return "";
        }
        if (Globals.giFlags[MSYNTAXERR] > 0)
        {
            Globals.giFlags[STATUS] = GMSYNTAXERR;
            return "";
        }

        //----from here on, media table is internally OK----------
        //------ but does it have what we need?-------------------
        //----------------set up RT13 LUTs----------------------

        for (int j=1; j<=MAXSURFS; j++)
            rt13.gO2M[j] = ABSENT;  // LUT: each jsurf gives glass ID

        for (int k=1; k<=MAXRAYS; k++)
            rt13.gR2W[k] = ABSENT;  // LUT: each kray gives wavel ID

        //--------search the glass names in use----------------
        //------But! skip any glass names that are numeric-----

        int unkglassrec = ABSENT;
        String unkglassname = "";
        boolean trouble = false;
        for (int jsurf = 1; jsurf<= Globals.giFlags[ONSURFS]; jsurf++)
        {
            // if (OEJIF.oglasses[jsurf].length() > 0)
            double refr = rt13.surfs[jsurf][OREFRACT];
            if (Double.isNaN(refr))            // invalid numeric
            {
                for (int mrec = 1; mrec <= Globals.giFlags[MNGLASSES]; mrec++)
                    //if (OEJIF.oglasses[jsurf].equals(MEJIF.mglasses[mrec]))
                    if (optDataModel.oglasses()[jsurf].equals(medDataModel.mglasses(mrec)))
                    {
                        rt13.gO2M[jsurf] = mrec; // found it! jsurf uses glass number "mrec"
                        break;                   // abandon search.
                    }
                if (rt13.gO2M[jsurf] == ABSENT)
                {
                    unkglassrec = jsurf;
                    //unkglassname = OEJIF.oglasses[jsurf];
                    unkglassname = optDataModel.oglasses()[jsurf];
                    trouble = true;
                    break; // break out of required glass loop
                }
            }
        }
        if (trouble)
        {
            Globals.giFlags[STATUS] = GOGLASSABSENT;
            return unkglassname;
        }

        //-----if we get this far, we have all our glasses------
        //------next: do we have all our wavelengths?-----------

        int unkwaverec = ABSENT;
        String unkwavename = "";
        trouble = false;
        for (int kray = 1; kray<= Globals.giFlags[RNRAYS]; kray++)
        {
            if (rayDataModel.wavenames(kray).length() > 0)  // empty is trouble here.
                //if (REJIF.wavenames[kray].length() > 0)  // empty is trouble here.
                for (int f = 1; f <= Globals.giFlags[MNWAVES]; f++)
                    if (rayDataModel.wavenames(kray).equals(medDataModel.mwaves(f)))
                    //if (REJIF.wavenames[kray].equals(MEJIF.mwaves[f]))
                    {
                        rt13.gR2W[kray] = f; // found it! kray uses wavel ID "f"
                        break;               // abandon search.
                    }
            if (rt13.gR2W[kray] == ABSENT)
            {
                unkwaverec = kray;
                //unkwavename = REJIF.wavenames[kray];
                unkwavename = rayDataModel.wavenames(kray);
                trouble = true;
                break;
            }
        }
        if (trouble)
        {
            Globals.giFlags[STATUS] = GRWAVEABSENT;
            return unkwavename;
        }
        Globals.giFlags[STATUS] = GPARSEOK;
        return "";
    }

}
