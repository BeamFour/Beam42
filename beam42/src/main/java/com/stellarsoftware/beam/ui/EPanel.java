package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.B4constants;
import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.U;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;          // font metric
import java.io.*;                // files
import javax.swing.*;            // everything else


@SuppressWarnings("serial")

/**
  * A164: installing CopyFieldBottom()
  *   key listener: line 1027
  *   implementor:  line 1221
  *
  *
  * A158:  testing permanent scrollbars, to possibly eliminate the
  *  self-scrolling on Mac platform triggered by resize drag.
  *
  * A152:  TextMode needs line-break pushdown and line-yank pullup.
  *
  *
  * EPanel.java  --- an editor panel for the EJIF frame. 
  *  This class contains a private full size charTable...
  *     private char charTable[][] = new char[JMAX+1][IMAX+1];
  *  but clients may have tighter limitations on table size.
  *
  *  No need to mention: AdjustmentListener, KeyListener, MouseListener, 
  *    MouseMotionListener, FocusListener, accelerator...
  *
  *  2D edit technique: char charTable[][] contains the text.   
  *
  *  This class replaces JTextArea and adds many new features. 
  *  
  *  A129: has DMF.nEdits, helping graphics to coordinate repaints. 
  *
  *  Caret on/off is driven by BJIF and EJIF, not locally. 
  *  Caret location is managed here.
  *
  *  Focus is driven by EJIF's internalFrameListener. 
  *  JScrollBars are managed here. 
  *
  *  A118 implements a simple undo with a single String buffer. 
  *  stashUndo is called at the start of methods...
  *     doDelete()
  *     vLoadString()
  *     clearTable()
  *     putTypedChar()
  *     widenTable()
  *     narrowTable()
  *     copyFieldDown(). 
  *
  *  stashForUndo() is not called in myEJIF.setDirty(true) because that method
  *  of myGJIF is called very frequently: each microchange in the table. 
  *  doUndo() is called only within MyKeyHandler::keyPressed for VK_Z.
  *
  *  Seems to me that stashForUndo() ought to be called at start of AutoAdjust
  *  to permit backing out of .OPT and .RAY adjustments.   To accomplish this
  *  it will need to be public, not private. DONE; and conveyed through EJIF.
  *
  *
  * (c) 2006 Stellar Software
  **/
class EPanel extends JPanel implements B4constants, MouseWheelListener
{ 
    // public static final long serialVersionUID = 42L;  // Xlint 8 Oct 2014

    EPanel(EJIF ejif) // constructor
    { 
        // no need to initialize base class JPanel "super()"
        
        ///// testing suggestion from StackOverflow....
        ///// this.setDoubleBuffered(false); 
        ///// does it work?  Nope.
        
        myEJIF = ejif;   
        vsbReference = null; 
        clearTable(); 

        //---initialize sizes; also refresh these each repaint().
        //---repaint() gets called for every resizing. 

        px = getSize().width;     // pixels
        py = getSize().height;    // pixels
        iWidth = px/charwidth;    // number of chars wide
        jHeight = py/charheight;  // number of chars tall

        //---focusTraversal must be disabled for VK_TAB key to work
        //---Caret management does not use focus: mere epiphenomenon. 
        //---so we don't need a focusListener.  
        //---Focus influences keystrokes only. 
        //---But wait! how can LossOfFocus force a caretfree repaint?

        this.setFocusTraversalKeysEnabled(false); 
        this.setFocusable(true);    
        this.addKeyListener(new MyKeyHandler()); 

        this.addMouseListener(new MyMouseHandler()); 
        this.addMouseMotionListener(new MyMouseMotionHandler()); 

        // possible simplification here:
        // Swing MouseInputAdapter = MouseListener + MouseMotionListener().
        // but it does not include any wheel listening.

        this.addMouseWheelListener(this); 

        // Now find out editor contents to manage scroll bars:
        // No no install permanent scrollbars: A158, A163

        hsbReference = myEJIF.createHSB();
        iOff = 0; 
        vsbReference = myEJIF.createVSB(); 
        jOff = 0; 
        getAllLineLengths(); 
        myEJIF.dataModel.setDirty(false);
    }  //--------end constructor---------------

    void setCaretFlag(boolean b)
    // Required support for abstract class EJIF
    {
        bCaret = b;
    } 

    void refreshSizes()
    // After major edits, reestablishes table size & line lengths
    // These are needed internally for further editing. 
    {
        getAllLineLengths();
    }

    public void paintComponent(Graphics g)
    // Paints the visible JPanel and the caret each blink.
    // The frame title is repainted by EJIF. 
    {  
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        fontsize = U.parseInt(Globals.reg.getuo(UO_EDIT, 6));
        fontsize = Math.max(3, Math.min(100, fontsize)); 
        boolean bBold = "T".equals(Globals.reg.getuo(UO_EDIT, 7));
        int iBold = bBold ? Font.BOLD : Font.PLAIN; 
        Font myFont = new Font("Monospaced", iBold, fontsize); 
        g2.setFont(myFont); 
        charheight = fontsize; 
        charwidth = getFontWidth(g2, myFont); 

        g2.setPaint(Color.BLACK); 

        setEditSmoothing(g2); 

        px = getSize().width;          // pixels showing
        iWidth = px/charwidth;         // characters showing
        py = getSize().height;         // pixels showing
        jHeight = py/charheight;       // characters showing

        manageVSB();  // scroll bar management when size changes
        manageHSB(); 

        int jmin = jOff;  
        int jmax = jOff + jHeight;

        for (int j=jmin; j<jmax; j++)  // j = row of table
        {
            int jwin = j-jmin;         // jwin = row within window
            if (j < JMAX-1)            // within the table
            {
                g2.setPaint(isRowMarked(j) ? BLUEGRAY : Color.WHITE); 
                g2.fillRect(0, jwin*charheight+JPOFF, px, charheight); 
                g2.setPaint(Color.BLACK);  
//                linelen[j] = getOneLineLength(j); // WHY setting linelen ? TODO Check
                int linelen = getOneLineLength(j);
                int count = Math.max(0, linelen - iOff);
                String s = myEJIF.model().getLine(j, iOff, count);
                g2.drawString(s, 0, (jwin+1)*charheight);
                // alternative: g2.drawChars(....)
            }
            else                      // beyond the table
            {
                g2.setPaint(Color.GRAY); 
                g2.fillRect(0, jwin*charheight+JPOFF, px, charheight); 
            }
        } 

        if (myEJIF.getCaretStatus())
        {
            int i = (iCaret-iOff)*charwidth + IOC; 
            int j = (jCaret-jOff)*charheight + JOC; 
            int caretwidth = charwidth; 
            if("T".equals(Globals.reg.getuo(UO_EDIT, 10)))  // text mode
              caretwidth = charwidth/4; 
            g2.setXORMode(Color.YELLOW);
            g2.fillRect(i, j, caretwidth, charheight); 
        }
        // EJIF manages its own title, has own paintComponent(). 

    } //----end paintComponent()

    void setCaretXY(int field, int row)
    {
        int nfields = getFieldInfo(); 
        if ((field>=0) && (field<nfields))
          //iCaret = iFieldStartCol[field];
            iCaret = myEJIF.model().getiFieldStartCol(field);
        if ((row>=0) && (row<JMAX-2))
          jCaret = row; 
    }

    int getCaretY()  // added A106 for AutoRayGen
    {
        return jCaret;
    }
    

    //------------ public administrative methods---------------

    private void setEditSmoothing(Graphics2D g2)
    {
        if ("T".equals(Globals.reg.getuo(UO_EDIT, 8)))
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        else
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
    }

    void setVerticalPosition(int rowNumber)
    // Called by Frame when scrollbar produces a message
    {
        jOff = rowNumber; 
        repaint();   // OS eventually calls paintComponent() below
    }

    void setHorizontalPosition(int colNumber)
    // Called by Frame when scrollbar produces a message
    {
        iOff = colNumber; 
        repaint(); 
    }

    int getFieldInfo() {
        return myEJIF.model().getFieldInfo();
    }

    String getFieldFull(int f, int jrow) {
        return myEJIF.model().getFieldFull(f, jrow);
    }

    String getFieldTrim(int ifield, int jrow)
    // Returns the trimmed field, no leading or trailing blanks
    {
        return getFieldFull(ifield, jrow).trim(); 
    }

    double getFieldDouble(int ifield, int jrow)
    // empty returns -0.0; badnum returns Double.NaN
    // U.suckDouble() includes trimming and -0 for empty.
    {
        return U.suckDouble(getFieldFull(ifield, jrow)); 
    }

    int getFieldWidth(int f) {
        return myEJIF.dataModel.getFieldWidth(f);
    }

    void putFieldString(int f, int jrow, String s) {
        myEJIF.model().putFieldString(f, jrow, s);
    }

    void forceFieldString(int f, int jrow, String s) {
        myEJIF.model().forceFieldString(f, jrow, s);
    }

    void putFieldDouble(int f, int jrow, double d) {
        myEJIF.dataModel.putFieldDouble(f, jrow, d);
    }

    void forceFieldDouble(int f, int jrow, double d) {
        myEJIF.model().forceFieldDouble(f, jrow, d);
    }

    char getTagChar(int f, int jrow) {
        return myEJIF.model().getTagChar(f, jrow);
    }

    void putTagChar(int f, int jrow, char c) {
        myEJIF.model().putTagChar(f, jrow, c);
    }

    boolean hasContent()
    {
        return (getAllLineLengths() > 0); 
    }

    int getLineCount()
    {
        return getAllLineLengths(); 
    } 

    int getLineLength(int jrow) {
        return myEJIF.model().getLineLength(jrow);
    }

    int getICaret()
    {
        return iCaret;
    }
   
    int getJCaret()
    {
        return jCaret;
    }

    int getCaretFieldNum()
    {
        return getWhichField(iCaret); 
    }

    int getNumFields()
    {
        return getFieldInfo(); 
    }
   
    int getGuideNumber() {
        return myEJIF.dataModel.getGuideNumber();
    }

    boolean isMarked()  // public for edit graying
    {
        return jDrag >= 0; 
    }

    void doUnmarkRepaint()
    {
        jDrag = -1;
        repaint(); 
    }

    void doSelectAll()   // for edit menu
    {
        getAllLineLengths(); 
        jDown = 0; 
        jDrag = myEJIF.model().getNlines()-1;
        repaint(); 
    }


    //------------- public i/o methods-------------------------

    String getLine(int j)
    // assumes that getAllLineLengths() has been called first
    // to initialize nlines and individual line lengths. 
    {
        if ((j<0) || (j>=myEJIF.model().getNlines()))
          return ""; 
        return myEJIF.model().getLine(j, 0, myEJIF.model().getLineLen(j));
    }

    void vLoadSkeleton() {
        myEJIF.model().vLoadSkeleton();
        iMouse = jMouse = jDown = iCaret = jCaret = iOff = jOff = 0;
        jDrag = -1;
    }

    boolean save(File f)
    // Uses println() to generate local platform EOLs.
    {
        return myEJIF.model().save(f);
    }


    String getMarkedText()
    // Reads text in a table.
    // This does not upset jCaret or existing markings,
    // so that "cut" can subsequently doDelete().
    // Performs tab delimiter substitutions when option=TABS.
    {
        getAllLineLengths();
        if (!isMarked())
            return "";
        int j0 = Math.min(jDown, jDrag);
        int j1 = Math.max(jDown, jDrag);
        return myEJIF.model().getText(j0, j1);
    }


    void setCaretToMark()
    {
        int jMark = Math.min(jDown, jDrag); 
        if (jMark >= 0)
          jCaret = jMark; 
    }

    void doDelete()
    // deletes lines that are in the marked zone
    // this might be faster using System.arrayCopy()
    {
        Globals.nEdits++;
        getAllLineLengths();
        stashForUndo();
        if (!isMarked())
          return;
        int j0 = Math.min(jDown, jDrag);
        int j1 = Math.max(jDown, jDrag);
        myEJIF.model().doDelete(j0, j1);
        // any need to rearrange jCaret?
        getAllLineLengths();
        doUnmarkRepaint();
    }


    void paste(String s) {
        iCaret = 0;
        stashForUndo();
        jCaret = myEJIF.model().vLoadString(s, false, jCaret);
        myEJIF.model().setDirty(true);
        repaint();
    }


    void stashForUndo()
    // called locally for significant changes to the charTable, 
    // or via EJIF at start of AutoAdjust (hence public).
    {
        sUndo = getTableString(); 
        nstash++; 
    }

    //-----------------end of public methods-------------------------





    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //--------move all these constants into Constants.java??----------

    private static final Color BLUEGRAY = new Color(188, 188, 255); 
    private static final int JPOFF = 3;   // vert paint offset
    private static final int IOC = 0;     // horiz caret offset
    private static final int JOC = 2;     // vert caret offset
    private static final int TABJUMP = 8;  
    private static final int VERTJUMP = 8; 

    //-------------other private fields---------------

    private String sUndo = new String(""); 
    private int nstash = 0;       // diagnostics only
    
    private int fontsize = 16;
    private int charwidth = 8;    // horiz char spacing
    private int charheight = 16;  // vert char spacing

    private boolean bCaret = false; 
    private int iCaret, jCaret;   // caret column & row
    private int iOff, jOff;       // scroll offset column & row
    private int iMouse, jMouse;   // unused
    private int jDown=0;          // start of drag, for graying
    private int jDrag=-1;         // -1=noMark, else end of drag 

    private int px, py;           // client window w,h in pixels
    private int iWidth, jHeight;  // client window w,h in chars

    private JScrollBar vsbReference = null;
    private JScrollBar hsbReference = null; 
    private EJIF myEJIF             = null;


    //--------------private methods-------------------

    private String getTableString() {
        return myEJIF.model().getTableString();
    }
    
    private void putTableString(String sGiven) {
        myEJIF.model().putTableString(sGiven);
        repaint();
    }

    private void swapUndo()
    // called by local myKeyHandler() method for Ctl-Z.
    {
        Globals.nEdits++;
        if (sUndo.length() < 1)
          return; 
        String sTemp = getTableString();        
        putTableString(sUndo); 
        sUndo = sTemp; 
    }

    int doBackTab(int i) {
        return myEJIF.model().doBackTab(i);
    }

    private int getWhichField(int icol) {
        return myEJIF.model().getWhichField(icol);
    }

    private void clearLine(int j) {
        myEJIF.model().clearLine(j);
    }

    private void pushDownOneLine(int j) {
        myEJIF.model().pushDownOneLine(j);
    }

    private void pullUpOneLine(int j) {
        myEJIF.model().pullUpOneLine(j);
    }

    private void clearTable() {
        stashForUndo();
        myEJIF.model().clearTable();
        iMouse = jMouse = jDown = iCaret = jCaret = iOff = jOff = 0;
        jDrag = -1;
    }

    private int getDelimiterStatus(String s, int jcaret) {
        return myEJIF.model().getDelimiterStatus(s, jcaret);
    }

    private boolean isRowMarked(int j)
    {
        if (jDrag<0)
          return false; 
        return (j-jDown)*(j-jDrag) <= 0; 
    }

    private int getAllLineLengths() {
        // Sets local nlines and linelen[] and maxlinelen.
        // Individual linelen[] can be as big as IMAX-1
        int n = myEJIF.model().getAllLineLengths();
        manageVSB();
        return n;
    }

    private void putLineWithColons(int j) {
        myEJIF.model().putLineWithColons(j);
    }

    private int getOneLineLength(int j) {
        return myEJIF.model().getOneLineLength(j);
    }

    StringBuffer fieldToStringBuffer() {
        // converts field or marked segment into a stringBuffer
        getAllLineLengths();
        int jmin = isMarked() ? Math.min(jDown, jDrag) : 0;
        int jmax = isMarked() ? Math.max(jDown, jDrag) : myEJIF.model().getNlines()-1;
        return myEJIF.model().fieldToStringBuffer(jmin, jmax);
    }

    private class MyKeyHandler implements KeyListener
    {  
        public void keyPressed(KeyEvent event)
        {  
            switch (event.getKeyCode())
            {
               case KeyEvent.VK_HOME:
                   iCaret = jCaret = 0; 
                   break; 

               case KeyEvent.VK_PAGE_UP:
                   jCaret = Math.max(0, jCaret-VERTJUMP); 
                   break; 

               case KeyEvent.VK_PAGE_DOWN:
                   jCaret = Math.min(JMAX-2, jCaret+VERTJUMP); 
                   break; 

               case KeyEvent.VK_Z: 
                   if (event.isControlDown() || event.isMetaDown())
                     swapUndo();
                   break; 
                   
               case KeyEvent.VK_LEFT:
                   if (event.isControlDown() || event.isMetaDown())
                     narrowTable(iCaret);       // narrow
                   else
                     iCaret = Math.max(0, iCaret-1); 
                   break; 

               case KeyEvent.VK_RIGHT:
                   if (event.isControlDown() || event.isMetaDown())
                     widenTable(iCaret, false); // widen, no colons 
                   else if (event.isAltDown())  // Alt Right 
                     widenTable(iCaret, true);  // widen, insert colons
                   else
                     iCaret = Math.min(IMAX-2, iCaret+1); 
                   break; 

               case KeyEvent.VK_UP:
                   jCaret = Math.max(0, jCaret-1); 
                   break; 

               case KeyEvent.VK_DOWN:
                   if (event.isAltDown())       // Alt + Down
                   {
                       if (event.isControlDown())
                         CopyFieldBottom(); 
                       else
                         CopyFieldDown(); 
                   }
                   else
                     jCaret = Math.min(JMAX-2, jCaret+1); 
                   break; 

               case KeyEvent.VK_ENTER:
                   if ("T".equals(Globals.reg.getuo(UO_EDIT, 10)))   // text mode
                   {
                       if (jCaret < JMAX)
                       {
                           stashForUndo(); 
                           pushDownOneLine(jCaret+1);        // clear line below
                           int ncopy = IMAX - iCaret;        // nchars to copy 
                           myEJIF.model().move(jCaret, iCaret, ncopy);
                           // pushDownOneLine already set dirty = true
                           jCaret++;
                           iCaret = 0; 
                       }   
                   }
                   else             // table mode, no table changes.
                   {
                       iCaret = 0; 
                       jCaret = Math.min(JMAX-2, jCaret+1); 
                   }
                   break; 

               case KeyEvent.VK_BACK_SPACE:
               case KeyEvent.VK_DELETE:
                   stashForUndo(); 
                   if ("T".equals(Globals.reg.getuo(UO_EDIT, 10)))   // text mode
                   {
                       if ((iCaret==0) && (jCaret>0))            // pull up OK
                       {
                           int istart = getOneLineLength(jCaret-1);
                           int iavail = IMAX - istart;   
                           myEJIF.model().appendAbove(jCaret, istart, iavail);
                           pullUpOneLine(jCaret);                // raise lines below
                           jCaret--; 
                           iCaret = istart; 
                       }
                       else if (iCaret>0)      // pull chars leftward
                       {
                           iCaret--; 
                           myEJIF.model().pullLeft(jCaret, iCaret);
                       }
                   }
                   else if (iCaret > 0)   // table mode
                   {
                       iCaret--; 
                       myEJIF.model().replacech(jCaret, iCaret, ' ');
                   }
                   myEJIF.dataModel.setDirty(true);
                   if (jCaret == RULER)
                     getFieldInfo(); 
                   break; 

               case KeyEvent.VK_TAB: 
                   if (event.isShiftDown()) 
                     iCaret = doBackTab(iCaret);
                   else
                     iCaret = 1+myEJIF.model().rulerTagPos(iCaret, myEJIF.model().getPeriod());
                   break;    

               case KeyEvent.VK_F7:
                   narrowTable(iCaret); 
                   break; 

               case KeyEvent.VK_F8:
                   widenTable(iCaret, false); 
                   break; 

               case KeyEvent.VK_F9:
                   widenTable(iCaret, true); 
                   break; 

               case KeyEvent.VK_F10: 
                   CopyFieldDown(); 
                   event.consume(); 
                   break; 
            }

            if (iCaret < iOff)
              iOff = iCaret; 
            if (iCaret >= iOff + iWidth-1)
              iOff = Math.max(0, iCaret-iWidth+1); 

            if (jCaret < jOff)
              jOff = jCaret; 
            if (jCaret >= jOff + jHeight-1)
              jOff = Math.max(0, jCaret-jHeight+1); 

            if (vsbReference != null)
              vsbReference.setValue(jOff); 
            if (hsbReference != null)
              hsbReference.setValue(iOff); 
            repaint();
        }

        public void keyReleased(KeyEvent event) 
        { 
        }

        public void keyTyped(KeyEvent event)
        // Process typed chars only, lower & upper case. 
        // Accelerator keys are handled elsewhere.
        {  
            Globals.nEdits++;
            char c = event.getKeyChar(); 
            int mod = event.getModifiers(); 
            if ((c>=' ') && (c<='~'))             // avoids BKSP and DEL
              if ((mod==0) || (mod==java.awt.Event.SHIFT_MASK))
              {
                  stashForUndo(); 
                  iCaret = Math.max(0, Math.min(IMAX-2, iCaret)); 
                  jCaret = Math.max(0, Math.min(JMAX-1, jCaret));
                  myEJIF.model().setchar(jCaret, iCaret, c);
                  iCaret = Math.min(IMAX-2, iCaret+1);   // increment iCaret
                  if ((c>' ') && (jCaret+1>myEJIF.model().getNlines()))
                      myEJIF.model().setNlines(jCaret+1);                   // helps Vscrolling
                  if ((c>' ') && (iCaret>myEJIF.model().getMaxlinelen()))
                      myEJIF.model().setMaxlinelen(iCaret);                 // helps Hscrolling

                  myEJIF.dataModel.setDirty(true);
              }
            if (jCaret == RULER)
              getFieldInfo(); 
        }

    } //---end private class MyKeyHandler------------
    

    void widenTable(int i, boolean bColons) {
        stashForUndo();
        myEJIF.model().widenTable(i, bColons);
    }

    void narrowTable(int i) {
        if ((i<0) || (i>IMAX-2))
          return;
        stashForUndo();
        myEJIF.model().narrowTable(i);
    }

    void CopyFieldDown() {
        jCaret = myEJIF.model().CopyFieldDown(jCaret, iCaret);
    }

    void CopyFieldBottom()
    /// copies field and tag all the way to the bottom
    {
        myEJIF.model().CopyFieldBottom(jCaret, iCaret);
    }
    

    //------------------- mouse stuff --------------------        

    private class MyMouseHandler extends MouseAdapter
    {
        public void mousePressed(MouseEvent event)
        {
            iCaret = ((int) event.getPoint().getX())/charwidth + iOff; 
            iCaret = Math.max(0, Math.min(IMAX-2, iCaret)); 
            jCaret = ((int) event.getPoint().getY())/charheight + jOff; 
            jCaret = Math.max(0, Math.min(JMAX-1, jCaret));
            jDown = jCaret; 
            jDrag = -1; 
            repaint();
        }
    }


    private class MyMouseMotionHandler implements MouseMotionListener
    {
        public void mouseMoved(MouseEvent event)
        {
        }

        public void mouseDragged(MouseEvent event)
        // Horstmann & Cornell v.1 p.308: drag beyond borders OK.
        // Uses beyond-borders drag numbers to force scrolling. 
        // Remember to reposition the caret. 
        // Remember to keep caret within display area. 
        // Remember to update the vertical scrollbar.
        {
            int jPix = event.getY(); 
            if ((jPix < 0) && (jOff > 0))
              jOff--; 
            if ((jPix > py) && (jOff < JMAX-10))
              jOff++; 
            jPix = Math.max(0, Math.min(py, jPix)); // anti escape
            jDrag = jPix/charheight + jOff; 
            jDrag = Math.max(0, Math.min(JMAX-1, jDrag)); 
            jCaret = jDrag; 
            if (vsbReference != null)
              vsbReference.setValue(jOff); // update vertical scrollbar
            repaint(); 
        }
    }


    public void mouseWheelMoved(MouseWheelEvent e) 
    {
        int notches = e.getWheelRotation(); 
        if (notches == 0)
          return; 
        jOff += 4*notches; 
        jOff = Math.max(0, Math.min(JMAX-10, jOff)); 

        // now move the host frame's scroll button
        if (vsbReference != null)
          vsbReference.setValue(jOff); 
        repaint();  
    }
    

    //---------------scrollbar management----------------

    private void manageVSB()  // vertical scrollbar
    {
        boolean bNeed = (myEJIF.model().getNlines()>=jHeight) || (jOff>0);
        if ((myEJIF!=null) && (vsbReference==null) && bNeed)
          vsbReference = myEJIF.createVSB(); 

        /**********no no do not destroy*************
        if ((myEJIF!=null) && (vsbReference!=null) && !bNeed)
          vsbReference = myEJIF.destroyVSB(); 
        *******************************************/

        /// added Mar 2012, A135 making VSB track actual nlines
        if ((myEJIF!=null) && (vsbReference!=null) && bNeed)
          vsbReference.setMaximum(myEJIF.model().getNlines());
    }
    

    private void manageHSB()  // horizontal scrollbar
    {
        boolean bNeed = (myEJIF.model().getMaxlinelen()>=iWidth) || (iOff>0);
        if ((myEJIF!=null) && (hsbReference==null) && bNeed)
          hsbReference = myEJIF.createHSB(); 
       
        /*************no no do not destroy************************
        if ((myEJIF!=null) && (hsbReference!=null) && !bNeed)
          hsbReference = myEJIF.destroyHSB(); 
        *******************************************************/
    }


    //-------------- static utilities --------------

    static void beep()
    {
       Toolkit.getDefaultToolkit().beep(); 
    }


    static int getFontWidth(Graphics2D g2, Font f)
    // Given a size:   int size = 32; 
    // Define a font:  Font font = new Font("Monospaced", Font.BOLD, size);
    // Set the font:   g2.setFont(font);
    // Then, call this:
    {
        FontRenderContext frc = g2.getFontRenderContext();
        return (int) f.getStringBounds("a", frc).getWidth(); 
    }


} //-----------end EPanel class------------------------------
