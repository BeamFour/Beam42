package com.stellarsoftware.beam.core;

import java.util.ArrayList;

import static com.stellarsoftware.beam.core.B4constants.*;

/**
 * Parses .OPT files and builds the data model.
 * Most of what is here was originally in OEJIF.
 * The function of parse() is to set values into DMF.giFlags[] and RT13.surfs[][].
 *
 * Uses B4 constants.
 * parse() has no dirty bit worksavers; it always parses.
 *
 *  @author M.Lampton (c) 2004-2012 STELLAR SOFTWARE all rights reserved.
 */
public class OPTDataModel extends B4DataModel {

    private char cTags[][] = new char[JMAX][MAXFIELDS];
    private char typetag[] = new char[JMAX];
    private String headers[] = new String[MAXFIELDS];
    private int oF2I[] = new int[MAXFIELDS]; /* Maps field type to column */
    private int oI2F[] = new int[ONPARMS]; /* Maps column to field type */
    private String oglasses[] = new String[JMAX + 1];

    private int nsurfs;
    private ArrayList<Adjustment> adjustables = new ArrayList<Adjustment>();
    private double dOsize = 0.0;

    public OPTDataModel(RT13 rt13) {
        super(rt13);
    }

    public String[] oglasses() {
        return oglasses;
    }

    public int oF2I(int i) {
        return oF2I[i];
    }
    public int oI2F(int i) {
        return oI2F[i];
    }

    public static int getOptFieldAttrib(String s)
    // Given an optics table column header field, this routine returns a
    // number 0..122 for identified optics table fields, or else returns ABSENT.
    // This function is called by ParseOpt to fill in its fieldop[] array.
    // Free standing allows return from any depth! unlike break.
    // Table data should be numerical, except ABSENT, OFORM, OTYPE, OREFRACT.
    // Radius of curvature is written "RxCxxxx" i.e. c2up='C'.
    {
        // System.out.println("OEJIF method getOptFieldAttrib() is given arg = "+s);
        char c0=' ', c0up=' ', c1up=' ', c2up=' ', c3up=' ', c4up=' ';
        String svls = "";

        s = s.trim();
        int len = s.length();
        if (len < 1)
          return ABSENT;
        c0 = s.charAt(0);                  // save case of c0
        c0up = Character.toUpperCase(c0);
        if ((len == 4) && (c0up == 'V'))
          svls = s.toUpperCase();
        s = s.toUpperCase();               // ignore case beyond c0
        if (len > 1)
          c1up = s.charAt(1);
        if (len > 2)
          c2up = s.charAt(2);
        if (len > 3)
          c3up = s.charAt(3);
        if (len > 4)
          c4up = s.charAt(4);
        switch (c0)
        {
          case 'A':
          case 'a': switch (c1up)  // asphericity coefficients
                    {
                       case '1': switch(c2up)
                       {
                          case '0': return OA10;
                          case '1': return OA11;
                          case '2': return OA12;
                          case '3': return OA13;
                          case '4': return OA14;
                          case '5':
                          case '6':
                          case '7':
                          case '8':
                          case '9': return ABSENT;
                          default: return OA1;
                       }
                       case '2': return OA2;
                       case '3': return OA3;
                       case '4': return OA4;
                       case '5': return OA5;
                       case '6': return OA6;
                       case '7': return OA7;
                       case '8': return OA8;
                       case '9': return OA9;
                       case 'C': return OTYPE; // "ACTION"
                    }
                    if (s.endsWith("X"))
                      return OASPHX;
                    if (s.endsWith("Y") && (len < 9)) // allows 'asphericity'
                      return OASPHY;
                    return OASPHER;

          case 'C':
          case 'c': if (s.contains("X"))  // curvatures
                      return OCURVX;
                    if (s.contains("Y"))
                      return OCURVY;
                    return OCURVE;

          case 'D': if (s.contains("X"))
                      return OODIAX;
                    if (s.contains("Y"))
                      return OODIAY;
                    return OODIAM;

          case 'd': if (s.contains("X"))
                      return OIDIAX;
                    if (s.contains("Y"))
                      return OIDIAY;
                    return OIDIAM;

          case 'F':
          case 'f': return OFORM;  // "form" = nonnumerical

          case 'G':
          case 'g': switch(c1up)   // Group or Grating groove density
                    {
                       case 'R': return OGROUP;
                       case 'X': return OGX;
                       case 'Y': return OGY;
                       // case '1': return OVLS1;    // removed 22 March 2016 A192
                       // case '2': return OVLS2;
                       // case '3': return OVLS3;
                       // case '4': return OVLS4;
                    }
                    return ABSENT;

          case 'H':
          case 'h': if (len < 4)
                return ABSENT;
                if (c1up=='G')
                   return OHGAUSS;  // height of 2D Gaussian

                switch(c3up)         // HOE entries
                {
                    case 'L':
                    case 'l': return OHOELAM;
                    case 'X':
                    case 'x': if (c4up == '1')
                                return OHOEX1;
                              if (c4up == '2')
                                return OHOEX2;
                              return ABSENT;
                    case 'Y':
                    case 'y': if (c4up == '1')
                                return OHOEY1;
                              if (c4up == '2')
                                return OHOEY2;
                              return ABSENT;
                    case 'Z':
                    case 'z': if (c4up == '1')
                                return OHOEZ1;
                              if (c4up == '2')
                                return OHOEZ2;
                              return ABSENT;
                }
                return ABSENT;

          case 'I':
          case 'i': return OREFRACT;  // refractive index or glass name

          case 'L':
          case 'l':
          case 'M':
          case 'm':  return OTYPE;  // "Lens" "mirror" etc

          case 'N':
          case 'n':  if (c1up=='S')
                       return ONSPIDER;
                     if (c1up=='X')
                       return ONARRAYX;
                     if (c1up=='Y')
                       return ONARRAYY;
                     return ABSENT;

          case 'O':  // OffIX, OffIY, OffOX, OffOY
          case 'o':  if ((c3up=='O') && (c4up=='X')) return OFFOX;
                     if ((c3up=='O') && (c4up=='Y')) return OFFOY;
                     if ((c3up=='I') && (c4up=='X')) return OFFIX;
                     if ((c3up=='I') && (c4up=='Y')) return OFFIY;
                     return OORDER;

          case 'P':
          case 'p':  return OPITCH;

          case 'R':
          case 'r':  if (c1up=='G') return ORGAUSS;   // 2D Gaussian radius or sigma
                     if ((c2up=='C') && (c3up=='X')) return ORADX;
                     if ((c2up=='C') && (c3up=='Y')) return ORADY;
                     if (c2up=='C') return ORAD;
                     return OROLL;

          case 'S':
          case 's':  if (c1up=='C')  return OSCATTER;  // angle, degrees
                     return OSHAPE;

          case 'T':
          case 't':  if (c1up == 'Y')  return OTYPE;
                     return OTILT;     // tilt.

          case 'V':   // twenty curl-free explicit VLS coefficients
          case 'v':
                     if (svls.equals("VX00")) return OGX;    // synonym
                     if (svls.equals("VX10")) return OVX10;
                     if (svls.equals("VX20")) return OVX20;
                     if (svls.equals("VX30")) return OVX30;
                     if (svls.equals("VX40")) return OVX40;
                     if (svls.equals("VY00")) return OGY;    // synonym
                     if (svls.equals("VY01")) return OVY01;
                     if (svls.equals("VY02")) return OVY02;
                     if (svls.equals("VY03")) return OVY03;
                     if (svls.equals("VY04")) return OVY04;
                     if (svls.equals("VY10")) return OVY10;
                     if (svls.equals("VY11")) return OVY11;
                     if (svls.equals("VY12")) return OVY12;
                     if (svls.equals("VY13")) return OVY13;
                     if (svls.equals("VY20")) return OVY20;
                     if (svls.equals("VY21")) return OVY21;
                     if (svls.equals("VY22")) return OVY22;
                     if (svls.equals("VY30")) return OVY30;
                     if (svls.equals("VY31")) return OVY31;
                     if (svls.equals("VY40")) return OVY40;
                     return ABSENT;


                    /***************************************
                    switch (c1up)
                     {
                        case 'X': switch(c2up)
                                  {
                                     case '1': return OVLX10;
                                     case '2': return OVLX20;
                                     case '3': return OVLX30;
                                     default: return ABSENT;
                                  }
                        case 'Y': switch (c2up)
                                  {
                                    case '0':  switch(c3up)
                                               {
                                                   case '1': return OVLY01;
                                                   case '2': return OVLY02;
                                                   case '3': return OVLY03;
                                                   default: return ABSENT;
                                                }
                                    case '1': switch(c3up)
                                              {
                                                  case '0': return OVLY10;
                                                  case '1': return OVLY11;
                                                  case '2': return OVLY12;
                                                  default: return ABSENT;
                                              }
                                     case '2': switch (c3up)
                                               {
                                                   case '0': return OVLY20;
                                                   case '1': return OVLY21;
                                                   default: return ABSENT;
                                                }
                                     case '3': if (c3up == '0') return OVLY30;
                                  }
                     }
                     return ABSENT;
                     ********************************/



          case 'W':
          case 'w':  if (c1up=='S')
                       return OWSPIDER;
                     else return ABSENT;

          case 'X':
          case 'x':  return OX;

          case 'Y':
          case 'y':  return OY;

          case 'Z':
          case 'z':  if (c1up=='E')  // e.g.Zern6
                     {
                        int i = U.suckInt(s);
                        return ((i>=0) && (i<36)) ? OZ00+i : ABSENT;
                     }
                     else return OZ;

          default:   return ABSENT;
        }
    } //---end of getOptFieldAttrib----------

    void setupDefaults() {
        //-------------set up default surface data--------------

        Globals.giFlags[OMEDIANEEDED] = FALSE; // ok=notNeeded; TRUE=needed

        for (int j=1; j<=MAXSURFS; j++)
        {
            oglasses[j] = "";
            for (int ia=0; ia<ONPARMS; ia++)
                rt13.surfs[j][ia] = -0.0; // minus zero means blank entry.

            rt13.surfs[j][OREFRACT] = 1.0;
        }

        for (int f=0; f<MAXFIELDS; f++)
            headers[f] = "";

        for (int r=0; r<JMAX; r++)
        {
            typetag[r] = ':';
            for (int f=0; f<MAXFIELDS; f++)
                cTags[r][f] = ':';
        }
    }


    void parseHeadingLine() {
        //--build the two one-way lookup tables for field IDs-------

        for (int i=0; i<ONPARMS; i++)   // ABSENT = -1
            oI2F[i] = ABSENT;

        for (int f=0; f<MAXFIELDS; f++) // ABSENT = -1
            oF2I[f] = ABSENT;

        int ntries=0, nunrecognized=0;
        for (int f=0; f<nfields; f++)
        {
            ntries++;
            int iatt = OPTDataModel.getOptFieldAttrib(headers[f]); // bottom of this file...
            oF2I[f] = iatt;
            if ((iatt > ABSENT) && (iatt < ONPARMS))
                oI2F[iatt] = f;
            else
                nunrecognized++;  // unused.
        }
    }

    void parseOpticType() {
        //-----first parse the optics type column---------

        int ifield = oI2F[OTYPE];
        if (ifield > ABSENT)
            for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            {
                String s = getFieldTrim(ifield, 2+jsurf);
                char c0 = U.getCharAt(s, 0);
                char c2 = U.getCharAt(s.toUpperCase(), 2);
                char c4 = U.getCharAt(s.toUpperCase(), 4);
                switch(c0)
                {
                    case 'b': // bimodal lens front="bif" "bir" "bib" "bim"
                    case 'B': if (c2 == 'F')
                        rt13.surfs[jsurf][OTYPE]  = OTBLFRONT;
                        if ((c2 == 'R') || (c2 == 'B'))
                            rt13.surfs[jsurf][OTYPE] = OTBLBACK;
                        if (c2 == 'M')
                            rt13.surfs[jsurf][OTYPE] = OTBMIRROR;
                        if (c2 == 'T')
                            rt13.surfs[jsurf][OTYPE] = OTTERMINATE;
                        break;

                    case 'c':     // coordinate breaks
                    case 'C': if (c2 == 'I')
                    {
                        rt13.surfs[jsurf][OTYPE] = OTCBIN;
                        break;
                    }
                        if (c2 == 'O')
                        {
                            rt13.surfs[jsurf][OTYPE] = OTCBOUT;
                            break;
                        }
                        break;

                    case 'i': // iris
                    case 'I': rt13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTIRISARRAY : OTIRIS; break;

                    case 'G': rt13.surfs[jsurf][OTYPE]
                            = (c2=='C') ? OTGSCATTER : OTMIRROR; break;  // A195

                    case 'l':
                    case 'L': rt13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTLENSARRAY : OTLENS; break;

                    case 'm':
                    case 'M': rt13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTMIRRARRAY : OTMIRROR; break;

                    // phantom is just a refracting surface with equal indices.

                    case 'r':
                    case 'R': rt13.surfs[jsurf][OTYPE] = OTRETRO; break;

                    // case 's':  // spider: replaced by iris with legs.

                    case 's':
                    case 'S':  rt13.surfs[jsurf][OTYPE] = OTGSCATTER; break;  // A195 Gaussian scatter

                    case 't':
                    case 'T':  rt13.surfs[jsurf][OTYPE] = OTTERMINATE; break;  // Term is alternate to BiTerm

                    case 'u':
                    case 'U':  rt13.surfs[jsurf][OTYPE] = OTUSCATTER; break;  // A195 uniform scatter

                    case 'd':     // optical path distorters
                    case 'D':
                    case 'w':
                    case 'W': rt13.surfs[jsurf][OTYPE] = OTDISTORT; break;


                    default: rt13.surfs[jsurf][OTYPE] = OTLENS; break;
                }
                typetag[jsurf] = getTag(ifield, 2+jsurf);
            };
    }

    void parseOpticFormColumn() {
        //--------parse the optics forms column----------

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            rt13.surfs[jsurf][OFORM] = OFELLIP;  // default

        int ifield = oI2F[OFORM];
        if (ifield > ABSENT)
            for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            {
                //---enforce idea of all arrays rectangular-------
                int i = (int) rt13.surfs[jsurf][OTYPE];
                boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));

                String s = getFieldTrim(ifield, 2+jsurf);
                char c0 = U.getCharAt(s, 0);
                char c1 = U.getCharAt(s, 1);
                rt13.surfs[jsurf][OFORM] = OFELLIP;
                if ((c0=='s') || (c1=='s'))
                    rt13.surfs[jsurf][OFORM] += OFIRECT;
                if ((c0=='S') || (c1=='S') || bArray)
                    rt13.surfs[jsurf][OFORM] += OFORECT;
            }
    }

    void parseRefractionData() {
        //----------refraction: sometimes numerical data--------------
        //---if refraction LUT is needed, OREFRACT will be NaN.----

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            oglasses[jsurf] = "";       // default: all numeric

        boolean bAllRefractNumeric = true;       // default: true;

        int ifield = oI2F[OREFRACT];
        if (ifield > ABSENT)
            for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            {
                oglasses[jsurf] = getFieldTrim(ifield, 2+jsurf);
                rt13.surfs[jsurf][OREFRACT] = U.suckDouble(oglasses[jsurf]);
                if (Double.isNaN(rt13.surfs[jsurf][OREFRACT]))
                    bAllRefractNumeric = false;
                if (0.0 == rt13.surfs[jsurf][OREFRACT])
                    rt13.surfs[jsurf][OREFRACT] = 1.0;
            }
        Globals.giFlags[OMEDIANEEDED] = bAllRefractNumeric ? FALSE : TRUE;
    }

    int parseNumericData() {
        //-------Now get numerical data records-------------------
        //---except ABSENT, OFORM, OTYPE, OREFRACT, OGROUP-----

        boolean bAllOtherNumeric = true;
        int badline=0, badfield=0, osyntaxerr=0;
        double d;
        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            for (int f=0; f<nfields; f++)
            {
                int ia = oF2I[f];   // attribute of this surface

                if (ia == OTYPE)    // types were analyzed above...
                    continue;
                if (ia == OFORM)    // forms were analyzed above...
                    continue;
                if (ia == OREFRACT) // refraction analyzed above...
                    continue;
                if (ia == OGROUP)   // group analyzed above...
                    continue;
                if (ia > ABSENT)    // all numerical fields can overwrite negZero here.
                {
                    // first, fill in the datum....
                    d = rt13.surfs[jsurf][ia] = getFieldDouble(f, 2+jsurf);

                    // then check for trouble and correct it....
                    if (U.isNegZero(d) && isAdjustable(jsurf, ia))
                        rt13.surfs[jsurf][ia] = +0.0;         // active

                    // d = value, or NaN, or -0.0=unused, or +0.0=in use.
                    // now, U.NegZero(d) indicates no use whatsoever.

                    if (Double.isNaN(d))
                    {
                        bAllOtherNumeric = false;
                        badline = jsurf+2;
                        badfield = f;
                        osyntaxerr = badfield + 100*badline;
                        break;
                    }

                    // allow defined shape to determine asphericity...
                    if ((ia == OSHAPE) && !U.isNegZero(d))
                        rt13.surfs[jsurf][OASPHER] = d - 1.0;

                    // allow defined radii of curvature to determine curvature...
                    if ((ia == ORAD) && (d != 0.0))
                        rt13.surfs[jsurf][OCURVE] = 1.0/d;
                    if ((ia == ORADX) && (d != 0.0))
                        rt13.surfs[jsurf][OCURVX] = 1.0/d;
                    if ((ia == ORADY) && (d != 0.0))
                        rt13.surfs[jsurf][OCURVY] = 1.0/d;
                }
            }
            if (osyntaxerr > 0)
                break;
        }

        Globals.giFlags[OSYNTAXERR] = osyntaxerr;
        return osyntaxerr;
    }


    void setDefaultOpticType() {
        //----gather individual surface types: nonnumerical data-------
        //----a grating is not a type: it's a groovy mirror or lens----
        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            rt13.surfs[jsurf][OTYPE] = OTLENS;  // default
        }
    }

    void parseAdjustables() {
        //----data are now cleansed stashed & indexed------------
        //-------Perform all the post-parse cleanup here------------

        Globals.giFlags[ONADJ] = iParseAdjustables(nsurfs);

        //----force all CoordBreaks to be planar? or not? rev 168----
        // for (int j=1; j<nsurfs; j++)
        //   if ((RT13.surfs[j][OTYPE]==OTCBIN) || (RT13.surfs[j][OTYPE]==OTCBOUT))
        //   {
        //       RT13.surfs[j][OPROFILE] = OSPLANO;
        //       for (int iatt=OCURVE; iatt<=OZ35; iatt++)
        //         RT13.surfs[j][iatt] = 0.0;
        //   }
    }

    void parseDiameters() {
        //-------evaluate diameters DIAX, DIAY-------------------
        for (int j = 1; j <= nsurfs; j++) {
            boolean bM = rt13.surfs[j][OIDIAM] > 0.0;
            boolean bX = rt13.surfs[j][OIDIAX] > 0.0;
            boolean bY = rt13.surfs[j][OIDIAY] > 0.0;
            if (!bX) {
                if (bM)
                    rt13.surfs[j][OIDIAX] = rt13.surfs[j][OIDIAM];
                else if (bY)
                    rt13.surfs[j][OIDIAX] = rt13.surfs[j][OIDIAY];
            }
            if (!bY) {
                if (bM)
                    rt13.surfs[j][OIDIAY] = rt13.surfs[j][OIDIAM];
                else if (bX)
                    rt13.surfs[j][OIDIAY] = rt13.surfs[j][OIDIAX];
            }
            bM = rt13.surfs[j][OODIAM] > 0.0;
            bX = rt13.surfs[j][OODIAX] > 0.0;
            bY = rt13.surfs[j][OODIAY] > 0.0;
            if (!bX) {
                if (bM)
                    rt13.surfs[j][OODIAX] = rt13.surfs[j][OODIAM];
                else if (bY)
                    rt13.surfs[j][OODIAX] = rt13.surfs[j][OODIAY];
            }
            if (!bY) {
                if (bM)
                    rt13.surfs[j][OODIAY] = rt13.surfs[j][OODIAM];
                else if (bX)
                    rt13.surfs[j][OODIAY] = rt13.surfs[j][OODIAX];
            }
        }
        boolean bAllDiamsPresent = true;
        for (int j = 1; j <= nsurfs; j++) {
            boolean bX = rt13.surfs[j][OODIAX] > 0.0;
            boolean bY = rt13.surfs[j][OODIAY] > 0.0;
            if (!bX || !bY)
                bAllDiamsPresent = false;
        }
        Globals.giFlags[OALLDIAMSPRESENT] = bAllDiamsPresent ? TRUE : FALSE; // ints!
    }

    void testGroovyness() {
        //------------Test each surface for groovyness----------------
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bGroovy = false;
            for (int kg=OORDER; kg<OGROOVY; kg++)
                if (rt13.surfs[j][kg] != 0.0)
                    bGroovy = true;
            rt13.surfs[j][OGROOVY] = bGroovy ? 1.0 : 0.0;
        }
    }

    void verifyArrayDims() {
        //---------verify that array diams are within cells------------
        for (int j=1; j<nsurfs; j++)
        {
            int i = (int) rt13.surfs[j][OTYPE];
            boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));
            if (bArray)
            {
                double diax = rt13.surfs[j][OODIAX];
                if (U.isNegZero(diax))
                    diax = rt13.surfs[j][OODIAM];
                int nx = (int) rt13.surfs[j][ONARRAYX];
                if ((diax<=TOL) || (nx<1))
                    continue; // continue, not return!
                double px = diax/nx;
                if(rt13.surfs[j][OIDIAX] > diax/nx)
                    rt13.surfs[j][OIDIAX] = diax/nx;

                double diay = rt13.surfs[j][OODIAM];
                int ny = (int) rt13.surfs[j][ONARRAYY];
                if ((diay<=TOL) || (ny<1))
                    continue; // continue, not return!
                double py = diay/ny;
                if (rt13.surfs[j][OIDIAM] > diay/ny)
                    rt13.surfs[j][OIDIAM] = diay/ny;

                if (nx*ny > MAXHOLES)
                    rt13.surfs[j][ONARRAYY] = (MAXHOLES)/nx;
            }
        }
    }

    void classifyProfileSolvers() {
        //---------classify each surface profile for solvers------------
        //---CX cyl & torics: ternary logic. See MNOTES May 25 2007-----
        //
        //              C=blank,   zero,  nonzero
        //             ---------  ------  -------
        //   CX=blank:   PLANO     PLANO   CONIC
        //    CX=zero:   PLANO     PLANO   CYCYL
        // CX=nonzero:   CXCYL     CXCYL   TORIC
        //
        // Adjustability:  see below.
        // Special case added in A119 Dec 2010:
        //    CX=nonblank and CY=nonblank: OSBICONIC
        //
        //  TERNARY LOGIC: see lines 453-463.

        boolean badZern = false;     // flag for single warning message at end

        for (int j=1; j<=nsurfs; j++)
        {
            double  ce = rt13.surfs[j][OCURVE];
            double  cx = rt13.surfs[j][OCURVX];
            double  cy = rt13.surfs[j][OCURVY];
            double  ae = rt13.surfs[j][OASPHER];
            double  ax = rt13.surfs[j][OASPHX];
            double  ay = rt13.surfs[j][OASPHY];

            //---TERNARY LOGIC EVALUATOR starts here-----
            //---three states: empty field, entry=0, entry is nonzero------
            //---Determined by Curv and Cx; Cy has no influence---------

            boolean bCEactive = (ce!=0.0) || isAdjustable(j, OCURVE);
            boolean bCXactive = (cx!=0.0) || isAdjustable(j, OCURVX);
            int tce = bCEactive ? 2 : U.isNegZero(ce) ? 0 : 1;   // 0, 1, or 2.
            int tcx = bCXactive ? 2 : U.isNegZero(cx) ? 0 : 1;   // 0, 1, or 2.
            int tg[] = { OSPLANO, OSPLANO, OSCONIC, OSPLANO, OSPLANO, OSYCYL, OSXCYL,  OSXCYL,  OSTORIC};
            int arg = tce + 3*tcx;
            int iProfile = tg[arg];

            // String osnames[] = {"OSPLANO", "OSPLANO", "OSCONIC", "OSPLANO", "OSPLANO", "OSYCYL", "OSXCYL", "OSXCYL", "OSTORIC"};
            // if (j==1)
            //   System.out.println("OEJIF ternary logic result: iProfile = "+iProfile+"  "+osnames[arg]);

            // Rules, A190:
            // PolyCyl: requires axis=x, Cx=0 (uncurved in XZ plane), Curve=Nonzero, poly in y: OSYCYL.
            // CircCyl: can have axis=x, Cx=0 (uncurved in XZ plane), Curve=Nonzero, no poly;  OSYCYL.
            // CircCyl: or have  axis=y, Cx=nonzero, Curve=blank or zero, no poly terms:  OSXCYL
            // Toric:   requires Curve=nonzero, Cx=nonzero.
            //
            //----ternary logic evaluator ends here-----

            //----test for biconic---------------------
            //--this can overwrite the ternary logic----

            boolean bBCXactive = !U.isNegZero(cx) || isAdjustable(j,OCURVX);
            boolean bBCYactive = !U.isNegZero(cy) || isAdjustable(j,OCURVY);
            boolean bBAXactive = !U.isNegZero(ax) || isAdjustable(j,OASPHX);
            boolean bBAYactive = !U.isNegZero(ay) || isAdjustable(j,OASPHY);
            if (bBCXactive && bBCYactive && bBAXactive && bBAYactive)
                iProfile = OSBICONIC;

            //-------polynomial----------

            boolean bPoly = false;
            for (int i=OA1; i<=OA14; i++)
                if ((0 != rt13.surfs[j][i]) || isAdjustable(j,i))
                    bPoly = true;

            //----Zernike flag and diameter test  -----------------
            //---CoordinateBreaks set zernikes to zero, not -0  -----
            //---here it is important to accept zeros---------------

            boolean bZern = false;
            for (int i=OZ00; i<=OZ35; i++)
                if ((0 != rt13.surfs[j][i]) || isAdjustable(j,i))
                    bZern = true;

            if (bZern && (rt13.surfs[j][OODIAM] == 0.0))
                badZern = true; // gives warning "Zernikes require Diameters"

            //-------upgrade if poly or zern is present--------

            if (bPoly)
                switch (iProfile)
                {
                    case OSPLANO:  iProfile = OSPOLYREV; break;
                    case OSCONIC:  iProfile = OSPOLYREV; break;
                    case OSXCYL:   iProfile = OSTORIC; break;
                    case OSYCYL:   iProfile = OSTORIC; break;
                    case OSTORIC:  iProfile = OSTORIC; break;
                }

            if (bZern)
                iProfile = (iProfile==OSTORIC) ? OSZERNTOR : OSZERNREV;

            if (rt13.surfs[j][ORGAUSS] > 0.)  // found a Gaussian surface profile
            {
                iProfile = OSGAUSS;
            }
            //---------apply hints to conic or cyl, not higher----------

            switch (iProfile)
            {
                case OSCONIC:
                    if ('<' == typetag[j])  iProfile = OSCONICLT;
                    if ('>' == typetag[j])  iProfile = OSCONICGT;
                    break;
                case OSXCYL:
                    if ('<' == typetag[j])  iProfile = OSXCYLLT;
                    if ('>' == typetag[j])  iProfile = OSXCYLGT;
                    break;
                case OSYCYL:
                    if ('<' == typetag[j])  iProfile = OSYCYLLT;
                    if ('>' == typetag[j])  iProfile = OSYCYLGT;
                    break;
            }
            // System.out.println("OEJIF parse() surface= "+j+" finds iProfile= "+iProfile+"  "+sProfiles[iProfile]);

            rt13.surfs[j][OPROFILE] = iProfile;
        }
        if (badZern)
            System.err.println("Zernikes without Diameter are ignored.");
//          JOptionPane.showMessageDialog(this, "Zernikes without Diameter are ignored.");
    }

    void calcDOsize() {
        //---------feel out dOsize for getDelta()----------

        dOsize = 0.0;
        for (int j=1; j<=nsurfs; j++)
        {
            dOsize = Math.max(dOsize, Math.abs(rt13.surfs[j][OX]));
            dOsize = Math.max(dOsize, Math.abs(rt13.surfs[j][OY]));
            dOsize = Math.max(dOsize, Math.abs(rt13.surfs[j][OZ]));
            dOsize = Math.max(dOsize, Math.abs(rt13.surfs[j][OODIAM]));
            dOsize = Math.max(dOsize, Math.abs(rt13.surfs[j][OODIAX]));
        }
        if (dOsize < TOL)
            dOsize = 1.0;
    }

    void parseHeaders() {
        //----get headers using getFieldTrim()---------------
        for (int f=0; f<nfields; f++)
            headers[f] = getFieldTrim(f, 1);
    }

    // replaces the abstract parse() in EJIF.
    // This is NOT PRIVATE; DMF:vMasterParse calls it, triggered by blinker, etc.
    @Override
    public void parse() {

        adjustables = new ArrayList<Adjustment>();

        // First, communicate EJIF results to DMF.giFlags[]
        // vPreParse() takes care of parsing title line.

        int status[] = new int[NGENERIC];
        vPreParse(status);
        Globals.giFlags[OPRESENT] = status[GPRESENT];
        Globals.giFlags[ONLINES]  = status[GNLINES];
        Globals.giFlags[ONSURFS]  = nsurfs = status[GNRECORDS];
        Globals.giFlags[ONFIELDS] = nfields = status[GNFIELDS];
        if (nsurfs < 1)
          return;

        setupDefaults();
        parseHeaders();
        parseHeadingLine();
        setDefaultOpticType();
        parseOpticType();
        parseOpticFormColumn();
        parseRefractionData();
        if (parseNumericData() < 0)
            // syntax error
            return;

        //----data are now cleansed stashed & indexed------------
        //-------Perform all the post-parse cleanup here------------
        parseAdjustables();
        parseDiameters();
        //------------set the Euler angle matrix------------------
        rt13.setEulers();
        testGroovyness();
        verifyArrayDims();
        classifyProfileSolvers();
        calcDOsize();
    }

    //-----------public functions for AutoAdjust------------
    //-----Now that Adjustment is a public class,
    //-----cannot Auto get its own data?----------------
    //-----Nope. ArrayList adjustments is private.----------
    //
    //---Yikes, sometimes at startup adjustables is all -1 even with good adjustables.
    //-----What should initialize adjustables??


    public double getOsize()
    // called ONLY by DMF, in support of its static method.
    {
        return dOsize;
    }

    public double getAdjValue(int i)
    // Fetch appropriate value from RT13.surfs[][].
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables!=null) && (i>=0) && (i<adjustables.size()))
       {
           int jsurf = adjustables.get(i).getRecord();
           int iattr = adjustables.get(i).getAttrib();
           if ((jsurf>0) && (jsurf<=nsurfs) && (iattr>=0) && (iattr<OFINALADJ))
             return rt13.surfs[jsurf][iattr];
       }
       return 0.0;
    }

    public int getAdjAttrib(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getAttrib();
       else
         return -1;
    }

    public int getAdjSurf(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getRecord();
       else
         return -1;
    }

    public int getAdjField(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getField();
       else
         return -1;
    }

    public ArrayList<Integer> getSlaves(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getList();
       else
         return null;
    }

    //-------------private stuff----------------



    private int iParseAdjustables(int nsurfs)
    // fills in private ArrayList of adjustables, with slaves.
    // Returns how many groups were found based on tags.
    {
        boolean bLookedAt[] = new boolean[nsurfs+1];
        adjustables.clear();
        for (int field=0; field<nfields; field++)
        {
            int attrib = oF2I[field];
            if ((attrib<0) || (attrib>OFINALADJ))  // or other validity test
                continue;

            for (int record=1; record<=nsurfs; record++)
                bLookedAt[record] = false;

            for (int record=1; record<=nsurfs; record++)
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
                    for (int k=record+1; k<=nsurfs; k++)
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
                adjustables.add(new Adjustment(attrib, record, field, slaves));
            } // done with all groups in this field
        }// done with all fields
        return adjustables.size();
    }

    boolean isAdjustable(int jsurf, int iatt)
    // Tests for range of adjustable attributes & tag chars.
    // Assumes that oI2F[] and cTags[][] are properly set.
    // HOWEVER THIS IS DEAF TO THE NEW GANGED PARADIGM.
    {
        if ((iatt < 0) || (iatt > OFINALADJ))
            return false;
        int field = oI2F[iatt];
        if ((field < 0) || (field >= nfields))
            return false;
        char c = getTag(field, jsurf+2);  // cTags[jsurf][field];
        return isAdjustableTag(c);
    }

    boolean isAdjustableTag(char c)
    {
        return (c=='?') || Character.isLetter(c);
    }

}
