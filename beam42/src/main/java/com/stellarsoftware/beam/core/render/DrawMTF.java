package com.stellarsoftware.beam.core.render;

import com.stellarsoftware.beam.core.B4constants;
import com.stellarsoftware.beam.core.U;

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
public class DrawMTF extends DrawBase
{
    // public static final long serialVersionUID = 42L;

    final double EXTRAROOM = 2.0;
    final double MINSPAN = 1E-6;
    final int MAXBINS = 1025;
    private double histospan = 1.0;
    private double deltaf = 1.0;
    private double freqspan = 1.0;
    public int nbins = 1;
    private int ncomplexpairs = 1;
    private int nplotfreqs = 1;
    private double cData[] = new double[2*MAXBINS];  // real, imag, real...
    public double[] dPower = new double[MAXBINS];
    private int  CADstyle=0;
    private int hnticks, hndigits, vnticks, vndigits;
    private double hticks[] = new double[12];
    private double vticks[] = new double[12];
    private DrawH1D myH1DPanel = null;


    public DrawMTF(DrawH1D h1d)
    {
        bClobber = true; // protected; random redo() needs new artwork

        // The following will get the most recently constructed g1D
        // but this is not necessarily the g1D that is currently in front.

        myH1DPanel = h1d; // FIXME review this
        nbins = myH1DPanel.getNbins();
        while ((ncomplexpairs < nbins) && (ncomplexpairs < 1024))
            ncomplexpairs *= 2;

        nplotfreqs = ncomplexpairs / 8; // 16;  // 8;  // 4;
        nplotfreqs = 16;  // 32;
        histospan = myH1DPanel.getHistoSpan();
        deltaf = 1.0 / histospan;
        freqspan = deltaf * nplotfreqs;

        for (int i=0; i<MAXBINS; i++)  // ok beyond nbins
        {
            cData[2*i] = (double) myH1DPanel.getHisto(i);
            cData[2*i+1] = 0.0;
        }

        // now set the local scale factors in host GPanel...
        uxspan = EXTRAROOM * freqspan;
        uxcenter = 0.5 * freqspan;
        uyspan = EXTRAROOM * 100.0;  // 100% MTF
        uycenter = 50.0;

        // now do the rulers....
        int results[] = new int[2];
        U.ruler(0, freqspan, true, hticks, results);
        hnticks = results[0];
        hndigits = results[1];

        vnticks = 6;
        vndigits = 0;
        for (int i=0; i<=5; i++)
            vticks[i] = i*20;


        // now do the Fourier transform....
        int nn[] = new int[1];
        nn[0] = ncomplexpairs;
        U.fourn(cData, nn, 1, 1);
        for (int i=0; i<nplotfreqs; i++)
            dPower[i] = cData[2*i]*cData[2*i] + cData[2*i+1]*cData[2*i+1];
        if (dPower[0] > 0.0)
        {
            double coef = 100.0 / dPower[0];
            for (int i=0; i<nplotfreqs; i++)
                dPower[i] *= coef;
        }
    }

//-----------protected methods concretizing GPanel-------

    @Override
    public void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel when fresh artwork is needed:
    // Ignotes bFullArt, always writes complete diagram.
    {
        doArt();
    }

    protected void doRotate(int i, int j) // replaces abstract method
    {
        // do nothing
    }

    public boolean doRandomRay()          // replaces abstract "do" method
    {
        // alert myP1DPanel !  NOT YET IMPLEMENTED.
        return false;
    }


    //--------------ARTWORK-------------------
    //--------------ARTWORK-------------------
    //--------------ARTWORK-------------------

    private void add2D(double x, double y, int op)  // local shorthand
    {
        addScaled(x, y, 0.0, op, B4constants.QBASE);   // GPanel service
    }


    private void doArt()
    {
        int iFontcode = getUOGraphicsFontCode();
        int iHpoints = iFontcode / 10000;
        int iWpoints = 1 + iHpoints / 2;
        double scaledW = iWpoints * uxspan / dUOpixels;
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels;
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;
        double scaledH = iHpoints * uyspan / dUOpixels;
        double hyoffset = -scaledH;                 // for horiz scale
        double vyoffset = -0.4*scaledH;             // for vert scale
        double vrhgap = 0.2;                        // LowerLeftOrigin

        //------draw the furniture--------

        clearList(B4constants.QBASE);
        addRaw(0., 0., 0., B4constants.SETWHITEBKG, B4constants.QBASE);      // unscaled
        addRaw(0., 0., 0., B4constants.SETCOLOR+ B4constants.BLACK, B4constants.QBASE);   // unscaled
        addRaw(1., 0., 0., B4constants.SETSOLIDLINE, B4constants.QBASE);     // unscaled
        addRaw(0., 0., 0., B4constants.COMMENTRULER, B4constants.QBASE);     // unscaled


        //----the X ruler at Y=0----

        double yruler = 0.0;
        add2D(hticks[0], yruler, B4constants.MOVETO);
        add2D(hticks[0], yruler+ytick, B4constants.PATHTO);
        add2D(hticks[0], yruler, B4constants.PATHTO);
        for (int i=1; i<hnticks; i++)
        {
            add2D(hticks[i], yruler, B4constants.PATHTO);
            add2D(hticks[i], yruler+ytick, B4constants.PATHTO);
            int op = (i < hnticks-1) ? B4constants.PATHTO : B4constants.STROKE;
            add2D(hticks[i], yruler, op);
        }

        // labelling loop...

        for (int i=0; i<hnticks; i++)
        {
            // String s = U.fwd(hticks[i], 16, hndigits).trim();
            String s = U.fwe(hticks[i]);
            int nchars = s.length();
            double dmid = 0.5*nchars;
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) s.charAt(k) + iFontcode;
                double x = hticks[i] + scaledW*(k-dmid);
                add2D(x, yruler+hyoffset, ic);
            }
        }

        // title for horizontal axis...
        String hst = "frequency";
        int hnchars = hst.length();
        for (int k=0; k<hnchars; k++)
        {
            int ic = (int) hst.charAt(k) + iFontcode;
            double x = uxcenter + scaledW*(k-hnchars/2);
            add2D(x, yruler-2.5*scaledH, ic);
        }


        //////// v ruler at left /////////////

        addRaw(0., 0., 0., B4constants.COMMENTRULER, B4constants.QBASE);
        double xruler = 0.0;
        add2D(xruler, vticks[0], B4constants.MOVETO);
        add2D(xruler+xtick, vticks[0], B4constants.PATHTO);
        add2D(xruler, vticks[0], B4constants.PATHTO);
        for (int i=1; i<vnticks; i++)
        {
            add2D(xruler, vticks[i], B4constants.PATHTO);
            add2D(xruler+xtick, vticks[i], B4constants.PATHTO);
            int op = (i < vnticks-1) ? B4constants.PATHTO : B4constants.STROKE;
            add2D(xruler, vticks[i], op);
        }

        // labelling loop...

        for (int i=0; i<vnticks; i++)
        {
            String s = U.fwd(vticks[i], 16, vndigits).trim();
            int nchars = s.length();
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) s.charAt(k) + iFontcode;
                double x = xruler + scaledW*(k-nchars-vrhgap);
                add2D(x, vticks[i]+vyoffset, ic); // coord = CharCenter.
            }
        }

        // title for vertical axis...
        String vst = "%MTF";
        int vnchars = vst.length();
        for (int k=0; k<vnchars; k++)
        {
            int ic = (int) vst.charAt(k) + iFontcode;
            double x = xruler + (k-vnchars-1)*scaledW;
            add2D(x, uycenter, ic);   // coord = CharCenter.
        }

        ////// Now plot the histogram....

        double dx = freqspan / nplotfreqs;
        add2D(0, 0, B4constants.MOVETO);
        add2D(0, dPower[0], B4constants.PATHTO);     // up
        add2D(dx, dPower[0], B4constants.PATHTO);    // and over
        for (int i=1; i<nplotfreqs; i++)
        {
            add2D(i*dx, dPower[i], B4constants.PATHTO);
            int op = (i < nplotfreqs-1) ? B4constants.PATHTO : B4constants.STROKE;
            add2D((i+1)*dx, dPower[i], op);
        }
    }  // end of doArt()
}

