package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.render.DrawLayout;

@SuppressWarnings("serial")

/**
  * Custom artwork class furnishes layout artwork to GPanel.
  * 26 Dec 2018 A207 line 1123 eliminated negative surfaces
  * 30 Dec 2018 A207.11 skipping negative zero Z ray points -- line 1140; 
  *   ^^ this allows bimodals to skip surfaces leaving -0.0 there.
  *
  * Fontcode accompanies each char, and use CenterOrigin for fontXY.
  * Internal coord frame is user dimensions cube, center origin, +y=up.
  * Output coord frame is dUOpixels cube, center origin, +y=up.
  *
  *
  * Affines: uxcenter, uxspan, uycenter, uyspan, uzcenter, uzspan
  * are protected fields of Gpanel.
  * They are initially computed here by setScaleFactors().
  * Therefter, they are managed by Gpanel in its pan/zoom/twirl behavior. 
  *
  * Random calls doRandomRay: adds 1 ray to myRandList.
  * Draws NRI media shaded.
  *
  * June 2009: added user selectable linewidths for tidier layouts.
  * Rendered on-screen and in bitmaps by GPanel, limited to screen resolution.
  * Also available in PostScript (see CAD)  as of August 2009 for hi-rez.
  *
  * May 2010 A112 added groups. nsurfs for drawing surfs, ngroups for drawing rays. 
  * 
  * Nov 2010 A118 added Sticky view option: az, el, pan, zoom.
  * Layout now has an asterisk when it is Sticky. 
  *
  * May 2011 A126 added Connectors for refractive surface pairs. 
  *
  * July 2011 A127 added bRetroVisible controlling full artwork,
  *  but leaving Retro surfaces always visible in skeleton artwork.
  *
  * 5 Sept 2011 A129 improved separation of Layouts with differing display
  * factors, independent randoms: simultaneous views of one optic.
  *
  * Regeneration policy: no need to regenerate if nsurfs changes;
  *   therefore doParse() only at startup and UOptions change.
  *   This policy retains current pan & zoom.
  *
  * 3 Aug 2012 A136: zero width lines now disappear. 
  *
  * 18 Aug 2012 A136custom: trying {shading,arcs,rays,finish} in doFullArt() line 962
  *  ooops won't work: doFullArt is sorted, i.e. near shading must block distant arcs.
  *
  * 10 Oct 2012 A137: coordinate break introduced, line 1185
  *
  * M.Lampton STELLAR SOFTWARE (c) 2004-2012 all rights reserved.
  */
class LayoutPanel extends GPanel   // implements B4constants via GPanel
{
    // public static final long serialVersionUID = 42L;

    private DrawLayout drawLayout;


    LayoutPanel(GJIF gj)
    {
        super(new DrawLayout());
        myGJIF = gj;            // protected; used here & GPanel
        drawLayout = (DrawLayout)drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawBase.doTechList(bArtStatus);
    }

    @Override
    protected void doCursor(int ix, int iy)  // replaces abstract method
    // Called by cursor motion; 
    // Delivers current cursor coordinates to title bar.
    // NOTE: SCALEFACTOR DISPLAY DWELLS IN GPANEL NOT HERE. 
    {
        String title = drawLayout.bStickyUO ? "Layout (sticky)" : "Layout";
        if (ix<0)
          myGJIF.cleanupTitle(title); // retains any warnings
        else
          myGJIF.postCoords(title + "Hor=" + U.fwd(drawLayout.getuxPixel(ix),18,6).trim()
                               + "  Vert=" + U.fwd(drawLayout.getuyPixel(iy),18,6).trim());
    }

    @Override
    protected double getStereo()    // replaces abstract "get" method
    // Not used locally because local methods are pure monoscopic.
    // Allows GPanel base class to get user's stereo preference. 
    {
        if ("T".equals(Globals.reg.getuo(UO_LAYOUT, 17)))
        {
            String ss = Globals.reg.getuo(UO_LAYOUT, 18);
            double d = U.suckDouble(ss); 
            if (d == Double.NaN)
              d = 0.0; 
            return d; 
        }
        return 0.0; 
    }

    @Override
    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    }

}
