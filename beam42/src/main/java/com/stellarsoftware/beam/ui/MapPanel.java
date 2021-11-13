package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.render.DrawMap;

import java.awt.event.*;   // Events

@SuppressWarnings("serial")

/** Dec 2018 A207 eliminating groups (yay!)
  * March 2015: adopted explicit QBASE for artwork quads. 
  * March 2015: improved output text file, including .CSV format.
  * 2012: Scales added for HorVar and VertVar
  * 2011: With this plan every options starts a fresh new run.
  * But... changes in the optics or ray tables do not start a fresh run.
  * In MultiPlot, they do. 
  * Would be nice if ray or optics changes trigger a fresh run.
  *
  * Here we offer only WFE and PSF (x,y) not PSF(u,v). 
  *
  *========SCHEMATIC FLOW DRIVEN BY TIMER TICKS=====
  *
  *    MapPanel() constructor
  *    {
  *        startMap(); 
  *    }
  *
  *    startMap()  ---<<--from constructor or doTechList()
  *    {
  *       dList = new ArrayList<Double>();  
  *       doParseUO();
  *       startBunches();
  *    }
  *    
  *    startBunches()
  *    {
  *       create myTimer(assign task doTick());
  *    }
  *    
  *    doTick()
  *    {
  *       if (running)
  *       {
  *          doBunchRays();
  *          GPanel.redo(); --->>>-GPanel.redo(): calls repaint()
  *       }                    then OS calls GPanel.paintComponent()
  *    }                       which then calls GPanel.drawPage(g2)
  *                            which then calls doTechList() here.
  *    doBunchRays()
  *    // called as TimerTask doTick().
  *    {
  *       save & modify parms;
  *       run rays;
  *       calc result and add to list;
  *       restore parms.
  *    }
  *    
  *    doTechList()  ----<<<---called by GPanel.drawPage().
  *    {
  *       if (bPleaseParseUO) 
  *         startMap();
  *       else
  *         doArt();
  *    }
  *    
  *=====SCHEMATIC FLOW DRIVEN BY OPTIONS:MAP=====
  *    
  *    Options.doMapDialog()
  *    {
  *       ...
  *       if (RM_MAP == DMF.getFrontGJFType())
  *       {    
  *           GJIF g= DMF.getFrontGJIF();
  *           g.doParseUOandPlot();
  *       }
  *    }
  *    
  *    GPanel.doParseUOandPlot()
  *    {
  *        bPleaseParseUO = true; // just briefly!
  *        getNewArtwork(true); 
  *    }
  *    
  *    GPanel.getNewArtwork()
  *    {
  *        g2Tech = null;  // discards old artwork
  *        repaint();    ----->>----Calls OS
  *    }                      which calls GPanel.paintComponent()
  *                           which calls GPanel.drawPage(g2)
  *                           which calls doTechList(g2) here.
  *    
  *  Scale factors:  -1<x<+1; -1<y<+1. 
  *  But the thermometer lies outside this square. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2011 all rights reserved.
  */
class MapPanel extends GPanel // implements Runnable
{
    // public static final long serialVersionUID = 42L;

    private DrawMap drawMap;

    MapPanel(GJIF gj)
    {
        super(new DrawMap());
        myGJIF = gj;
        drawMap = (DrawMap) drawBase;
    }
    
    //-----------protected methods----------------
    @Override
    protected void buildTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel for artwork: new, pan, zoom, & random ray group.
    // THIS RESPONDS TO THE OS CALL TO GPanel::paintComponent()
    // It gets called NxM times to make a complete Map.
    // Do not request repaint() here: this is a provider not a requestor. 
    {
        boolean flag = drawMap.bPleaseParseUO;
        drawMap.doTechList(bFullArt);
        if (flag)
            startMap();
    }

    @Override
    protected void doCursor(int ix, int iy)  // replaces abstract method
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



//----------private methods-----------------

    private void startMap()
    // Called only by doTechList().
    {
        String warn = drawMap.doParseUO();
        drawMap.displayMessage(warn);
        if (warn.length() > 0)
        {
            myGJIF.postWarning(warn); 
            repaint(); // need to kick off warning artwork?
            return;
        }
        drawMap.prepareBunches();
        startBunches(); // starts the timing loop
    }




    //---TIMING LOOP CODE STARTS HERE------------------
    //     Interfaces are.....
    //     doBunchRays() above.
    //     redo()      in GPanel; with bClobber=true, calls doArt() here.
    //     doFinishFile()  is called at end of timer run here. 

    private javax.swing.Timer myTimer; 

    private void startBunches()
    {
        myTimer = new javax.swing.Timer(20, doTick);
        myTimer.start(); 
    }

    ActionListener doTick = new ActionListener()
    {
        public void actionPerformed(ActionEvent ae)
        {
            if (drawMap.bRunning)
            {
                drawMap.doBunch();
                redo();  // GPanel: myBatchList -> g2Tech, and blit.
            }
            else
            {
                myTimer.stop(); 
                drawMap.doFinishFile();
            }
        } 
    };

}  //----------end of MapPanel--------------------
