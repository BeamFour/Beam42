package com.stellarsoftware.beam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.stellarsoftware.beam.B4constants.*;

/* Base class for parsing .OPT, .MED and .RAY files */
public abstract class ParserBase {

    protected int nfields = 0;
    protected int iFieldStartCol[]      = new int[MAXFIELDS];
    protected int iFieldWidth[]         = new int[MAXFIELDS];
    protected int iFieldTagCol[]        = new int[MAXFIELDS];
    protected int iFieldDecimalPlaces[] = new int[MAXFIELDS];
    protected char cFieldFormat[]       = new char[MAXFIELDS];
    protected char cColons[]            = new char[IMAX];
    protected int nlines = 0;
    protected int maxlinelen = 0;
    protected int linelen[] = new int[JMAX];
    protected int iCaret, jCaret;   // caret column & row
    protected int period = 10;      // fieldwidth+1

    //-------------here is the char table-----------------

    private char charTable[][] = new char[JMAX+1][IMAX+1];

    public boolean bLoadFile(File f)
    // Extracts string from file; calls ePanel.vLoadString().
    // No internal smarts about EOL or CSV/Tab.
    // Analogous to doPasteInto().
    {
        if (f ==null)
        {
            return false;
        }
        if (!f.exists())
        {
            return false;
        }
        if (!f.canRead())
        {
            return false;
        }

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String text = null;
            StringBuffer sb = new StringBuffer();
            while((text = br.readLine()) != null)
            {
                sb.append(text);
                sb.append("\n");
            }
            br.close();
            if (sb.length() < 2)
            {
                return false;
            }
            String s = new String(sb);
            vLoadString(s, true);    // preclear=true.
//            if (getNumLines() > maxrecords+2)
//                iCountdown = -10;              // warning.
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    void clearLine(int j)
    // When exactly is this called?
    {
        for (int i=0; i<IMAX; i++)
            charTable[j][i] = ' ';
    }

    private void clearTable()
    {
        for (int j=0; j<JMAX; j++)
            clearLine(j);
        iCaret = jCaret= 0;
        getFieldInfo();
    }

    private int getOneLineLength(int j)
    {
        charTable[j][IMAX-1] = SPACE; // enforce terminal SP
        int len = 0;
        for (int i=IMAX-1; i>=0; i--)
            if (charTable[j][i] != SPACE)
            {
                len = i+1;
                break;
            }
        return len;
    }


    public int getFieldInfo()
    // field start is the transition (start or colon)->noncolon
    // field end is transition noncolon->(colon or end)
    // iFieldWidth[] **excludes** the tag char.
    // returns the number of fields found.
    {
        /// start by zeroing the field globals...
        nfields = 0;

        for (int f=0; f<MAXFIELDS; f++)
        {
            iFieldStartCol[f] = 0;
            iFieldWidth[f] = 0;
            iFieldTagCol[f] = 0;
            iFieldDecimalPlaces[f] = 0;
            cFieldFormat[f] = '-';
        }

        int rlen = getOneLineLength(RULER);

        if (rlen < 5)
            return 0;  // no fields!

        /// search the ruler for its colons...
        char colon = ':';
        char tc = colon;
        char pc = colon;
        int point = 0;
        boolean bstart, btag;
        for (int i=0; i<rlen; i++)
        {
            tc = (i < rlen-1) ? charTable[RULER][i] : colon;
            bstart = (pc == colon) && (tc != colon);
            btag = (pc != colon) && (tc == colon);
            if (bstart)
                iFieldStartCol[nfields] = i;
            if ((tc == '.') || (tc == ','))
            {
                point = i;
                cFieldFormat[nfields] = tc;
            }
            if ((tc=='E') || (tc=='e'))
                cFieldFormat[nfields] = tc;
            if (btag)
            {
                iFieldTagCol[nfields] = i;
                iFieldWidth[nfields] = i - iFieldStartCol[nfields];
                if (point > iFieldStartCol[nfields])
                    iFieldDecimalPlaces[nfields] = i-point-1;
                else
                    iFieldDecimalPlaces[nfields] = iFieldWidth[nfields]/2;

                if (nfields < MAXFIELDS-1)
                    nfields++;
            }
            pc = tc;
        }
        return nfields;
    }

    private int getAllLineLengths()
    // Sets local nlines and linelen[] and maxlinelen.
    // Individual linelen[] can be as big as IMAX-1
    {
        nlines = 0;
        maxlinelen = 0;
        for (int j=JMAX-1; j>=0; j--)
        {
            linelen[j] = getOneLineLength(j);
            if ((nlines==0) && (linelen[j]>0))
                nlines = j+1;
            if (linelen[j] > maxlinelen)
                maxlinelen = linelen[j];
        }
        for (int i=0; i<IMAX; i++)
            cColons[i] = (charTable[RULER][i] == ':') ? ':' : ' ';
        return nlines;
    }

    private int getDelimiterStatus(String s, int jcaret)
    // Examines a prospective data string for its delimiters.
    // if jcaret<3, tests rulerline, else tests lineZero.
    // Returns 0=unknown, 1=colons=native, 2=foreign=CSV/Tab
    // Needed if a string is to be inserted at jCaret=0,
    // because native->UseColonPattern; foreign->Use UO_EDIT_FWIDTH.
    {
        int j=0, ftype=0;
        int jtest = Math.max(0, 2-jcaret);
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i);
            if (c=='\n')
                j++;
            if (j==jtest)  // test line
            {
                switch (c)
                {
                    case COLON: ftype=1; break;
                    // case COMMA:
                    // case SEMICOLON:
                    case TAB:  ftype=2; break;
                }
                if (ftype != 0)
                    break;
            }
            if (j>jtest)
                break;
        }
        return ftype;
    }

    private void pushDownOneLine(int j)
    // Used by vLoadString() and TextMode ENTER key
    // Inserts one blank line into the table at "j".
    // For multiple line calls, we want only the initial preview saved.
    // so here, no stashForUndo().
    {
        j = Math.max(0, j);
        for (int t=JMAX; t>j; t--)
            System.arraycopy(charTable[t-1], 0, charTable[t], 0, IMAX);
        clearLine(j);
    }

    private int formulaTagPos(int i, int p)
    // Used by vLoadString to generate tags when ruler=formula.
    // if p=10: 0...9->9; 10...19->19 etc
    {
        return ((i+p)/p)*p - 1;  // new formula, equal field widths
    }

    private int rulerTagPos(int i,  int p)
    // Used by vLoadString to generate tags from a given ruler.
    // Used by myKeyHandler to implement TAB function.
    // If "i" lies beyond final ruler colon, it reverts to the formula.
    // Least possible result is i, that is no skipping; nondecreasing.
    // Assumes charTable[2][i] has been properly set up!
    {
        for (int k=i; k<IMAX-2; k++)
            if (charTable[2][k] == COLON)
                return k;
        return formulaTagPos(i, p);
    }

    public void vLoadString(String s, boolean preclear)
    // Handles PC, Mac, Unix EOL format strings from file or clipboard.
    // Always inserts, never overwrites.
    // Can receive entire table: decides colons vs CSV/Tab;
    // or just the data portion of a table, uses existing ruler.
    // Called by EJIF.pasteInto() or by EJIF.loadFile().
    // For pastes, want caret left at bottom of each.
    // For fresh loads, prefer jCaret = 0.
    // What about complete pastes where jStart == 0? Bottom.
    // So, use preclear to set jCaret = 0.
    {
        if (s.length() < 1)
            return;
        if (preclear)
            clearTable();  // also zeroes caret, scrollers

        getAllLineLengths();
        int i=0;
        char c, cprev=' ';

        boolean bComplete = (jCaret == 0);
        boolean bForeign = (getDelimiterStatus(s, jCaret) == 2);

        period = 1 + U.parseInt(DMF.reg.getuo(UO_EDIT, 3));
        period = Math.max(4, Math.min(20, period));

        // start loading at line = jCaret...

        for (int k=0; k<s.length(); k++)
        {
            if (i==0)                  // start a new line?
                pushDownOneLine(jCaret); // insertion; clears jCaret line too.

            c = U.getCharAt(s, k);
            switch (c)
            {
                // case COMMA:
                // case SEMICOLON:
                case TAB:
                    if (jCaret < 3)     // build new ruler
                        i = formulaTagPos(i, period);
                    else                // use existing ruler
                        i = rulerTagPos(i, period);
                    if ((i<IMAX) && (jCaret<JMAX) && (jCaret>1))
                        charTable[jCaret][i] = COLON;
                    if (i<IMAX-2)
                        i++;              // prepare for next character
                    break;
                case LF:
                    if (cprev==CR) break;
                    i=0; jCaret++; break;
                case CR:
                    i=0; jCaret++; break;
                default:
                    if ((c>=SPACE) && (c<='~') && (i<IMAX-2))
                    {
                        charTable[jCaret][i] = c;
                        i++;
                    }
            }
            if (jCaret>=JMAX-2)
                break;
            cprev = c;
        }
        getAllLineLengths();
        iCaret = 0;
        if (preclear)
            jCaret = 0;
        getFieldInfo();
    }

    public String getFieldFull(int f, int jrow)
    // String excludes tag char but includes leading & trailing blanks
    // For clean string contents, user should apply .trim()
    {
        if ((f<0) || (f >= nfields))
            return new String("");
        if ((jrow<0) || (jrow>=nlines))
            return new String("");
        int iLeft = iFieldStartCol[f];
        int width = iFieldWidth[f];
        return new String(charTable[jrow], iLeft, width);
    }

    public String getFieldTrim(int ifield, int jrow)
    // Returns the trimmed field, no leading or trailing blanks
    {
        return getFieldFull(ifield, jrow).trim();
    }

    public double getFieldDouble(int ifield, int jrow)
    // empty returns -0.0; badnum returns Double.NaN
    // U.suckDouble() includes trimming and -0 for empty.
    {
        return U.suckDouble(getFieldFull(ifield, jrow));
    }

    public int getFieldWidth(int f)
    {
        return iFieldWidth[f];
    }

    public abstract void parse();
}
