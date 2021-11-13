package com.stellarsoftware.beam.ui;

import static com.stellarsoftware.beam.core.Globals.RT13;

import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.RAYDataModel;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.render.DrawPlot2;

@SuppressWarnings("serial")

/**
  * A207.11: eliminated groups; using bimodals; 
  *    dropping "additional surface" option;
  *    uses howfarOK[] and skips negative zero data points.
  *
  * Custom artwork class extends GPanel, supplies artwork.
  * Generates local font information from GetUOGraphicsFontCode().
  * Properly responds to changes in nsurfs & nrays  (A112). 
  * Random implements doRandomRay: adds 1 ray artwork to myRandList.
  *
  * Implements CenterOrigin for character locations. 
  * Implements additional surface "jOtherSurface" 
  * Not yet implemented: optical path. 
  * Not yet implemented: manual scaling; diam scaling. 
  * Does caret shut down when focus is lost?
  *
  * Adopting explicit QBASE methods.
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
  */
class Plot2Panel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances------

    private DrawPlot2 drawPlot2;

    //----------------public methods------------------------

    Plot2Panel(GJIF gj)
    {
        super(new DrawPlot2());
        // implicitly calls super() with no arguments
        myGJIF = gj;            // protected; used here & GPanel
        drawPlot2 = (DrawPlot2) drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawPlot2.doTechList(bArtStatus);
    }


    protected void doCursor(int ix, int iy)  // replaces abstract method
    // Given mouse cursor coordinates in pixels, 
    // delivers current cursor user coordinates
    // or if outside window it refreshes GJIF title/warning.
    {
        if (ix<0)
          myGJIF.cleanupTitle(); // retains any warnings
        else
          myGJIF.postCoords("Hor=" + U.fwd(drawPlot2.getuxPixel(ix),18,6).trim()
                       + "  Vert=" + U.fwd(drawPlot2.getuyPixel(iy),18,6).trim());
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 

}  //----------end of public class------------------    

