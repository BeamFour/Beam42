package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.render.DrawDemo;

@SuppressWarnings("serial")

/**
  * Custom artwork class furnishes artwork to GPanel.
  * Test article demonstrates Zsorting.
  * Fontcode accompanies each char, and use CenterOrigin for fontXY.
  * Internal coord frame is unit cube, center origin, +y=up.
  * Output coord frame is dUOpixels cube, center origin, +y=up.
  *
  * Adopting explicit baseList artwork. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
*/
class DemoPanel extends GPanel // implements Runnable
{
    // public static final long serialVersionUID = 42L;

    DrawDemo drawDemo;

    DemoPanel(GJIF gj) // constructor
    {
        super(new DrawDemo());
        myGJIF = gj;          // protected; used here & GPanel
        drawDemo = (DrawDemo) drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawDemo.doTechList(bArtStatus);
    }


    //-----protected methods-----------


    @Override
    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return;
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


