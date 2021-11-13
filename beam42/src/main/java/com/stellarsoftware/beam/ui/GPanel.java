package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.*;
import com.stellarsoftware.beam.core.render.CAD;
import com.stellarsoftware.beam.core.render.DrawBase;

import java.awt.*;         // frame, BasicStroke, Color, Font
import java.awt.event.*;   // KeyEvent MouseEvent etc
import java.awt.image.*;   // BufferedImage; transparent Blue color
import java.awt.font.*;    // font metric
import java.awt.print.*;   // printing
import java.io.File;
import java.util.*;        // ArrayList
import javax.swing.*;      // Graphics2D features


@SuppressWarnings("serial")

/**  
  * GPanel A172 adopting five quadLists and quadList tools.
  * GPanel A172 installing finishList following randList. 
  *
  *
  * Abstract class GPanel handles all artwork and annotation.
  * Has listeners for keystrokes, mouse, wheel, scrollers.
  * No sizeListener(); instead uses getSize() at each repaint.
  *
  * Seven abstract methods must be supplied by every extension:
  *
  *     protected void doTechList()  to create a new artwork tech list
  *     protected void doRotate()
  *     protected boolean doRandomRay()
  *     protected void doCursor(i,j) to manage the cursor in user space
  *     protected double getStereo()
  *     protected void doSaveData()  to a file, for histograms
  * 
  * Extensions needing mouse pan zoom must use AddScaledItem(xyz, opcode)
  * to add a vertex to a drawing and MUST FIRST SET THE AFFINES
  *    uxcenter, uxspan, uycenter, uyspan, uzcenter, uzspan
  * and must not modify these affines again.   
  * The mouse pan zoom feature will modify these affines as needed. 
  *
  * Here the manageXXXzoom() methods coordinate panels and zoom state.
  * For LAYOUT, also the zoom scalefactors can be displayed on the titlebar. 
  * For Plot2D the var and surf error message is displayed on the titlebar. 
  * Implements Zoom (wheel or F7) and Pan (left button drag).
  * Implements Rotate (right button drag) by calling new artwork.
  * Would be nice to center rotation on visible feature: line 614
  *
  * This abstract class is concreted by clientPanels doing artwork:
  *   Layout; Plot2D; MPlot, Plot3D, H1D; H2D; MTF; Test.
  *
  * Native system is CenterOriginPoints, +X=right, +Y=up, +Z=out.
  * so artwork quads have -250.0 < x,y,z < +250.0
  * Pixels are the same, except origin = ULCorner and +Y=down. 
  * Conversion to pixels uses local getIXPIX(x), getIYPIX(y).
  *
  * To get user coords from the artwork, the quadlist includes affines:
  *    userconsts x,y,z = offsets for that quadlist view;
  *    userslopes x,y,z = magnifications for that quadlist view;
  *    UserValue x,y,z = userConst + userSlope * quadListValue.
  * These affine userconsts & userslopes should be evaluated once,
  * at startup; then when doing artwork the affines should be added by the 
  * client at the beginning of its quadlist, by calling local addAffines().
  * The client must then NOT MODIFY the affines futher since the mouse
  * pan zoom actions will have taken over. 
  *
  *    protected void addAffines()
  *    {
  *       addXYZO(uxcenter, uycenter, uzcenter, USERCONSTS); 
  *       double d = (dUOpixels > 0) ? dUOpixels : 500.0; 
  *       addXYZO(uxspan/d, uyspan/d, uzspan/d, USERSLOPES);
  *    }
  *
  * Q: might it be better to not divide by d? Show just the spans?
  * That way they would be USERSPANS.  The renderer does not care since
  * the quadlist is already in render units.  However the user of the
  * affines (only CAD::DXF, I think) would have to know the "d" value to
  * convert quads to real space coords:
  *    UserValue = userConst + (userSpan / d) * quadListValue.
  *
  * A: No, leave it as is.  Works OK, simplifies CAD. 
  *
  * The BJIF caret is used to operate the blinking caret overlay.
  * Focus is driven by GJIF's internalFrameListener. 
  * 
  * Includes redo() which responds to Random.
  *
  * DRAMATIS PERSONAE
  *    baseList    is an ArrayList of XYZO "quads" from artwork generator.
  *    biTech        is a local private bitmap that is screen compatible.
  *    g2Tech        is a private Graphics2D context from biTech.  
  *    doTechList()  (abstract) is how we request new artwork from client. 
  *    renderList()  is the local method that draws any ArrayList onto a bitmap. 
  *
  * General artwork: baseList   -> g2Tech or g2CAD or g2Print.
  * Random batch:    batchList  -> g2Tech and blit to screen.
  * Random accum:    randList   -> g2CAD or g2Print
  * Annotation art:  annoList   -> g2Tech or g2CAD or g2Print.
  * Finish Layout:   finishList ->
   
  * Caret blinking is handled by a BJIF timer that alternates caret=true, false, true...
  * and calls OS repaint() which calls local paintComponent(), hence drawPage(), 
  * which for each state does three things:
  *     1.  Blits biTech onto the current display; (quick!)
  *     2.  Uses renderList(annoList) to refresh annotation;
  *     3.  Draws, or not, the caret block using setXORMode(). 
  * This is always blindingly fast. 
  *
  * renderList() manages its own graphic alias smoothing.
  * Seems to me cleaner to have doCAD and paintComponent()
  * impose smoothing on each's g2D.           << DONE
  *
  * Bug: annotation fails to deliver its color to .PS files
  * although it works fine on screen. << FIXED.
  *
  * To do: build annoColor into the annoList.   <<DONE
  * Bug: BasicStroke has spiky JOIN_BEVELs.     <<fixed: JOIN_ROUNDs 
  *
  *
  * (c) 2004 - 2015 Stellar Software all rights reserved. 
  */
abstract class GPanel extends JPanel implements B4constants, Printable
{
    //-----Each extension must supply values for the following---------

    protected GJIF  myGJIF;                  // set by descendant panel
    protected DrawBase drawBase;

    abstract void    buildTechList(boolean bArtStatus);
    abstract void    doCursor(int i, int j);
    abstract double  getStereo(); 
    abstract void    doSaveData(); 
    

    //--------Constructor---------------------

    GPanel(DrawBase drawBase)  // host class for artwork generators
    {
        ///// testing suggestion from StackOverflow...
        ///// this.setDoubleBuffered(false); 
        ///// does it work?  Nope.
        
        this.setFocusable(true);
        this.addKeyListener(new MyKeyHandler());
        this.addMouseListener(new MyMouseHandler());
        this.addMouseMotionListener(new MyMouseMotionHandler());
        this.addMouseWheelListener(new MyMouseZoomer());
        this.drawBase = drawBase;
    }

    public boolean doRandomRay() {
        return drawBase.doRandomRay();
    }

    void requestNewArtwork()
    // Allows each AutoAdjust() iteration to request fresh Layout artwork. 
    // Don't re-parse UO or sizes when this is called.
    // Just let drawPage() regenerate its new g2Tech and render it. 
    {
        drawBase.requestNewArtwork();
        if (g2Tech != null)
          g2Tech.dispose(); 
        g2Tech = null;      // should not spoil baseList
        repaint();          // call OS, which calls paintComponent() below.
    }
    
    @Override
    public void paintComponent(Graphics g)
    // Gets called when OS requests repaint() each caret blink.
    // GJIF offers myGJIF.setTitle(), myGJIF.getTitle().
    // This uses the stashed page, does not recalculate graphic.
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;  
        drawPage(g2); 
    }


    void redo()
    // Called by Random after random rays have augmented batchList.
    // For Layout and Plot, rays atop earlier artwork, bClobber=false:
    //   First it appends batchList to randList, retain for CAD/printing. 
    //   Then we render batchList onto biTech using g2Tech.
    //   Then clears batchList making room for more random rays. 
    //   Then requests a repaint() to blit biTech onto the screen. 
    // For H1D, H2D, MTF, Map, a total redraw is necessary, bClobber=true:
    //   Accomplished by nulling g2Tech while retaining biTech:
    //   can repaint biTech many times but not augment it. 
    {
        if (g2Tech == null)  // SNH.
          return; 
        
        int lRand = drawBase.randList.size();
        int lBatch = drawBase.batchList.size();

        boolean bTooBig = (lRand + lBatch > MAXRANDQUADS); 
        if (!bTooBig)
          for (int i=0; i<lBatch; i++)
            drawBase.randList.add(drawBase.batchList.get(i));
        double dStereo = getStereo(); 
        
        if (drawBase.bClobber)
          g2Tech = null;  // let drawPage() create fresh artwork. But what about biTech?
        else
        {
            renderList(drawBase.batchList, g2Tech, dStereo, true);
            if (dStereo != 0.0)
              renderList(drawBase.batchList, g2Tech, -dStereo, false);
        }
        drawBase.batchList.clear(); // all done with batchList.
        
        //--finally superpose finishList if any----
        
        renderList(drawBase.finishList, g2Tech, dStereo, true);
        if (dStereo != 0.0)
          renderList(drawBase.finishList, g2Tech, -dStereo, false);

        repaint(); 
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException
    {
        if (page >= 1)
          return Printable.NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g; 
        g2.translate(pf.getImageableX(), pf.getImageableY()); 
        drawPage(g2); 
        return Printable.PAGE_EXISTS;
    }



    void doCAD()  // called by DMF >> GJIF >> here
    // This routine supplies both a buffered screen image, and
    // the three quad lists, to an outboard CAD writer. 
    // Some CAD formats need the image, others need the lists. 
    // Rendering is done here because renderList() needs local affines.
    {
        int style = -1; 
        for (int i=0; i<9; i++)
          if ("T".equals(Globals.reg.getuo(UO_CAD, i)))
          {
             style = i;
             break; 
          } 
        if (style < 0)
        {
           U.beep();
           return; 
        }
        boolean bPortrait = "T".equals(Globals.reg.getuo(UO_CAD, 10));
        JFileChooser jfc = new JFileChooser();
        String sDir = DMF.sCurrentDir;
        if (sDir != null)
        {
            File fDir = new File(sDir);
            if (fDir != null)
                if (fDir.isDirectory())
                    jfc.setCurrentDirectory(fDir);
        }
        int q = jfc.showSaveDialog(null);
        if (q == JFileChooser.CANCEL_OPTION)
            return;
        File file = jfc.getSelectedFile();
        if (file == null)
        {
            return;
        }
        DMF.sCurrentDir = file.getParent();
        CAD.doCAD(style, bPortrait, drawBase.baseList, drawBase.randList, drawBase.finishList, drawBase.annoList, file);
    }
    

    void doUpdateUO()
    // Options calls this via GJIF when options change
    {
        drawBase.doUpdateUO();
        if (g2Tech != null)    // discard old artwork
          g2Tech.dispose();    // discard old artwork
        g2Tech = null;         // discard old artwork
        repaint();             // request OS repaint.
    }


    void doQualifiedRedraw()
    // GJIF calls this when coming forward: check for table edits.
    // How to implement bSticky, retain previous magnification?
    {
        drawBase.doQualifiedRedraw();
        if (g2Tech != null)    // discard old artwork
          g2Tech.dispose();    // discard old artwork
        g2Tech = null;         // discard old artwork
        repaint();             // request OS repaint.
    }

    //--------------private & client support area-----------
    //--------------private & client support area-----------
    //--------------private & client support area-----------


    //------------graphics and blitting-----------------

    private Graphics2D g2Tech;       // Graphics2D for local Tech drawing
    private BufferedImage biTech;    // unannotated bitmap
    private Dimension dim;           // current panel size  
    private int prevwidth=0;         // display size
    private int prevheight=0;        // display size
    private double dStereo=0.0;      // display stereo convergence

    

    private void drawPage(Graphics2D g2)
    // This routine does all the blitting: blinker, anno, biTech.
    //
    // Annotation worksaver: technical image is saved in biTech,
    // and blitted to screen before anno character list is painted.
    // This blit & annoPaint is done for each anno keystroke.
    // However zoom, pan, rotate require totally new artwork.
    //
    // Here renderList() handles all font work because fontsize
    // and boldness is built into each char.
    //
    // Includes caret blink via host BJIF and paintComponent().
    {
        dim = getSize(); 
        drawBase.imid = dim.width / 2;
        drawBase.jmid = dim.height / 2;

        //----totally new artwork---------
        
        if ((prevwidth != dim.width) || (prevheight != dim.height)
        || (biTech==null) || (g2Tech==null))
        {
            if (g2Tech != null)
              g2Tech.dispose(); 

            drawBase.baseList.clear();
            drawBase.batchList.clear();
            drawBase.randList.clear();
            drawBase.finishList.clear();
            drawBase.annoList.clear();
            prevwidth = dim.width; 
            prevheight = dim.height; 
            biTech = new BufferedImage(dim.width, dim.height,
                       BufferedImage.TYPE_INT_RGB); 
            g2Tech = (Graphics2D) biTech.getGraphics();
            setGraphicSmoothing(g2Tech);

            buildTechList(drawBase.bArtStatus); // locally stashed bArtStatus
            double dStereo = getStereo(); 
            
            if (dStereo == 0.0)
            {
                renderList(drawBase.baseList, g2Tech, 0.0, true);
                renderList(drawBase.finishList, g2Tech, 0.0, true);
            }
            else
            {
                renderListTwice(drawBase.baseList, biTech, dStereo, true);
                renderListTwice(drawBase.finishList, biTech, dStereo, true);
            }
        }

        //----all artwork------------
        
        setGraphicSmoothing(g2);                    // prep screen
        g2.drawImage(biTech, 0, 0, null);           // blit biTech
        if (drawBase.annoList.size() > 0)
          renderList(drawBase.annoList, g2, 0.0, false);     // annotate

        if ((myGJIF != null) && myGJIF.getCaretStatus())    
        {
            int f = drawBase.getUOAnnoFont();                // fontsize points
            int i = drawBase.icaret - f/4;
            int j = drawBase.jcaret - f/3;
            int w = f/2;
            int h = (2*f)/3; 
            g2.setXORMode(Color.YELLOW);
            g2.fillRect(i, j, w, h); 
        }
    }




    //----locally stashed fields---------------------


    //------caret wheel zoom support for clients-----------
        
    static java.util.Timer wheelTimer; 
    private int wheelTimerCount=0;
    private int wheelTimerMax=2; 


    //-----------mouse action support------------

    private boolean bLeftButton=false;            
    private boolean bRightButton=false; 
    private boolean bDragged=false; 


    private void setGraphicSmoothing(Graphics2D g2)
    {
        if ("T".equals(Globals.reg.getuo(UO_GRAPH, 7)))
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




    //----------support for mouse pan zoom twirl-------------------

    protected void getNewMouseArtwork(boolean bFinal)  
    // protected allowing thread update.
    // Don't re-parse UO or sizes when this is called!
    // Called by Zoom, mouseDragTranslate, rotate.
    // Not called for simple mousePressed/mouseReleased.
    // bFinal=false when mouse is still down and skeleton is wanted.
    // bFinal=true when mouse is released and final artwork is wanted.
    {
        drawBase.getNewMouseArtwork(bFinal);
        if (g2Tech != null)
          g2Tech.dispose(); 
        g2Tech = null;   
        repaint();           // call OS
    }


    private void manageZoomIn()  // called by F7 and Wheel
    // ZoomIn centered on the current caret location. 
    // New artwork will use the new centers & spans.
    {
        drawBase.manageZoomIn();
        startWheelTimer();
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(Globals.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(drawBase.sScaleFactors());
    }

    private void manageVertZoomIn()  // called by F5 and WheelShift
    // ZoomIn centered on the current caret location. 
    // New artwork will use the new centers & spans.
    {
        drawBase.manageVertZoomIn();
        startWheelTimer();
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(Globals.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(drawBase.sScaleFactors());
    }


    private void manageZoomOut()  // called by F8 and Wheel
    {
        drawBase.manageZoomOut();
        startWheelTimer();
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(Globals.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(drawBase.sScaleFactors());
    }

    private void manageVertZoomOut()  // called by F6 and WheelShift
    {
        drawBase.manageVertZoomOut();
        startWheelTimer();
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(Globals.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(drawBase.sScaleFactors());
    }

    private void manageDragTranslate(int di, int dj) 
    // Called by drag.
    // New artwork will use the modified centers. 
    {
        drawBase.manageDragTranslate(di, dj);
        getNewMouseArtwork(bSKELETON);
    }


    private void manageDragRotate(int i, int j) 
    // Called by drag.
    // Relies upon client's doRotate() to create rotated artwork. 
    // Uses average outOf zzz of target objects to translate image. 
    // This translation is done here, without client help.
    {
        drawBase.manageDragRotate(i, j);
        getNewMouseArtwork(bSKELETON);  // Have client do temporary artwork.
    }



    //-------Artwork & WheelTimer implementation-------------

    private void startWheelTimer()
    // Discards previous wheelTimer and creates a new one. 
    // Multiple starts OK; extendable delay.
    {
        wheelTimerCount = 0; 
        if (wheelTimer != null)
          wheelTimer.cancel(); 
        wheelTimer = null; 
        wheelTimer = new java.util.Timer(); 
        wheelTimer.schedule(new Incrementor(), 0, 50); 
    }


    private class Incrementor extends TimerTask
    // internal class eases communication with local variables
    // Delay avoids excessive wheel driven recomputations. 
    {
        public void run()
        {
            wheelTimerCount++; 
            if (wheelTimerCount > wheelTimerMax)
            {
               wheelTimer.cancel(); 
               wheelTimer = null; 
               getNewMouseArtwork(bFULLART); 
            }
        }
    }


    //------------Here are the event listeners----------

    private class MyKeyHandler extends KeyAdapter
    {
        int charwidth=0; 

        public void keyPressed(KeyEvent ke)
        {
            int fontcode = drawBase.getUOAnnoFontCode();
            int charH = fontcode / 10000;  
            int charW = 1 + fontcode / 20000;   

            int ic = ke.getKeyCode();  

            if ((ic==KeyEvent.VK_DELETE) || (ic==KeyEvent.VK_BACK_SPACE))
            {
                drawBase.deleteLastAnno();
                int step = (drawBase.getCurrentChar()=='\n') ? 0 : charW;
                drawBase.setNextCaretCoords(step);
                repaint();    
            }

            if (ic==KeyEvent.VK_ENTER)
            {
                drawBase.doKeyEnter(charH);
                repaint();
            }

            if (ic==KeyEvent.VK_UP)
            {
                drawBase.jcaret -= ke.isControlDown() ? 1 : charH;
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_DOWN)
            {
                drawBase.jcaret += ke.isControlDown() ? 1 : charH;
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_LEFT)
            {
                drawBase.icaret -= ke.isControlDown() ? 1 : charW;
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_RIGHT)
            {
                drawBase.icaret += ke.isControlDown() ? 1 : charW;
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_F5)
            {
                manageVertZoomIn();
            }

            if (ic==KeyEvent.VK_F6)
            {
                manageVertZoomOut(); 
            }

            if (ic==KeyEvent.VK_F7)
            {
                manageZoomIn();
            }

            if (ic==KeyEvent.VK_F8)
            {
                manageZoomOut(); 
            }
        }

        public void keyTyped(KeyEvent ke)
        {
            int fontcode = drawBase.getUOAnnoFontCode();
            int charH = fontcode / 10000;  
            int charW = 1 + fontcode / 20000;   

            if (drawBase.annoList.size() < 1)      // startup.
            {
               int foreground = SETCOLOR + BLACK; 
               if (g2Tech != null)
                 if (g2Tech.getBackground() == Color.BLACK)
                   foreground = SETCOLOR + WHITE; 
               drawBase.addAnno(0.0, 0.0, (char) foreground);
            }

            char c = ke.getKeyChar(); 
            if ((c>=' ') && (c<='~') && (drawBase.icaret>0))
            {
                drawBase.addAnno(drawBase.getAXPIX(drawBase.icaret), drawBase.getAYPIX(drawBase.jcaret), c);
                drawBase.setNextCaretCoords(charW);
                repaint(); 
            }
        }
    }


    private class MyMouseHandler extends MouseAdapter
    {
        public void mouseEntered(MouseEvent event)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        public void mouseExited(MouseEvent event)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); 
            doCursor(-1, -1); // doCursor() is abstract; see client GPanel
        }

        public void mousePressed(MouseEvent event)  // mouse down
        {
            drawBase.icaret = drawBase.imouse = event.getX();
            drawBase.jcaret = drawBase.jmouse = event.getY();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
            if (event.getButton()==MouseEvent.BUTTON1)
              bLeftButton= true; 
            if (event.getButton()==MouseEvent.BUTTON3)
              bRightButton= true; 

            drawBase.uxanchor = drawBase.getux(drawBase.getAXPIX(drawBase.icaret));
            drawBase.uyanchor = drawBase.getuy(drawBase.getAYPIX(drawBase.jcaret));
        }

        public void mouseClicked(MouseEvent event) // mouse up
        {
        }

        public void mouseReleased(MouseEvent event)
        {
            if (bDragged && bLeftButton)
            {
                drawBase.icaret = event.getX();
                drawBase.jcaret = event.getY();
                manageDragTranslate(drawBase.icaret-drawBase.imouse, drawBase.jcaret-drawBase.jmouse);
                drawBase.imouse = drawBase.icaret;
                drawBase.jmouse = drawBase.jcaret;
            }
            if (bDragged && bRightButton)
            {
                drawBase.icaret = event.getX();
                drawBase.jcaret = event.getY();
                manageDragRotate(drawBase.icaret-drawBase.imouse, drawBase.jcaret-drawBase.jmouse);
                drawBase.imouse = drawBase.icaret;
                drawBase.jmouse = drawBase.jcaret;
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            bLeftButton = false; 
            bRightButton = false; 
            if (bDragged)
              getNewMouseArtwork(bFULLART); 
            bDragged = false; 
        }
    }


    private class MyMouseMotionHandler implements MouseMotionListener
    {
        public void mouseMoved(MouseEvent event)
        {
            if (DMF.getFrontGJIF() == myGJIF)
              doCursor(event.getX(), event.getY()); // abstract
        }

        public void mouseDragged(MouseEvent event)
        {
            bDragged = true; 
            doCursor(event.getX(), event.getY());  // abstract
            
            //--------perform dynamic dragging----------
            if (bDragged && bLeftButton)
            {
                drawBase.icaret = event.getX();
                drawBase.jcaret = event.getY();
                manageDragTranslate(drawBase.icaret-drawBase.imouse, drawBase.jcaret-drawBase.jmouse);
                drawBase.imouse = drawBase.icaret;
                drawBase.jmouse = drawBase.jcaret;
            }
            if (bDragged && bRightButton)
            {
                drawBase.icaret = event.getX();
                drawBase.jcaret = event.getY();
                manageDragRotate(drawBase.icaret-drawBase.imouse, drawBase.jcaret-drawBase.jmouse);
                drawBase.imouse = drawBase.icaret;
                drawBase.jmouse = drawBase.jcaret;
            }
        }
    }


    private class MyMouseZoomer implements MouseWheelListener
    {
        public void mouseWheelMoved(MouseWheelEvent mwe)
        {
            int i = mwe.getWheelRotation();
            int j = "T".equals(Globals.reg.getuo(UO_GRAPH, 0)) ? 1 : -1;
            boolean bShift = mwe.isShiftDown(); 
            if (i*j>0)
              if (bShift)
                manageVertZoomIn();
              else
                manageZoomIn();
            if (i*j<0)
              if (bShift)
                manageVertZoomOut();
              else
                manageZoomOut(); 
        }
    }


    //---------Output routines---------------

    private void renderList(ArrayList<XYZO> aList, Graphics2D gX,
                            double dStereo, boolean bPreClear)
    // Renders a given List onto a given Graphics2D.
    // Called by redo(), doCAD(), and drawPage(). 
    // Renders g2Tech and gAnno. 
    // So use an EXPLICIT clearRect() at start of g2Tech.
    // A clearRect() here would have gAnno obliterate g2Tech,
    //   if gAnno contained a SETXXXBKG as its initial element. 
    // CenterOrigin for character locations. 
    // adopted BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
    {
        final double DECISIONRADIUS = 100000.0;

        if ((aList==null) || (gX==null) || (aList.size()<1))
          return; 

        int fontcodeprev=0;

        gX.setPaintMode();

        gX.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                                      BasicStroke.JOIN_ROUND));   
        final int MAXPOLY = 2020; 
        int polycount = 0; 
        int ipoly[] = new int[MAXPOLY+1];  // pixels
        int jpoly[] = new int[MAXPOLY+1];  // pixels
        float fdash[] = {3.0f, 2.0f};      // pixels
        int pxoffset=0, pyoffset=0;        // pixels, for centering chars

        if (drawBase.getRadius(aList) > DECISIONRADIUS)
          drawBase.localclip(aList);

        for (int t=0; t<aList.size(); t++)
        {
            // convert list into screen pixels for use by gX....
            XYZO myXYZO = aList.get(t); 
            
            double x = myXYZO.getX();  // rightward
            double y = myXYZO.getY();  // upward
            double z = myXYZO.getZ();  // out of screen 
            z -= drawBase.uzcenter;             // for stereo balance
            float  fline = 0.0f;       // for line widths

            int ipx = drawBase.getIXPIX( (int) (x - STEREO*dStereo*z));
            int ipy = drawBase.getIYPIX( (int) y);

            int opint = aList.get(t).getO(); 
            int opcode = opint % 1000;  
            int fontcode = opint / 1000;         // = ibold + 10*fontsize

            switch (opcode)
            {
               case SETWHITEBKG:  // can only be set as zeroth element
                 if ((t==0) && bPreClear)
                 {
                     if (dStereo==0.0)
                     {
                         gX.setBackground(Color.WHITE);
                         gX.clearRect(0, 0, dim.width, dim.height); 
                         gX.setColor(Color.BLACK); 
                     }
                     else
                     {
                         gX.setBackground(Color.BLACK);
                         gX.clearRect(0, 0, dim.width, dim.height); 
                         if (dStereo > 0.0)
                         {
                             gX.setColor(Color.BLUE); 
                         }
                         if (dStereo < 0.0)
                         {
                             gX.setColor(DRED); 
                         }
                     }
                 }
                 break; 

               case SETBLACKBKG:  // can only be set as zeroth element
                 if ((t==0) && bPreClear)
                 {
                     gX.setBackground(Color.BLACK);
                     gX.clearRect(0, 0, dim.width, dim.height); 
                     gX.setColor(Color.WHITE); 
                     if (dStereo > 0.0)
                     {
                         gX.setColor(Color.BLUE); 
                     }
                     if (dStereo < 0.0)
                     {
                         gX.setColor(DRED); 
                     }
                 }
                 break; 

               case SETSOLIDLINE: 
                  x = Math.max(0.0, Math.min(5.0, x)); 
                  fline = (x==0.0) ? 1.0f : (float) x; 
                  gX.setStroke(new BasicStroke(fline, 
                     BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));   
                  break; 

               case SETDOTTEDLINE:
                  x = U.minmax(x, 0.0, 5.0);; 
                  fline = (x==0.0) ? 1.0f : (float) x;                               
                  gX.setStroke(new BasicStroke(fline, BasicStroke.CAP_BUTT,
                     BasicStroke.JOIN_ROUND, fline+2.0f, fdash, 0.0f));
                  break; 
                  
               case SETRGB:
                  int r = (int) Math.round(255.0 * U.minmax(x, 0, 1)); 
                  int g = (int) Math.round(255.0 * U.minmax(y, 0, 1)); 
                  int b = (int) Math.round(255.0 * U.minmax(z, 0, 1)); 
                  gX.setPaint(new Color(r,g,b)); 
                  break; 
                  
               case MOVETO:
                  polycount=0; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  break; 

               case PATHTO:
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  break; 

               case STROKE:
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  polycount++;
                  gX.drawPolyline(ipoly, jpoly, polycount); 
                  polycount = 0; 
                  break; 

               case FILL:
                  // use existing color setting
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  polycount++; 
                  gX.fillPolygon(ipoly, jpoly, polycount); 
                  polycount = 0; 
                  break; 

               default:
                  if ((opcode>31) && (opcode<127))  // ASCII characters
                  {
                     if (fontcode != fontcodeprev)  // new fontcode.
                     {
                        int fpoints = fontcode / 10;
                        int ibold = fontcode % 2; 
                        Font f = new Font("Monospaced", ibold, fpoints); 
                        gX.setFont(f); 
                        // FontRenderContext frc = gX.getFontRenderContext();
                        // charwidth = (int) f.getStringBounds("a", frc).getWidth();
                        pxoffset = 1 + fpoints/4;
                        pyoffset = fpoints/3; 
                        fontcodeprev = fontcode; 
                     }
                     gX.drawString(""+(char)opcode, ipx-pxoffset, ipy+pyoffset); 
                  }
                  else if ((opcode>=130) && (opcode<=179)) // colored symbols
                  {
                      int colorcode = opcode % 10; // native colorcodes are 0....9

                      if (dStereo > 0.0)
                        colorcode = BLUE;
                      if (dStereo < 0.0)
                        colorcode = 10;    // is now DRED; 

                      switch (colorcode)
                      { 
                          case BLACK:   gX.setPaint(Color.BLACK); break; 
                          case RED:     gX.setPaint(Color.RED); break; 
                          case GREEN:   gX.setPaint(Color.GREEN); break; 
                          case YELLOW:  gX.setPaint(Color.YELLOW); break; 
                          case BLUE:    gX.setPaint(Color.BLUE); break; 
                          case MAGENTA: gX.setPaint(Color.MAGENTA); break;                       
                          case CYAN:    gX.setPaint(Color.CYAN); break; 
                          case WHITE:   gX.setPaint(Color.WHITE); break; 
                          case LTGRAY:  gX.setPaint(LGRAY); break;                    
                          case DKGRAY:  gX.setPaint(DGRAY); break; 
                          case 10:      gX.setPaint(DRED); break; 
                      }
                      int dotcode = opcode - (opcode % 10); 
                      switch (dotcode)
                      {
                        case DOT: 
                          gX.drawLine(ipx, ipy-1, ipx+1, ipy-1);
                          gX.drawLine(ipx, ipy,   ipx+1, ipy); 
                          break; 

                        case PLUS:
                          gX.drawLine(ipx-2, ipy, ipx+2, ipy); 
                          gX.drawLine(ipx, ipy-2, ipx, ipy+2); 
                          break; 

                        case SQUARE:
                          gX.drawLine(ipx-1, ipy-1, ipx+1, ipy-1); 
                          gX.drawLine(ipx-1, ipy,   ipx+1, ipy); 
                          gX.drawLine(ipx-1, ipy+1, ipx+1, ipy+1); 
                          break; 

                        case DIAMOND:
                          gX.drawLine(ipx,   ipy-2, ipx,   ipy-2); 
                          gX.drawLine(ipx-1, ipy-1, ipx+1, ipy-1); 
                          gX.drawLine(ipx-2, ipy,   ipx+2, ipy); 
                          gX.drawLine(ipx-1, ipy+1, ipx+1, ipy+1); 
                          gX.drawLine(ipx,   ipy+2, ipx,   ipy+2); 
                          break; 

                        case SETCOLOR:  break; // handled above.
                      }
                  }
                  break; 
            } // end switch(opcode)
        } // end for()
    } // end renderList()



    private void renderListTwice(ArrayList<XYZO> aList, BufferedImage bi, 
         double dStereo, boolean bPreClear)
    // Calls renderList() twice to create a stereo pair. 
    // Triple OR avoids clobbering underlying artwork.
    // Makes cleanup doFinish() unnecessary. 
    {
       BufferedImage biR = new BufferedImage(dim.width, dim.height, 
                 BufferedImage.TYPE_INT_RGB); 
       Graphics2D g2R = (Graphics2D) biR.getGraphics(); 
       renderList(aList, g2R, -dStereo, bPreClear);  // -dS gives red image

       BufferedImage biB = new BufferedImage(dim.width, dim.height, 
                 BufferedImage.TYPE_INT_RGB); 
       Graphics2D g2B = (Graphics2D) biB.getGraphics(); 
       renderList(aList, g2B, dStereo, bPreClear);   // +dS gives blue image

       for (int i=0; i<dim.width; i++)
         for (int j=0; j<dim.height; j++)
           bi.setRGB(i,j, bi.getRGB(i,j) | biR.getRGB(i,j) | biB.getRGB(i,j)); 
    }



    static int getFontWidth(Graphics2D gX, Font fX)
    // Given a size:   int size = 32; 
    // Define a font:  Font font = new Font("Monospaced", Font.BOLD, size);
    // Set the font:   gX.setFont(font);
    // Then, call this:
    {
        FontRenderContext frc = gX.getFontRenderContext();
        return (int) fX.getStringBounds("a", frc).getWidth(); 
    }

} //------end GPanel--------
