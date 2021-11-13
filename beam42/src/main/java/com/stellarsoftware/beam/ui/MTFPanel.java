package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.render.DrawMTF;

import java.io.*;          // fileWriter
import javax.swing.*;      // Graphics2D features

@SuppressWarnings("serial")

/**
  *
  * Custom artwork class furnishes artwork to GPanel.
  *
  * Font details are generated here when needed; uses LowerLeftOrigin.
  *
  * Needs to get properly centered and sized. Rect not square. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  */
class MTFPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;


    private H1DPanel myH1DPanel = null; 
    private DrawMTF drawMTF;

    MTFPanel(GJIF gj)
    {
        super(null);
        myGJIF = gj;     // protected; used here & GPanel

        // The following will get the most recently constructed g1D
        // but this is not necessarily the g1D that is currently in front. 
        
        GJIF g1D = DMF.gjifTypes[RM_H1D];
        if (g1D == null)
        {
            return; 
        }
        myH1DPanel = (H1DPanel) g1D.getGPanel();
        drawMTF = new DrawMTF(myH1DPanel.drawH1D);

    }

//-----------protected methods concretizing GPanel-------

    @Override
    protected void buildTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel when fresh artwork is needed:
    // Ignores bFullArt, always writes complete diagram.
    {
        drawMTF.doTechList(bFullArt);
    }

    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return; 
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

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
            for (int i=0; i<drawMTF.nbins; i++)
              pw.println(U.fwd(drawMTF.dPower[i],12,6));
            fw.close();
        }
        catch (Exception e)
        {}
    }

}

