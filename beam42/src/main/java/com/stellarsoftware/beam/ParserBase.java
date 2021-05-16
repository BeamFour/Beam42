package com.stellarsoftware.beam;

import java.io.*;

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

    //protected int iCaret, jCaret;   // caret column & row
    protected int period = 10;      // fieldwidth+1
    private boolean bDirty=false;         // avoid exit if unsaved changes
    protected boolean bNeedsParse=false;    // BJIF timer calls vMasterParse

    //-------------here is the char table-----------------

    private char charTable[][] = new char[JMAX+1][IMAX+1];

    protected ParserBase() {
        //--------set up fieldArray helpers-----------------

        nfields = 0;

        for (int f=0; f<MAXFIELDS; f++)
        {
            iFieldStartCol[f] = 0;
            iFieldWidth[f] = 0;
            iFieldTagCol[f] = 0;
            iFieldDecimalPlaces[f] = 0;
            cFieldFormat[f] = '-';
        }
        bDirty = false;
        bNeedsParse = true;
    }

    public int getiFieldStartCol(int field) {
        return iFieldStartCol[field];
    }

    public String getLine(int j, int iOff, int count) {
        return new String(charTable[j], iOff, count);
    }

    public int getLineLen(int j) {
        return linelen[j];
    }

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
            vLoadString(s, true, 0);    // preclear=true.
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public void vLoadSkeleton()
    {
        int ifw = U.parseInt(DMF.reg.getuo(UO_EDIT, 3));
        ifw = Math.max(6, Math.min(100, ifw));
        DMF.nEdits++;
        clearTable();
        for (int i=0; i<100; i++)
            charTable[2][i] = '-';
        for (int j=2; j<15; j++)
            for (int i=ifw; i<100; i+=ifw)
                charTable[j][i] = ':';
        setDirty(false);
        getAllLineLengths();
        getFieldInfo();
    }

    public void move(int jCaret, int iCaret, int ncopy) {
        System.arraycopy(charTable[jCaret], iCaret, charTable[jCaret+1], 0, ncopy);
        for (int i=iCaret; i<IMAX; i++)   // blank source chars
            charTable[jCaret][i] = SPACE;
    }

    public void appendAbove(int jCaret, int istart, int iavail) {
        for (int k=0; k<iavail; k++)          // append to above
            charTable[jCaret-1][k+istart] = charTable[jCaret][k];
    }

    public void pullLeft(int jCaret, int iCaret) {
        for (int k=iCaret; k<IMAX-2; k++)
            charTable[jCaret][k] = charTable[jCaret][k+1];
    }

    public void replacech(int jCaret, int iCaret, char ch) {
        charTable[jCaret][iCaret] = ch;
    }

    public void setchar(int jCaret, int iCaret, char c) {
        if("T".equals(DMF.reg.getuo(UO_EDIT, 10)))  // text mode shove right
            for (int k=IMAX-1; k>iCaret; k--)
                charTable[jCaret][k] = charTable[jCaret][k-1];
        charTable[jCaret][iCaret] = c;
    }

    public boolean isDirty() {
        return bDirty;
    }

    public boolean save(File f)
    // Uses println() to generate local platform EOLs.
    {
        getAllLineLengths();   // sets up linelengths & nlines.
        try
        {
            PrintWriter pw = new PrintWriter(new FileWriter(f), true);
            for (int j=0; j<nlines; j++)
            {
                String s = new String(charTable[j], 0, linelen[j]);
                pw.println(s);
            }
            pw.close();
            setDirty(false);
            return true;
        }
        catch (IOException ioe)
        { return false; }
    }

    public String getText(int j0, int j1) {
        StringBuffer sb = new StringBuffer(10000);

        for (int j=j0; j<=j1; j++)
        {
            sb.append(charTable[j], 0, linelen[j]);
            sb.append('\n');
        }

        //------perform tab substitutions-------------
        //-------use charTable[2] as ruler------------
        //------don't clobber any EOLs!---------------

        if (DMF.reg.getuo(UO_EDIT, 5).equals("T"))
        {
            int iX=0, j=j0;
            for (int i=0; i<sb.length(); i++)
            {
                if (j>0)
                    if ((charTable[2][iX]==COLON) && (sb.charAt(i)!='\n'))
                        sb.setCharAt(i, TAB);

                iX++;
                if (sb.charAt(i) == '\n')
                {
                    iX=0;
                    j++;
                }
            }
        }
        return new String(sb);
    }

    public void setDirty(boolean state)
    // Called "true" by EPanel when user inputs modify the table.
    // Called "false by EPanel when Epanel saves the file.
    {
        bDirty = state;
        if (state)
            bNeedsParse = true;
    }

    public void setNeedsParse(boolean v) {
        bNeedsParse = v;
    }

    String getTableString()
    // Multipurpose string sucker.
    // Called by private swapUndo() and by public stashForUndo().
    {
        StringBuffer sb = new StringBuffer(1000);
        getAllLineLengths();
        for (int j=0; j<nlines; j++)
        {
            sb.append(charTable[j], 0, linelen[j]);
            sb.append('\n');
        }
        return sb.toString();
    }

    void putTableString(String sGiven)
    // Clears the charTable and installs a given String.
    // Also tidies up the diagnostics, and redisplays.
    // Assumes EOL is '\n' and so is not multiplatform.
    {
        DMF.nEdits++;
        int i=0, j=0, k=0;
        for (j=0; j<JMAX; j++)    // clear the table
            for (i=0; i<IMAX; i++)
                charTable[j][i] = ' ';
        i=0;
        j=0;
        for (k=0; k<sGiven.length(); k++)  // char loop
        {
            char c = U.getCharAt(sGiven, k);
            if (c == '\n')
            {
                j++;
                i=0;
            }
            else
            {
                charTable[j][i] = c;
                i++;
            }
        }
        getAllLineLengths();
        getFieldInfo();
    }

    public int doBackTab(int i)
    // Used by myKeyHandler to implement BackTab.
    // Avoids use of field organizers.
    {
        for (int k=i-2; k>0; k--)
            if (charTable[2][k] == COLON)
                return k+1;
        return 0;
    }

    public int getWhichField(int icol)
    // Each field must have a ruler colon tag.
    {
        if (nfields < 1)
            return ABSENT;
        if (icol < iFieldStartCol[0])
            return ABSENT;
        if (icol > iFieldTagCol[nfields-1])
            return ABSENT;  // no such field
        for (int f=0; f<MAXFIELDS; f++)
            if (iFieldTagCol[f] >= icol)
                return f;
        return 0;
    }

    void clearLine(int j)
    // When exactly is this called?
    {
        DMF.nEdits++;
        for (int i=0; i<IMAX; i++)
            charTable[j][i] = ' ';
        setDirty(true);
    }

    public void clearTable()
    {
        DMF.nEdits++;
        for (int j=0; j<JMAX; j++)
            clearLine(j);
        getFieldInfo();
        setDirty(false);
    }

    public int getOneLineLength(int j)
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

    public StringBuffer fieldToStringBuffer(int jmin, int jmax)
    // converts field or marked segment into a stringBuffer
    {
        StringBuffer sb = new StringBuffer((jmax-jmin+2)*IMAX);
        for (int j=jmin; j<=jmax; j++)
        {
            sb.append(charTable[j], 0, linelen[j]);
            sb.append(LF);
        }
        return sb;
    }

    public char getTag(int f, int jrow)
    {
        return charTable[jrow][iFieldTagCol[f]];
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

    public void putLineWithColons(int j)
    // use this only after having run getAllLineLengths()
    {
        DMF.nEdits++;
        if (j > RULER)
            for (int i=0; i<IMAX; i++)
                charTable[j][i] = cColons[i];
        setDirty(true);
    }

    public int getAllLineLengths()
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

    public int getNlines() {
        return nlines;
    }
    public void setNlines(int n) { nlines = n; }

    public int getMaxlinelen() { return  maxlinelen; }
    public void setMaxlinelen(int n) { maxlinelen = n; }

    public int getDelimiterStatus(String s, int jcaret)
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

    public void widenTable(int i, boolean bColons)
    {
        DMF.nEdits++;
        getAllLineLengths();
        for (int j=1; j<nlines; j++)
        {
            System.arraycopy(charTable[j], i, charTable[j], i+1, IMAX-i-1);
            charTable[j][i] = SPACE;
        }
        if (bColons)
            for (int j=RULER; j<nlines; j++)
                charTable[j][i] = ':';
        else
            charTable[RULER][i] = '-';
        setDirty(true);
        getAllLineLengths();
        getFieldInfo();
    }

    public void narrowTable(int i)
    {
        DMF.nEdits++;
        if ((i<0) || (i>IMAX-2))
            return;
        getAllLineLengths();
        for (int j=1; j<nlines; j++)
            System.arraycopy(charTable[j], i+1, charTable[j], i, IMAX-i-1);
        getAllLineLengths();
        getFieldInfo();
        setDirty(true);
    }

    public void forceFieldString(int f, int jrow, String s)
    // Enlarges table if necessary to accommodate jrow
    {
        if ((f<0) || (f>=nfields) || (jrow<0) || (jrow>JMAX-5))
            return;
        if (jrow >= nlines)
        {
            putLineWithColons(jrow);
            nlines = jrow+1;
        }
        int ileft = iFieldStartCol[f];
        int len = iFieldWidth[f];  // excludes tag.
        for (int i=0; i<len; i++)
            charTable[jrow][ileft + i] = U.getCharAt(s, i);
        setDirty(true);
        // note: U.getCharAt() returns blanks as needed.
    }

    public void putFieldString(int f, int jrow, String s)
    // Puts a field into an existing table with room.
    {
        if ((f<0) || (f>=nfields) || (jrow<0) || (jrow>nlines))
          return;
        int ileft = iFieldStartCol[f];
        int len = (jrow==0) ? s.length() : iFieldWidth[f];  // no tag.
        for (int i=0; i<len; i++)
          charTable[jrow][ileft + i] = U.getCharAt(s, i);
        setDirty(true);
        // note: U.getCharAt() returns blanks as needed.
    }

    public void putFieldDouble(int f, int jrow, double d)
    {
        String s =  U.fmtc(d,
                iFieldWidth[f],
                iFieldDecimalPlaces[f],
                cFieldFormat[f]);
        putFieldString(f, jrow, s);
        setDirty(true);
    }

    public void forceFieldDouble(int f, int jrow, double d)
    {
        String s =  U.fmtc(d,
                iFieldWidth[f],
                iFieldDecimalPlaces[f],
                cFieldFormat[f]);
        forceFieldString(f, jrow, s);
        setDirty(true);
    }

    public boolean needsParse() {
        return bNeedsParse;
    }

    public char getTagChar(int f, int jrow)
    {
        return charTable[jrow][iFieldTagCol[f]];
    }

    public void putTagChar(int f, int jrow, char c)
    {
        charTable[jrow][iFieldTagCol[f]] = c;
        setDirty(true);
    }

    public int getLineLength(int jrow)
    {
        return linelen[jrow];
    }

    int CopyFieldDown(int jCaret, int iCaret)
    /// copies the data field and its tag char.
    {
        DMF.nEdits++;
        if ((jCaret>RULER) && (jCaret<nlines-1))
        {
            // stashForUndo(); // yikes! ruins the function.
            int field = getWhichField(iCaret);
            String s = getFieldFull(field, jCaret);
            char c = getTagChar(field, jCaret);
            jCaret++;
            putFieldString(field, jCaret, s);
            putTagChar(field, jCaret, c);
            setDirty(true);
        }
        return jCaret;
    }

    public void CopyFieldBottom(int jCaret, int iCaret)
    /// copies field and tag all the way to the bottom
    {
        DMF.nEdits++;
        if ((jCaret>RULER) && (jCaret<nlines-1))
        {
            // stashForUndo(); // yikes! ruins the function.
            int field = getWhichField(iCaret);
            String s = getFieldFull(field, jCaret);
            char c = getTagChar(field, jCaret);
            for (int j=jCaret+1; j<nlines; j++)
            {
                putFieldString(field, j, s);
                putTagChar(field, j, c);
            }
            setDirty(true);
        }
    }

    public void pushDownOneLine(int j)
    // Used by vLoadString() and TextMode ENTER key
    // Inserts one blank line into the table at "j".
    // For multiple line calls, we want only the initial preview saved.
    // so here, no stashForUndo().
    {
        DMF.nEdits++; // TODO
        j = Math.max(0, j);
        for (int t=JMAX; t>j; t--)
            System.arraycopy(charTable[t-1], 0, charTable[t], 0, IMAX);
        clearLine(j);
        setDirty(true);
    }

    public void pullUpOneLine(int j)
    // line j will vanish, receiving text of j+1, etc.
    // used by TextMode backspace at jCaret=0
    {
        DMF.nEdits++;
        for (int t=j; t<JMAX; t++)
            System.arraycopy(charTable[t+1], 0, charTable[t], 0, IMAX);
    }

    private int formulaTagPos(int i, int p)
    // Used by vLoadString to generate tags when ruler=formula.
    // if p=10: 0...9->9; 10...19->19 etc
    {
        return ((i+p)/p)*p - 1;  // new formula, equal field widths
    }

    public int rulerTagPos(int i,  int p)
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

    public int vLoadString(String s, boolean preclear, int jCaret)
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
        // TODO check
//        stashForUndo();

        DMF.nEdits++;
        if (s.length() < 1)
            return jCaret;
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
        //iCaret = 0;
        if (preclear)
            jCaret = 0;
        getFieldInfo();
        return jCaret;
    }

    public String getFieldFull(int f, int jrow)
    // String excludes tag char but includes leading & trailing blanks
    // For clean string contents, user should apply .trim()
    {
        if ((f<0) || (f >= nfields))
            return "";
        if ((jrow<0) || (jrow>=nlines))
            return "";
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

    //-----------protected internal methods for derived classes-----------
    public int getLineCount()
    {
        return getAllLineLengths();
    }

    public int getGuideNumber()
    // Returns the intended number of user records in the table.
    {
        String s = new String(charTable[0], 0, 20);
        return U.suckInt(s);
    }

    protected void vPreParse(int results[])
    // Called by extended class to pre-gather table information.
    {
        for (int i=0; i<NGENERIC; i++)
            results[i] = 0;
        results[GPRESENT] = 1;
        results[GNLINES] = getLineCount();
        int nguide = getGuideNumber();
        results[GNRECORDS] = Math.min(nguide, results[GNLINES]-3);
        results[GNFIELDS] = getFieldInfo();
    }

    public int doDelete(int j0, int j1) {
        // deletes lines that are in the marked zone
        // this might be faster using System.arrayCopy()
        int nmarked = 1 + j1 - j0;
        for (int j=j0; j<JMAX-nmarked; j++)
            for (int i=0; i<IMAX; i++)
                charTable[j][i] = charTable[j+nmarked][i];
        for (int j=JMAX-nmarked; j<JMAX; j++)
            clearLine(j);
        // any need to rearrange jCaret?
        int j = getAllLineLengths();
        setDirty(true);
        return j;
    }

    public int getPeriod() {
        return period;
    }

}
