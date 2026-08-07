package com.stellarsoftware.beam.core.render;

import com.stellarsoftware.beam.core.B4constants;
import com.stellarsoftware.beam.core.Globals;
import com.stellarsoftware.beam.core.U;
import com.stellarsoftware.beam.core.analysis.LineSpreadMTF;

import java.util.Arrays;

@SuppressWarnings("serial")

/**
 *
 * Custom artwork class furnishes artwork to GPanel.
 *
 * Font details are generated here when needed; uses LowerLeftOrigin.
 *
 * Needs to get properly centered and sized. Rect not square.
 *
 * The MTF is calculated from the line-spread histogram produced by DrawH1D.
 * DrawH1D may be configured with an exact (trimmed) wavelength-name filter
 * matching the text in the RAY table's @wave column. A blank filter includes
 * all wavelengths. Filtering is applied before the automatic bounds and the
 * histogram are calculated, so DrawMTF only receives the selected rays.
 *
 * Random rays use reserved ray slot zero; table rays are numbered from one.
 * Before a random ray is traced, RT13 randomly chooses a table ray for its
 * discrete properties and copies that ray's @wave text to slot zero. After the
 * trace, DrawH1D applies its wavelength filter to slot zero and adds the random
 * ray to the line-spread histogram only when the copied name matches (or the
 * filter is blank). DrawMTF therefore requires no separate wavelength handling:
 * it transforms the already-filtered H1D histogram.
 *
 * The H1D options also provide an MTF maximum plotting frequency. A blank or
 * non-positive value plots the complete result through Nyquist. A positive
 * value below Nyquist truncates the displayed and exported result at that
 * frequency; the endpoint magnitude is linearly interpolated between the two
 * surrounding FFT samples. The complete FFT is still calculated first.
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
    public double[] dPower = new double[0];
    public double[] dFrequency = new double[0];
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
        histospan = myH1DPanel.getHistoSpan();
        int[] lineSpread = new int[nbins];
        for (int i=0; i<nbins; i++)
            lineSpread[i] = myH1DPanel.getHisto(i);
        LineSpreadMTF mtf = new LineSpreadMTF(lineSpread, histospan / nbins);
        double[] frequency = mtf.frequency();
        double[] magnitude = mtf.magnitude();
        // If user set a max frequency then limit the plot to user specified
        // max frequency
        double maxFrequency = U.suckDouble(Globals.reg.getuo(UO_1D, 9));
        if (Double.isFinite(maxFrequency) && maxFrequency > 0.0
                && maxFrequency < frequency[frequency.length - 1]) {
            int upper = 1;
            while (frequency[upper] < maxFrequency)
                upper++;
            double fraction = (maxFrequency - frequency[upper - 1])
                    / (frequency[upper] - frequency[upper - 1]);
            double endpoint = magnitude[upper - 1]
                    + fraction * (magnitude[upper] - magnitude[upper - 1]);
            frequency = Arrays.copyOf(frequency, upper + 1);
            magnitude = Arrays.copyOf(magnitude, upper + 1);
            frequency[upper] = maxFrequency;
            magnitude[upper] = endpoint;
        }
        dFrequency = frequency;
        nplotfreqs = magnitude.length;
        nbins = nplotfreqs;
        dPower = new double[nplotfreqs];
        deltaf = frequency.length > 1 ? frequency[1] : 1.0;
        freqspan = frequency[frequency.length - 1];
        for (int i=0; i<nplotfreqs; i++)
            dPower[i] = 100.0 * magnitude[i];

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

        add2D(dFrequency[0], dPower[0], B4constants.MOVETO);
        for (int i=1; i<nplotfreqs; i++)
            add2D(dFrequency[i], dPower[i],
                    i < nplotfreqs-1 ? B4constants.PATHTO : B4constants.STROKE);
    }  // end of doArt()
}
