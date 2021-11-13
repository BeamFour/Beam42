package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.render.DrawH1D;

import javax.swing.*;      // Graphics2D features
import java.io.*;          // Save as file


@SuppressWarnings("serial")

/**
  * H1DPanel draws a 1D binned ray histogram.
  * A207: eliminating groups.
  * Custom artwork class furnishes TechList to GPanel.
  * Rendering is done within GPanel. 
  *
  * LIKE other art, creates new histo when UO=OK
  *  or when nsurfs or nrays change.  
  *
  * Unit square coordinates, like P2D unit cube coordinates:
  * no intermediate scale factors but instead scales integers.
  *
  * Has Auto, Diameter, and Manual spans.
  * Uses precomputed tick values. 
  *
  * Artwork:  GPanel calls this.doTechList(). 
  *
  * Repaint: handled within GPanel using blit.
  *
  * Zoom/Pan: GPanel manages zoom/pan.....
  *     modify zoom coefficients; 
  *     gTech.dispose();
  *     repaint();
  * ...which forces a call to this.doTechList().
  *
  * Random ray:  calls this.addRandomRay()....
  *   ... which augments histo[] without updating artwork.
  *   
  * Random ray burst repaint is done by GPanel::redo()....
  *     gTech.dispose(); 
  *     repaint(); 
  * ...which forces a call to this.doTechList() thereby
  *  showing the current contents of the histogram. 
  *
  *  A169 March 2015: added average display of histogrammed data;
  *  also improved the typeface scaling and locations.
  * 
  * @author M.Lampton (c) STELLAR SOFTWARE 2004, 2015 all rights reserved.
  */
class H1DPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;
    
    DrawH1D drawH1D;


    H1DPanel(GJIF gj) // the constructor
    // Called by GJIF to begin a new P1D panel.
    // Sets up parameters, runs table rays, builds initial histogram.
    // No artwork here.
    // Later, GJIF will display its GPanel, hence doTechList() below.
    // paintComponent(), annotate() etc are done in GPanel.
    {
        // implicitly calls super() with no arguments
        super(new DrawH1D());
        myGJIF = gj;           // protected; used here & GPanel
        drawH1D = (DrawH1D) drawBase;
    }

    @Override
    void buildTechList(boolean bArtStatus) {
        drawH1D.doTechList(bArtStatus);
    }


    //-----------protected methods----------------


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
            for (int i=0; i<drawH1D.nbins; i++)
              pw.println(drawH1D.histo[i]);
            fw.close();
        }
        catch (Exception e)
        {}
    } 


    protected void doPlainSaveData()  // stupider version of the above
    {
        File file = new File("H1D.TXT");   // import java.io.*;
        FileWriter fw = null;              // import java.io.*;
        PrintWriter pw = null;             // import java.io.*;
        try
        {
            fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            for (int i=0; i<drawH1D.nbins; i++)
              pw.println(drawH1D.histo[i]);
            fw.close();
        }
        catch (Exception e)
        {}
    } 

}
