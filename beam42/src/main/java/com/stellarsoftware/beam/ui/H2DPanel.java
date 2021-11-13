package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.render.DrawH2D;

import javax.swing.*;      // Graphics2D; JFileChooser
import java.io.*;          // Save Data

@SuppressWarnings("serial")

/**
  * H2DPanel extends GPanel, draws 2D binned ray histogram.
  * Random ray responder is installed.
  * A207: eliminates groups
  * 
  * Because the x, y, and z scaling factors are different,
  * integer bins & counts are scaled to a unit cube for display.
  *
  * Automatic scaling only, so far:  
  * Manual & Diameter scaling is not yet installed. 
  * 
  * Converted to QBASE, March 2015, line 655. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
  */
class H2DPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances----
    private DrawH2D drawH2D;


    H2DPanel(GJIF gj)
    {
        super(new DrawH2D());
        // implicitly calls super() with no arguments
        myGJIF = gj;            // protected; used here & GPanel
        drawH2D = (DrawH2D) drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawBase.doTechList(bArtStatus);
    }

    @Override
    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return; 
    }

    @Override
    protected double getStereo()    // replaces abstract "get" method
    {
        double d = 0.0; 
        boolean bS = "T".equals(Globals.reg.getuo(UO_2D, 17));
        if (bS)
        {
            String ss = Globals.reg.getuo(UO_2D, 18);
            d = U.suckDouble(ss); 
            if (d == Double.NaN)
              d = 0.0; 
        }
        return d; 
    }

    @Override
    protected void doSaveData()     // replaces abstract "do" method
    {
        JFileChooser fc = new JFileChooser(); // import javax.swing.*; 
        String sDir = DMF.sCurrentDir; 
        if (sDir != null)
        {
            File fDir = new File(sDir); 
            if (fDir != null)
              if (fDir.isDirectory())
                fc.setCurrentDirectory(fDir);
        } 
        int q = fc.showSaveDialog(null); 
        if (q == JFileChooser.CANCEL_OPTION)
          return; 
        File file = fc.getSelectedFile(); 
        if (file == null)
          return; 

        FileWriter fw = null;              // import java.io.*;
        PrintWriter pw = null;             // import java.io.*;
        try
        {
            fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            for (int j=0; j<drawH2D.nvbins; j++)
            {
                for (int i=0; i<drawH2D.nhbins-1; i++)
                  pw.print(drawH2D.histo[i][j] + ", ");
                pw.println(drawH2D.histo[drawH2D.nhbins-1][j]);
            }
            fw.close();
        }
        catch (Exception e)
        {}
    } 

}
