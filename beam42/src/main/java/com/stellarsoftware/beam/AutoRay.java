package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/** AutoRay.java
  *
  *  A174: improved results dialog. 
  *
  * Adjusts one or two ray starts to deliver a distant pupil
  * Does ray tracing by calls to RT13.bRunOneRay().
  * Avoids use of Comparo.
  *
  * This version uses no timer, runs full speed, tight loop.
  * No i/o during the loop, only at the end. 
  *
  * Work is organized by RayHost.  After setup, it steps through each ray:
  *   tests ray initially; 
  *   initializes resid[] and sos; 
  *   starts new LMray instance; 
  *   OUTER LOOP iteratively calls LMray.iLMiter() as long as downward sos trend,
  *      but will not exceed MAXITER. 
  * When all rays are done, calls for InOut to display the new .RAY table. 
  *
  * LMray performs its initialization, then for each iLMiter() call:
  *   gets current resid[] and sos; 
  *   gets current Jacobian;
  *   evaluates current undamped curvature matrix.  
  *   if BIGVAL ray failure happens, requests an exit. 
  *   Local INNER LOOP then...
  *      applies some damping, tries a step;
  *      if downhill, retains step, reduces damping, exits to OUTER LOOP;
  *      if uphill, reverses step, raises damping, and tries again; 
  *      if level, exits to host OUTER LOOP; 
  *      if lambda>LAMBDAMAX, exits to host OUTER LOOP.
  * 
  *
  *  RayHost supplies bBuildJacobian() which calls dNudge(), which calls dPerformResid().
  *
  *
  * 
  * @author: M.Lampton (c) 2012 STELLAR SOFTWARE all rights reserved.
  */
class AutoRay
{
    private JDialog jd      = null;       // to post results when done.

    public AutoRay() {
        REJIF rayEditor = DMF.rejif;
        OEJIF optEditor = DMF.oejif;
        if (rayEditor == null || optEditor == null)
            return;
        rayEditor.doStashForUndo();
        AutoRayGenerator myhost = new AutoRayGenerator(optEditor.model(), rayEditor.model());       // does everything.
        if (!myhost.generate()) {
            JOptionPane.showMessageDialog(optEditor, "AutoRay: failed");
            return;
        }
        myhost.vUpdateRayStarts(); // post ray start adjustments to .RAY table
        rayEditor.repaint();
        InOut myIO = new InOut();  // post ray trace results to .RAY table
        Comparo comparo = myhost.getResults(); // show local AutoRay result summary.
        vPostSummary(myhost, comparo);
    }

    private void vPostSummary(AutoRayGenerator myhost, Comparo comparo)
    {
        //----prepare the results-------------

        int nptest = comparo.iGetNPTS();
        double sos = comparo.dGetSOS();
        double rms = comparo.dGetRMS();

        //-----prepare the dialog-------------

        JFrame jf = DMF.getJFrame();
        if (jd == null)
            jd = new JDialog(jf, "AutoRay", false);
        jd.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                jd.dispose();
            }
        });

        Container cp = jd.getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        int nLabels = 7;
        JLabel jL[] = new JLabel[nLabels];
        for (int i=0; i<nLabels; i++)
        {
            jL[i] = new JLabel();
            jL[i].setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        jL[0].setText("RMS 1D average = "+U.fwe(rms));
        jL[1].setText("Nrays = "+myhost.nrays);
        jL[2].setText("Ngoals = "+myhost.ngoals);
        jL[4].setText("Nstarts = "+myhost.navail);
        jL[5].setText("Nadjusted = "+myhost.nfinishes);
        jL[6].setText("Nfailed = "+myhost.nwentbad);

        for (int i=0; i<nLabels; i++)
            cp.add(jL[i]);
        cp.add(Box.createRigidArea(new Dimension(205, 5))); // pleasant width

        JButton jbDone = new JButton("Done");
        jbDone.setAlignmentX(Component.CENTER_ALIGNMENT);
        jbDone.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent aa)
            {
                jd.dispose();
            }
        });
        cp.add(jbDone);
        jd.pack();
        jd.setVisible(true);
        jd.toFront();
    }
}
