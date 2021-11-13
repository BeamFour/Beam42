package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.Globals;
import static com.stellarsoftware.beam.core.Globals.RT13;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.render.DrawMPlot;

import java.util.*;        // ArrayList

@SuppressWarnings("serial")

/** MultiPlot Artwork Generator
  * A207: eliminating groups
  * A173: Adopting five GPanel quadLists and GPanel helper methods.
  *
  * A150: uses new Options dialog with explicit lists of variable values;
  *   allows wavelength & color to be linked to U0 or V0.
  *   Uses a new feature: RT13.gwave permits commandeering wavelength.
  *   Color is local, so no commandeering needed on that score. 
  *
  *
  * Upgraded to kcolor[][][] for both box coords and ray number, April 2013.
  * kcolor[][][] is now initialized to ABSENT=-1
  *    here... line 355,408: kcolor[] is taken from Options color parser. 
  *    now.... line 645.... if (kcolor==ABSENT) then use RayStart color. 
  *    
  *
  * A150 planned improvement: set up a separate timer thread and
  *   display 1box, 2boxes, 3boxes... at each step modifying scales,
  *   until the whole pattern is done, then kill the timer.   
  *
  *
  * A145: Show boxes only out to radius _____.
  *
  * A136custom: reduced the title to <rms> only, line 735, for 1 column display
  * also the decoration numbers are MICRONS not user mm.
  *
  * A134: Random Ray feature is under construction but its menu is grayed.
  *
  * Previous version did not stash all result points
  *   and so could not show dynamic update with scaling;
  *   it had to do all boxes first with a black screen and
  *   then suddenly display the finished product.
  *
  * An improved scheme would be to stash all the result points
  *   and recompute the display scale, box by box, and show
  *   the partial computations: keep the user in the loop. 
  * 
  * Nice feature would be auto regenerate for new options and
  *   for changes to .OPT or .RAY.  
  *   Problem: how to detect if .OPT or .RAY has changed.
  *   Presently is spotty: OPT changes are detected (how?)
  *   and RAY changes in X0, Y0, @wave are detected (how?)
  *   yet RAY changes in U0, V0 are ignored (why?)
  *   Fix is to move the initialization out of the constructor
  *   and put it into doTechList. Works for XYZ; fails for P,T 22 Feb 2011.
  *   (That was due to a lack of Euler matrix updating.)
  *
  *   When fixed, please extend this feature to MapPanel.  DONE. 
  *
  * MultiPlot artwork class extends GPanel, supplies artwork.
  * Makes multiple plots of good table rays. 
  * No random ray capability.  Yet.
  * Can step through raystart params or optics params.
  * Suffix zero: raystart.  suffix 1...ngroups: optics.
  *
  *  A106: remote pupil feature. 
  *  A106: enlarged group of displayable box data.
  *
  *  A106: plot2, mplot, plot3 all need a way to 
  * exit gracefully from case of failed Parse() with clear
  * error dialog and no plot.  Have a flag bParseOK.
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2007-2015 all rights reserved.
  */
class MPlotPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    private DrawMPlot drawMPlot;

    MPlotPanel(GJIF gj)
    {
        super(new DrawMPlot());
        myGJIF = gj;           // protected; used here & GPanel
        drawMPlot = (DrawMPlot) drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawBase.doTechList(bArtStatus);
    }

    @Override
    protected void doCursor(int ix, int iy)  // replaces abstract method
    // posts current cursor.
    {
        myGJIF.setTitle("MultiPlot"); 
    }

    @Override
    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

    @Override
    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    }
}
