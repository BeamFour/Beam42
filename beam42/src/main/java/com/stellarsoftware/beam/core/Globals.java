package com.stellarsoftware.beam.core;

public class Globals {
    public static int[] giFlags = new int[B4constants.NFLAGS];
    public static int nEdits;           // increments for each edit.
    public static Registry reg;         // permanent *visible* home for reg
    public static String sAutoErr = ""; // RT13 reports to AutoAdj, if necessary
    public static boolean bAutoBusy = false;    // forbid parsing when AutoAdj is running
    public static RT13 RT13 = new RT13(); // Interim solution for migrating static data
}
