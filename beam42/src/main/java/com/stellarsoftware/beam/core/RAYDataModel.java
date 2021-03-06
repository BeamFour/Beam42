package com.stellarsoftware.beam.core;

import java.util.ArrayList;

import static com.stellarsoftware.beam.core.B4constants.*;

/**
 * Parses and generates the RAY Data model.
 * Originally part pof REJIF.
 *
 *  Output is to RT13.raystarts[nrays][nattribs]; see line 194.
 *  It supplies EJIF's abstract method parse().
 *  It implements Consts via EJIF.
 *  A174: includes ray-intercept normal components {i,j,k}
 *  A156: allows "wa" to substitute for "@wave"
 *
 *  Uses U for suckInt(), suckDouble, getCharAt(), and for debugging.
 *
 *  Uses EJIF for getTag(f,r) and getFieldTrim(f,r).
 *
 * Writes to external RT13.raystarts[][]
 * also to RT13.smins[], RT13.spans[] for random <<RORDER??
 * Writes/reads external DMF.giFlags[] etc
 *
 * Caution: irec=1...RNRAYS; record zero is a special ray.
 * Lookup table rF2I[] is public for raystarts[][], InOut, Auto.
 * Lookup table rI2F[] exists for raystart attribs 0...8 = RX...RORDER.
 * Lookup table rI2F[] does not exist for all possible args
 * because >10000 ray attributes and they are few and sparse.
 * Better to just search the rF2I[] list.
 * Table returns RABSENT for unrecognized field.
 * But remember to reinterpret RFINAL and RGOAL for output usage.
 *
 * To support AutoAdjust, need an output list for all goals,
 * and field numbers for those goals, so that each ray can
 * have its discrepancy computed. DCRFs are defined in OEJIF.
 *
 *  Added RTANGLE attribute into RNATTRIBS, March 2015 MLL.
 *  Added RTNORMX, RTNORMY, RTNORMZ as new ray attributes.
 *  Added lower case i, j, k to read out these normal components.
 *
 * Although this does all the parsing and initial setup work, the actual results
 * get posted by InOut grabbing data from RT13's output data tables.
 *
 * @author M.Lampton (c) STELLAR SOFTWARE 2004, 2015 all rights reserved.
 */
public class RAYDataModel extends B4DataModel {
    private String wavenames[] = new String[JMAX];
    private int rF2I[] = new int[MAXFIELDS];
    private int rI2F[] = new int[RNSTARTS];  // 10 raystart attributes: B4constants.java
    private int wavefield;
    private String  headers[] = new String[MAXFIELDS];
    private int     nrays, nfields, fwfe;
    private ArrayList<Adjustment> adjustables;

    public RAYDataModel(RT13 rt13) {
        super(rt13);
    }

    public int rF2I(int i) {
        return rF2I[i];
    }
    public int rI2F(int i) {
        return rI2F[i];
    }

    public String wavenames(int i) {
        return wavenames[i];
    }

    @Override
    public void parse() // replaces the abstract parse() in EJIF
    {
        adjustables = new ArrayList<Adjustment>();

        //---First set DMF.giFlags[] defaults for rays--------------

        int status[] = new int[NGENERIC];
        vPreParse(status);                          // EJIF generic parser
        Globals.giFlags[RPRESENT]     = status[GPRESENT];
        Globals.giFlags[RNLINES]      = status[GNLINES];
        Globals.giFlags[RNRAYS]       = nrays = status[GNRECORDS];
        Globals.giFlags[RNFIELDS]     = nfields = status[GNFIELDS];
        Globals.giFlags[RWFEFIELD]    = fwfe = RABSENT; // -1
        Globals.giFlags[RNWFEGROUPS]  = 1;              // one group
        Globals.giFlags[RALLWAVESPRESENT] = 0;          // false

        Globals.giFlags[RNRAYADJ]     = 0;        // Nadj in raystart #1; can be up to 6!
        Globals.giFlags[RAYADJ0]      = RABSENT;  // no autoray attributes yet
        Globals.giFlags[RAYADJ1]      = RABSENT;
        Globals.giFlags[RAYGOALATT0]  = RABSENT;  // no autoray goals yet either
        Globals.giFlags[RAYGOALATT1]  = RABSENT;
        Globals.giFlags[RAYGOALFIELD0]= RABSENT;
        Globals.giFlags[RAYGOALFIELD1]= RABSENT;
        Globals.giFlags[RNGOALS]      = 0;        // no ray table goals yet

        // Blunder here keeps RayGenerator from finding its headers.
        // The detailed field IDs are needed even with nrays = 0.
        //
        // if (nrays < 1)
        //   return;

        //----Then zero the output arrays------------------

        for (int kray=0; kray<=MAXRAYS; kray++)
        {
            wavenames[kray] = "";
            for (int ia=0; ia<RNSTARTS; ia++)
                rt13.raystarts[kray][ia] = -0.0; // -0.0 means absentee data
        }

        for (int f=0; f<MAXFIELDS; f++)
            headers[f] = "";

        wavefield = RABSENT;

        //---------set headers and lookup table--------------

        for (int f=0; f<MAXFIELDS; f++)
        {
            rF2I[f] = RABSENT;
            headers[f] = "";
        }

        for (int i=0; i<RNSTARTS; i++)
            rI2F[i] = RABSENT;

        wavefield = Globals.giFlags[RWAVEFIELD] = RABSENT;
        int ntries=0, nunrecognized=0, ngoals=0;
        int iU=0, iV=0, iW=0;
        for (int field=0; field<nfields; field++)
        {
            ntries++;
            headers[field] = getFieldTrim(field, 1);
            int op = getCombinedRayFieldOp(headers[field]); // below
            if (op <= RABSENT)
                nunrecognized++;
            rF2I[field] = op;
            if ((op >= RX) && (op < RNSTARTS))  // allows inputs only.
                rI2F[op] = field;                 // array size = inputs only.
            if (op == RU)
                iU=1;
            if (op == RV)
                iV=1;
            if (op == RW)
                iW=1;
            if ((op == RSWAVEL) && (wavefield == ABSENT))
            {
                wavefield = Globals.giFlags[RWAVEFIELD] = field;
                Globals.giFlags[RALLWAVESPRESENT] = 1;  // true;
            }
            if ((op>=RGOAL) && (op<RGOAL+13))    // RGOAL=10100=Xg; 10101=YG; ... 10112=wg.
                ngoals++;
            if (op % 100 == RTWFE)
                Globals.giFlags[RWFEFIELD] = fwfe = field;
        }
        Globals.giFlags[RNGOALS] = ngoals;  // + ((fwfe>=0) ? 1 : 0);
        // System.out.println("Ngoals = "+ ngoals);

        Globals.giFlags[RUVWCODE] = iU + 2*iV + 4*iW;

        //--------------get data records, by field----------

        boolean allWavesNumeric = true;
        int badline=0, badfield=0, rsyntaxerr=0;
        double t=0.0;

        for (int field = 0; field< Globals.giFlags[RNFIELDS]; field++)
        {
            int op = rF2I[field];

            // get raystarts:  RX,RY,RZ,RU,RV,RW,RPATH,RSWAVEL,RSCOLOR,RSORDER
            // these get stored in RT13.raystarts[][] overwriting -0.0

            if ((op>=RX) && (op<RNSTARTS)) // 10 Raystarts: X,Y,Z,U,V,W,P,wave,color,order.
            {
                //------autoray adjustables found in initial ray record--------
                //--count all the question marks but save only the first two---

                if (getTag(field, 3) == '?')
                {
                    if (Globals.giFlags[RNRAYADJ] == 0)
                        Globals.giFlags[RAYADJ0] = op;
                    else if (Globals.giFlags[RNRAYADJ] == 1)
                        Globals.giFlags[RAYADJ1] = op;
                    Globals.giFlags[RNRAYADJ]++;
                }

                //------now get all get raystarts----------------------

                for (int kray=1; kray<=nrays; kray++)
                {
                    t = rt13.raystarts[kray][op] = getFieldDouble(field, 2+kray);
                    if (Double.isNaN(t))
                    {
                        if (op == RSWAVEL)
                            allWavesNumeric = false;
                        else
                        {
                            badline = kray+2;
                            badfield = field;
                            rsyntaxerr = badfield + 100*badline;
                            break;
                        }
                    }
                    if (op == RSWAVEL)  // special case, no syntax check
                    {
                        wavenames[kray] = getFieldTrim(field, 2+kray);
                        if (wavenames[kray].length() < 1)
                            Globals.giFlags[RALLWAVESPRESENT] = 0;  // false
                        rt13.raystarts[kray][RSCOLOR] = U.getColorCode(getTag(field, 2+kray));
                    }
                }
            }
            if (rsyntaxerr > 0)
                break; // break out of field loop to preserve location.

            // Now syntax-test any goal values that may be present,
            // but don't store them anywhere.
            // InOut and Auto will store them internally as needed.

            if ((op >= RGOAL) && (op <= RGOAL+RTWL))  // greater than 10100 !!
            {
                if (Globals.giFlags[RAYGOALATT0] > RABSENT)
                {
                    Globals.giFlags[RAYGOALFIELD1] = field;
                    Globals.giFlags[RAYGOALATT1] = op;
                }
                else
                {
                    Globals.giFlags[RAYGOALFIELD0] = field;
                    Globals.giFlags[RAYGOALATT0] = op;
                }
                for (int kray=1; kray<=nrays; kray++)
                {
                    if (Double.isNaN(getFieldDouble(field, 2+kray)))
                    {
                        badline = kray+2;
                        badfield = field;
                        rsyntaxerr = badfield + 100*badline;
                        break;
                    }
                }
            }
            if (rsyntaxerr > 0)
                break; // break out of field loop to preserve location.
        } //-------end of field loop; fixupUVW() is done in RT13---------

        Globals.giFlags[RSYNTAXERR] = rsyntaxerr;
        Globals.giFlags[RALLWAVESNUMERIC] = allWavesNumeric ? 1 : 0;
        Globals.giFlags[RNADJ] = iParseAdjustables(nrays);

        setSminsSpans();

    }  //---------end of parse()---------------



    //--------public methods for this and other parsers to use---------------

    public static int getCombinedRayFieldOp(String s)
    /**  (c) 1993, 2004 M.Lampton STELLAR SOFTWARE
     *  Computes a combined field op code = RayAttrib + 100 * RaySurface.
     *  Runs within ray parser, when nsurfs is likely unknown.
     *  Returns RABSENT=-1 when input string is unrecognized.
     *  Returns 0..5, 6=RPATH, 7=RWAVEL, 8=RCOLOR, 9=RORDER; 10=RNSTARTS.
     *  Returns 100..up for output data fields
     *  Special cases for outputs RNOTE=133, RDEBUG=134, RFRONT=135.
     *  Careful: RFINAL=10000.
     *  Careful: RGOAL=10100 is both an input and output field.
     *  MLL Aug1998: added 'o', 'O' as synonyms for zero '0'
     *  MLL Oct2013: added "wa" as synonym for "@" wavelength
     *  MLL Apr2015: added i,j,k callouts for local surface normal
     *
     *  output data have surfcodes = 100*jsurf
     *  input data have surfcode = 0
     *  The interpretation of RFINAL cannot be done here since nsurfs may change.
     *  It has to be done within the output routine, not here.
     *  The methods RT13.getSurf(op) and RT13.getAttr(op) do this.
     */
    {
        int len = s.length();
        if (len < 1)
            return RABSENT;
        char c0=' ', c0up=' ', c1up=' ', c2up=' ';
        c0 = s.charAt(0);
        s = s.toUpperCase();
        c0up = s.charAt(0);
        if (len>1)
            c1up = s.charAt(1);
        if (len>2)
            c2up = s.charAt(2);

        if (c0up == 'N')             // notes output field
            return RNOTE;
        if (c0up == 'D')             // output debug field
            return RDEBUG;
        if (c0up == 'O')             // input diffraction order field
            return RSORDER;
        if (c0up == '@')             // input wavelength field.
            return RSWAVEL;
        if ((c0up=='W') && (c1up=='A'))
            return RSWAVEL;           // also a wavelength input field.

        if ((c0up=='W') && (c1up=='F') && (c2up=='E'))
            return RTWFE+RFINAL;

        /// calculate the surfcode:
        int surfcode = 0;
        switch (c1up)   // c1 determines the surface code. DO NOT USE C1 FOR OTHER THINGS.
        {
            case ' ':
            case 'F': surfcode = RFINAL; break;   // 10000
            case 'G': surfcode = RGOAL; break;    // 10100
            case 'O': surfcode = 0;  break;       // synonym for zero = ray start info.
            default:  surfcode = 100*U.getTwoDigitCode(s);
        }
        if (surfcode < 0)
            return RABSENT;

        boolean bNonzero = surfcode > 99;

        /// now affix the attribute code:
        switch (c0)
        {
            case 'X':  return RX+surfcode;
            case 'Y':  return RY+surfcode;
            case 'Z':  return RZ+surfcode;
            case 'U':  return RU+surfcode;
            case 'V':  return RV+surfcode;
            case 'W':  return RW+surfcode;
            case 'P':
            case 'p':  return RPATH+surfcode;
            case 'x':  return bNonzero ? RTXL+surfcode : RX;
            case 'y':  return bNonzero ? RTYL+surfcode : RY;
            case 'z':  return bNonzero ? RTZL+surfcode : RZ;
            case 'u':  return bNonzero ? RTUL+surfcode : RU;
            case 'v':  return bNonzero ? RTVL+surfcode : RV;
            case 'w':  return bNonzero ? RTWL+surfcode : RW;
            case 'A':
            case 'a':  return RTANGLE+surfcode;
            case 'i':  return RTNORMX+surfcode;
            case 'j':  return RTNORMY+surfcode;
            case 'k':  return RTNORMZ+surfcode;
            default:   return RABSENT;
        }
    }





    //--------- public methods for autoadjust inquiries-----------------

    public double getAdjValue(int i)
    // Fetch appropriate value from RT13.raystarts[][]
    {
        if ((adjustables!=null) && (i>=0) && (i<adjustables.size()))
        {
            int kray = adjustables.get(i).getRecord();
            int iattr = adjustables.get(i).getAttrib();
            if ((kray>0) && (kray<=nrays) && (iattr>=0) && (iattr<OFINALADJ))
                return rt13.raystarts[kray][iattr];
        }
        return 0.0;
    }

    public int getAdjAttrib(int i)
    {
        if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
            return adjustables.get(i).getAttrib();
        else
            return -1;
    }

    public int getAdjRay(int i)
    {
        if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
            return adjustables.get(i).getRecord();
        else
            return -1;
    }

    public int getAdjField(int i)
    {
        if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
            return adjustables.get(i).getField();
        else
            return -1;
    }

    public ArrayList<Integer> getSlaves(int i)
    {
        if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
            return adjustables.get(i).getList();
        else
            return null;
    }





    //-------------private methods-----------------------

    private int iParseAdjustables(int nrays)
    // fills in private ArrayList of adjustables, with slaves & antislaves.
    // Returns how many groups were found based on rayStart tags.
    {
        if (nrays < 1)
            return 0;
        boolean bLookedAt[] = new boolean[nrays+1];
        adjustables.clear();
        for (int field=0; field<nfields; field++)
        {
            int op = rF2I[field];
            if ((op<RX) || (op>RW))  // or other validity test
                continue;

            for (int record=1; record<=nrays; record++)
                bLookedAt[record] = false;

            for (int record=1; record<=nrays; record++)
            {
                char tag0 = getTag(field, record+2);
                boolean bAdj = isAdjustableTag(tag0);
                if (!bAdj || bLookedAt[record])
                {
                    bLookedAt[record] = true;
                    continue;
                }

                //---New adjustable parameter found------------
                bLookedAt[record] = true;
                ArrayList<Integer> slaves = new ArrayList<Integer>();
                if (Character.isLetter(tag0))
                {
                    boolean bUpper0 = Character.isUpperCase(tag0);
                    char tag0up = Character.toUpperCase(tag0);
                    for (int k=record+1; k<=nrays; k++)
                    {
                        if (!bLookedAt[k])  // find slaves & antislaves
                        {
                            char tagk = getTag(field, k+2);
                            boolean bUpperk = Character.isUpperCase(tagk);
                            char tagkup = Character.toUpperCase(tagk);
                            boolean bSameGroup = (tag0up == tagkup);
                            if (bSameGroup)
                            {
                                int iSlave = (bUpper0 == bUpperk) ? k : -k;
                                slaves.add(iSlave);
                                bLookedAt[k] = true;
                            }
                        }
                    }
                }
                adjustables.add(new Adjustment(op, record, field, slaves));
            } // done with all groups in this field
        }// done with all fields
        return adjustables.size();
    }

    private boolean isAdjustableTag(char c)
    {
        return (c=='?') || Character.isLetter(c);
    }

/*
    private int iParseWFEgroups(int nrays)
    // Fills in RT13.iWFEgroup[kray].
    // Groups are numbered 0, 1, ...ngroups-1.
    // Returns how many groups were found based on WFE tags.
    {
        for (int k=1; k<=nrays; k++)    // First, assign everything to
          RT13.iWFEgroup[k] = 0;        // group number zero.

        if (fwfe < 0)                   // No WFE field present?
          return 1;                     // Then just one huge group.

        int ngroups = 0;                // Initialize search engine.
        int nraysfound = 0;
        boolean bLookedAt[] = new boolean[nrays+1];
        for (int kray=1; kray<=nrays; kray++)
          bLookedAt[kray] = false;

        for (int ktop=1; ktop<=nrays; ktop++) // search for next top ray
        {
            if (bLookedAt[ktop])              // skip; already catalogued.
              continue;
            char tag = getTag(fwfe, ktop+2);

            // Search for all cohorts here and below
            int nrayspergroup = 0;
            for (int k=ktop; k<=nrays; k++)
            {
                if (!bLookedAt[k] && (tag==getTag(fwfe, k+2)))
                {
                    RT13.iWFEgroup[k] = ngroups;
                    bLookedAt[k] = true;
                    nrayspergroup++;     // diagnostic
                    nraysfound++;        // diagnostic
                }
            }
            ngroups++;
        }
        return ngroups;
    }
*/


    private void setSminsSpans()
    // Examines tabulated raystarts[] for extreme values;
    // Sets smins[][], spans[][] for use by randomizer in RT13.
    // Called only locally by REJIF.parse().
    // Special case for absentee data: let smin[]=span[]=-0.0
    // Uses RT13.iWFEgroup[] -- must run iParseWFEgroups() first!!
    // A207: WFEgroups eliminated
    {
        int nrays = Globals.giFlags[RNRAYS];
        for (int iatt=RX; iatt<=RW; iatt++)
        {
            // System.out.println("REJIF.setSminsSpans() is starting iatt = "+iatt);
            boolean bAngles = (iatt >= RU);
            double x=-0., xmin=-0., xmax=-0., xsave=-0.;
            int raycount = 0;
            for (int k=1; k<=nrays; k++)   // First, count the available entries
            {
                x = rt13.raystarts[k][iatt];
                if (!U.isNegZero(x))
                {
                    raycount++;
                    xsave = x;
                }
            }
            if (raycount<=1)
            {
                rt13.smaxs[iatt] = xsave;
                rt13.smins[iatt] = xsave;
                rt13.spans[iatt] = 0.0;
            }
            else  // two or more rays to determine span
            {
                xmin = xmax = xsave;
                for (int k=1; k<=nrays; k++)
                {
                    x = rt13.raystarts[k][iatt];
                    if (!U.isNegZero(x))
                    {
                        xmin = Math.min(xmin, x);
                        xmax = Math.max(xmax, x);
                    }
                }
                rt13.smaxs[iatt] = xmax;
                rt13.smins[iatt] = xmin;
                rt13.spans[iatt] = xmax - xmin;
            }
            // System.out.printf("REJIF.setSminsSpans() returning iatt, xmin, span = %3d %8.4f %8.4f \n", iatt, xmin, xmax-xmin);

        } //---------------done with all attributes---------------------
    } //-----------------done setting spans-----------------------

}
