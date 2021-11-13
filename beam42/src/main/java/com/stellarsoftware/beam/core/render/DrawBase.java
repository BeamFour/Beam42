package com.stellarsoftware.beam.core.render;

import com.stellarsoftware.beam.core.B4constants;
import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.XYZO;

import java.util.*;        // ArrayList


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
public abstract class DrawBase implements B4constants
{
    static final int FONT_BOLD = 1;
    static final int FONT_PLAIN = 0;

    //-----Each extension must supply values for the following---------

    public boolean bClobber;              // random = destroy prev art
    public boolean bPleaseParseUO;        // set by Options; reset by extension.
    protected double uxcenter = 0.0;         // set by extension setLocals() horiz
    protected double uycenter = 0.0;         // set by extension setLocals() vert
    public double uzcenter = 0.0;         // set by extension setLocals() depth
    protected double uxspan = 1.0;           // set by extension setLocals() horiz
    protected double uyspan = 1.0;           // set by extension setLocals() vert
    protected double uzspan = 1.0;           // set by extension setLocals() depth
    public double uxanchor = 0.0;         // set by mouse
    public double uyanchor = 0.0;         // set by mouse
    protected double uzanchor = 0.0;         // unused.
    protected double dUOpixels = 500.0;      // window pixel size set by User Options
    protected int    iEdits;                 // to compare with DMF.nEdits

    //---Abstract "do" methods; each extension must implement these-----

    public abstract void    doTechList(boolean bArtStatus);
    abstract void    doRotate(int i, int j);
    public abstract boolean doRandomRay();
//    abstract void    doCursor(int i, int j);
//    abstract double  getStereo();
//    abstract void    doSaveData();

    //----QuadLists available internally for assembling artwork----------------
    //---client users will call these using QBASE, QBATCH etc------------------

    public ArrayList<XYZO> baseList;    // vector art for Tech drawing
    public ArrayList<XYZO> batchList;   // vector art for random batch
    public ArrayList<XYZO> randList;    // vector art for accumulated random rays
    public ArrayList<XYZO> finishList;  // vector art finishing Layouts
    public ArrayList<XYZO> annoList;    // vector art for annotation

    //--------Constructor---------------------

    DrawBase()  // host class for artwork generators
    {
        ///// testing suggestion from StackOverflow...
        ///// this.setDoubleBuffered(false);
        ///// does it work?  Nope.

        baseList   = new ArrayList<XYZO>();
        batchList  = new ArrayList<XYZO>();
        randList   = new ArrayList<XYZO>();
        finishList = new ArrayList<XYZO>();
        annoList   = new ArrayList<XYZO>();

        iEdits = Globals.nEdits;
    }


    public void requestNewArtwork()
    // Allows each AutoAdjust() iteration to request fresh Layout artwork.
    // Don't re-parse UO or sizes when this is called.
    // Just let drawPage() regenerate its new g2Tech and render it.
    {
        bArtStatus = true;  // local stash for when OS paints.
        annoList.clear();
    }

    public void doUpdateUO()
    // Options calls this via GJIF when options change
    {
        bPleaseParseUO = true; // flag allows client doParse().
        bArtStatus = bFULLART; // stash for when OS paints
        annoList.clear();      // discard old artwork
    }


    public void doQualifiedRedraw()
    // GJIF calls this when coming forward: check for table edits.
    // How to implement bSticky, retain previous magnification?
    {
        if (Globals.nEdits == iEdits)
            return;              // no redraw needed.
        iEdits = Globals.nEdits;   // update local edits count
        // bPleaseParseUO = true; // flag allows client doParse().
        bArtStatus = bFULLART; // stash for when OS paints
        annoList.clear();    // discard old artwork
    }

    //---------protected methods for client use-----------

    protected void clearList(int which)
    {
        switch(which)
        {
            case QBASE:   baseList.clear(); break;
            case QBATCH:  batchList.clear(); break;
            case QRAND:   randList.clear(); break;
            case QFINISH: finishList.clear(); break;
            case QANNO:   annoList.clear(); break;
        }
    }

    protected void addRaw(double x, double y, double z, int op, int which)
    // unscaled; for linewidths, font sizes, colors....
    {
        XYZO quad = new XYZO(x, y, z, op);
        switch(which)
        {
            case QBASE:   baseList.add(quad); break;
            case QBATCH:  batchList.add(quad); break;
            case QRAND:   randList.add(quad); break;
            case QFINISH: finishList.add(quad); break;
            case QANNO:   annoList.add(quad); break;
        }
    }

    protected void addScaled(double xyz[], int op, int which)
    // scaled by the getXX() functions converting user to screen coordinates.
    {
        XYZO quad = new XYZO(getax(xyz[0]), getay(xyz[1]), getaz(xyz[2]), op);
        switch(which)
        {
            case QBASE:   baseList.add(quad); break;
            case QBATCH:  batchList.add(quad); break;
            case QRAND:   randList.add(quad); break;
            case QFINISH: finishList.add(quad); break;
            case QANNO:   annoList.add(quad); break;
        }
    }

    protected void addScaled(double x, double y, double z, int op, int which)
    // as above but with explicit coordinates
    {
        double xyz[] = {x, y, z};
        addScaled(xyz, op, which);
    }



    //--------------private & client support area-----------
    //--------------private & client support area-----------
    //--------------private & client support area-----------


    //----locally stashed fields---------------------


    // bArtStatus is stashed locally because it is set during
    // each mouse action yet must be made available at an unknown
    // future time when the OS repaints the artwork. Its value
    // specifies whether a skeleton or a fullart is wanted.

    public boolean bArtStatus = bFULLART;

    //------caret wheel zoom support for clients-----------

    public int icaret=250;
    public int jcaret=250;      // pixels
    public int imid=250;
    public int jmid=250;          // pixels
    public int imouse=0;
    public int jmouse=0;          // pixels


    public String sScaleFactors()
    //  pixels per user unit scalefactors();
    //  Can be used to substitute myGJIF.setTitle();
    {
        if (Math.abs(uxspan) < TOL)
            return "";
        if (Math.abs(uyspan) < TOL)
            return "";
        double hscale = dUOpixels / uxspan;
        double vscale = dUOpixels / uyspan;
        double ratio = uxspan / uyspan;
        return "Hor="+ U.fwd(hscale,8,2).trim()
                +"  Vert="+U.fwd(vscale,8,2).trim()
                +"  Ratio="+U.fwd(ratio,6,2).trim();
    }


    //----------helpers for this GPanel drawPage() doing annotation----------

    public int getUOAnnoFont()
    {
        int i = U.parseInt(Globals.reg.getuo(UO_GRAPH, 4));
        return Math.max(3, Math.min(100, i));
    }

    private int getUOAnnoBold()
    // as of Nov 2005, Font.BOLD=1, Font.PLAIN=0
    {
       return "T".equals(Globals.reg.getuo(UO_GRAPH, 5)) ? FONT_BOLD : FONT_PLAIN;
    }

    public int getUOAnnoFontCode()  // adds to ASCII
    {
        return 10000*getUOAnnoFont() + 1000*getUOAnnoBold();
    }

    private int getFixedAnnoFontCode()  // adds to ASCII, no UO
    {
        return 120000;   // 12 point plain
    }


    //------helpers for client assembling its quadList-----------

    protected int getUOGraphicsFont()
    {
        return U.parseInt(Globals.reg.getuo(UO_GRAPH, 2));
    }

    protected int getUOGraphicsBold()
    // as of Nov 2005, Font.BOLD=1, Font.PLAIN=0
    {
        return "T".equals(Globals.reg.getuo(UO_GRAPH, 3)) ? FONT_BOLD : FONT_PLAIN;
    }

    public int getUOGraphicsFontCode()  // adds to ASCII
    {
        return 10000*getUOGraphicsFont() + 1000*getUOGraphicsBold();
    }



    protected void addAffines()
    // stash user affine consts & slopes for DXF: pix->UserUnits
    {
        baseList.add(new XYZO(uxcenter, uycenter, uzcenter, USERCONSTS));
        double d = (dUOpixels > 0) ? dUOpixels : 500.0;
        baseList.add(new XYZO(uxspan/d, uyspan/d, uzspan/d, USERSLOPES));
    }



    //---------these helpers are general purpose--------------

    protected double getax(double ux)
    // converts userX to graphic coords: mouse drag, zoom, etc
    // Used by addScaledItem() here and in client code
    {
        return dUOpixels*(ux-uxcenter)/uxspan;
    }


    protected double getay(double uy)
    // converts userY to graphic y; mouse drag, zoom, etc
    // Used by addScaledItem() here and in client code
    {
        return dUOpixels*(uy-uycenter)/uyspan;
    }


    protected double getaz(double uz)
    // zero offset, and shares scale factor with yaxis.
    // No mouse drag or zoom in addScaledItem.
    {
        return dUOpixels*uz/uyspan;
    }


    public double getux(double ax)
    // converts annoX to userX
    {
        return uxcenter + ax*uxspan/dUOpixels;
    }


    public double getuy(double ay)
    // converts annoY to userY
    {
        return uycenter + ay*uyspan/dUOpixels;
    }


    //----------helpers for pixel rendering---------

    public double getuxPixel(int ipix)
    // Converts raw pixel coord into user coord ux.
    // Used by clients to show cursor coords in user space.
    {
        return uxcenter + (ipix-imid)*uxspan/dUOpixels;
    }


    public double getuyPixel(int jpix)
    // Converts raw pixel coord into user coord uy.
    // Used by clients to show cursor coords in user space.
    {
        return uycenter - (jpix-jmid)*uyspan/dUOpixels;
    }


    public double getAXPIX(int ipix) // pixel->annoPoints
    {
        return (double)(ipix - imid);
    }


    public double getAYPIX(int jpix) // pixel->annoPoints
    {
        return (double)(jmid - jpix);
    }


    public int getIXPIX(double x)  // annoPoints->pixel
    {
        return (int)(imid + x);
    }


    public int getIYPIX(double y)  // annoPoints->pixel
    {
        return (int)(jmid - y);
    }


    public int getUOpixels()
    // Returns User Option window size in pixels.
    {
        int i = U.parseInt(Globals.reg.getuo(UO_GRAPH, 6));
        if (i<=10)
            i = 500;
        return Math.min(3000, Math.max(100, i));
    }




    //----------annotation charlist management-------------

    public char getCurrentChar()
    {
        int len = annoList.size();
        if (len > 0)
        {
            int k = annoList.get(len-1).getO();
            if (k<=127)
                return (char) k;
        }
        return (char) 0;
    }


    public void setNextCaretCoords(int igiven)
    // Can I eliminate this entirely??
    // and then eliminate getIYPIX().. etc?  Nope.
    {
        int len = annoList.size();
        if (len > 0)
        {
            icaret = igiven + getIXPIX(annoList.get(len-1).getI());
            jcaret = getIYPIX(annoList.get(len-1).getJ());
            return;
        }

        // treat case of empty annoList length....
        icaret = imouse;
        jcaret = jmouse;
    }

    public void doKeyEnter(int charH) {
        icaret = imouse;
        jcaret += charH;
        addAnno(getAXPIX(icaret), getAYPIX(jcaret), '\n');
    }

    public void addAnno(double x, double y, char c)
    {
        int i = (int) c + getUOAnnoFontCode();
        annoList.add(new XYZO(x, y, 0.0, i));
    }


    public void deleteLastAnno()  // for backspace.
    {
        int i = annoList.size();
        if (i>0)
            annoList.remove(annoList.get(i-1));
    }


    private double getUserSlope()  // ZoomIn limiter
    {
        if (baseList==null)
            return 1.0;
        int reach = Math.min(baseList.size(), 5);
        for (int i=0; i<reach; i++)
        {
            XYZO myXYZO = baseList.get(i);
            if (myXYZO.getO() == USERSLOPES)
                return myXYZO.getX();
        }
        return 1.0;
    }




    //----------support for mouse pan zoom twirl-------------------

    public void getNewMouseArtwork(boolean bFinal)
    // protected allowing thread update.
    // Don't re-parse UO or sizes when this is called!
    // Called by Zoom, mouseDragTranslate, rotate.
    // Not called for simple mousePressed/mouseReleased.
    // bFinal=false when mouse is still down and skeleton is wanted.
    // bFinal=true when mouse is released and final artwork is wanted.
    {
        bArtStatus = bFinal; // local stash for when OS paints.
        annoList.clear();
    }


    public void manageZoomIn()  // called by F7 and Wheel
    // ZoomIn centered on the current caret location.
    // New artwork will use the new centers & spans.
    {
        double dzoom = 1.0 - ZOOMOUT; // here ZOOMOUT=0.7071..
        uxcenter += dzoom * uxspan*(icaret-imid)/dUOpixels;
        uycenter += dzoom * uyspan*(jmid-jcaret)/dUOpixels;
        if (getUserSlope() > 1E-14)
        {
            uxspan *= ZOOMOUT;
            uyspan *= ZOOMOUT;
            uzspan *= ZOOMOUT;
        }
    }

    public void manageVertZoomIn()  // called by F5 and WheelShift
    // ZoomIn centered on the current caret location.
    // New artwork will use the new centers & spans.
    {
        double dzoom = 1.0 - ZOOMOUT;
        uycenter += dzoom * uyspan*(jmid-jcaret)/dUOpixels;
        if (getUserSlope() > 1E-14)
        {
            uyspan *= ZOOMOUT;
        }
    }


    public void manageZoomOut()  // called by F8 and Wheel
    {
        double dzoom = (1.0/ZOOMOUT)-1.0;
        uxcenter -= dzoom * uxspan*(icaret-imid)/dUOpixels;
        uycenter -= dzoom * uyspan*(jmid-jcaret)/dUOpixels;
        uxspan /= ZOOMOUT;
        uyspan /= ZOOMOUT;
        uzspan /= ZOOMOUT;
    }

    public void manageVertZoomOut()  // called by F6 and WheelShift
    {
        double dzoom = (1.0/ZOOMOUT)-1.0;
        uycenter -= dzoom * uyspan*(jmid-jcaret)/dUOpixels;
        uyspan /= ZOOMOUT;
    }

    public void manageDragTranslate(int di, int dj)
    // Called by drag.
    // New artwork will use the modified centers.
    {
        uxcenter -= uxspan*(di)/dUOpixels;
        uycenter += uyspan*(dj)/dUOpixels;
    }


    public void manageDragRotate(int i, int j)
    // Called by drag.
    // Relies upon client's doRotate() to create rotated artwork.
    // Uses average outOf zzz of target objects to translate image.
    // This translation is done here, without client help.
    {
        double daz = (i/3)/57.3;  // radians azimuth change
        double del = (j/3)/57.3;  // radians elevation change
        double zzz=0;             // sum & average zvertex
        double xs=1, ys=1;        // slopes: userUnits/point
        int ncount = 0;
        if (baseList == null)
            return;
        int npts = Math.min(1000, baseList.size());
        if (npts < 1)
            return;

        for (int k=0; k<npts; k++)
        {
            XYZO m = baseList.get(k);
            int op = m.getO();
            double x = m.getX();
            double y = m.getY();
            double z = m.getZ();
            if (op==USERSLOPES) // has defined scale factors
            {
                xs = m.getX();
                ys = m.getY();
                continue;
            }
            if ((op==MOVETO) || (op==PATHTO) || (op==STROKE))
            {
                if ((x>-200) && (x<200) && (y>-200) && (y<200))
                {
                    zzz += z;
                    ncount++;
                }
            }
            if (op==COMMENTRULER) // exclude furniture from average
                break;
        }
        if (ncount > 1)
        {
            zzz /= ncount;
            uxcenter += xs * zzz * daz;
            uycenter -= ys * zzz * del;
        }
        doRotate(i, j);                 // Have client modify sinel & cosel.
    }



    //---------Output routines---------------


    public double getRadius(ArrayList<XYZO> xxList)
    // evaluates the max (x,y) radius of XYZOs in xxList
    {
        double r = 0.0;
        for (int i=0; i<xxList.size(); i++)
        {
            XYZO myXYZO = xxList.get(i);
            double x = myXYZO.getX();
            double y = myXYZO.getY();
            r = Math.max(r, Math.max(Math.abs(x), Math.abs(y)));
        }
        return r;
    }


    public void localclip(ArrayList<XYZO> xxList)
    // Clips artwork to a box, double precision.
    // Uses Clipper to do the dirty work.
    // Also unpacks polylines into separate line segments.
    // Discards all invisible parts.
    // Interprets fills as boundary line segments.
    // Clipped segments lose their third dimension, sorry.
    {
        ArrayList<XYZO> sList = new ArrayList<XYZO>();

        // copy the given xxList over to become our source...
        for (int i=0; i<xxList.size(); i++)
        {
            sList.add(xxList.get(i));
        }

        // now empty the given aList...
        xxList.clear();

        // now set up a clipper...
        Clipper myClip = new Clipper(-1000, -1000, 1000, 1000);
        double vec[] = new double[4];
        XYZO myXYZO;
        for (int t=0; t<sList.size(); t++)
        {
            myXYZO = sList.get(t); // copy preexisting object
            double x = myXYZO.getX();
            double y = myXYZO.getY();
            int op = myXYZO.getO();
            int opcode = op % 1000;

            switch(opcode)
            {
                case MOVETO:  // start decomposing this polyline...
                    vec[2] = x;
                    vec[3] = y;
                    break;

                case PATHTO:  // these are the same now
                case STROKE:  // these are the same now
                case FILL:    // added, eliminating skipto
                    vec[0] = vec[2];
                    vec[1] = vec[3];
                    vec[2] = x;
                    vec[3] = y;
                    if (myClip.clip(vec))
                    {
                        XYZO tempXYZO = new XYZO(vec[0], vec[1], 0.0, MOVETO);
                        xxList.add(tempXYZO);
                        tempXYZO = new XYZO(vec[2], vec[3], 0.0, STROKE);
                        xxList.add(tempXYZO);
                        // xxList.add(new XYZO(vec[0], vec[1], 0.0, MOVETO));
                        // xxList.add(new XYZO(vec[2], vec[3], 0.0, STROKE));
                    }
                    break;

                default:   // deal with singletons here...
                    if( Math.max(Math.abs(x), Math.abs(y)) < 1000)
                        xxList.add(new XYZO(x, y, 0.0, op));
                    break;
            }
        }
    }



} //------end GPanel--------
