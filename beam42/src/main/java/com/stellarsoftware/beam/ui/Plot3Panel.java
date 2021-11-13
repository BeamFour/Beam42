    package com.stellarsoftware.beam.ui;

    import com.stellarsoftware.beam.core.Globals;
    import com.stellarsoftware.beam.core.U;
    import com.stellarsoftware.beam.core.render.DrawPlot3;

    @SuppressWarnings("serial")

    /**
      * Plot 3D graphic artwork class extends GPanel.
      *
      * Ray values are scaled to a unit cube for display.
      * Unit Cube: uxcenter=0, uxspan=1, etc.
      *
      * Uses CenterOrigin for character locations.
      * A207: needs -zero detection for absent data, see line 223
      *
      * DeImplemented: additional surface "jOther"
      * Not yet implemented: optical path.
      *
      * Auto scaling is implemented.
      * Manual scaling is implemented.
      * Diameter scaling is not yet implemented.
      *
      * @author M.Lampton (c) STELLAR SOFTWARE 2006 all rights reserved.
      */
    class Plot3Panel extends GPanel
    {
        // public static final long serialVersionUID = 42L;

        private DrawPlot3 drawPlot3;

        Plot3Panel(GJIF gj)
        {
            super(new DrawPlot3());
            myGJIF = gj;           // protected; used here & GPanel
            drawPlot3 = (DrawPlot3) drawBase;
        }

        @Override
        void buildTechList(boolean bArtStatus) {
            drawPlot3.doTechList(bArtStatus);
        }


        protected void doCursor(int ix, int iy)
        // delivers current cursor coordinates
        {
            return;
        }

        protected double getStereo()  // replaces abstract "get" method
        {
            double d = 0.0;
            boolean bS = "T".equals(Globals.reg.getuo(UO_PLOT3, 17));
            if (bS)
            {
                String ss = Globals.reg.getuo(UO_PLOT3, 18);
                d = U.suckDouble(ss);
                if (d == Double.NaN)
                  d = 0.0;
            }
            return d;
        }

        protected void doSaveData()   // replaces abstract "do" method
        {
            return;
        }


    }
