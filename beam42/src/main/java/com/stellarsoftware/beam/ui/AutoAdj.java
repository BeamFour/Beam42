package com.stellarsoftware.beam.ui;

import com.stellarsoftware.beam.core.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import static com.stellarsoftware.beam.core.B4constants.*;


/** AutoAdj.java
  * A207: eliminated groups
  * class LMadj is at the bottom of this file. 
  * A190, Nov 2015: introducing weights.
  * @author: M.Lampton (c) 2003..2006 STELLAR SOFTWARE all rights reserved.
  */
class AutoAdj implements OPTDataModelListener, RAYDataModelListener {

    private javax.swing.Timer myTimer;
    private int nLabels = 8;
    private JLabel jL[] = new JLabel[nLabels];
    private JButton jbDone;
    private JDialog jd = null;
    private REJIF rayEditor;
    private OEJIF optEditor;
    private AutoAdjuster myHost;

    public AutoAdj() {
        rayEditor = DMF.rejif;
        optEditor = DMF.oejif;
        myHost = new AutoAdjuster(optEditor.model(), rayEditor.model(), this, this); // does everything.

        optEditor.doStashForUndo();
        rayEditor.doStashForUndo();

        DMF.bringEJIFtoFront(rayEditor);
        if (!myHost.start()) {
            JOptionPane.showMessageDialog(optEditor, "AutoAdjust failed");
            return;
        }
        optEditor.repaint();
        optEditor.toFront();

        vPostEmptyDialog();
        vStartTimer();         // performs the adjustment iterations

    }

    ///////////////// GUI interfaces //////////////////

    private void vPostEmptyDialog() {
        JFrame jf = DMF.getJFrame();
        jd = new JDialog(jf, "AutoAdjust", false);
        jd.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                shutdown();
            }
        });
        Container cp = jd.getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

        for (int i = 0; i < nLabels; i++) {
            jL[i] = new JLabel();
            jL[i].setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        jbDone = new JButton("Stop");
        jbDone.setAlignmentX(Component.CENTER_ALIGNMENT);
        jbDone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent aa) {
                shutdown();
            }
        });

        for (int i = 0; i < nLabels; i++)
            cp.add(jL[i]);

        //------empty space beneath the JLabels-----
        cp.add(Box.createRigidArea(new Dimension(195, 10)));
        cp.add(jbDone);
        jd.setVisible(true);
        jd.toFront();
        vUpdateDialog();
    }


    private void vUpdateDialog() {
        jL[0].setText("Iteration = " + myHost.hostiter);
        jL[1].setText("RMS Average =  " + U.fwe(myHost.rms));
        jL[2].setText("");
        if (myHost.autongoals > 1) {
            if (myHost.bWFE)
                jL[2].setText("Caution: WFE + other goals");
            else if (myHost.autongoals == 2)
                jL[2].setText("RSS Radius = " + U.fwe(myHost.ROOT2 * myHost.rms));
        }
        jL[3].setText("Nrays = " + U.fwi(myHost.ngood, 8).trim());
        jL[4].setText("Ngoals = " + U.fwi(myHost.autongoals, 1));
        jL[5].setText("Nterms = " + U.fwi(myHost.npts, 8).trim());
        jL[6].setText("Nadj = " + U.fwi(myHost.nadj, 8).trim());
        if (myHost.istatus == DOWNITER)
            jL[7].setText("Running...");
        else if (myHost.istatus == BADITER) {
            if (Globals.sAutoErr.equals(""))   // no previous message
                jL[7].setText("Stopping: " + B4constants.sResults[RT13.getFailCode()] + " " + RT13.getFailSurf());
            else                           // retain previous message
                jL[7].setText(Globals.sAutoErr);
        } else
            jL[7].setText("");

        //----finally set appropriate size------
        jd.pack();
    }


    private void vStartTimer() {
        DMF.bAutoBusy = true;
        myTimer = new javax.swing.Timer(500, doTimerTask);
        //hostiter = 0;
        myTimer.start();
        //bComplete = false;
    }

    private void vUpdateLayout() {
        GJIF g = DMF.getLayoutGJIF();
        if (g == null)
            return;
        g.getGPanel().requestNewArtwork();
        g.toFront();
        g.repaint();
    }

    @Override
    public void optModelUpdated() {
        optEditor.repaint();
    }

    @Override
    public void rayModelUpdated() {
        rayEditor.repaint();
    }

    ActionListener doTimerTask = new ActionListener()
            // Responds to timer tick with one LM iteration & update.
    {
        public void actionPerformed(ActionEvent ae) {
            if (!myHost.bComplete) {
                myHost.iterate();
                vUpdateDialog();
                myHost.vUpdateRayTable();
                rayEditor.repaint();
                rayEditor.toFront();
                myHost.vUpdateOpticsTable();
                optEditor.repaint();
                optEditor.toFront();
                vUpdateLayout();
            }
            if (myHost.bComplete) {
                myTimer.stop();
                jbDone.setText("Done");
                DMF.bAutoBusy = false;  // allow future parsings.
            }
        }
    };

    private void shutdown() {
        if (myTimer != null) {
            myTimer.stop();
            myTimer = null;
        }
        if (jd != null) {
            jd.dispose();      // does it all
            // jd.setVisible(false);
            // jd = null;
        }
        //---with dialog gone, can resync front & selected----
        DMF.bringEJIFtoFront(optEditor);
        DMF.bAutoBusy = false;  // allow future parsings.
    }
}

