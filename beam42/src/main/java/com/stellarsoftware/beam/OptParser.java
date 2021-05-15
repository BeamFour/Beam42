package com.stellarsoftware.beam;

import java.util.*;

import static com.stellarsoftware.beam.B4constants.*;

public class OptParser {

    Map<Integer, Integer> head2columns = new LinkedHashMap<>();
    Map<Integer, Integer> column2head = new LinkedHashMap<>();
    int nsurfs;
    String description;
    List<String[]> data = new ArrayList<>();
    ArrayList<String> oglasses = new ArrayList<>();
    ArrayList<Adjustment> adjustables = new ArrayList<Adjustment>();
    char typetag[] = new char[JMAX]; // FIXME not yet populated
    double dOsize = 0.0;

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
          return B4constants.ABSENT;
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
                          case '0': return B4constants.OA10;
                          case '1': return B4constants.OA11;
                          case '2': return B4constants.OA12;
                          case '3': return B4constants.OA13;
                          case '4': return B4constants.OA14;
                          case '5':
                          case '6':
                          case '7':
                          case '8':
                          case '9': return B4constants.ABSENT;
                          default: return B4constants.OA1;
                       }
                       case '2': return B4constants.OA2;
                       case '3': return B4constants.OA3;
                       case '4': return B4constants.OA4;
                       case '5': return B4constants.OA5;
                       case '6': return B4constants.OA6;
                       case '7': return B4constants.OA7;
                       case '8': return B4constants.OA8;
                       case '9': return B4constants.OA9;
                       case 'C': return OTYPE; // "ACTION"
                    }
                    if (s.endsWith("X"))
                      return B4constants.OASPHX;
                    if (s.endsWith("Y") && (len < 9)) // allows 'asphericity'
                      return B4constants.OASPHY;
                    return B4constants.OASPHER;

          case 'C':
          case 'c': if (s.contains("X"))  // curvatures
                      return B4constants.OCURVX;
                    if (s.contains("Y"))
                      return B4constants.OCURVY;
                    return B4constants.OCURVE;

          case 'D': if (s.contains("X"))
                      return B4constants.OODIAX;
                    if (s.contains("Y"))
                      return B4constants.OODIAY;
                    return B4constants.OODIAM;

          case 'd': if (s.contains("X"))
                      return B4constants.OIDIAX;
                    if (s.contains("Y"))
                      return B4constants.OIDIAY;
                    return B4constants.OIDIAM;

          case 'F':
          case 'f': return B4constants.OFORM;  // "form" = nonnumerical

          case 'G':
          case 'g': switch(c1up)   // Group or Grating groove density
                    {
                       case 'R': return B4constants.OGROUP;
                       case 'X': return B4constants.OGX;
                       case 'Y': return B4constants.OGY;
                       // case '1': return OVLS1;    // removed 22 March 2016 A192
                       // case '2': return OVLS2;
                       // case '3': return OVLS3;
                       // case '4': return OVLS4;
                    }
                    return B4constants.ABSENT;

          case 'H':
          case 'h': if (len < 4)
                return B4constants.ABSENT;
                if (c1up=='G')
                   return B4constants.OHGAUSS;  // height of 2D Gaussian

                switch(c3up)         // HOE entries
                {
                    case 'L':
                    case 'l': return B4constants.OHOELAM;
                    case 'X':
                    case 'x': if (c4up == '1')
                                return B4constants.OHOEX1;
                              if (c4up == '2')
                                return B4constants.OHOEX2;
                              return B4constants.ABSENT;
                    case 'Y':
                    case 'y': if (c4up == '1')
                                return B4constants.OHOEY1;
                              if (c4up == '2')
                                return B4constants.OHOEY2;
                              return B4constants.ABSENT;
                    case 'Z':
                    case 'z': if (c4up == '1')
                                return B4constants.OHOEZ1;
                              if (c4up == '2')
                                return B4constants.OHOEZ2;
                              return B4constants.ABSENT;
                }
                return B4constants.ABSENT;

          case 'I':
          case 'i': return B4constants.OREFRACT;  // refractive index or glass name

          case 'L':
          case 'l':
          case 'M':
          case 'm':  return OTYPE;  // "Lens" "mirror" etc

          case 'N':
          case 'n':  if (c1up=='S')
                       return B4constants.ONSPIDER;
                     if (c1up=='X')
                       return B4constants.ONARRAYX;
                     if (c1up=='Y')
                       return B4constants.ONARRAYY;
                     return B4constants.ABSENT;

          case 'O':  // OffIX, OffIY, OffOX, OffOY
          case 'o':  if ((c3up=='O') && (c4up=='X')) return B4constants.OFFOX;
                     if ((c3up=='O') && (c4up=='Y')) return B4constants.OFFOY;
                     if ((c3up=='I') && (c4up=='X')) return B4constants.OFFIX;
                     if ((c3up=='I') && (c4up=='Y')) return B4constants.OFFIY;
                     return B4constants.OORDER;

          case 'P':
          case 'p':  return B4constants.OPITCH;

          case 'R':
          case 'r':  if (c1up=='G') return B4constants.ORGAUSS;   // 2D Gaussian radius or sigma
                     if ((c2up=='C') && (c3up=='X')) return B4constants.ORADX;
                     if ((c2up=='C') && (c3up=='Y')) return B4constants.ORADY;
                     if (c2up=='C') return B4constants.ORAD;
                     return B4constants.OROLL;

          case 'S':
          case 's':  if (c1up=='C')  return B4constants.OSCATTER;  // angle, degrees
                     return B4constants.OSHAPE;

          case 'T':
          case 't':  if (c1up == 'Y')  return OTYPE;
                     return B4constants.OTILT;     // tilt.

          case 'V':   // twenty curl-free explicit VLS coefficients
          case 'v':
                     if (svls.equals("VX00")) return B4constants.OGX;    // synonym
                     if (svls.equals("VX10")) return B4constants.OVX10;
                     if (svls.equals("VX20")) return B4constants.OVX20;
                     if (svls.equals("VX30")) return B4constants.OVX30;
                     if (svls.equals("VX40")) return B4constants.OVX40;
                     if (svls.equals("VY00")) return B4constants.OGY;    // synonym
                     if (svls.equals("VY01")) return B4constants.OVY01;
                     if (svls.equals("VY02")) return B4constants.OVY02;
                     if (svls.equals("VY03")) return B4constants.OVY03;
                     if (svls.equals("VY04")) return B4constants.OVY04;
                     if (svls.equals("VY10")) return B4constants.OVY10;
                     if (svls.equals("VY11")) return B4constants.OVY11;
                     if (svls.equals("VY12")) return B4constants.OVY12;
                     if (svls.equals("VY13")) return B4constants.OVY13;
                     if (svls.equals("VY20")) return B4constants.OVY20;
                     if (svls.equals("VY21")) return B4constants.OVY21;
                     if (svls.equals("VY22")) return B4constants.OVY22;
                     if (svls.equals("VY30")) return B4constants.OVY30;
                     if (svls.equals("VY31")) return B4constants.OVY31;
                     if (svls.equals("VY40")) return B4constants.OVY40;
                     return B4constants.ABSENT;


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
                       return B4constants.OWSPIDER;
                     else return B4constants.ABSENT;

          case 'X':
          case 'x':  return B4constants.OX;

          case 'Y':
          case 'y':  return B4constants.OY;

          case 'Z':
          case 'z':  if (c1up=='E')  // e.g.Zern6
                     {
                        int i = U.suckInt(s);
                        return ((i>=0) && (i<36)) ? B4constants.OZ00+i : B4constants.ABSENT;
                     }
                     else return B4constants.OZ;

          default:   return B4constants.ABSENT;
        }
    } //---end of getOptFieldAttrib----------

    String[] splitLine(String line) {
        List<String> words = new ArrayList<>();
        while (line.length() > 0) {
            int pos = line.indexOf(':');
            if (pos < 0) {
                words.add(line);
                break;
            } else if (pos == 0) {
                words.add("");
                line = line.substring(1);
            } else {
                words.add(line.substring(0, pos));
                line = line.substring(pos + 1);
            }
        }
        return words.toArray(new String[words.size()]);
    }

    void parseCountAndDescriptionLine(String line) {
        int index = line.indexOf(' ');
        String countPart = line;
        if (index > 0) {
            countPart = line.substring(0, index+1);
            description = line.substring(index+1);
        }
        nsurfs = U.suckInt(countPart);
    }

    void parseHeadingLine(String line) {
        String[] fields = splitLine(line);
        for (int i = 0; i < fields.length; i++) {
            int h = getOptFieldAttrib(fields[i]);
            if (h == B4constants.OABSENT) {
                System.out.println("Heading [" + fields[i] + " not recognized; ignoring");
                continue;
            }
            head2columns.put(h, i);
            column2head.put(i, h);
        }
    }

    void parseDataLine(String line) {
        String[] fields = splitLine(line);
        data.add(fields);
    }

    String getFieldTrim(int ifield, int row) {
        String[] fields = data.get(row); // FIXME row offset
        if (ifield > fields.length && fields[ifield] != null)
            return fields[ifield].trim();
        return "";
    }

    // empty returns -0.0; badnum returns Double.NaN
    // U.suckDouble() includes trimming and -0 for empty.
    double getFieldDouble(int ifield, int row) {
        String[] fields = data.get(row); // FIXME row offset
        if (ifield > fields.length && fields[ifield] != null)
            return U.suckDouble(fields[ifield]);
        return -0.0;
    }

    int getNFields(int row) {
        // FXME row needs adjustment
        String[] fields = data.get(row);
        return fields.length;
    }

    void parseOpticType() {
        //-----first parse the optics type column---------

        int ifield = head2columns.get(OTYPE);
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
                        RT13.surfs[jsurf][OTYPE]  = OTBLFRONT;
                        if ((c2 == 'R') || (c2 == 'B'))
                            RT13.surfs[jsurf][OTYPE] = OTBLBACK;
                        if (c2 == 'M')
                            RT13.surfs[jsurf][OTYPE] = OTBMIRROR;
                        if (c2 == 'T')
                            RT13.surfs[jsurf][OTYPE] = OTTERMINATE;
                        break;

                    case 'c':     // coordinate breaks
                    case 'C': if (c2 == 'I')
                    {
                        RT13.surfs[jsurf][OTYPE] = OTCBIN;
                        break;
                    }
                        if (c2 == 'O')
                        {
                            RT13.surfs[jsurf][OTYPE] = OTCBOUT;
                            break;
                        }
                        break;

                    case 'i': // iris
                    case 'I': RT13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTIRISARRAY : OTIRIS; break;

                    case 'G': RT13.surfs[jsurf][OTYPE]
                            = (c2=='C') ? OTGSCATTER : OTMIRROR; break;  // A195

                    case 'l':
                    case 'L': RT13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTLENSARRAY : OTLENS; break;

                    case 'm':
                    case 'M': RT13.surfs[jsurf][OTYPE]
                            = (c4=='A') ? OTMIRRARRAY : OTMIRROR; break;

                    // phantom is just a refracting surface with equal indices.

                    case 'r':
                    case 'R': RT13.surfs[jsurf][OTYPE] = OTRETRO; break;

                    // case 's':  // spider: replaced by iris with legs.

                    case 's':
                    case 'S':  RT13.surfs[jsurf][OTYPE] = OTGSCATTER; break;  // A195 Gaussian scatter

                    case 't':
                    case 'T':  RT13.surfs[jsurf][OTYPE] = OTTERMINATE; break;  // Term is alternate to BiTerm

                    case 'u':
                    case 'U':  RT13.surfs[jsurf][OTYPE] = OTUSCATTER; break;  // A195 uniform scatter

                    case 'd':     // optical path distorters
                    case 'D':
                    case 'w':
                    case 'W': RT13.surfs[jsurf][OTYPE] = OTDISTORT; break;


                    default: RT13.surfs[jsurf][OTYPE] = OTLENS; break;
                }
                //FIXME typetag[jsurf] = getTag(ifield, 2+jsurf);
            };

    }

    void parseOpticFormColumn() {
        //--------parse the optics forms column----------

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            RT13.surfs[jsurf][OFORM] = OFELLIP;  // default

        int ifield = head2columns.get(OFORM);
        if (ifield > ABSENT)
            for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            {
                //---enforce idea of all arrays rectangular-------
                int i = (int) RT13.surfs[jsurf][OTYPE];
                boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));

                String s = getFieldTrim(ifield, 2+jsurf);
                char c0 = U.getCharAt(s, 0);
                char c1 = U.getCharAt(s, 1);
                RT13.surfs[jsurf][OFORM] = OFELLIP;
                if ((c0=='s') || (c1=='s'))
                    RT13.surfs[jsurf][OFORM] += OFIRECT;
                if ((c0=='S') || (c1=='S') || bArray)
                    RT13.surfs[jsurf][OFORM] += OFORECT;
            }
    }

    void parseRefractionData() {
        //----------refraction: sometimes numerical data--------------
        //---if refraction LUT is needed, OREFRACT will be NaN.----

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            oglasses.set(jsurf, "");       // default: all numeric

        boolean bAllRefractNumeric = true;       // default: true;

        int ifield = head2columns.get(OREFRACT);
        if (ifield > ABSENT)
            for (int jsurf=1; jsurf<=nsurfs; jsurf++)
            {
                oglasses.set(jsurf, getFieldTrim(ifield, 2+jsurf));
                RT13.surfs[jsurf][OREFRACT] = U.suckDouble(oglasses.get(jsurf));
                if (Double.isNaN(RT13.surfs[jsurf][OREFRACT]))
                    bAllRefractNumeric = false;
                if (0.0 == RT13.surfs[jsurf][OREFRACT])
                    RT13.surfs[jsurf][OREFRACT] = 1.0;
            }
        DMF.giFlags[OMEDIANEEDED] = bAllRefractNumeric ? FALSE : TRUE;
    }

    char getTag(int f, int r) {
        // FIXME
        return ' ';
    }

    boolean isAdjustable(int jsurf, int iatt)
    // Tests for range of adjustable attributes & tag chars.
    // Assumes that oI2F[] and cTags[][] are properly set.
    // HOWEVER THIS IS DEAF TO THE NEW GANGED PARADIGM.
    {
        if ((iatt < 0) || (iatt > OFINALADJ))
            return false;
        int field = head2columns.get(iatt);
        if ((field < 0) || (field >= getNFields(jsurf)))
            return false;
        char c = getTag(field, jsurf+2);  // cTags[jsurf][field];
        return isAdjustableTag(c);
    }


    boolean isAdjustableTag(char c)
    {
        return (c=='?') || Character.isLetter(c);
    }

    private int iParseAdjustables(int nsurfs)
    // fills in private ArrayList of adjustables, with slaves.
    // Returns how many groups were found based on tags.
    {
        boolean bLookedAt[] = new boolean[nsurfs+1];
        adjustables.clear();
        for (int field=0; field<head2columns.size(); field++) // nfields
        {
            int attrib = head2columns.get(field);
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
                                slaves.add(new Integer(iSlave));
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

    void parseNumericData() {
        //-------Now get numerical data records-------------------
        //---except ABSENT, OFORM, OTYPE, OREFRACT, OGROUP-----

        boolean bAllOtherNumeric = true;
        int badline=0, badfield=0, osyntaxerr=0;
        double d;
        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            int nfields = getNFields(jsurf);
            for (int f=0; f<nfields; f++)
            {
                int ia = column2head.get(f);   // attribute of this surface

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
                    d = RT13.surfs[jsurf][ia] = getFieldDouble(f, 2+jsurf);

                    // then check for trouble and correct it....
                    if (U.isNegZero(d) && isAdjustable(jsurf, ia))
                        RT13.surfs[jsurf][ia] = +0.0;         // active

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
                        RT13.surfs[jsurf][OASPHER] = d - 1.0;

                    // allow defined radii of curvature to determine curvature...
                    if ((ia == ORAD) && (d != 0.0))
                        RT13.surfs[jsurf][OCURVE] = 1.0/d;
                    if ((ia == ORADX) && (d != 0.0))
                        RT13.surfs[jsurf][OCURVX] = 1.0/d;
                    if ((ia == ORADY) && (d != 0.0))
                        RT13.surfs[jsurf][OCURVY] = 1.0/d;
                }
            }
            if (osyntaxerr > 0)
                break;
        }

        DMF.giFlags[OSYNTAXERR] = osyntaxerr;
        if (osyntaxerr > 0)
            throw new IllegalArgumentException("Syntax error");
    }


    void setDefaultOpticType() {
        //----gather individual surface types: nonnumerical data-------
        //----a grating is not a type: it's a groovy mirror or lens----
        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            RT13.surfs[jsurf][OTYPE] = OTLENS;  // default
        }
    }

    void parseAdjustables() {
        //----data are now cleansed stashed & indexed------------
        //-------Perform all the post-parse cleanup here------------

        DMF.giFlags[ONADJ] = iParseAdjustables(nsurfs);
    }

    void parseDiameters() {
        //-------evaluate diameters DIAX, DIAY-------------------
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bM = RT13.surfs[j][OIDIAM] > 0.0;
            boolean bX = RT13.surfs[j][OIDIAX] > 0.0;
            boolean bY = RT13.surfs[j][OIDIAY] > 0.0;
            if (!bX)
            {
                if (bM)
                    RT13.surfs[j][OIDIAX] = RT13.surfs[j][OIDIAM];
                else if (bY)
                    RT13.surfs[j][OIDIAX] = RT13.surfs[j][OIDIAY];
            }
            if (!bY)
            {
                if (bM)
                    RT13.surfs[j][OIDIAY] = RT13.surfs[j][OIDIAM];
                else if (bX)
                    RT13.surfs[j][OIDIAY] = RT13.surfs[j][OIDIAX];
            }
            bM = RT13.surfs[j][OODIAM] > 0.0;
            bX = RT13.surfs[j][OODIAX] > 0.0;
            bY = RT13.surfs[j][OODIAY] > 0.0;
            if (!bX)
            {
                if (bM)
                    RT13.surfs[j][OODIAX] = RT13.surfs[j][OODIAM];
                else if (bY)
                    RT13.surfs[j][OODIAX] = RT13.surfs[j][OODIAY];
            }
            if (!bY)
            {
                if (bM)
                    RT13.surfs[j][OODIAY] = RT13.surfs[j][OODIAM];
                else if (bX)
                    RT13.surfs[j][OODIAY] = RT13.surfs[j][OODIAX];
            }
        }
        boolean bAllDiamsPresent = true;
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bX = RT13.surfs[j][OODIAX] > 0.0;
            boolean bY = RT13.surfs[j][OODIAY] > 0.0;
            if (!bX || !bY)
                bAllDiamsPresent = false;
        }
        DMF.giFlags[OALLDIAMSPRESENT] = bAllDiamsPresent ? TRUE : FALSE; // ints!
    }

    void testGroovyness() {
        //------------Test each surface for groovyness----------------
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bGroovy = false;
            for (int kg=OORDER; kg<OGROOVY; kg++)
                if (RT13.surfs[j][kg] != 0.0)
                    bGroovy = true;
            RT13.surfs[j][OGROOVY] = bGroovy ? 1.0 : 0.0;
        }
    }

    void verifyArrayDims() {
        //---------verify that array diams are within cells------------
        for (int j=1; j<nsurfs; j++)
        {
            int i = (int) RT13.surfs[j][OTYPE];
            boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));
            if (bArray)
            {
                double diax = RT13.surfs[j][OODIAX];
                if (U.isNegZero(diax))
                    diax = RT13.surfs[j][OODIAM];
                int nx = (int) RT13.surfs[j][ONARRAYX];
                if ((diax<=TOL) || (nx<1))
                    continue; // continue, not return!
                double px = diax/nx;
                if(RT13.surfs[j][OIDIAX] > diax/nx)
                    RT13.surfs[j][OIDIAX] = diax/nx;

                double diay = RT13.surfs[j][OODIAM];
                int ny = (int) RT13.surfs[j][ONARRAYY];
                if ((diay<=TOL) || (ny<1))
                    continue; // continue, not return!
                double py = diay/ny;
                if (RT13.surfs[j][OIDIAM] > diay/ny)
                    RT13.surfs[j][OIDIAM] = diay/ny;

                if (nx*ny > MAXHOLES)
                    RT13.surfs[j][ONARRAYY] = (MAXHOLES)/nx;
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
            double  ce = RT13.surfs[j][OCURVE];
            double  cx = RT13.surfs[j][OCURVX];
            double  cy = RT13.surfs[j][OCURVY];
            double  ae = RT13.surfs[j][OASPHER];
            double  ax = RT13.surfs[j][OASPHX];
            double  ay = RT13.surfs[j][OASPHY];

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
                if ((0 != RT13.surfs[j][i]) || isAdjustable(j,i))
                    bPoly = true;

            //----Zernike flag and diameter test  -----------------
            //---CoordinateBreaks set zernikes to zero, not -0  -----
            //---here it is important to accept zeros---------------

            boolean bZern = false;
            for (int i=OZ00; i<=OZ35; i++)
                if ((0 != RT13.surfs[j][i]) || isAdjustable(j,i))
                    bZern = true;

            if (bZern && (RT13.surfs[j][OODIAM] == 0.0))
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

            if (RT13.surfs[j][ORGAUSS] > 0.)  // found a Gaussian surface profile
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

            RT13.surfs[j][OPROFILE] = iProfile;
        }
    }

    void calcDOsize() {
        //---------feel out dOsize for getDelta()----------

        dOsize = 0.0;
        for (int j=1; j<=nsurfs; j++)
        {
            dOsize = Math.max(dOsize, Math.abs(RT13.surfs[j][OX]));
            dOsize = Math.max(dOsize, Math.abs(RT13.surfs[j][OY]));
            dOsize = Math.max(dOsize, Math.abs(RT13.surfs[j][OZ]));
            dOsize = Math.max(dOsize, Math.abs(RT13.surfs[j][OODIAM]));
            dOsize = Math.max(dOsize, Math.abs(RT13.surfs[j][OODIAX]));
        }
        if (dOsize < TOL)
            dOsize = 1.0;
    }

    void parse(String[] lines) {
        if (lines.length < 3) {
            throw new IllegalArgumentException("At least 3 lines must be present in the input");
        }
        parseCountAndDescriptionLine(lines[0]);
        parseHeadingLine(lines[1]);
        // Ignore the --- line
        for (int i = 3; i < lines.length; i++) {
            parseDataLine(lines[i]);
        }
        setDefaultOpticType();
        parseOpticType();
        parseOpticFormColumn();
        parseRefractionData();
        parseAdjustables();
        parseDiameters();
        //------------set the Euler angle matrix------------------
        RT13.setEulers();
        testGroovyness();
        classifyProfileSolvers();
        calcDOsize();
    }

}
